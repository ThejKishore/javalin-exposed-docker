package com.tk.learn.shared

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.valiktor.ConstraintViolationException

class ModelsTest {

    @Test
    fun `CreateUserRequest validates not blank`() {
        assertThrows(ConstraintViolationException::class.java) {
            CreateUserRequest("")
        }
        // valid
        CreateUserRequest("Alice")
    }

    @Test
    fun `UpdateUserRequest validates not blank`() {
        assertThrows(ConstraintViolationException::class.java) {
            UpdateUserRequest("   ")
        }
        UpdateUserRequest("Bob")
    }

    @Test
    fun `User validates id positive and name not numeric`() {
        assertThrows(ConstraintViolationException::class.java) { User(0, "John") }
        assertThrows(ConstraintViolationException::class.java) { User(1, "12345") }
        User(1, "John")
    }

    @Test
    fun `AppConfig and DatabaseConfig validate`() {
        val ex = assertThrows(ConstraintViolationException::class.java) {
            AppConfig(DatabaseConfig(url = "", driver = "", username = "", password = ""))
        }
        // ensure some violations exist
        assertEquals(true, ex.constraintViolations.isNotEmpty())

        // valid minimal config
        AppConfig(DatabaseConfig(url = "jdbc:h2:mem:test", driver = "org.h2.Driver", username = "sa", password = ""))
    }
}
