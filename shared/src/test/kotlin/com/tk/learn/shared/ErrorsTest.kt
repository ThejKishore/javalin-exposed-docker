package com.tk.learn.shared

import io.javalin.http.HttpStatus
import org.jdbi.v3.core.statement.UnableToCreateStatementException
import org.jdbi.v3.core.statement.UnableToExecuteStatementException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.valiktor.ConstraintViolationException
import org.valiktor.functions.isNotBlank
import org.valiktor.validate

class ErrorsTest {

    @Test
    fun `ApiException carries status and message`() {
        val ex = ApiException.NotFound("Nope")
        assertEquals(HttpStatus.NOT_FOUND, ex.status)
        assertEquals("Nope", ex.message)
    }

    data class Dummy(val field: String) {
        init {
            validate(this) { validate(Dummy::field).isNotBlank() }
        }
    }

    @Test
    fun `ErrorTranslator from Valiktor maps to Validation`() {
        val thrown = try { Dummy(""); null } catch (e: ConstraintViolationException) { e }
        val api = ErrorTranslator.fromThrowable(thrown!!)
        assertEquals(ApiException.Validation::class, api::class)
        val details = (api as ApiException.Validation).details as List<*>
        assertEquals(true, details.isNotEmpty())
    }

    @Test
    fun `ErrorTranslator from Jdbi exceptions`() {
        val ues = UnableToExecuteStatementException("unique constraint")
        val api1 = ErrorTranslator.fromThrowable(ues) as ApiException.Db
        assertEquals(true, api1.isClientError)

        val ucs = UnableToCreateStatementException("syntax error")
        val api2 = ErrorTranslator.fromThrowable(ucs)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, (api2 as ApiException.Db).status)
    }
}
