package com.rafgittools.benchmark

import android.content.Context
import android.os.Build
import android.os.Process
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.UUID

/**
 * App-private append-only-by-convention receipt records.
 * SHA-256 chain detects changes against an independently preserved anchor;
 * it is NOT a signature, TPM attestation, WORM store or third-party proof.
 */
class RafBenchmarkReceiptStore(private val context: Context) {
    private val directory: File get() =
        File(context.filesDir, "rafbenchmark/receipts").also { check(it.isDirectory || it.mkdirs()) }

    data class Receipt(val id: String, val hash: String, val previousHash: String)
    private fun sha(bytes: String) =
        MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 255) }

    @Synchronized
    fun verify(): Int {
        var previous = "GENESIS"
        var count = 0
        for (file in directory.listFiles().orEmpty().filter { it.isFile && it.name.endsWith(".json") }
            .sortedBy { it.name }) {
            val row = JSONObject(file.readText(Charsets.UTF_8))
            require(row.getString("previous_hash") == previous) { "CHAIN_BROKEN_PREDECESSOR" }
            val calculated = sha(previous + "\n" + row.getString("payload"))
            require(row.getString("hash") == calculated) { "CHAIN_BROKEN_HASH" }
            previous = calculated
            count++
        }
        return count
    }

    @Synchronized
    fun append(
        result: RafBenchmarkCore.Result,
        proposalSha256: String = "TOKEN_VAZIO_MANUAL_JOB"
    ): Receipt {
        require(proposalSha256 == "TOKEN_VAZIO_MANUAL_JOB" ||
            Regex("[0-9a-f]{64}").matches(proposalSha256)) { "BLOCKED_PROPOSAL_ID" }
        // Fail closed: do not append to a known corrupt local chain.
        verify()
        val previous = directory.listFiles().orEmpty()
            .filter { it.isFile && it.name.endsWith(".json") }
            .maxByOrNull { it.name }?.let {
                JSONObject(it.readText(Charsets.UTF_8)).getString("hash")
            } ?: "GENESIS"
        val id = System.currentTimeMillis().toString().padStart(13, '0') +
            "-" + UUID.randomUUID().toString()
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "TOKEN_VAZIO_ABI"
        val payload = listOf(
            "rafbenchmark.receipt.v1", id, abi, Process.is64Bit().toString(),
            result.workers.toString(), result.bufferKiB.toString(),
            result.rounds.toString(), result.elapsedNs.toString(),
            result.processedBytes.toString(), result.checksum.toString(),
            result.outputSha256, proposalSha256, result.state, result.claimAllowed.toString()
        ).joinToString("|")
        val hash = sha(previous + "\n" + payload)
        val json = JSONObject().apply {
            put("schema", "rafbenchmark.receipt.v1")
            put("id", id)
            put("payload", payload)
            put("previous_hash", previous)
            put("hash", hash)
            put("attestation", "TOKEN_VAZIO_INDEPENDENT_ANCHOR")
            put("sync", "LOCAL_ONLY")
        }.toString(2)
        val tmp = File(directory, "$id.tmp")
        val target = File(directory, "$id.json")
        try {
            tmp.writeText(json, Charsets.UTF_8)
            check(!target.exists() && tmp.renameTo(target)) { "RECEIPT_WRITE_FAILED" }
        } finally {
            if (tmp.exists()) tmp.delete()
        }
        return Receipt(id, hash, previous)
    }

    /** Read-only export via user-chosen SAF destination (never automatic). */
    @Synchronized
    fun exportJson(receiptId: String): String {
        require(Regex("[0-9]{13}-[0-9a-f-]{36}").matches(receiptId)) {
            "BLOCKED_RECEIPT_ID"
        }
        verify()
        val file = File(directory, "$receiptId.json")
        require(file.isFile) { "RECEIPT_NOT_FOUND" }
        return file.readText(Charsets.UTF_8)
    }
}
