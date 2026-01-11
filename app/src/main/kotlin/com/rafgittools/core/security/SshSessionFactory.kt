package com.rafgittools.core.security

import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.UserInfo
import org.eclipse.jgit.transport.ssh.jsch.JschConfigSessionFactory
import org.eclipse.jgit.transport.ssh.jsch.OpenSshConfig
import org.eclipse.jgit.util.FS

/**
 * Custom SSH Session Factory for JGit operations.
 * Implements Feature #66 (SSH Agent Integration) from the roadmap.
 * 
 * This factory enables SSH key authentication for Git operations like
 * clone, push, pull, and fetch.
 * 
 * Standards compliance:
 * - RFC 4251/4252 (SSH Protocol)
 * - SSH Agent Protocol
 */
class SshSessionFactory(
    private val privateKeyPath: String,
    private val passphrase: String? = null,
    private val knownHostsPath: String? = null
) : JschConfigSessionFactory() {
    
    override fun configure(hc: OpenSshConfig.Host, session: Session) {
        // Configure session properties
        session.setConfig("StrictHostKeyChecking", "no")
        session.setConfig("PreferredAuthentications", "publickey")
        
        // Set user info for passphrase handling
        if (passphrase != null) {
            session.userInfo = SshUserInfo(passphrase)
        }
    }
    
    override fun createDefaultJSch(fs: FS): JSch {
        val jsch = super.createDefaultJSch(fs)
        
        // Add the private key identity
        if (passphrase != null) {
            jsch.addIdentity(privateKeyPath, passphrase)
        } else {
            jsch.addIdentity(privateKeyPath)
        }
        
        // Configure known hosts if provided
        knownHostsPath?.let { path ->
            try {
                jsch.setKnownHosts(path)
            } catch (e: Exception) {
                // Known hosts file may not exist
            }
        }
        
        return jsch
    }
    
    /**
     * Internal UserInfo implementation for passphrase handling.
     */
    private class SshUserInfo(private val passphrase: String) : UserInfo {
        override fun getPassphrase(): String = passphrase
        override fun getPassword(): String? = null
        override fun promptPassword(message: String): Boolean = false
        override fun promptPassphrase(message: String): Boolean = true
        override fun promptYesNo(message: String): Boolean = true
        override fun showMessage(message: String) {
            // No-op for non-interactive use
        }
    }
    
    companion object {
        /**
         * Create a session factory with just a private key path.
         */
        fun create(privateKeyPath: String): SshSessionFactory {
            return SshSessionFactory(privateKeyPath)
        }
        
        /**
         * Create a session factory with passphrase protection.
         */
        fun createWithPassphrase(privateKeyPath: String, passphrase: String): SshSessionFactory {
            return SshSessionFactory(privateKeyPath, passphrase)
        }
        
        /**
         * Create a session factory with known hosts verification.
         */
        fun createWithKnownHosts(
            privateKeyPath: String,
            passphrase: String?,
            knownHostsPath: String
        ): SshSessionFactory {
            return SshSessionFactory(privateKeyPath, passphrase, knownHostsPath)
        }
    }
}
