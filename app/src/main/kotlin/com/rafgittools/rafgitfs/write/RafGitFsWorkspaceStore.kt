package com.rafgittools.rafgitfs.write

import android.content.Context
import com.rafgittools.rafgitfs.data.StagedOperationDao
import com.rafgittools.rafgitfs.data.StagedOperationEntity
import com.rafgittools.rafgitfs.data.WorkspaceDao
import com.rafgittools.rafgitfs.data.WorkspaceEntity
import com.rafgittools.rafgitfs.sync.RafGitFsCanonical
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RafGitFsWorkspaceStore @Inject constructor(
    @ApplicationContext context: Context,
    private val workspaceDao: WorkspaceDao,
    private val stagedDao: StagedOperationDao
) {
    private val root = File(context.filesDir, "rafgitfs-workspaces-v1").apply { mkdirs() }.canonicalFile
    private val rootPrefix = root.canonicalPath + File.separator

    suspend fun create(
        profileId: String,
        repositoryFullName: String,
        baseRef: String
    ): WorkspaceEntity {
        require(profileId.isNotBlank())
        require(repositoryFullName.contains('/'))
        require(baseRef.isNotBlank())
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val directory = safeWorkspaceRoot(id).apply { mkdirs() }
        val workspace = WorkspaceEntity(
            workspaceId = id,
            profileId = profileId,
            repositoryFullName = repositoryFullName,
            baseRef = baseRef,
            branchName = null,
            localRoot = directory.absolutePath,
            state = "OPEN",
            createdAt = now,
            updatedAt = now,
            claimAllowed = false
        )
        workspaceDao.upsert(workspace)
        return workspace
    }

    suspend fun stageText(
        workspaceId: String,
        path: String,
        content: String,
        baseSha: String?
    ): StagedOperationEntity = stageBytes(
        workspaceId, path, content.toByteArray(Charsets.UTF_8), baseSha, "100644"
    )

    suspend fun stageBytes(
        workspaceId: String,
        path: String,
        content: ByteArray,
        baseSha: String?,
        mode: String = "100644"
    ): StagedOperationEntity {
        val workspace = workspaceDao.getById(workspaceId)
            ?: throw IllegalArgumentException("WORKSPACE_NOT_FOUND")
        check(workspace.state in setOf("OPEN", "STAGED", "CONFLICT")) { "WORKSPACE_NOT_EDITABLE" }
        require(content.size <= MAX_WORKSPACE_FILE_BYTES) { "WORKSPACE_FILE_TOO_LARGE" }
        require(mode in setOf("100644", "100755")) { "UNSUPPORTED_FILE_MODE" }
        val normalized = normalizePath(path)
        val target = safeFile(workspaceId, normalized)
        writeAtomic(target, content)
        val now = System.currentTimeMillis()
        val payloadHash = RafGitFsCanonical.sha256(content.toString(Charsets.ISO_8859_1))
        val operation = StagedOperationEntity(
            operationId = "$workspaceId:${RafGitFsCanonical.sha256(normalized).take(20)}",
            jobId = null,
            workspaceId = workspaceId,
            operationType = "UPSERT_FILE:$mode",
            repositoryFullName = workspace.repositoryFullName,
            refName = workspace.baseRef,
            path = normalized,
            baseSha = baseSha,
            localSha = payloadHash,
            payloadHash = payloadHash,
            state = "STAGED",
            createdAt = now
        )
        stagedDao.upsert(operation)
        workspaceDao.upsert(workspace.copy(state = "STAGED", updatedAt = now, claimAllowed = false))
        return operation
    }

    suspend fun rollbackFile(workspaceId: String, operationId: String): Boolean {
        val operation = stagedDao.listForWorkspace(workspaceId).firstOrNull { it.operationId == operationId }
            ?: return false
        val path = operation.path ?: return false
        val deleted = safeFile(workspaceId, path).let { !it.exists() || it.delete() }
        if (!deleted) return false
        stagedDao.delete(operationId)
        val workspace = workspaceDao.getById(workspaceId) ?: return true
        val remaining = stagedDao.listForWorkspace(workspaceId)
        workspaceDao.upsert(
            workspace.copy(
                state = if (remaining.isEmpty()) "OPEN" else "STAGED",
                updatedAt = System.currentTimeMillis(),
                claimAllowed = false
            )
        )
        return true
    }

    suspend fun stagedFiles(workspaceId: String): List<RafGitFsWorkspaceFile> =
        stagedDao.listForWorkspace(workspaceId)
            .filter { it.state == "STAGED" && it.operationType.startsWith("UPSERT_FILE:") }
            .map { operation ->
                val path = operation.path ?: throw IllegalStateException("STAGED_PATH_MISSING")
                val file = safeFile(workspaceId, path)
                if (!file.isFile || file.length() > MAX_WORKSPACE_FILE_BYTES) {
                    throw IOException("STAGED_FILE_MISSING_OR_OVERSIZED:$path")
                }
                RafGitFsWorkspaceFile(
                    path = path,
                    bytes = file.readBytes(),
                    mode = operation.operationType.substringAfter(':', "100644"),
                    baseSha = operation.baseSha,
                    payloadHash = operation.payloadHash ?: throw IllegalStateException("PAYLOAD_HASH_MISSING")
                )
            }

    suspend fun setPublished(workspaceId: String, branchName: String) {
        val workspace = workspaceDao.getById(workspaceId) ?: return
        workspaceDao.upsert(
            workspace.copy(
                branchName = branchName,
                state = "PULL_REQUEST_OPEN",
                updatedAt = System.currentTimeMillis(),
                claimAllowed = false
            )
        )
    }

    suspend fun setConflict(workspaceId: String) {
        val workspace = workspaceDao.getById(workspaceId) ?: return
        workspaceDao.upsert(workspace.copy(state = "CONFLICT", updatedAt = System.currentTimeMillis(), claimAllowed = false))
    }

    private fun safeWorkspaceRoot(workspaceId: String): File {
        require(workspaceId.matches(Regex("[A-Za-z0-9-]{8,64}"))) { "INVALID_WORKSPACE_ID" }
        return ensureInsideRoot(File(root, workspaceId).canonicalFile)
    }

    private fun safeFile(workspaceId: String, relativePath: String): File =
        ensureInsideRoot(File(safeWorkspaceRoot(workspaceId), normalizePath(relativePath)).canonicalFile)

    private fun ensureInsideRoot(file: File): File {
        require(file.canonicalPath.startsWith(rootPrefix)) { "WORKSPACE_PATH_ESCAPE" }
        return file
    }

    private fun normalizePath(path: String): String {
        val normalized = path.replace('\\', '/').trim('/').replace(Regex("/+"), "/")
        require(normalized.isNotBlank()) { "EMPTY_PATH" }
        require(normalized.length <= 512) { "PATH_TOO_LONG" }
        require(normalized.split('/').none { it.isBlank() || it == "." || it == ".." }) { "UNSAFE_PATH" }
        require(!normalized.startsWith(".git/", ignoreCase = true) && normalized != ".git") { "GIT_INTERNAL_PATH_BLOCKED" }
        return normalized
    }

    private fun writeAtomic(target: File, bytes: ByteArray) {
        target.parentFile?.mkdirs()
        val part = File(target.parentFile, target.name + ".part")
        FileOutputStream(part).use { output ->
            output.write(bytes)
            output.flush()
            output.fd.sync()
        }
        if (target.exists() && !target.delete()) {
            part.delete()
            throw IOException("WORKSPACE_REPLACE_FAILED")
        }
        if (!part.renameTo(target)) {
            part.delete()
            throw IOException("WORKSPACE_ATOMIC_RENAME_FAILED")
        }
    }

    companion object {
        const val MAX_WORKSPACE_FILE_BYTES = 10 * 1024 * 1024
    }
}

data class RafGitFsWorkspaceFile(
    val path: String,
    val bytes: ByteArray,
    val mode: String,
    val baseSha: String?,
    val payloadHash: String
)
