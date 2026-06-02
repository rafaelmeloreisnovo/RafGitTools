package com.rafgittools.core.security

import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.security.MessageDigest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecurityManagerBase64CompatibilityTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val securityManager = SecurityManager(context)

    @Test
    fun hashString_usesNoWrapBase64Output() {
        val input = "compatibility-input"

        val actual = securityManager.hashString(input)
        val expectedHash = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        val expected = Base64.encodeToString(expectedHash, Base64.NO_WRAP)

        assertThat(actual).isEqualTo(expected)
        assertThat(actual).doesNotContain("\n")
    }

    @Test
    fun generateSecureRandomString_hasExpectedLengthAndNoLineBreaks() {
        val generated = securityManager.generateSecureRandomString(48)

        assertThat(generated).hasLength(48)
        assertThat(generated).doesNotContain("\n")
    }

    @Test
    fun encryptAndDecrypt_roundTrip_preservesIvCiphertextContract() {
        val plaintext = "payload-123-ç"

        val encrypted = securityManager.encryptData(plaintext)
        assertThat(encrypted.isSuccess).isTrue()

        val ciphertext = encrypted.getOrThrow()
        assertThat(ciphertext).doesNotContain("\n")

        val combined = Base64.decode(ciphertext, Base64.NO_WRAP)
        assertThat(combined.size).isGreaterThan(12)

        val decrypted = securityManager.decryptData(ciphertext)
        assertThat(decrypted.isSuccess).isTrue()
        assertThat(decrypted.getOrThrow()).isEqualTo(plaintext)
    }
}
