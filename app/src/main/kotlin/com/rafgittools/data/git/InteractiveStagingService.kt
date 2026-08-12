package com.rafgittools.data.git

import com.rafgittools.domain.model.DiffChangeType
import com.rafgittools.domain.model.DiffHunk
import com.rafgittools.domain.model.DiffLineType
import com.rafgittools.domain.model.GitDiff
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.dircache.DirCache
import org.eclipse.jgit.dircache.DirCacheEditor
import org.eclipse.jgit.dircache.DirCacheEntry
import org.eclipse.jgit.lib.Constants
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interactive staging for one textual diff hunk at a time.
 *
 * Safety boundaries:
 * - works only on tracked MODIFY entries represented by [GitDiff];
 * - never rewrites the working tree;
 * - revalidates the hunk against the current JGit diff before touching index;
 * - verifies the index object id remained stable across revalidation and lock;
 * - for staging, verifies working-tree bytes stayed bit-identical during the operation;
 * - rejects unmerged entries, binary/non-UTF-8 data and missing-final-newline text,
 *   because the current DiffHunk model cannot represent those cases losslessly;
 * - uses DirCacheEditor.commit(), which atomically publishes the index lock file;
 * - always calls DirCache.unlock() in finally; after a successful commit this is a no-op,
 *   and after a validation/runtime exception it prevents a zombie index.lock;
 * - invalidates cached index stat metadata after a partial edit so status must
 *   re-check the working tree instead of trusting stale size/mtime values.
 */
