package com.tk.learn.kube

import com.tk.learn.infrastructure.AppJdbi
import com.tk.learn.shared.DatabaseConfig
import io.javalin.http.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.jdbi.v3.core.HandleCallback
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KubeRoutesTest {

    private lateinit var routes: KubeRoutes

    @BeforeEach
    fun setUp() {
        routes = KubeRoutes(PrometheusMeterRegistry(io.micrometer.prometheusmetrics.PrometheusConfig.DEFAULT))
        // real DB for readiness success path
        AppJdbi.init(DatabaseConfig(
            url = "jdbc:h2:mem:test-kube;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
            driver = "org.h2.Driver",
            username = "sa",
            password = ""
        ))
        AppJdbi.createSchema()
    }

    @Test
    fun `liveness and health return expected`() {
        val ctx = mockk<Context>(relaxed = true)
        routes.health(ctx)
        verify { ctx.json(match<Map<String, String>> { it["status"] == "UP" }) }

        val ctx2 = mockk<Context>(relaxed = true)
        routes.liveness(ctx2)
        verify { ctx2.status(200); ctx2.json(match<Map<String, String>> { it["status"] == "ALIVE" }) }
    }

    @Test
    fun `readiness success returns READY`() {
        val ctx = mockk<Context>(relaxed = true)
        routes.readiness(ctx)
        verify { ctx.status(200); ctx.json(match<Map<String, String>> { it["status"] == "READY" && it["db"] == "CONNECTED" }) }
    }
}
