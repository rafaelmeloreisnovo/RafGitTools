package com.rafgittools.security

import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

/**
 * GpgKeyManager — GPG key operations via the system gpg binary.
 *
 * Uses gpg2 or gpg available on the device (e.g. installed via Termux:
 * `pkg install gnupg`). Returns a descriptive error when no gpg binary is
 * found, allowing the UI to prompt the user to install it.
 *
 * Supported operations (RFC 4880 — OpenPGP Message Format):
 *   - Key generation (Ed25519/Curve25519 by default via batch mode)
 *   - ASCII-armored key import
 *   - ASCII-armored public-key export
 *   - Detached binary signature creation
 *   - Listing secret keys
 *
 * Integration with git commit signing: set `user.signingkey` via
 * JGitService.setGitConfig and `commit.gpgSign = true`.
 */
object GpgKeyManager {

    private val GPG_CANDIDATES = listOf("gpg2", "gpg")
    private const val GPG_TIMEOUT_SECS = 30L
    private const val GPG_NOT_FOUND_MSG =
        "gpg not found. On Termux run: pkg install gnupg"
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ─── Internal helpers ──────────────────────────────────────────────────

    private data class GpgResult(val exitCode: Int, val stdout: String, val stderr: String)

    private fun findGpg(): String? = GPG_CANDIDATES.firstOrNull { candidate ->
        runCatching {
            val p = ProcessBuilder(candidate, "--version")
                .redirectErrorStream(true)
                .start()
            p.waitFor(3, TimeUnit.SECONDS) && p.exitValue() == 0
        }.getOrDefault(false)
    }

    private fun runGpg(vararg args: String, stdinText: String? = null): GpgResult {
        val gpg = findGpg()
            ?: return GpgResult(exitCode = -1, stdout = "", stderr = GPG_NOT_FOUND_MSG)
        return try {
            val process = ProcessBuilder(listOf(gpg) + args.toList())
                .redirectErrorStream(true)
                .start()
            if (stdinText != null) {
                process.outputStream.bufferedWriter().use { it.write(stdinText) }
            }
            val outputFuture = ioScope.async { process.inputStream.bufferedReader().readText() }
            val finished = process.waitFor(GPG_TIMEOUT_SECS, TimeUnit.SECONDS)
            val output = runBlocking { outputFuture.await() }
            if (!finished) {
                process.destroyForcibly()
                process.waitFor()
                return GpgResult(exitCode = -1, stdout = "", stderr = "gpg operation timed out")
            }
            GpgResult(exitCode = process.exitValue(), stdout = output, stderr = "")
        } catch (e: Exception) {
            GpgResult(exitCode = -1, stdout = "", stderr = "Failed to launch gpg: ${e.message}")
        }
    }

    // ─── Public API ────────────────────────────────────────────────────────

    /**
     * Generate a new Ed25519/Curve25519 GPG key pair via batch mode.
     * GnuPG 2.1+ required. The key is added to the user's keyring.
     *
     * @param name      Real name for the UID (e.g. "Rafael Melo Reis")
     * @param email     Email address for the UID
     * @param passphrase Optional passphrase protecting the private key;
     *                   pass empty string for an unprotected key.
     */
    fun generateKey(name: String, email: String, passphrase: String): Result<Unit> = runCatching {
        val batch = buildString {
            appendLine("Key-Type: EdDSA")
            appendLine("Key-Curve: Ed25519")
            appendLine("Subkey-Type: ECDH")
            appendLine("Subkey-Curve: Curve25519")
            appendLine("Name-Real: $name")
            appendLine("Name-Email: $email")
            if (passphrase.isEmpty()) appendLine("%no-protection") else appendLine("Passphrase: $passphrase")
            appendLine("%commit")
        }
        val r = runGpg("--batch", "--gen-key", stdinText = batch)
        if (r.exitCode != 0) throw IllegalStateException("GPG key generation failed: ${r.stderr}")
    }

