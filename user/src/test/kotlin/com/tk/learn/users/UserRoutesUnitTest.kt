package com.tk.learn.users

import com.tk.learn.infrastructure.AppJdbi
import com.tk.learn.shared.DatabaseConfig
import org.jdbi.v3.core.kotlin.mapTo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UserRoutesUnitTest {

    @BeforeEach
    fun setupDb() {
        AppJdbi.init(DatabaseConfig(
            url = "jdbc:h2:mem:test-user-unit;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
            driver = "org.h2.Driver",
            username = "sa",
            password = ""
        ))
        AppJdbi.createSchema()
    }

    @Test
    fun `create update delete user via handle helpers`() {
        val jdbi = AppJdbi.getJdbi()
        val created = jdbi.withHandle<UserRoutes.ResultCodeUser, Exception> { h ->
            UserRoutes.createUserDb(h, "UnitUser")
        }
        assertEquals("UnitUser", created.user.name)

        val updated = jdbi.withHandle<UserRoutes.ResultCode, Exception> { h ->
            UserRoutes.updateUser(h, created.user.userId, "NewName")
        }
        assertEquals(1, updated.code)

        val fetched = jdbi.withHandle<String, Exception> { h ->
            h.createQuery("select name from users where id = :id")
                .bind("id", created.user.userId)
                .mapTo<String>().one()
        }
        assertEquals("NewName", fetched)

        val deleted = jdbi.withHandle<UserRoutes.ResultCode, Exception> { h ->
            UserRoutes.deleteUser(h, created.user.userId)
        }
        assertEquals(1, deleted.code)

        val count = jdbi.withHandle<Int, Exception> { h ->
            h.createQuery("select count(*) from users where id = :id")
                .bind("id", created.user.userId)
                .mapTo<Int>().one()
        }
        assertEquals(0, count)
    }
}
