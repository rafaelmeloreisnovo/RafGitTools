package com.rafgittools.core.security

import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

        assertEquals(expected, actual)
        assertFalse(actual.contains("\n"))
    }

    @Test
    fun generateSecureRandomString_hasExpectedLengthAndNoLineBreaks() {
        val generated = securityManager.generateSecureRandomString(48)

        assertEquals(48, generated.length)
        assertFalse(generated.contains("\n"))
    }

    @Test
    fun encryptAndDecrypt_roundTrip_preservesIvCiphertextContract() {
        val plaintext = "payload-123-ç"

        val encrypted = securityManager.encryptData(plaintext)
        assertTrue(encrypted.isSuccess)

        val ciphertext = encrypted.getOrThrow()
        assertFalse(ciphertext.contains("\n"))

        val combined = Base64.decode(ciphertext, Base64.NO_WRAP)
        assertTrue(combined.size > 12)

        val decrypted = securityManager.decryptData(ciphertext)
        assertTrue(decrypted.isSuccess)
        assertEquals(plaintext, decrypted.getOrThrow())
    }
}
