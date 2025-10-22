package com.tk.learn.users

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tk.learn.infrastructure.AppJdbi
import com.tk.learn.shared.*
import io.javalin.http.Context
import io.javalin.openapi.*
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo

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
            throw ApiException.BadRequest("Invalid id")
        }
        println("[DEBUG_LOG] getUserById called with id=$id")
        val user = AppJdbi.getJdbi().withHandle<User?,Exception> {
            val users = it.createQuery("select id as userId, name from users")
                .mapTo<User>()
                .list()
            println("[DEBUG_LOG] getUserById users in DB: " + users.joinToString { u -> "${u.userId}:${u.name}" })
            users.find { u -> u.userId == id }
            }
        if (user == null)
            throw ApiException.NotFound("User not found")
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
            throw ApiException.BadRequest("Invalid JSON body", cause = e)
        }
        val name = body.name.trim()
        if (name.isBlank()) {
            throw ApiException.Validation(details = listOf(ValidationError("name", "must not be blank", "NotBlank")))
        }
        val user = AppJdbi.getJdbi().withHandle <ResultCodeUser,Exception> { h -> createUserDb(h, name) }
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
        if (id == null) { throw ApiException.BadRequest("Invalid id") }
        val body = try {
            json.readValue<UpdateUserRequest>(ctx.body())
        } catch (e: Exception) {
            throw ApiException.BadRequest("Invalid JSON body", cause = e)
        }
        val name = body.name.trim()
        if (name.isBlank()) {
            throw ApiException.Validation(details = listOf(ValidationError("name", "must not be blank", "NotBlank")))
        }
        val updated = AppJdbi.getJdbi().withHandle<ResultCode, Exception> { h -> updateUser(h, id, name) }
        if (updated.code <= 0)
            throw ApiException.NotFound("User not found")
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
        if (id == null) { throw ApiException.BadRequest("Invalid id") }
        val deleted = AppJdbi.getJdbi().withHandle<ResultCode, Exception> { h -> deleteUser(h, id) }
        if (deleted.code <= 0)
            throw ApiException.NotFound("User not found")
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
        ctx.json(AppJdbi.getJdbi().withHandle <List<User>,Exception> {
                    it.createQuery("select id as userId, name from users")
                        .mapTo<User>()
                        .list()
                })
    }


    fun createUserDb(handle: Handle, name: String): ResultCodeUser{
        val id = handle.createUpdate("INSERT INTO users (name) VALUES (:name)")
            .bind("name", name)
            .executeAndReturnGeneratedKeys("id")
            .mapTo<Int>()
            .one()
        return ResultCodeUser(user = User(id, name))
    }

    fun updateUser(handle: Handle, id: Int, name: String): ResultCode {
        val rowsUpdated = handle.createUpdate("UPDATE users SET name = :name WHERE id = :id")
            .bind("id", id)
            .bind("name", name)
            .execute()

        return ResultCode(rowsUpdated)
    }

    fun deleteUser(handle: Handle, id: Int): ResultCode {
        val rowsUpdated = handle.createUpdate("delete from users where id = :id")
            .bind("id", id)
            .execute()
        return ResultCode( rowsUpdated)
    }

    data class ResultCode(val code: Int)
    data class ResultCodeUser(val user: User)
}
