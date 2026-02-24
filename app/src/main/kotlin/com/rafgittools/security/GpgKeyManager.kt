package com.rafgittools.security

/**
 * GpgKeyManager stub.
 *
 * Provides placeholder methods for GPG key management such as
 * generating, importing, exporting and signing data. Real GPG
 * operations would require native libraries or integration with
 * existing Android crypto APIs. These functions currently perform
 * no operations and simply return success values.
 */
object GpgKeyManager {
    fun generateKey(name: String, email: String, passphrase: String): Result<Unit> =
        Result.failure(NotImplementedError("GPG key generation is not implemented yet"))

    fun importKey(armored: String): Result<Unit> =
        Result.failure(NotImplementedError("GPG key import is not implemented yet"))

    fun exportKey(fingerprint: String): Result<String> =
        Result.failure(NotImplementedError("GPG key export is not implemented yet"))

    fun signData(data: ByteArray, fingerprint: String, passphrase: String): Result<ByteArray> =
        Result.failure(NotImplementedError("GPG data signing is not implemented yet"))
}
