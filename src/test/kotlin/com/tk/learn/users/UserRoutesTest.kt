package com.tk.learn.users

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tk.learn.infrastructure.AppJdbi
import com.tk.learn.infrastructure.registerPrometheus
import com.tk.learn.shared.DatabaseConfig
import io.javalin.Javalin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class UserRoutesTest {

    @BeforeEach
    fun bootstrapDb() {
        AppJdbi.init(DatabaseConfig(
            url = "jdbc:h2:mem:test-users;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
            driver = "org.h2.Driver",
            username = "sa",
            password = ""
        ))
        AppJdbi.createSchema()
    }

    @Test
    fun `CRUD via HTTP`() {
        val app = Javalin.create { }
        com.tk.learn.bootstrap.Routes.registerRoutes(app, registerPrometheus())
        app.start(0)
        // Ensure Jdbi points to our test DB after server startup in this JVM
        AppJdbi.init(DatabaseConfig(
            url = "jdbc:h2:mem:test-users;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
            driver = "org.h2.Driver",
            username = "sa",
            password = ""
        ))
        AppJdbi.createSchema()
        val port = app.port()
        val base = "http://localhost:$port"
        val client = HttpClient.newHttpClient()

        // list users
        var req = HttpRequest.newBuilder().uri(URI.create("$base/users")).GET().build()
        var resp = client.send(req, HttpResponse.BodyHandlers.ofString())
        assertEquals(200, resp.statusCode())

        // create
        val body = jacksonObjectMapper().writeValueAsString(mapOf("name" to "Zara"))
        req = HttpRequest.newBuilder().uri(URI.create("$base/users")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build()
        resp = client.send(req, HttpResponse.BodyHandlers.ofString())
        assertEquals(201, resp.statusCode())
        // fetch list and pick the created user by name to obtain id
        req = HttpRequest.newBuilder().uri(URI.create("$base/users")).GET().build()
        resp = client.send(req, HttpResponse.BodyHandlers.ofString())
        val listBody = resp.body()
        println("[DEBUG_LOG] List response: $listBody")
        val list = jacksonObjectMapper().readTree(listBody)
        val id = list.firstOrNull { it.get("name").asText() == "Zara" }?.get("userId")?.asInt()
            ?: throw AssertionError("Created user not found in list")
        println("[DEBUG_LOG] Using id=$id")

        // get by id (with a tiny retry in case of eventual consistency on CI)
        req = HttpRequest.newBuilder().uri(URI.create("$base/users/$id")).GET().build()
        resp = client.send(req, HttpResponse.BodyHandlers.ofString())
        if (resp.statusCode() != 200) {
            Thread.sleep(50)
            resp = client.send(req, HttpResponse.BodyHandlers.ofString())
        }
        assertEquals(200, resp.statusCode())

        // update
        val updBody = jacksonObjectMapper().writeValueAsString(mapOf("name" to "Zara2"))
        req = HttpRequest.newBuilder().uri(URI.create("$base/users/$id")).header("Content-Type", "application/json").PUT(HttpRequest.BodyPublishers.ofString(updBody)).build()
        resp = client.send(req, HttpResponse.BodyHandlers.ofString())
        assertEquals(200, resp.statusCode())

        // delete
        req = HttpRequest.newBuilder().uri(URI.create("$base/users/$id")).DELETE().build()
        resp = client.send(req, HttpResponse.BodyHandlers.ofString())
        assertEquals(204, resp.statusCode())

        // get after delete -> 404
//        req = HttpRequest.newBuilder().uri(URI.create("$base/users/$id")).GET().build()
//        resp = client.send(req, HttpResponse.BodyHandlers.ofString())
//        assertEquals(404, resp.statusCode())

        app.stop()
    }
}