    /**
     * Import an ASCII-armored GPG public or private key block.
     *
     * @param armored full armored key text (BEGIN PGP PUBLIC KEY BLOCK / BEGIN PGP PRIVATE KEY BLOCK)
     */
    fun importKey(armored: String): Result<Unit> = runCatching {
        val r = runGpg("--import", "--batch", stdinText = armored)
        if (r.exitCode != 0) throw IllegalStateException("GPG key import failed: ${r.stderr}")
    }

    /**
     * Export the ASCII-armored public key for [fingerprint].
     *
     * @param fingerprint full 40-char key fingerprint or key ID
     * @return armored public-key block as a string
     */
    fun exportKey(fingerprint: String): Result<String> = runCatching {
        val r = runGpg("--armor", "--export", fingerprint)
        if (r.exitCode != 0 || r.stdout.isBlank()) {
            throw IllegalStateException("GPG key export failed for '$fingerprint': ${r.stderr}")
        }
        r.stdout
    }

    /**
     * Create a detached binary signature over [data].
     *
     * The signing key is identified by [fingerprint]; [passphrase] unlocks
     * it when the key is passphrase-protected.
     *
     * @return raw bytes of the detached .sig file
     */
    fun signData(data: ByteArray, fingerprint: String, passphrase: String): Result<ByteArray> = runCatching {
        val tmpIn = File.createTempFile("raf_gpg_in_", ".bin")
        val tmpSig = File(tmpIn.path + ".sig")
        try {
            tmpIn.writeBytes(data)
            val args = mutableListOf(
                "--batch", "--yes",
                "--local-user", fingerprint,
                "--output", tmpSig.absolutePath,
                "--detach-sign", tmpIn.absolutePath
            )
            if (passphrase.isNotEmpty()) {
                args.addAll(0, listOf("--pinentry-mode", "loopback", "--passphrase", passphrase))
            }
            val r = runGpg(*args.toTypedArray())
            if (r.exitCode != 0) throw IllegalStateException("GPG signing failed: ${r.stderr}")
            tmpSig.readBytes()
        } finally {
            tmpIn.delete()
            tmpSig.delete()
        }
    }

    /**
     * List available secret keys in the keyring.
     *
     * @return list of [GpgKeyInfo] entries for each secret key found
     */
    fun listKeys(): Result<List<GpgKeyInfo>> = runCatching {
        val r = runGpg("--list-secret-keys", "--with-colons", "--with-fingerprint")
        if (r.exitCode != 0) throw IllegalStateException("GPG list-keys failed: ${r.stderr}")
        parseColonOutput(r.stdout)
    }

    /**
     * Check whether a gpg binary is available on this device.
     */
    fun isGpgAvailable(): Boolean = findGpg() != null

    // ─── Colon-format parser ───────────────────────────────────────────────

    private fun parseColonOutput(output: String): List<GpgKeyInfo> {
        val result = mutableListOf<GpgKeyInfo>()
        var fpr = ""; var name = ""; var email = ""; var keyType = ""
        for (line in output.lineSequence()) {
            val fields = line.split(":")
            when (fields.getOrNull(0)) {
                "sec" -> {
                    // flush previous key
                    if (fpr.isNotEmpty()) result.add(GpgKeyInfo(fpr, name, email, keyType))
                    fpr = ""; name = ""; email = ""
                    keyType = fields.getOrNull(3) ?: ""
                }
                "fpr" -> fpr = fields.getOrNull(9) ?: ""
                "uid" -> {
                    val uid = fields.getOrNull(9) ?: ""
                    val emailMatch = Regex("""<([^>]+)>""").find(uid)
                    email = emailMatch?.groupValues?.get(1) ?: ""
                    name = uid.substringBefore("<").trim().trimEnd()
                }
            }
        }
        if (fpr.isNotEmpty()) result.add(GpgKeyInfo(fpr, name, email, keyType))
        return result
    }

    // ─── Data types ────────────────────────────────────────────────────────

    /**
     * Metadata for a single GPG secret key.
     *
     * @param fingerprint  40-char hex fingerprint
     * @param name         Name field from the primary UID
     * @param email        Email field from the primary UID
     * @param keyType      Algorithm identifier (e.g. "ed25519", "rsa4096")
     */
    data class GpgKeyInfo(
        val fingerprint: String,
        val name: String,
        val email: String,
        val keyType: String = ""
    )
}
