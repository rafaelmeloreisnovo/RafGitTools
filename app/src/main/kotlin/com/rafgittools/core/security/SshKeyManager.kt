package com.rafgittools.core.security

import android.content.Context
import com.jcraft.jsch.JSch
import com.jcraft.jsch.KeyPair
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SSH Key Manager for generating, storing, and managing SSH keys.
 * Implements Features #64 (SSH Key Generation) and #65 (SSH Key Management) from the roadmap.
 * 
 * Supported key types:
 * - Ed25519 (recommended, most secure)
 * - RSA (4096 bits for security)
 * - ECDSA (nistp256, nistp384, nistp521)
 * 
 * Standards compliance:
 * - RFC 4253 (SSH Transport Layer Protocol)
 * - RFC 8709 (Ed25519 and Ed448 Public Key Algorithms for SSH)
 * - RFC 5656 (ECDSA for SSH)
 * - NIST SP 800-131A (Cryptographic key strength)
 */
@Singleton
class SshKeyManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val SSH_KEYS_DIR = "ssh_keys"
        private const val DEFAULT_KEY_NAME = "id_rsa"
        
        // Key type constants
        const val KEY_TYPE_RSA = KeyPair.RSA
        const val KEY_TYPE_DSA = KeyPair.DSA
        const val KEY_TYPE_ECDSA = KeyPair.ECDSA
        const val KEY_TYPE_ED25519 = KeyPair.ED25519
        
        // Key size constants
        const val RSA_KEY_SIZE = 4096
        const val ECDSA_KEY_SIZE = 256 // nistp256
    }
    
    private val sshKeysDir: File by lazy {
        File(context.filesDir, SSH_KEYS_DIR).apply {
            if (!exists()) mkdirs()
        }
    }
    
    /**
     * Generate a new SSH key pair.
     * 
     * @param keyType The type of key to generate (RSA, ECDSA, ED25519)
     * @param keyName The name for the key file (without extension)
     * @param passphrase Optional passphrase to protect the private key
     * @param comment Optional comment for the key (usually email)
     * @return Result containing the generated SshKeyInfo or an error
     */
    fun generateKeyPair(
        keyType: Int = KEY_TYPE_ED25519,
        keyName: String = DEFAULT_KEY_NAME,
        passphrase: String? = null,
        comment: String = ""
    ): Result<SshKeyInfo> = runCatching {
        val jsch = JSch()
        
        val keyPair = when (keyType) {
            KEY_TYPE_RSA -> KeyPair.genKeyPair(jsch, KeyPair.RSA, RSA_KEY_SIZE)
            KEY_TYPE_ECDSA -> KeyPair.genKeyPair(jsch, KeyPair.ECDSA, ECDSA_KEY_SIZE)
            KEY_TYPE_ED25519 -> KeyPair.genKeyPair(jsch, KeyPair.ED25519)
            else -> throw IllegalArgumentException("Unsupported key type: $keyType")
        }
        
        // Set comment if provided
        if (comment.isNotEmpty()) {
            keyPair.setPublicKeyComment(comment)
        }
        
        // Generate file paths
        val privateKeyFile = File(sshKeysDir, keyName)
        val publicKeyFile = File(sshKeysDir, "$keyName.pub")
        
        // Write private key
        FileOutputStream(privateKeyFile).use { output ->
            if (passphrase != null) {
                keyPair.writePrivateKey(output, passphrase.toByteArray())
            } else {
                keyPair.writePrivateKey(output)
            }
        }
        
        // Write public key
        FileOutputStream(publicKeyFile).use { output ->
            keyPair.writePublicKey(output, comment)
        }
        
        // Get fingerprint
        val fingerprint = keyPair.fingerPrint
        
        // Get key type name
        val keyTypeName = when (keyType) {
            KEY_TYPE_RSA -> "RSA"
            KEY_TYPE_ECDSA -> "ECDSA"
            KEY_TYPE_ED25519 -> "ED25519"
            else -> "Unknown"
        }
        
        // Get public key content
        val publicKeyContent = publicKeyFile.readText()
        
        keyPair.dispose()
        
        SshKeyInfo(
            name = keyName,
            type = keyTypeName,
            fingerprint = fingerprint,
            publicKey = publicKeyContent,
            privateKeyPath = privateKeyFile.absolutePath,
            publicKeyPath = publicKeyFile.absolutePath,
            hasPassphrase = passphrase != null,
            comment = comment,
            createdAt = System.currentTimeMillis()
        )
    }
    
    /**
     * List all stored SSH keys.
     */
    fun listKeys(): Result<List<SshKeyInfo>> = runCatching {
        val keys = mutableListOf<SshKeyInfo>()
        
        sshKeysDir.listFiles()?.filter { 
            it.isFile && !it.name.endsWith(".pub") 
        }?.forEach { privateKeyFile ->
            val publicKeyFile = File(sshKeysDir, "${privateKeyFile.name}.pub")
            
            if (publicKeyFile.exists()) {
                try {
                    val jsch = JSch()
                    jsch.addIdentity(privateKeyFile.absolutePath)
                    
                    val publicKeyContent = publicKeyFile.readText()
                    val keyType = detectKeyType(publicKeyContent)
                    
                    keys.add(SshKeyInfo(
                        name = privateKeyFile.name,
                        type = keyType,
                        fingerprint = extractFingerprint(publicKeyContent),
                        publicKey = publicKeyContent,
                        privateKeyPath = privateKeyFile.absolutePath,
                        publicKeyPath = publicKeyFile.absolutePath,
                        hasPassphrase = false, // Cannot detect without trying to load
                        comment = extractComment(publicKeyContent),
                        createdAt = privateKeyFile.lastModified()
                    ))
                } catch (e: Exception) {
                    // Key might be password protected or invalid
                    keys.add(SshKeyInfo(
                        name = privateKeyFile.name,
                        type = "Unknown",
                        fingerprint = "Unable to read",
                        publicKey = if (publicKeyFile.exists()) publicKeyFile.readText() else "",
                        privateKeyPath = privateKeyFile.absolutePath,
                        publicKeyPath = publicKeyFile.absolutePath,
                        hasPassphrase = true, // Assume passphrase if can't load
                        comment = "",
                        createdAt = privateKeyFile.lastModified()
                    ))
                }
            }
        }
        
        keys.sortedByDescending { it.createdAt }
    }
    
    /**
     * Delete an SSH key pair.
     */
    fun deleteKey(keyName: String): Result<Unit> = runCatching {
        val privateKeyFile = File(sshKeysDir, keyName)
        val publicKeyFile = File(sshKeysDir, "$keyName.pub")
        
        if (privateKeyFile.exists()) {
            privateKeyFile.delete()
        }
        
        if (publicKeyFile.exists()) {
            publicKeyFile.delete()
        }
    }
    
    /**
     * Import an existing SSH private key.
     */
    fun importKey(
        keyName: String,
        privateKeyContent: String,
        passphrase: String? = null
    ): Result<SshKeyInfo> = runCatching {
        val privateKeyFile = File(sshKeysDir, keyName)
        val publicKeyFile = File(sshKeysDir, "$keyName.pub")
        
        // Write private key
        privateKeyFile.writeText(privateKeyContent)
        
        // Load and extract public key
        val jsch = JSch()
        val keyPair = if (passphrase != null) {
            KeyPair.load(jsch, privateKeyFile.absolutePath, passphrase)
        } else {
            KeyPair.load(jsch, privateKeyFile.absolutePath, null as String?)
        }
        
        // Write public key
        FileOutputStream(publicKeyFile).use { output ->
            keyPair.writePublicKey(output, "")
        }
        
        val fingerprint = keyPair.fingerPrint
        val keyType = when (keyPair.keyType) {
            KEY_TYPE_RSA -> "RSA"
            KEY_TYPE_ECDSA -> "ECDSA"
            KEY_TYPE_ED25519 -> "ED25519"
            KEY_TYPE_DSA -> "DSA"
            else -> "Unknown"
        }
        
        val publicKeyContent = publicKeyFile.readText()
        
        keyPair.dispose()
        
        SshKeyInfo(
            name = keyName,
            type = keyType,
            fingerprint = fingerprint,
            publicKey = publicKeyContent,
            privateKeyPath = privateKeyFile.absolutePath,
            publicKeyPath = publicKeyFile.absolutePath,
            hasPassphrase = passphrase != null,
            comment = "",
            createdAt = System.currentTimeMillis()
        )
    }
    
    /**
     * Export the public key content for a given key name.
     */
    fun exportPublicKey(keyName: String): Result<String> = runCatching {
        val publicKeyFile = File(sshKeysDir, "$keyName.pub")
        
        if (!publicKeyFile.exists()) {
            throw IllegalArgumentException("Public key not found: $keyName")
        }
        
        publicKeyFile.readText()
    }
    
    /**
     * Get the private key path for a given key name.
     */
    fun getPrivateKeyPath(keyName: String): String {
        return File(sshKeysDir, keyName).absolutePath
    }
    
    /**
     * Check if a key exists.
     */
    fun keyExists(keyName: String): Boolean {
        return File(sshKeysDir, keyName).exists()
    }
    
    /**
     * Validate a passphrase for a key.
     */
    fun validatePassphrase(keyName: String, passphrase: String): Result<Boolean> = runCatching {
        val privateKeyFile = File(sshKeysDir, keyName)
        
        if (!privateKeyFile.exists()) {
            throw IllegalArgumentException("Private key not found: $keyName")
        }
        
        try {
            val jsch = JSch()
            KeyPair.load(jsch, privateKeyFile.absolutePath, passphrase)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun detectKeyType(publicKeyContent: String): String {
        return when {
            publicKeyContent.startsWith("ssh-rsa") -> "RSA"
            publicKeyContent.startsWith("ssh-ed25519") -> "ED25519"
            publicKeyContent.startsWith("ecdsa-sha2") -> "ECDSA"
            publicKeyContent.startsWith("ssh-dss") -> "DSA"
            else -> "Unknown"
        }
    }
    
    private fun extractFingerprint(publicKeyContent: String): String {
        // Simple extraction - in production, compute SHA256 fingerprint
        return try {
            val parts = publicKeyContent.trim().split(" ")
            if (parts.size >= 2) {
                val keyData = java.util.Base64.getDecoder().decode(parts[1])
                val digest = java.security.MessageDigest.getInstance("SHA-256").digest(keyData)
                "SHA256:" + java.util.Base64.getEncoder().encodeToString(digest).trimEnd('=')
            } else {
                "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    private fun extractComment(publicKeyContent: String): String {
        val parts = publicKeyContent.trim().split(" ")
        return if (parts.size >= 3) parts.drop(2).joinToString(" ") else ""
    }
}

/**
 * Data class representing SSH key information.
 */
data class SshKeyInfo(
    val name: String,
    val type: String,
    val fingerprint: String,
    val publicKey: String,
    val privateKeyPath: String,
    val publicKeyPath: String,
    val hasPassphrase: Boolean,
    val comment: String,
    val createdAt: Long
)
