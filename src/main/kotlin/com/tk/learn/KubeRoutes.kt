package com.tk.learn

import io.javalin.http.Context
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object KubeRoutes {

    fun health(ctx: Context){
        ctx.json(mapOf("status" to "UP"))
    }

    fun liveness(ctx: Context){
        ctx.status(200).json(mapOf("status" to "ALIVE"))
    }

    fun readiness(ctx: Context){
        try {
            val ok = transaction {
                Users.selectAll().limit(1).toList().isNotEmpty()
            }
            if (ok) ctx.status(200).json(mapOf("status" to "READY"))
            else ctx.status(503).json(mapOf("status" to "NOT_READY", "reason" to "Unknown"))
        } catch (e: Exception) {
            ctx.status(503).json(mapOf("status" to "NOT_READY", "reason" to (e.message ?: "DB error")))
        }
    }
}