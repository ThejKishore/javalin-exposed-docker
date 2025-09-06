package com.tk.learn

import io.javalin.micrometer.MicrometerPlugin
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.DiskSpaceMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.binder.system.UptimeMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import java.io.File

object MetricsConfig {
    fun registerPrometheus() = PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

    fun registerMetrics(registry : PrometheusMeterRegistry) :MicrometerPlugin{
        // add a tag to all reported values to simplify filtering in large installations:
        registry.config().commonTags("application", "Javalin");
        ClassLoaderMetrics().bindTo(registry);
        JvmMemoryMetrics().bindTo(registry);
        JvmGcMetrics().bindTo(registry);
        JvmThreadMetrics().bindTo(registry);
        UptimeMetrics().bindTo(registry);
        ProcessorMetrics().bindTo(registry);
        DiskSpaceMetrics(File("/")).bindTo(registry);

        val micrometerPlugin = MicrometerPlugin(){
                micrometerPluginConfig -> micrometerPluginConfig.registry = registry
        }

        return micrometerPlugin
    }
}