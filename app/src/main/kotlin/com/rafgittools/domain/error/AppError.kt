package com.rafgittools.domain.error

/**
 * Domain-level error classification following IEEE 1044 standard.
 * 
 * These error types represent business logic errors that can occur
 * across the application, independent of the data layer implementation.
 */
sealed class AppError(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause) {
    
    /**
     * Network-related errors (API calls, connectivity)
     */
    sealed class NetworkError(message: String, cause: Throwable? = null) : AppError(message, cause) {
        class NoConnection(message: String = "No internet connection") : NetworkError(message)
        class Timeout(message: String = "Request timed out") : NetworkError(message)
        class ServerError(val code: Int, message: String) : NetworkError(message)
        class CleartextNotPermitted(
            message: String = "Cleartext HTTP traffic is blocked. Use HTTPS/TLS or enable cleartext in a dev build."
        ) : NetworkError(message)
        class Unknown(message: String, cause: Throwable? = null) : NetworkError(message, cause)
    }
    
    /**
     * Authentication and authorization errors
     */
    sealed class AuthError(message: String, cause: Throwable? = null) : AppError(message, cause) {
        class Unauthorized(message: String = "Invalid credentials") : AuthError(message)
        class TokenExpired(message: String = "Authentication token has expired") : AuthError(message)
        class TokenInvalid(message: String = "Invalid authentication token") : AuthError(message)
        class PermissionDenied(message: String = "Permission denied") : AuthError(message)
        class BiometricFailed(message: String = "Biometric authentication failed") : AuthError(message)
    }
    
    /**
     * Git operation errors
     */
    sealed class GitError(message: String, cause: Throwable? = null) : AppError(message, cause) {
        class RepositoryNotFound(message: String = "Repository not found") : GitError(message)
        class BranchNotFound(val branch: String) : GitError("Branch not found: $branch")
        class CloneFailed(message: String, cause: Throwable? = null) : GitError(message, cause)
        class CommitFailed(message: String, cause: Throwable? = null) : GitError(message, cause)
        class PushFailed(message: String, cause: Throwable? = null) : GitError(message, cause)
        class PullFailed(message: String, cause: Throwable? = null) : GitError(message, cause)
        class MergeConflict(val conflicts: List<String>) : GitError("Merge conflict in: ${conflicts.joinToString()}")
        class InvalidRemote(val remote: String) : GitError("Invalid remote: $remote")
        class DirtyWorkingTree(message: String = "Working tree has uncommitted changes") : GitError(message)
    }
    
    /**
     * File and storage errors
     */
    sealed class StorageError(message: String, cause: Throwable? = null) : AppError(message, cause) {
        class FileNotFound(val path: String) : StorageError("File not found: $path")
        class PermissionDenied(val path: String) : StorageError("Permission denied: $path")
        class InsufficientSpace(message: String = "Insufficient storage space") : StorageError(message)
        class IOError(message: String, cause: Throwable? = null) : StorageError(message, cause)
    }
    
    /**
     * Validation errors for user input
     */
    sealed class ValidationError(message: String) : AppError(message) {
        class EmptyField(val fieldName: String) : ValidationError("$fieldName cannot be empty")
        class InvalidFormat(val fieldName: String, val reason: String) : ValidationError("Invalid $fieldName: $reason")
        class InvalidUrl(val url: String) : ValidationError("Invalid URL: $url")
        class InvalidEmail(val email: String) : ValidationError("Invalid email: $email")
    }
    
    /**
     * General application errors
     */
    sealed class GeneralError(message: String, cause: Throwable? = null) : AppError(message, cause) {
        class Unknown(message: String = "An unknown error occurred", cause: Throwable? = null) : GeneralError(message, cause)
        class NotImplemented(val feature: String) : GeneralError("Feature not yet implemented: $feature")
        class Configuration(message: String) : GeneralError(message)
    }
}

/**
 * Extension function to convert generic exceptions to AppError
 */
fun Throwable.toAppError(): AppError {
    return when (this) {
        is AppError -> this
        is java.net.UnknownHostException -> AppError.NetworkError.NoConnection()
        is java.net.SocketTimeoutException -> AppError.NetworkError.Timeout()
        is java.io.IOException -> {
            val message = this.message.orEmpty()
            if (message.contains("cleartext", ignoreCase = true)) {
                AppError.NetworkError.CleartextNotPermitted()
            } else {
                AppError.NetworkError.Unknown(message.ifBlank { "Network error" }, this)
            }
        }
        is java.io.FileNotFoundException -> AppError.StorageError.FileNotFound(message ?: "unknown")
        is SecurityException -> AppError.StorageError.PermissionDenied(message ?: "unknown")
        is IllegalArgumentException -> AppError.ValidationError.InvalidFormat("input", message ?: "invalid")
        is NotImplementedError -> AppError.GeneralError.NotImplemented(message ?: "feature")
        else -> AppError.GeneralError.Unknown(message ?: "Unknown error", this)
    }
}
