package com.rafgittools.data.privacy

import android.content.Context
import com.google.gson.GsonBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Stores sanitized receipts only in Android app-private storage. */
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
        val file = File(directory, "visibility-${receipt.createdAtEpochMs}.json")
        file.writeText(gson.toJson(receipt))
        file.absolutePath
    }
}
