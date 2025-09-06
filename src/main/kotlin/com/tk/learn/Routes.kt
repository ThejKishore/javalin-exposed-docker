package com.tk.learn

import io.javalin.Javalin
import io.javalin.http.Context
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry


object Routes {
    fun registerRoutes(app: Javalin, registry: PrometheusMeterRegistry) {
        // Home
        app.get("/") { ctx: Context -> ctx.result("Hello World") }

        // User routes
        app.get("/users", UserRoutes::fetchUsers)
        app.get("/users/{id}", UserRoutes::getUserById)
        app.post("/users", UserRoutes::createUser)
        app.put("/users/{id}", UserRoutes::updateUser)
        app.delete("/users/{id}", UserRoutes::deleteUser)

        // Misc
        app.get("/test/{name}") { ctx ->
            val name = ctx.pathParam("name")
            ctx.result("Hello $name")
        }

        // Prometheus metrics
        app.get("/prometheus") { ctx ->
            ctx.contentType("text/plain; version=0.0.4; charset=utf-8")
                .result(registry.scrape())
        }

        // Kubernetes health endpoints
        app.get("/health", KubeRoutes::health)
        app.get("/liveness", KubeRoutes::liveness)
        app.get("/readiness", KubeRoutes::readiness)
    }
}