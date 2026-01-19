package com.rafgittools.core.error

import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler

/**
 * Comprehensive Error Handler
 * 
 * Implements multi-layer error handling and prevention strategy:
 * - Input validation layer
 * - Process execution layer with try-catch
 * - Output validation layer
 * - Error recovery mechanisms
 * - Audit logging for all errors
 * 
 * Design Philosophy: Zero Runtime Errors
 * Computer code must be deterministic and error-free. This handler ensures:
 * 1. All inputs are validated before processing
 * 2. All exceptions are caught and handled gracefully
 * 3. All outputs are validated before return
 * 4. All errors are logged for analysis and prevention
 */
object ErrorHandler {
    
    private const val TAG = "ErrorHandler"
    private var errorLogger: ErrorLogger? = null
    
    /**
     * Initialize error handler with logger
     */
    fun initialize(logger: ErrorLogger) {
        errorLogger = logger
    }
    
    /**
     * Execute operation with comprehensive error handling
     * 
     * Layers:
     * 1. Input validation
     * 2. Try-catch execution
     * 3. Output validation
     * 4. Error logging
     * 5. Recovery or safe fallback
     * 
     * @param operation The operation to execute
     * @param inputValidator Optional input validation
     * @param outputValidator Optional output validation
     * @param fallback Fallback value if operation fails
     * @param context Context information for error logging
     * @return Result with success value or error
     */
    inline fun <T> execute(
        operation: () -> T,
        noinline inputValidator: (() -> Boolean)? = null,
        noinline outputValidator: ((T) -> Boolean)? = null,
        fallback: T? = null,
        context: String = "unknown"
    ): Result<T> {
        return try {
            // Layer 1: Input Validation
            if (inputValidator != null && !inputValidator()) {
                val error = ErrorDetails(
                    type = ErrorType.VALIDATION_ERROR,
                    message = "Input validation failed",
                    context = context,
                    timestamp = System.currentTimeMillis()
                )
                logErrorInternal(error)
                return if (fallback != null) {
                    Result.success(fallback)
                } else {
                    Result.failure(ValidationException("Input validation failed in $context"))
                }
            }
            
            // Layer 2: Execute with error catching
            val result = operation()
            
            // Layer 3: Output Validation
            if (outputValidator != null && !outputValidator(result)) {
                val error = ErrorDetails(
                    type = ErrorType.VALIDATION_ERROR,
                    message = "Output validation failed",
                    context = context,
                    timestamp = System.currentTimeMillis()
                )
                logErrorInternal(error)
                return if (fallback != null) {
                    Result.success(fallback)
                } else {
                    Result.failure(ValidationException("Output validation failed in $context"))
                }
            }
            
            Result.success(result)
            
        } catch (e: Exception) {
            // Layer 4: Error Logging
            val error = ErrorDetails(
                type = ErrorType.fromException(e),
                message = e.message ?: "Unknown error",
                context = context,
                timestamp = System.currentTimeMillis(),
                stackTrace = e.stackTraceToString()
            )
            logErrorInternal(error)
            
            // Layer 5: Recovery or Fallback
            if (fallback != null) {
                Result.success(fallback)
            } else {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Execute suspend operation with error handling
     */
    suspend inline fun <T> executeSuspend(
        crossinline operation: suspend () -> T,
        noinline inputValidator: (suspend () -> Boolean)? = null,
        noinline outputValidator: (suspend (T) -> Boolean)? = null,
        fallback: T? = null,
        context: String = "unknown"
    ): Result<T> {
        return try {
            // Layer 1: Input Validation
            if (inputValidator != null && !inputValidator()) {
                val error = ErrorDetails(
                    type = ErrorType.VALIDATION_ERROR,
                    message = "Input validation failed",
                    context = context,
                    timestamp = System.currentTimeMillis()
                )
                logErrorInternal(error)
                return if (fallback != null) {
                    Result.success(fallback)
                } else {
                    Result.failure(ValidationException("Input validation failed in $context"))
                }
            }
            
            // Layer 2: Execute with error catching
            val result = operation()
            
            // Layer 3: Output Validation
            if (outputValidator != null && !outputValidator(result)) {
                val error = ErrorDetails(
                    type = ErrorType.VALIDATION_ERROR,
                    message = "Output validation failed",
                    context = context,
                    timestamp = System.currentTimeMillis()
                )
                logErrorInternal(error)
                return if (fallback != null) {
                    Result.success(fallback)
                } else {
                    Result.failure(ValidationException("Output validation failed in $context"))
                }
            }
            
            Result.success(result)
            
        } catch (e: Exception) {
            // Layer 4: Error Logging
            val error = ErrorDetails(
                type = ErrorType.fromException(e),
                message = e.message ?: "Unknown error",
                context = context,
                timestamp = System.currentTimeMillis(),
                stackTrace = e.stackTraceToString()
            )
            logErrorInternal(error)
            
            // Layer 5: Recovery or Fallback
            if (fallback != null) {
                Result.success(fallback)
            } else {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Create coroutine exception handler
     */
    fun createCoroutineExceptionHandler(context: String = "coroutine"): CoroutineExceptionHandler {
        return CoroutineExceptionHandler { _, throwable ->
            val error = ErrorDetails(
                type = ErrorType.fromException(throwable),
                message = throwable.message ?: "Coroutine exception",
                context = context,
                timestamp = System.currentTimeMillis(),
                stackTrace = throwable.stackTraceToString()
            )
            logErrorInternal(error)
            Log.e(TAG, "Coroutine exception in $context", throwable)
        }
    }
    
    /**
     * Log error to persistent storage and analytics (internal, accessible from inline functions)
     */
    @PublishedApi
    internal fun logErrorInternal(error: ErrorDetails) {
        Log.e(TAG, "Error [${error.type}] in ${error.context}: ${error.message}")
        errorLogger?.log(error)
    }
    
    /**
     * Log error to persistent storage and analytics
     */
    private fun logError(error: ErrorDetails) {
        logErrorInternal(error)
    }
    
    /**
     * Validate input is not null
     */
    fun <T : Any> requireNotNull(value: T?, context: String): T {
        return value ?: throw ValidationException("Required value is null in $context")
    }
    
    /**
     * Validate condition is true
     */
    fun require(condition: Boolean, context: String, message: String) {
        if (!condition) {
            throw ValidationException("Requirement failed in $context: $message")
        }
    }
}

/**
 * Error types for classification
 */
enum class ErrorType {
    VALIDATION_ERROR,
    NETWORK_ERROR,
    DATABASE_ERROR,
    ENCRYPTION_ERROR,
    AUTHENTICATION_ERROR,
    PERMISSION_ERROR,
    IO_ERROR,
    UNKNOWN_ERROR;
    
    companion object {
        fun fromException(e: Throwable): ErrorType {
            return when (e) {
                is ValidationException -> VALIDATION_ERROR
                is java.io.IOException -> {
                    val message = e.message.orEmpty()
                    if (message.contains("cleartext", ignoreCase = true)) {
                        NETWORK_ERROR
                    } else {
                        IO_ERROR
                    }
                }
                is javax.crypto.AEADBadTagException,
                is java.security.GeneralSecurityException -> ENCRYPTION_ERROR
                is SecurityException -> PERMISSION_ERROR
                else -> UNKNOWN_ERROR
            }
        }
    }
}

/**
 * Error details for logging and analysis
 */
data class ErrorDetails(
    val type: ErrorType,
    val message: String,
    val context: String,
    val timestamp: Long,
    val stackTrace: String? = null
)

/**
 * Custom validation exception
 */
class ValidationException(message: String) : Exception(message)

/**
 * Error logger interface
 */
interface ErrorLogger {
    fun log(error: ErrorDetails)
    fun getErrors(limit: Int = 100): List<ErrorDetails>
    fun clearErrors()
}
