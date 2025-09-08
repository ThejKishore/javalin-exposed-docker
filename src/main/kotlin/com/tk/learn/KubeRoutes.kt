package com.tk.learn

import io.javalin.http.Context
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

class KubeRoutes(val registry: PrometheusMeterRegistry) {

    fun health(ctx: Context){
        ctx.json(mapOf("status" to "UP"))
    }

    fun liveness(ctx: Context){
        ctx.status(200).json(mapOf("status" to "ALIVE"))
    }

    fun readiness(ctx: Context){
        try {
            // Perform a lightweight DB check to ensure connectivity and schema readiness
            AppJdbi.getJdbi().useHandle<Exception> { handle ->
                // simple query that should always work if DB is up and 'users' table exists
                handle.createQuery("select 1").mapTo(Int::class.java).one()
            }
            ctx.status(200).json(mapOf("status" to "READY", "db" to "CONNECTED"))
        } catch (e: Exception) {
            ctx.status(503).json(mapOf("status" to "NOT_READY", "db" to "DISCONNECTED", "reason" to (e.message ?: "DB error")))
        }
    }

    fun prometheusScrape(ctx: Context) {
        ctx.contentType("text/plain; version=0.0.4; charset=utf-8")
            .result(registry.scrape())
    }
}