@Singleton
class InteractiveStagingService @Inject constructor(
    private val jGitService: JGitService
) {

    suspend fun stageHunk(
        repoPath: String,
        diff: GitDiff,
        hunk: DiffHunk
    ): Result<Unit> = mutateHunk(
        repoPath = repoPath,
        requestedDiff = diff,
        requestedHunk = hunk,
        direction = Direction.STAGE
    )

    suspend fun unstageHunk(
        repoPath: String,
        diff: GitDiff,
        hunk: DiffHunk
    ): Result<Unit> = mutateHunk(
        repoPath = repoPath,
        requestedDiff = diff,
        requestedHunk = hunk,
        direction = Direction.UNSTAGE
    )

    private suspend fun mutateHunk(
        repoPath: String,
        requestedDiff: GitDiff,
        requestedHunk: DiffHunk,
        direction: Direction
    ): Result<Unit> = withContext(Dispatchers.IO) {
        resultPreservingCancellation {
            require(requestedDiff.changeType == DiffChangeType.MODIFY) {
                "Interactive hunk staging currently supports tracked MODIFY entries only"
            }

            val filePath = canonicalDiffPath(requestedDiff)
            val repoRoot = File(repoPath).canonicalFile
            val workFile = File(repoRoot, filePath).canonicalFile
            require(workFile.path.startsWith(repoRoot.path + File.separator)) {
                "Diff path escapes repository root"
            }
            require(workFile.isFile) { "Working-tree file is missing: $filePath" }
            require(workFile.length() <= MAX_TEXT_BYTES) {
                "Interactive staging file exceeds $MAX_TEXT_BYTES bytes"
            }

            val workBytesBefore: ByteArray? = if (direction == Direction.STAGE) {
                workFile.readBytes().also(::validateTargetText)
            } else {
                null
            }

            jGitService.openRepository(repoPath).getOrThrow().use { git ->
                val repository = git.repository
                val indexBefore = snapshotIndexEntry(repository.readDirCache(), filePath)
                val headBefore = repository.resolve(Constants.HEAD)?.name

                val freshDiff = currentDiffFor(
                    repoPath = repoPath,
                    filePath = filePath,
                    cached = direction == Direction.UNSTAGE
                )
                val freshHunk = freshDiff.hunks.firstOrNull { it == requestedHunk }
                    ?: throw IllegalStateException(
                        "Selected hunk is stale; refresh diff before changing the index"
                    )

                val indexAfterDiff = snapshotIndexEntry(repository.readDirCache(), filePath)
                require(indexAfterDiff == indexBefore) {
                    "Index changed while validating selected hunk"
                }
                require(repository.resolve(Constants.HEAD)?.name == headBefore) {
                    "HEAD changed while validating selected hunk"
                }

                if (direction == Direction.STAGE) {
                    val expectedWorkBytes = requireNotNull(workBytesBefore)
                    val workBytesAfterDiff = workFile.readBytes()
                    require(workBytesAfterDiff.contentEquals(expectedWorkBytes)) {
                        "Working tree changed while validating selected hunk"
                    }
                } else {
                    validateTargetText(
                        freshDiff.oldContent?.toByteArray(StandardCharsets.UTF_8)
                            ?: throw IllegalStateException(
                                "Cannot losslessly reconstruct HEAD side for selected hunk"
                            )
                    )
                }

                val cache = repository.lockDirCache()
                try {
                    val lockedSnapshot = snapshotIndexEntry(cache, filePath)
                    require(lockedSnapshot == indexBefore) {
                        "Index changed before lock acquisition"
                    }
                    require(repository.resolve(Constants.HEAD)?.name == headBefore) {
                        "HEAD changed before index mutation"
                    }

                    val entryIndex = cache.findEntry(filePath)
                    require(entryIndex >= 0) { "Tracked index entry disappeared: $filePath" }
                    require(cache.nextEntry(entryIndex) == entryIndex + 1) {
                        "Unmerged/multi-stage index entry cannot be interactively staged"
                    }
                    val entry = cache.getEntry(entryIndex)
                    require(entry.stage == 0) { "Unmerged index entry cannot be interactively staged" }

                    val indexBytes = repository.open(entry.objectId, Constants.OBJ_BLOB).bytes
                    validateSourceText(indexBytes)
                    val newIndexBytes = applyHunk(indexBytes, freshHunk, direction)
                    require(!indexBytes.contentEquals(newIndexBytes)) {
                        "Selected hunk produces no index change"
                    }

                    if (direction == Direction.STAGE) {
                        val expectedWorkBytes = requireNotNull(workBytesBefore)
                        val workBytesBeforeCommit = workFile.readBytes()
                        require(workBytesBeforeCommit.contentEquals(expectedWorkBytes)) {
                            "Working tree changed before index commit"
                        }
                    }

                    repository.newObjectInserter().use { inserter ->
                        val blobId = inserter.insert(Constants.OBJ_BLOB, newIndexBytes)
                        inserter.flush()

                        val editor = cache.editor()
                        editor.add(object : DirCacheEditor.PathEdit(filePath) {
                            override fun apply(ent: DirCacheEntry) {
                                require(ent.stage == 0) {
                                    "Index entry became unmerged during hunk staging"
                                }
                                ent.setObjectId(blobId)
                                // DirCache length/mtime describe the working-tree stat cache,
                                // not the blob. Smudge them so JGit must re-check content.
                                ent.setLength(0)
                                ent.setLastModified(Instant.EPOCH)
                                ent.setUpdateNeeded(true)
                            }
                        })

                        check(editor.commit()) { "Failed to atomically commit index update" }
                    }
                } finally {
                    // DirCache.unlock() is idempotent: after a successful commit myLock is
                    // already null; after an exception it aborts/removes any surviving lock.
                    cache.unlock()
                }
            }
        }
    }

    private suspend fun currentDiffFor(
        repoPath: String,
        filePath: String,
        cached: Boolean
    ): GitDiff {
        return jGitService.getDiff(repoPath, cached).getOrThrow()
            .firstOrNull { diff ->
                diff.changeType == DiffChangeType.MODIFY && canonicalDiffPath(diff) == filePath
            }
            ?: throw IllegalStateException(
                if (cached) "No staged MODIFY diff remains for $filePath"
                else "No unstaged MODIFY diff remains for $filePath"
            )
    }

    private fun canonicalDiffPath(diff: GitDiff): String {
        val oldPath = diff.oldPath
        val newPath = diff.newPath
        require(!oldPath.isNullOrBlank() && oldPath == newPath) {
            "Interactive hunk staging requires one stable tracked file path"
        }
        require(!oldPath.startsWith("/") && !oldPath.split('/').contains("..")) {
            "Unsafe repository-relative diff path"
        }
        return oldPath
    }

    private fun snapshotIndexEntry(cache: DirCache, filePath: String): IndexSnapshot {
        val entryIndex = cache.findEntry(filePath)
        require(entryIndex >= 0) { "Tracked index entry not found: $filePath" }
        require(cache.nextEntry(entryIndex) == entryIndex + 1) {
            "Unmerged/multi-stage index entry cannot be interactively staged"
        }
        val entry = cache.getEntry(entryIndex)
        require(entry.stage == 0) { "Unmerged index entry cannot be interactively staged" }
        return IndexSnapshot(
            objectId = entry.objectId.name,
            rawMode = entry.rawMode,
            stage = entry.stage
        )
    }

    private fun applyHunk(
        indexBytes: ByteArray,
        hunk: DiffHunk,
        direction: Direction
    ): ByteArray {
        val indexText = decodeUtf8Strict(indexBytes)
        val sourceLines = splitRepresentableLines(indexText)

        val start = when (direction) {
            Direction.STAGE -> hunk.oldStart
            Direction.UNSTAGE -> hunk.newStart
        }
        val expectedConsumed = when (direction) {
            Direction.STAGE -> hunk.oldLines
            Direction.UNSTAGE -> hunk.newLines
        }
        val expectedProduced = when (direction) {
            Direction.STAGE -> hunk.newLines
            Direction.UNSTAGE -> hunk.oldLines
        }

        val cursorStart = if (start == 0) 0 else start - 1
        require(cursorStart in 0..sourceLines.size) { "Hunk start is outside current index content" }

        val estimatedSize = maxOf(0, sourceLines.size + expectedProduced - expectedConsumed)
        val result = ArrayList<String>(estimatedSize)
        result.addAll(sourceLines.subList(0, cursorStart))

        var cursor = cursorStart
        var consumed = 0
        var produced = 0

        for (line in hunk.lines) {
            val operation = when (direction) {
                Direction.STAGE -> line.type
                Direction.UNSTAGE -> when (line.type) {
                    DiffLineType.ADD -> DiffLineType.DELETE
                    DiffLineType.DELETE -> DiffLineType.ADD
                    DiffLineType.CONTEXT -> DiffLineType.CONTEXT
                }
            }

            when (operation) {
                DiffLineType.CONTEXT -> {
                    requireLineMatches(sourceLines, cursor, line.content, "context")
                    result.add(sourceLines[cursor])
                    cursor++
                    consumed++
                    produced++
                }
                DiffLineType.DELETE -> {
                    requireLineMatches(sourceLines, cursor, line.content, "delete")
                    cursor++
                    consumed++
                }
                DiffLineType.ADD -> {
                    result.add(line.content)
                    produced++
                }
            }
        }

        require(consumed == expectedConsumed) {
            "Hunk old/new line count no longer matches current index"
        }
        require(produced == expectedProduced) {
            "Hunk output line count is inconsistent"
        }

        result.addAll(sourceLines.subList(cursor, sourceLines.size))
        return if (result.isEmpty()) {
            ByteArray(0)
        } else {
            (result.joinToString("\n") + "\n").toByteArray(StandardCharsets.UTF_8)
        }
    }

    private fun requireLineMatches(
        source: List<String>,
        cursor: Int,
        expected: String,
        kind: String
    ) {
        require(cursor < source.size && source[cursor] == expected) {
            "Hunk $kind line no longer matches current index at line ${cursor + 1}"
        }
    }

    private fun validateSourceText(bytes: ByteArray) {
        require(bytes.size.toLong() <= MAX_TEXT_BYTES) { "Index blob exceeds interactive staging limit" }
        if (bytes.isEmpty()) return
        require(bytes.none { it == 0.toByte() }) { "Binary file cannot be interactively staged" }
        val text = decodeUtf8Strict(bytes)
        require(text.endsWith('\n')) {
            "Missing-final-newline files require a richer diff model before hunk staging"
        }
    }

    private fun validateTargetText(bytes: ByteArray) {
        require(bytes.size.toLong() <= MAX_TEXT_BYTES) { "Target text exceeds interactive staging limit" }
        if (bytes.isEmpty()) return
        require(bytes.none { it == 0.toByte() }) { "Binary file cannot be interactively staged" }
        val text = decodeUtf8Strict(bytes)
        require(text.endsWith('\n')) {
            "Missing-final-newline files require a richer diff model before hunk staging"
        }
    }

    private fun decodeUtf8Strict(bytes: ByteArray): String {
        val decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return decoder.decode(ByteBuffer.wrap(bytes)).toString()
    }

    private fun splitRepresentableLines(text: String): List<String> {
        if (text.isEmpty()) return emptyList()
        require(text.endsWith('\n')) {
            "Missing-final-newline files require a richer diff model before hunk staging"
        }
        return text.dropLast(1).split('\n')
    }

    private suspend inline fun <T> resultPreservingCancellation(
        crossinline block: suspend () -> T
    ): Result<T> {
        return try {
            Result.success(block())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            Result.failure(error)
        }
    }

    private data class IndexSnapshot(
        val objectId: String,
        val rawMode: Int,
        val stage: Int
    )

    private enum class Direction {
        STAGE,
        UNSTAGE
    }

    companion object {
        private const val MAX_TEXT_BYTES = 2L * 1024L * 1024L
    }
}
