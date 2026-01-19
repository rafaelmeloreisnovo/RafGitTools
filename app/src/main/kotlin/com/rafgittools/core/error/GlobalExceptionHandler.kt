package com.rafgittools.core.error

/**
 * Global exception handler for uncaught exceptions.
 *
 * Logs uncaught exceptions before delegating to the default handler.
 */
class GlobalExceptionHandler(
    private val delegate: Thread.UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler(),
    private val context: String = "global"
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        val error = ErrorDetails(
            type = ErrorType.fromException(throwable),
            message = throwable.message ?: "Uncaught exception",
            context = "$context:${thread.name}",
            timestamp = System.currentTimeMillis(),
            stackTrace = throwable.stackTraceToString()
        )
        ErrorHandler.logErrorInternal(error)
        delegate?.uncaughtException(thread, throwable)
    }
}
