package com.tk.learn.infrastructure

import com.tk.learn.shared.DatabaseConfig
import org.jdbi.v3.core.kotlin.mapTo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AppJdbiTest {

    @BeforeEach
    fun setup() {
        AppJdbi.init(DatabaseConfig(
            url = "jdbc:h2:mem:test-appjdbi;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
            driver = "org.h2.Driver",
            username = "sa",
            password = ""
        ))
        AppJdbi.createSchema()
    }

    @Test
    fun `schema created and seeded once`() {
        val first = AppJdbi.getJdbi().withHandle<Int, Exception> { h ->
            h.createQuery("select count(*) from users").mapTo<Int>().one()
        }
        // Expect at least the 4 seed rows
        assertEquals(true, first >= 4)

        // Running createSchema again should not duplicate
        AppJdbi.createSchema()
        val second = AppJdbi.getJdbi().withHandle<Int, Exception> { h ->
            h.createQuery("select count(*) from users").mapTo<Int>().one()
        }
        assertEquals(first, second)
    }
}
