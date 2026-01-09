package com.rafgittools.core.security

import android.content.Context
import com.rafgittools.core.error.ErrorHandler
import com.rafgittools.core.error.Validator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encryption Manager with Comprehensive Error Handling
 * 
 * Provides simplified encryption/decryption interface using SecurityManager.
 * Used by privacy and data protection components.
 * 
 * Error Handling Strategy:
 * - Input validation before encryption/decryption
 * - Try-catch around all crypto operations
 * - Output validation after operations
 * - Detailed error logging for analysis
 * - Safe fallback values to prevent crashes
 */
@Singleton
class EncryptionManager @Inject constructor(
    private val context: Context
) {
    private val securityManager = SecurityManager(context)
    
    /**
     * Encrypt a string using AES-256-GCM with comprehensive error handling
     * 
     * Error handling layers:
     * 1. Input validation (not null, not empty)
     * 2. Encryption with try-catch
     * 3. Output validation (not null, not empty)
     * 4. Error logging
     * 5. Safe fallback (empty string)
     * 
     * @param plaintext Data to encrypt
     * @param keyAlias Optional key alias (default: "privacy_data")
     * @return Encrypted string or empty string on error
     */
    fun encrypt(plaintext: String, keyAlias: String = "privacy_data"): String {
        return ErrorHandler.execute(
            operation = {
                // Validate inputs
                Validator.validateString(
                    value = plaintext,
                    allowEmpty = false,
                    maxLength = 1_000_000, // 1MB max
                    context = "encrypt:plaintext"
                ).getOrThrow()
                
                Validator.validateString(
                    value = keyAlias,
                    minLength = 1,
                    maxLength = 256,
                    context = "encrypt:keyAlias"
                ).getOrThrow()
                
                // Perform encryption
                val result = securityManager.encryptData(plaintext, keyAlias)
                    .getOrThrow()
                
                // Validate output
                Validator.validateString(
                    value = result,
                    allowEmpty = false,
                    context = "encrypt:result"
                ).getOrThrow()
                
                result
            },
            outputValidator = { it.isNotEmpty() },
            fallback = "",
            context = "EncryptionManager.encrypt"
        ).getOrElse { "" }
    }
    
    /**
     * Decrypt a string using AES-256-GCM with comprehensive error handling
     * 
     * Error handling layers:
     * 1. Input validation (not null, not empty)
     * 2. Decryption with try-catch
     * 3. Output validation
     * 4. Error logging
     * 5. Safe fallback (empty string)
     * 
     * @param ciphertext Encrypted data
     * @param keyAlias Optional key alias (default: "privacy_data")
     * @return Decrypted string or empty string on error
     */
    fun decrypt(ciphertext: String, keyAlias: String = "privacy_data"): String {
        // Early return for empty input
        if (ciphertext.isBlank()) return ""
        
        return ErrorHandler.execute(
            operation = {
                // Validate inputs
                Validator.validateString(
                    value = ciphertext,
                    allowEmpty = false,
                    maxLength = 2_000_000, // 2MB max (encrypted is larger)
                    context = "decrypt:ciphertext"
                ).getOrThrow()
                
                Validator.validateString(
                    value = keyAlias,
                    minLength = 1,
                    maxLength = 256,
                    context = "decrypt:keyAlias"
                ).getOrThrow()
                
                // Perform decryption
                val result = securityManager.decryptData(ciphertext, keyAlias)
                    .getOrThrow()
                
                result
            },
            fallback = "",
            context = "EncryptionManager.decrypt"
        ).getOrElse { "" }
    }
    
    /**
     * Securely hash data (one-way) with error handling
     * 
     * @param data Data to hash
     * @return Hashed string or empty string on error
     */
    fun hash(data: String): String {
        return ErrorHandler.execute(
            operation = {
                // Validate input
                Validator.validateString(
                    value = data,
                    allowEmpty = false,
                    maxLength = 10_000,
                    context = "hash:data"
                ).getOrThrow()
                
                // Perform hashing
                val result = securityManager.hashPassword(data)
                    .getOrThrow()
                
                // Validate output
                Validator.validateString(
                    value = result,
                    allowEmpty = false,
                    context = "hash:result"
                ).getOrThrow()
                
                result
            },
            outputValidator = { it.isNotEmpty() },
            fallback = "",
            context = "EncryptionManager.hash"
        ).getOrElse { "" }
    }
}
