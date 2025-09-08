package com.tk.learn.bootstrap

import com.tk.learn.kube.KubeRoutes
import com.tk.learn.users.UserRoutes
import io.javalin.Javalin
import io.javalin.http.Context
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

object Routes {

        fun registerRoutes(app: Javalin, registry: PrometheusMeterRegistry) {
            val kubeRoutes = KubeRoutes(registry)
            app.get("/") { ctx: Context -> ctx.result("Hello World") }

            val userById = "/users/{id}"
            // User routes
            app.get("/users", UserRoutes::fetchUsers)
            app.get(userById, UserRoutes::getUserById)
            app.post("/users", UserRoutes::createUser)
            app.put(userById, UserRoutes::updateUser)
            app.delete(userById, UserRoutes::deleteUser)


            // Kubernetes health endpoints
            // Prometheus metrics
            app.get("/prometheus",kubeRoutes::prometheusScrape)
            app.get("/health",kubeRoutes::health)
            app.get("/liveness",kubeRoutes::liveness)
            app.get("/readiness",kubeRoutes::readiness)
        }
}
