package com.tk.learn

import io.javalin.http.Context
import io.mockk.*
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserRoutingTest {

    @BeforeAll
    fun setupAll() {
        Db.init()
    }

    @BeforeEach
    fun resetDb() {
        transaction {
            SchemaUtils.drop(Users)
            SchemaUtils.create(Users)
        }
    }

    private fun mockCtx(
        body: String? = null,
        idParam: String? = null
    ): Triple<Context, Captured, CapturedHeader> {
        val ctx = mockk<Context>(relaxed = true)

        val statusSlot = slot<Int>()
        every { ctx.status(capture(statusSlot)) } returns ctx

        val headerName = slot<String>()
        val headerValue = slot<String>()
        every { ctx.header(capture(headerName), capture(headerValue)) } returns ctx

        val jsonSlot = slot<Any>()
        every { ctx.json(capture(jsonSlot)) } returns ctx

        if (body != null) {
            every { ctx.body() } returns body
        }
        if (idParam != null) {
            every { ctx.pathParam("id") } returns idParam
        } else {
            every { ctx.pathParam("id") } returnsMany listOf("", "")
        }

        return Triple(ctx, Captured(statusSlot, jsonSlot), CapturedHeader(headerName, headerValue))
    }

    data class Captured(val status: CapturingSlot<Int>, val json: CapturingSlot<Any>)
    data class CapturedHeader(val name: CapturingSlot<String>, val value: CapturingSlot<String>)

    // createUser
    @Test
    fun `createUser returns 201 with Location and body`() {
        val (ctx, cap, header) = mockCtx(body = "{" + "\"name\":\"Charlie\"" + "}")
        createUser(ctx)
        assertTrue(cap.status.isCaptured)
        assertEquals(201, cap.status.captured)
        assertTrue(cap.json.isCaptured)
        val user = cap.json.captured as User
        assertEquals("Charlie", user.name)
        assertTrue(user.userId > 0)
        assertTrue(header.name.isCaptured && header.value.isCaptured)
        assertEquals("Location", header.name.captured)
        val expectedLocation = "/javalin/api/users/${user.userId}"
        assertEquals(expectedLocation, header.value.captured)
    }

    @Test
    fun `createUser returns 400 on invalid JSON`() {
        val (ctx, cap, _) = mockCtx(body = "not json")
        createUser(ctx)
        assertTrue(cap.status.isCaptured)
        assertEquals(400, cap.status.captured)
    }

    @Test
    fun `createUser returns 400 on blank name`() {
        val (ctx, cap, _) = mockCtx(body = "{" + "\"name\":\"   \"" + "}")
        createUser(ctx)
        assertTrue(cap.status.isCaptured)
        assertEquals(400, cap.status.captured)
    }

    // getUserById
    @Test
    fun `getUserById returns 400 on invalid id`() {
        val (ctx, cap, _) = mockCtx(idParam = "abc")
        getUserById(ctx)
        assertTrue(cap.status.isCaptured)
        assertEquals(400, cap.status.captured)
    }

    @Test
    fun `getUserById returns 404 when not found`() {
        val (ctx, cap, _) = mockCtx(idParam = "9999")
        getUserById(ctx)
        assertTrue(cap.status.isCaptured)
        assertEquals(404, cap.status.captured)
    }

    @Test
    fun `getUserById returns 200 and user when found`() {
        val created = Db.createUser("Alice")
        val (ctx, cap, _) = mockCtx(idParam = created.userId.toString())
        getUserById(ctx)
        // 200 not explicitly set; verify JSON body
        assertTrue(cap.json.isCaptured)
        val user = cap.json.captured as User
        assertEquals(created, user)
    }

    // updateUser
    @Test
    fun `updateUser returns 400 on invalid id`() {
        val (ctx, cap, _) = mockCtx(body = "{" + "\"name\":\"X\"" + "}", idParam = "abc")
        updateUser(ctx)
        assertTrue(cap.status.isCaptured)
        assertEquals(400, cap.status.captured)
    }

    @Test
    fun `updateUser returns 400 on invalid JSON`() {
        val (ctx, cap, _) = mockCtx(body = "not json", idParam = "1")
        updateUser(ctx)
        assertTrue(cap.status.isCaptured)
        assertEquals(400, cap.status.captured)
    }

    @Test
    fun `updateUser returns 400 on blank name`() {
        val (ctx, cap, _) = mockCtx(body = "{" + "\"name\":\"   \"" + "}", idParam = "1")
        updateUser(ctx)
        assertTrue(cap.status.isCaptured)
        assertEquals(400, cap.status.captured)
    }

    @Test
    fun `updateUser returns 404 when not found`() {
        val (ctx, cap, _) = mockCtx(body = "{" + "\"name\":\"New\"" + "}", idParam = "12345")
        updateUser(ctx)
        assertTrue(cap.status.isCaptured)
        assertEquals(404, cap.status.captured)
    }

    @Test
    fun `updateUser returns 200 and body when updated`() {
        val created = Db.createUser("Old")
        val (ctx, cap, _) = mockCtx(body = "{" + "\"name\":\"New\"" + "}", idParam = created.userId.toString())
        updateUser(ctx)
        // 200 not explicitly set; verify JSON body
        assertTrue(cap.json.isCaptured)
        val user = cap.json.captured as User
        assertEquals("New", user.name)
        assertEquals(created.userId, user.userId)
    }

    // deleteUser
    @Test
    fun `deleteUser returns 400 on invalid id`() {
        val (ctx, cap, _) = mockCtx(idParam = "abc")
        deleteUser(ctx)
        assertTrue(cap.status.isCaptured)
        assertEquals(400, cap.status.captured)
    }

    @Test
    fun `deleteUser returns 404 when not found`() {
        val (ctx, cap, _) = mockCtx(idParam = "98765")
        deleteUser(ctx)
        assertTrue(cap.status.isCaptured)
        assertEquals(404, cap.status.captured)
    }

    @Test
    fun `deleteUser returns 204 when deleted`() {
        val created = Db.createUser("Tmp")
        val (ctx, cap, _) = mockCtx(idParam = created.userId.toString())
        deleteUser(ctx)
        assertTrue(cap.status.isCaptured)
        assertEquals(204, cap.status.captured)
        assertNull(Db.getUser(created.userId))
    }
}
