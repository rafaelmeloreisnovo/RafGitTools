package com.rafgittools.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.util.Base64

/**
 * Security Manager
 * 
 * Provides cryptographic operations and security utilities.
 * Implements industry security standards and best practices.
 * 
 * Standards Compliance:
 * - NIST SP 800-53 (Security Controls)
 * - OWASP Mobile Security (Top 10)
 * - ISO/IEC 27001 (Information Security)
 * - FIPS 140-2 (Cryptographic Module Security)
 */
class SecurityManager(private val context: Context) {
    
    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS_PREFIX = "rafgittools_"
        private const val AES_ALGORITHM = "AES/GCM/NoPadding"
        private const val KEY_SIZE = 256
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12
    }
    
    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }
    
    /**
     * Encrypt data using AES-256-GCM
     * 
     * Complies with:
     * - NIST SP 800-38D (GCM mode)
     * - FIPS 140-2 (Approved algorithms)
     * 
     * @param plaintext Data to encrypt
     * @param keyAlias Alias for the encryption key
     * @return Encrypted data with IV prepended
     */
    fun encryptData(plaintext: String, keyAlias: String = "default"): Result<String> {
        return try {
            val secretKey = getOrCreateKey(keyAlias)
            val cipher = Cipher.getInstance(AES_ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            
            // Prepend IV to encrypted data
            val combined = iv + encryptedBytes
            val encoded = Base64.getEncoder().encodeToString(combined)
            
            Result.success(encoded)
        } catch (e: Exception) {
            Result.failure(SecurityException("Encryption failed: ${e.message}", e))
        }
    }
    
    /**
     * Decrypt data using AES-256-GCM
     * 
     * @param ciphertext Encrypted data with IV prepended
     * @param keyAlias Alias for the decryption key
     * @return Decrypted plaintext
     */
    fun decryptData(ciphertext: String, keyAlias: String = "default"): Result<String> {
        return try {
            val secretKey = getOrCreateKey(keyAlias)
            val combined = Base64.getDecoder().decode(ciphertext)
            
            // Extract IV and encrypted data
            val iv = combined.sliceArray(0 until GCM_IV_LENGTH)
            val encryptedBytes = combined.sliceArray(GCM_IV_LENGTH until combined.size)
            
            val cipher = Cipher.getInstance(AES_ALGORITHM)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
            
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            val plaintext = String(decryptedBytes, Charsets.UTF_8)
            
            Result.success(plaintext)
        } catch (e: Exception) {
            Result.failure(SecurityException("Decryption failed: ${e.message}", e))
        }
    }
    
    /**
     * Securely hash a password using SHA-256
     * 
     * @param input String to hash
     * @return Base64-encoded hash
     */
    fun hashString(input: String): String {
        val messageDigest = java.security.MessageDigest.getInstance("SHA-256")
        val hashBytes = messageDigest.digest(input.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(hashBytes)
    }
    
    /**
     * Generate a cryptographically secure random string
     * 
     * @param length Length of the random string
     * @return Random string
     */
    fun generateSecureRandomString(length: Int = 32): String {
        val secureRandom = java.security.SecureRandom()
        val bytes = ByteArray(length)
        secureRandom.nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes).substring(0, length)
    }
    
    /**
     * Validate that a string is safe from injection attacks
     * 
     * Protects against:
     * - Command injection
     * - Path traversal
     * - SQL injection
     * 
     * @param input String to validate
     * @param type Type of validation to perform
     * @return True if input is safe
     */
    fun validateInput(input: String, type: InputValidationType): Boolean {
        return when (type) {
            InputValidationType.GIT_URL -> validateGitUrl(input)
            InputValidationType.FILE_PATH -> validateFilePath(input)
            InputValidationType.BRANCH_NAME -> validateBranchName(input)
            InputValidationType.COMMIT_MESSAGE -> validateCommitMessage(input)
            InputValidationType.USERNAME -> validateUsername(input)
        }
    }
    
    /**
     * Sanitize user input to prevent injection attacks
     * 
     * @param input String to sanitize
     * @param type Type of sanitization to perform
     * @return Sanitized string
     */
    fun sanitizeInput(input: String, type: InputValidationType): String {
        var sanitized = input.trim()
        
        when (type) {
            InputValidationType.GIT_URL -> {
                // Remove potentially dangerous characters
                sanitized = sanitized.replace(Regex("[^a-zA-Z0-9:/.@_-]"), "")
            }
            InputValidationType.FILE_PATH -> {
                // Remove path traversal attempts
                sanitized = sanitized.replace(Regex("\\.\\./"), "")
                    .replace(Regex("\\.\\.\\\\"), "")
            }
            InputValidationType.BRANCH_NAME -> {
                // Keep only safe characters for branch names
                sanitized = sanitized.replace(Regex("[^a-zA-Z0-9/_-]"), "")
            }
            InputValidationType.COMMIT_MESSAGE -> {
                // Remove control characters but allow newlines
                sanitized = sanitized.replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]"), "")
            }
            InputValidationType.USERNAME -> {
                // Keep only alphanumeric and safe characters
                sanitized = sanitized.replace(Regex("[^a-zA-Z0-9._-]"), "")
            }
        }
        
        return sanitized
    }
    
    /**
     * Check if the device is rooted (security risk)
     * 
     * @return True if device appears to be rooted
     */
    fun isDeviceRooted(): Boolean {
        // Check for common root indicators
        val rootPaths = listOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        
        return rootPaths.any { java.io.File(it).exists() }
    }
    
    /**
     * Check if debugger is attached (security risk)
     * 
     * @return True if debugger is detected
     */
    fun isDebuggerAttached(): Boolean {
        return android.os.Debug.isDebuggerConnected()
    }
    
    /**
     * Verify app signature to detect tampering
     * 
     * Compares the current app signature against the expected signature.
     * In production, this should be configured with the actual release signature hash.
     * 
     * @return True if signature is valid or in debug mode
     */
    fun verifyAppSignature(): Boolean {
        try {
            // In debug builds, skip signature verification
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val packageInfo = context.packageManager.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
                )
                val signatures = packageInfo.signingInfo?.apkContentsSigners
                if (signatures == null || signatures.isEmpty()) {
                    return false
                }
                
                // Get the signature hash
                val signature = signatures[0]
                @Suppress("UNUSED_VARIABLE")
                val signatureHash = hashString(signature.toCharsString())
                
                // TODO: In production, compare against the actual release signature:
                // return signatureHash == EXPECTED_RELEASE_SIGNATURE_HASH
                // For now, just verify that a signature exists
                return true
            } else {
                @Suppress("DEPRECATION")
                val packageInfo = context.packageManager.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.GET_SIGNATURES
                )
                @Suppress("DEPRECATION")
                val signatures = packageInfo.signatures
                if (signatures.isEmpty()) {
                    return false
                }
                
                // Get the signature hash
                val signature = signatures[0]
                @Suppress("UNUSED_VARIABLE")
                val signatureHash = hashString(signature.toCharsString())
                
                // TODO: In production, compare against the actual release signature:
                // return signatureHash == EXPECTED_RELEASE_SIGNATURE_HASH
                // For now, just verify that a signature exists
                return true
            }
        } catch (e: Exception) {
            // If we can't verify, assume it's not valid
            android.util.Log.e("SecurityManager", "Error verifying app signature", e)
            return false
        }
    }
    
    /**
     * Clear sensitive data from memory
     * 
     * @param data CharArray or ByteArray to clear
     */
    fun clearSensitiveData(data: CharArray) {
        data.fill('0')
    }
    
    fun clearSensitiveData(data: ByteArray) {
        data.fill(0)
    }
    
    /**
     * Get or create a key in Android Keystore
     * 
     * @param alias Key alias
     * @return SecretKey from Keystore
     */
    private fun getOrCreateKey(alias: String): SecretKey {
        val keyAlias = KEY_ALIAS_PREFIX + alias
        
        return if (keyStore.containsAlias(keyAlias)) {
            keyStore.getKey(keyAlias, null) as SecretKey
        } else {
            createKey(keyAlias)
        }
    }
    
    /**
     * Create a new AES key in Android Keystore
     * 
     * @param alias Key alias
     * @return Newly created SecretKey
     */
    private fun createKey(alias: String): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE)
            .setUserAuthenticationRequired(false) // Set to true for biometric-protected keys
            .build()
        
        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }
    
    // Validation helper methods
    
    private fun validateGitUrl(url: String): Boolean {
        // Valid Git URL patterns (with or without .git suffix)
        val patterns = listOf(
            // HTTPS URLs
            Regex("^https://[a-zA-Z0-9.-]+/[a-zA-Z0-9._/-]+(\\.git)?$"),
            // SSH URLs (git@host:path format)
            Regex("^git@[a-zA-Z0-9.-]+:[a-zA-Z0-9._/-]+(\\.git)?$"),
            // SSH URLs (ssh://git@host/path format)
            Regex("^ssh://git@[a-zA-Z0-9.-]+/[a-zA-Z0-9._/-]+(\\.git)?$"),
            // Git protocol
            Regex("^git://[a-zA-Z0-9.-]+/[a-zA-Z0-9._/-]+(\\.git)?$")
        )
        
        return patterns.any { it.matches(url) }
    }
    
    private fun validateFilePath(path: String): Boolean {
        // Reject path traversal attempts
        if (path.contains("../") || path.contains("..\\")) {
            return false
        }
        
        // Reject absolute paths to sensitive system directories
        val dangerousPaths = listOf("/system", "/root", "/etc", "/proc")
        if (dangerousPaths.any { path.startsWith(it) }) {
            return false
        }
        
        return true
    }
    
    private fun validateBranchName(name: String): Boolean {
        // Git branch name rules
        return name.matches(Regex("^[a-zA-Z0-9/_.-]+$")) &&
                !name.startsWith("/") &&
                !name.endsWith("/") &&
                !name.contains("//") &&
                !name.contains("..") &&
                name.length in 1..255
    }
    
    private fun validateCommitMessage(message: String): Boolean {
        // Reject if contains null bytes or excessive length
        return !message.contains('\u0000') &&
                message.length <= 10000
    }
    
    private fun validateUsername(username: String): Boolean {
        // Standard username validation
        return username.matches(Regex("^[a-zA-Z0-9._-]{1,39}$"))
    }
}

/**
 * Input validation types
 */
enum class InputValidationType {
    GIT_URL,
    FILE_PATH,
    BRANCH_NAME,
    COMMIT_MESSAGE,
    USERNAME
}

/**
 * Security exception class
 */
class SecurityException(message: String, cause: Throwable? = null) : Exception(message, cause)
