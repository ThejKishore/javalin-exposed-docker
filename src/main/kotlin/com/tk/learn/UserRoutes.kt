package com.tk.learn

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.javalin.http.Context
import io.javalin.openapi.*

private val json = jacksonObjectMapper()

object UserRoutes {

    @OpenApi(
        path = "/users/{id}",
        methods = [HttpMethod.GET],
        summary = "Get a user by ID",
        pathParams = [
            OpenApiParam(name = "id", type = Int::class, required = true, description = "User ID")
        ],
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(from = User::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(from = Map::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(from = Map::class)])
        ]
    )
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

    @OpenApi(
        path = "/users",
        methods = [HttpMethod.POST],
        summary = "Create a user",
        requestBody = OpenApiRequestBody(content = [OpenApiContent(from = CreateUserRequest::class)]),
        responses = [
            OpenApiResponse(status = "201", content = [OpenApiContent(from = User::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(from = Map::class)])
        ]
    )
    fun createUser(ctx: Context) {
        val body = try {
            json.readValue<CreateUserRequest>(ctx.body())
        } catch (e: Exception) {
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

    @OpenApi(
        path = "/users/{id}",
        methods = [HttpMethod.PUT],
        summary = "Update a user",
        pathParams = [OpenApiParam(name = "id", type = Int::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(from = UpdateUserRequest::class)]),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(from = User::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(from = Map::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(from = Map::class)])
        ]
    )
    fun updateUser(ctx: Context) {
        val id = ctx.pathParam("id").toIntOrNull()
        if (id == null) {
            ctx.status(400).json(mapOf("error" to "Invalid id")); return
        }
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

    @OpenApi(
        path = "/users/{id}",
        methods = [HttpMethod.DELETE],
        summary = "Delete a user",
        pathParams = [OpenApiParam(name = "id", type = Int::class, required = true)],
        responses = [
            OpenApiResponse(status = "204"),
            OpenApiResponse(status = "400", content = [OpenApiContent(from = Map::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(from = Map::class)])
        ]
    )
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


    @OpenApi(
        path = "/users",
        methods = [HttpMethod.GET],
        summary = "List users",
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(from = Array<User>::class)])
        ]
    )
    fun fetchUsers(ctx: Context) {
        ctx.json(Db.listUsers())
    }
}

