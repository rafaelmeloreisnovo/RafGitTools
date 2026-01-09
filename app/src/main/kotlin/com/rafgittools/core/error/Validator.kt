package com.rafgittools.core.error

/**
 * Input/Output Validation Utilities
 * 
 * Provides comprehensive validation for input-process-output pipeline.
 * Ensures zero runtime errors through strict validation at each stage.
 */
object Validator {
    
    /**
     * Validate string input
     */
    fun validateString(
        value: String?,
        minLength: Int = 0,
        maxLength: Int = Int.MAX_VALUE,
        pattern: Regex? = null,
        allowEmpty: Boolean = true,
        context: String = "string"
    ): Result<String> {
        return ErrorHandler.execute(
            operation = {
                when {
                    value == null -> throw ValidationException("String is null in $context")
                    !allowEmpty && value.isEmpty() -> throw ValidationException("String is empty in $context")
                    value.length < minLength -> throw ValidationException("String too short (min: $minLength) in $context")
                    value.length > maxLength -> throw ValidationException("String too long (max: $maxLength) in $context")
                    pattern != null && !pattern.matches(value) -> throw ValidationException("String doesn't match pattern in $context")
                    else -> value
                }
            },
            context = "validateString:$context"
        )
    }
    
    /**
     * Validate number input
     */
    fun <T : Number> validateNumber(
        value: T?,
        min: T? = null,
        max: T? = null,
        context: String = "number"
    ): Result<T> {
        return ErrorHandler.execute(
            operation = {
                when {
                    value == null -> throw ValidationException("Number is null in $context")
                    min != null && value.toDouble() < min.toDouble() -> 
                        throw ValidationException("Number below minimum ($min) in $context")
                    max != null && value.toDouble() > max.toDouble() -> 
                        throw ValidationException("Number above maximum ($max) in $context")
                    else -> value
                }
            },
            context = "validateNumber:$context"
        )
    }
    
    /**
     * Validate collection input
     */
    fun <T> validateCollection(
        collection: Collection<T>?,
        minSize: Int = 0,
        maxSize: Int = Int.MAX_VALUE,
        allowEmpty: Boolean = true,
        context: String = "collection"
    ): Result<Collection<T>> {
        return ErrorHandler.execute(
            operation = {
                when {
                    collection == null -> throw ValidationException("Collection is null in $context")
                    !allowEmpty && collection.isEmpty() -> throw ValidationException("Collection is empty in $context")
                    collection.size < minSize -> throw ValidationException("Collection too small (min: $minSize) in $context")
                    collection.size > maxSize -> throw ValidationException("Collection too large (max: $maxSize) in $context")
                    else -> collection
                }
            },
            context = "validateCollection:$context"
        )
    }
    
    /**
     * Validate boolean condition
     */
    fun validateCondition(
        condition: Boolean,
        message: String,
        context: String = "condition"
    ): Result<Unit> {
        return ErrorHandler.execute(
            operation = {
                if (!condition) {
                    throw ValidationException("Condition failed: $message in $context")
                }
            },
            context = "validateCondition:$context"
        )
    }
    
    /**
     * Validate object is not null
     */
    fun <T : Any> validateNotNull(
        value: T?,
        context: String = "object"
    ): Result<T> {
        return ErrorHandler.execute(
            operation = {
                value ?: throw ValidationException("Value is null in $context")
            },
            context = "validateNotNull:$context"
        )
    }
    
    /**
     * Validate email format
     */
    fun validateEmail(
        email: String?,
        context: String = "email"
    ): Result<String> {
        val emailPattern = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return validateString(
            value = email,
            pattern = emailPattern,
            allowEmpty = false,
            context = context
        )
    }
    
    /**
     * Validate URL format
     */
    fun validateUrl(
        url: String?,
        context: String = "url"
    ): Result<String> {
        val urlPattern = Regex("^https?://[\\w.-]+(:[0-9]+)?(/.*)?$")
        return validateString(
            value = url,
            pattern = urlPattern,
            allowEmpty = false,
            context = context
        )
    }
    
    /**
     * Validate file path
     */
    fun validatePath(
        path: String?,
        mustExist: Boolean = false,
        context: String = "path"
    ): Result<String> {
        return ErrorHandler.execute(
            operation = {
                val validatedPath = path ?: throw ValidationException("Path is null in $context")
                
                // Check for path traversal
                if (validatedPath.contains("..")) {
                    throw ValidationException("Path contains traversal in $context")
                }
                
                // Check if file exists if required
                if (mustExist) {
                    val file = java.io.File(validatedPath)
                    if (!file.exists()) {
                        throw ValidationException("Path does not exist in $context")
                    }
                }
                
                validatedPath
            },
            context = "validatePath:$context"
        )
    }
    
    /**
     * Validate enum value
     */
    inline fun <reified T : Enum<T>> validateEnum(
        value: String?,
        context: String = "enum"
    ): Result<T> {
        return ErrorHandler.execute(
            operation = {
                val validatedValue = value ?: throw ValidationException("Enum value is null in $context")
                try {
                    enumValueOf<T>(validatedValue)
                } catch (e: IllegalArgumentException) {
                    throw ValidationException("Invalid enum value: $validatedValue in $context")
                }
            },
            context = "validateEnum:$context"
        )
    }
    
    /**
     * Validate JSON string
     */
    fun validateJson(
        json: String?,
        context: String = "json"
    ): Result<String> {
        return ErrorHandler.execute(
            operation = {
                val validatedJson = json ?: throw ValidationException("JSON is null in $context")
                
                // Basic JSON validation
                val trimmed = validatedJson.trim()
                if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
                    throw ValidationException("Invalid JSON format in $context")
                }
                if (!trimmed.endsWith("}") && !trimmed.endsWith("]")) {
                    throw ValidationException("Invalid JSON format in $context")
                }
                
                validatedJson
            },
            context = "validateJson:$context"
        )
    }
}
