package com.tk.learn

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.javalin.http.Context

private val json = jacksonObjectMapper()

fun getUserById(ctx: Context) {
    val id = ctx.pathParam("id").toIntOrNull()
    if (id == null) {
        ctx.status(400).json(mapOf("error" to "Invalid id"))
        return
    }
    val user = Db.getUser(id)
    if (user == null)
        ctx.status(404).json(mapOf("error" to "User not found"))
    else
        ctx.json(user)
}

fun createUser(ctx: Context) {
    val body = try { json.readValue<CreateUserRequest>(ctx.body()) } catch (e: Exception) {
        ctx.status(400).json(mapOf("error" to "Invalid JSON body")); return
    }
    val name = body.name.trim()
    if (name.isBlank()) {
        ctx.status(400).json(mapOf("error" to "Name must not be blank"));
        return
    }
    val user = Db.createUser(name)
    ctx.header("Location", "/javalin/api/users/${user.userId}")
    ctx.status(201).json(user)
}

fun updateUser(ctx: Context) {
    val id = ctx.pathParam("id").toIntOrNull()
    if (id == null) { ctx.status(400).json(mapOf("error" to "Invalid id")); return }
    val body = try {
        json.readValue<UpdateUserRequest>(ctx.body())
    } catch (e: Exception) {
        ctx.status(400).json(mapOf("error" to "Invalid JSON body"));
        return
    }
    val name = body.name.trim()
    if (name.isBlank()) {
        ctx.status(400).json(mapOf("error" to "Name must not be blank"));
        return
    }
    val updated = Db.updateUser(id, name)
    if (updated == null)
        ctx.status(404).json(mapOf("error" to "User not found"))
    else
        ctx.json(updated)
}

fun deleteUser(ctx: Context) {
    val id = ctx.pathParam("id").toIntOrNull()
    if (id == null) {
        ctx.status(400).json(mapOf("error" to "Invalid id"));
        return
    }
    val deleted = Db.deleteUser(id)
    if (!deleted)
        ctx.status(404).json(mapOf("error" to "User not found"))
    else
        ctx.status(204)
}

data class CreateUserRequest(val name: String)
data class UpdateUserRequest(val name: String)