package com.rafgittools.core.security

import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encryption Manager
 * 
 * Provides simplified encryption/decryption interface using SecurityManager.
 * Used by privacy and data protection components.
 */
@Singleton
class EncryptionManager @Inject constructor(
    private val context: Context
) {
    private val securityManager = SecurityManager(context)
    
    /**
     * Encrypt a string using AES-256-GCM
     * 
     * @param plaintext Data to encrypt
     * @param keyAlias Optional key alias (default: "privacy_data")
     * @return Encrypted string
     */
    fun encrypt(plaintext: String, keyAlias: String = "privacy_data"): String {
        return securityManager.encryptData(plaintext, keyAlias)
            .getOrElse { 
                // If encryption fails, return empty string
                // In production, this should be logged and handled appropriately
                ""
            }
    }
    
    /**
     * Decrypt a string using AES-256-GCM
     * 
     * @param ciphertext Encrypted data
     * @param keyAlias Optional key alias (default: "privacy_data")
     * @return Decrypted string
     */
    fun decrypt(ciphertext: String, keyAlias: String = "privacy_data"): String {
        if (ciphertext.isBlank()) return ""
        
        return securityManager.decryptData(ciphertext, keyAlias)
            .getOrElse { 
                // If decryption fails, return empty string
                // In production, this should be logged and handled appropriately
                ""
            }
    }
    
    /**
     * Securely hash data (one-way)
     * 
     * @param data Data to hash
     * @return Hashed string
     */
    fun hash(data: String): String {
        return securityManager.hashPassword(data)
            .getOrElse { "" }
    }
}
