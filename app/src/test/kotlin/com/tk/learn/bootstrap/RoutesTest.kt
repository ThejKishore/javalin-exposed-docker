package com.tk.learn.bootstrap

import com.tk.learn.infrastructure.registerPrometheus
import io.javalin.Javalin
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RoutesTest {

    @Test
    fun `registerRoutes registers endpoints without throwing`() {
        val app = Javalin.create()
        val registry = registerPrometheus()
        Routes.registerRoutes(app, registry)
        // Basic smoke test: server starts after routes registration
        app.start(0)
        assertTrue(app.port() > 0)
        app.stop()
    }
}
