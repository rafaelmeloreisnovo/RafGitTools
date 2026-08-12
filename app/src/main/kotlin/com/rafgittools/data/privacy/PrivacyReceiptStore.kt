package com.rafgittools.data.privacy

import android.content.Context
import android.util.AtomicFile
import com.google.gson.GsonBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores sanitized, append-only checkpoint receipts in Android app-private storage.
 *
 * Every checkpoint receives a unique sequence-numbered file. A checkpoint is first
 * written through [AtomicFile] and fsynced before publication, so a process/device
 * interruption cannot silently publish a truncated JSON receipt.
 */
@Singleton
class PrivacyReceiptStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun save(receipt: RepositoryPrivacyReceipt): Result<String> = runCatching {
        val directory = File(context.filesDir, "privacy_receipts")
        check(directory.exists() || directory.mkdirs()) {
            "Unable to create app-private privacy receipt directory"
        }

        val sequence = receipt.checkpointSequence.toString().padStart(4, '0')
        val safeOperationId = receipt.operationId.replace(UNSAFE_FILENAME, "_")
        val target = File(directory, "$safeOperationId-$sequence.json")
        check(!target.exists()) {
            "Privacy receipt checkpoint already exists; append-only contract refused overwrite"
        }

        val atomic = AtomicFile(target)
        var stream: FileOutputStream? = null
        try {
            stream = atomic.startWrite()
            stream.write(gson.toJson(receipt).toByteArray(StandardCharsets.UTF_8))
            stream.flush()
            stream.fd.sync()
            atomic.finishWrite(stream)
            stream = null
        } catch (error: Throwable) {
            stream?.let { atomic.failWrite(it) }
            throw error
        }

        target.absolutePath
    }

    companion object {
        private val UNSAFE_FILENAME = Regex("[^A-Za-z0-9._-]")
    }
}
