package com.tk.learn

import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DbTest {

    @BeforeAll
    fun setupAll() {
        Db.init()
    }

    @BeforeEach
    fun resetDb() {
        transaction {
            // Drop and recreate to ensure clean state and reset auto-increment
            SchemaUtils.drop(Users)
            SchemaUtils.create(Users)
        }
    }

    @Test
    fun `listUsers returns empty when no rows`() {
        val users = Db.listUsers()
        assertTrue(users.isEmpty(), "Expected empty list, found $users")
    }

    @Test
    fun `createUser inserts and returns user`() {
        val created = Db.createUser("Charlie")
        assertEquals("Charlie", created.name)
        assertTrue(created.userId > 0)

        val listed = Db.listUsers()
        assertEquals(1, listed.size)
        assertEquals("Charlie", listed[0].name)

        val fetched = Db.getUser(created.userId)
        assertNotNull(fetched)
        assertEquals(created, fetched)
    }

    @Test
    fun `getUser returns null for missing id`() {
        assertNull(Db.getUser(999999))
    }

    @Test
    fun `updateUser updates existing and returns updated`() {
        val u = Db.createUser("Old")
        val updated = Db.updateUser(u.userId, "New")
        assertNotNull(updated)
        assertEquals("New", updated!!.name)
        assertEquals(u.userId, updated.userId)
    }

    @Test
    fun `updateUser returns null for missing id`() {
        assertNull(Db.updateUser(999999, "X"))
    }

    @Test
    fun `deleteUser returns true on success`() {
        val u = Db.createUser("Tmp")
        val ok = Db.deleteUser(u.userId)
        assertTrue(ok)
        assertNull(Db.getUser(u.userId))
    }

    @Test
    fun `deleteUser returns false for missing id`() {
        assertFalse(Db.deleteUser(424242))
    }
}
