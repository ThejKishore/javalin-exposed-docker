package com.tk.learn.infrastructure

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class MetricsConfigTest {

    @Test
    fun `registerPrometheus creates registry and registerMetrics returns plugin`() {
        val registry = registerPrometheus()
        val plugin = registerMetrics(registry)
        assertNotNull(registry)
        assertNotNull(plugin)
    }
}
