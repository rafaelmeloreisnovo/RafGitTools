package com.rafgittools.rafgitfs.cache

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RafGitFsAtomicFileStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val root: File = File(context.filesDir, "rafgitfs-cache-v1").apply { mkdirs() }
    private val rootCanonical: String = root.canonicalPath + File.separator

    fun resolve(cacheKey: String): File = safeResolve(RafGitFsCacheKeys.relativePath(cacheKey))

    fun partial(cacheKey: String): File = safeResolve(RafGitFsCacheKeys.relativePath(cacheKey) + ".part")

    fun writeAtomic(cacheKey: String, bytes: ByteArray): File {
        val finalFile = resolve(cacheKey)
        val partFile = partial(cacheKey)
        finalFile.parentFile?.mkdirs()
        if (partFile.exists() && !partFile.delete()) {
            throw IOException("PARTIAL_DELETE_FAILED")
        }
        FileOutputStream(partFile).use { output ->
            output.write(bytes)
            output.flush()
            output.fd.sync()
        }
        if (finalFile.exists() && !finalFile.delete()) {
            partFile.delete()
            throw IOException("FINAL_REPLACE_FAILED")
        }
        if (!partFile.renameTo(finalFile)) {
            partFile.delete()
            throw IOException("ATOMIC_RENAME_FAILED")
        }
        return finalFile
    }

    fun read(cacheKey: String, maxBytes: Long): ByteArray? {
        val file = resolve(cacheKey)
        if (!file.isFile || file.length() !in 0L..maxBytes) return null
        return file.inputStream().use { input ->
            val expected = file.length().toInt()
            val bytes = ByteArray(expected)
            var offset = 0
            while (offset < expected) {
                val read = input.read(bytes, offset, expected - offset)
                if (read < 0) return null
                offset += read
            }
            bytes
        }
    }

    fun delete(cacheKey: String): Boolean {
        val finalDeleted = resolve(cacheKey).let { !it.exists() || it.delete() }
        val partialDeleted = partial(cacheKey).let { !it.exists() || it.delete() }
        return finalDeleted && partialDeleted
    }

    fun discardPartial(cacheKey: String): Boolean = partial(cacheKey).let { !it.exists() || it.delete() }

    private fun safeResolve(relative: String): File {
        val candidate = File(root, relative).canonicalFile
        val candidatePath = candidate.canonicalPath
        require(candidatePath.startsWith(rootCanonical)) { "CACHE_PATH_ESCAPE" }
        return candidate
    }
}
