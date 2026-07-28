package com.rafgittools.data.auth

import com.rafgittools.core.security.SshKeyManager
import com.rafgittools.data.github.AddSshKeyRequest
import com.rafgittools.data.github.GithubApiService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages SSH key rotation: generates a new Ed25519 key pair, uploads it to
 * GitHub via the API, and removes the old key.
 *
 * The rotation is atomic from the caller's perspective:
 * 1. New key generated + uploaded (if upload fails, old key is untouched).
 * 2. Old key deleted only after the new key is confirmed on GitHub.
 * 3. Private key file written to app-internal storage; public key content
 *    is what GitHub receives.
 */
@Singleton
class SshKeyRotationManager @Inject constructor(
    private val sshKeyManager: SshKeyManager,
    private val githubApiService: GithubApiService
) {

    sealed class RotationResult {
        data class Success(val newKeyTitle: String, val deletedKeyId: Long?) : RotationResult()
        data class Failure(val reason: String, val cause: Throwable? = null) : RotationResult()
    }

    /**
     * Rotate the SSH key identified by [oldKeyId] on GitHub.
     *
     * Generates a new Ed25519 key locally, registers it on the user's GitHub
     * account, then deletes [oldKeyId]. If [oldKeyId] is null the old key is
     * not deleted (useful for adding a first key).
     *
     * @param oldKeyId  GitHub SSH key ID to remove after rotation, or null to skip deletion.
     * @param keyTitle  Title shown in GitHub SSH key settings.
     * @param keyName   Local filename for the private key (default: "id_ed25519_rotated").
     * @param comment   OpenSSH public-key comment (typically an email).
     */
    suspend fun rotate(
        oldKeyId: Long?,
        keyTitle: String,
        keyName: String = "id_ed25519_rotated",
        comment: String = ""
    ): RotationResult {
        val keyInfo = sshKeyManager.generateKeyPair(
            keyType = SshKeyManager.KEY_TYPE_ED25519,
            keyName = keyName,
            comment = comment
        ).getOrElse { e ->
            return RotationResult.Failure("Key generation failed", e)
        }

        val uploaded = runCatching {
            githubApiService.addSshKey(
                AddSshKeyRequest(title = keyTitle, key = keyInfo.publicKey)
            )
        }.getOrElse { e ->
            sshKeyManager.deleteKey(keyName)
            return RotationResult.Failure("GitHub key upload failed", e)
        }

        var deletedId: Long? = null
        if (oldKeyId != null) {
            runCatching { githubApiService.deleteSshKey(oldKeyId) }
                .onSuccess { deletedId = oldKeyId }
        }

        return RotationResult.Success(newKeyTitle = uploaded.title, deletedKeyId = deletedId)
    }

    /**
     * List all SSH keys currently registered on the authenticated GitHub account.
     */
    suspend fun listRemoteKeys() = runCatching { githubApiService.getSshKeys() }
}
