package com.tk.learn

import io.javalin.http.HttpStatus
import org.jdbi.v3.core.statement.UnableToExecuteStatementException
import org.jdbi.v3.core.statement.UnableToCreateStatementException
import org.valiktor.ConstraintViolationException

// Sealed exception hierarchy for API errors
sealed class ApiException(
    val status: HttpStatus,
    override val message: String,
    val details: Any? = null,
    cause: Throwable? = null
) : RuntimeException(message, cause) {
    class BadRequest(message: String, details: Any? = null, cause: Throwable? = null) :
        ApiException(HttpStatus.BAD_REQUEST, message, details, cause)

    class Validation(message: String = "Validation failed", details: Any? = null) :
        ApiException(HttpStatus.BAD_REQUEST, message, details)

    class NotFound(message: String = "Resource not found", details: Any? = null) :
        ApiException(HttpStatus.NOT_FOUND, message, details)

    class Conflict(message: String, details: Any? = null, cause: Throwable? = null) :
        ApiException(HttpStatus.CONFLICT, message, details, cause)

    class Db(message: String = "Database error", details: Any? = null, cause: Throwable? = null, val isClientError: Boolean = false) :
        ApiException(if (isClientError) HttpStatus.BAD_REQUEST else HttpStatus.INTERNAL_SERVER_ERROR, message, details, cause)

    class Unauthorized(message: String = "Unauthorized") :
        ApiException(HttpStatus.UNAUTHORIZED, message)

    class Forbidden(message: String = "Forbidden") :
        ApiException(HttpStatus.FORBIDDEN, message)
}

// Error response DTOs
data class ErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
    val details: Any? = null,
    val correlationId: String? = null
)

data class ValidationError(
    val field: String,
    val message: String,
    val constraint: String
)

// Helpers to translate common exceptions to ApiException
object ErrorTranslator {
    fun fromThrowable(t: Throwable): ApiException = when (t) {
        is ApiException -> t
        is ConstraintViolationException -> ApiException.Validation(
            details = t.constraintViolations.map { v ->
                ValidationError(
                    field = v.property,
                    message = v.constraint.name, 
                    constraint = v.constraint.name
                )
            }
        )
        is UnableToExecuteStatementException -> {
            // Determine if this is likely a client error (e.g., constraint violation)
            val msg = (t.cause?.message ?: t.message) ?: "DB execute error"
            val client = msg.contains("constraint", true) || msg.contains("unique", true)
            ApiException.Db(message = sanitizeDbMessage(msg), cause = t, isClientError = client)
        }
        is UnableToCreateStatementException -> {
            val msg = (t.cause?.message ?: t.message) ?: "DB statement error"
            ApiException.Db(message = sanitizeDbMessage(msg), cause = t)
        }
        else -> ApiException.BadRequest(t.message ?: "Internal Server Error", cause = t)
    }

    private fun sanitizeDbMessage(msg: String): String {
        // Remove potentially sensitive parts; keep concise
        return msg.lines().firstOrNull()?.take(500) ?: "Database error"
    }
}
