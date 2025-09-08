package com.tk.learn.bootstrap

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addResourceSource
import com.tk.learn.infrastructure.AppJdbi
import com.tk.learn.infrastructure.registerMetrics
import com.tk.learn.infrastructure.registerPrometheus
import com.tk.learn.shared.ApiException
import com.tk.learn.shared.AppConfig
import com.tk.learn.shared.ErrorResponse
import com.tk.learn.shared.ErrorTranslator
import io.javalin.Javalin
import io.javalin.http.HttpStatus
import io.javalin.openapi.OpenApiInfo
import io.javalin.openapi.plugin.OpenApiPlugin
import io.javalin.openapi.plugin.redoc.ReDocPlugin
import io.javalin.openapi.plugin.swagger.SwaggerPlugin

val log = org.slf4j.LoggerFactory.getLogger("Main")!!


fun main() {

    val appConfig = ConfigLoaderBuilder.default()
        .addResourceSource("/application.yaml", optional = true)
        .build()
        .loadConfigOrThrow<AppConfig>()


    log.info("Config is ${appConfig.database} ")
    //Initializing the h2 database using exposed kotlin framework
    AppJdbi.init(appConfig.database)
    AppJdbi.createSchema()

    //Registering the promethus and setting it in MicrometerPlugin
    val registry = registerPrometheus()
    val micrometerPlugin = registerMetrics(registry)

    val app = Javalin.create { config ->
        config.contextResolver.ip = { ctx -> ctx.header("X-Forwarded-For") ?: ctx.req().remoteAddr }
        config.router.treatMultipleSlashesAsSingleSlash = true
        config.router.ignoreTrailingSlashes = true
        config.router.contextPath = "/javalin/api"
        config.requestLogger.http { ctx, ms ->
            log.info("Request from Ip :${ctx.req().remoteAddr} , Method: ${ctx.method()} Url Path :${ctx.path()} took $ms ms")
        }
        config.useVirtualThreads = true
        config.showJavalinBanner = false

        config.registerPlugin(OpenApiPlugin { pluginConfig ->
            pluginConfig.withDefinitionConfiguration { version, definition ->
                definition.withInfo { info: OpenApiInfo ->
                    info.title = "Javalin OpenAPI example "
                }
                definition.withServer { server ->
                    server.url = "http://localhost:7070/javalin/api"
                    server.description = "Local Server"
                }
            }
        })
        config.registerPlugin(SwaggerPlugin())
        config.registerPlugin(ReDocPlugin())
        config.registerPlugin(micrometerPlugin)

    }.apply {
        exception(ApiException::class.java) { e, ctx ->
            val correlation = java.util.UUID.randomUUID().toString()
            val body = ErrorResponse(
                status = e.status.code,
                error = e.status.message,
                message = e.message,
                details = e.details,
                correlationId = correlation
            )
            ctx.status(e.status).json(body)
        }
        exception(org.valiktor.ConstraintViolationException::class.java) { e, ctx ->
            val api = ErrorTranslator.fromThrowable(e) as ApiException
            val correlation = java.util.UUID.randomUUID().toString()
            val body = ErrorResponse(
                status = api.status.code,
                error = api.status.message,
                message = api.message,
                details = api.details,
                correlationId = correlation
            )
            ctx.status(api.status).json(body)
        }
        exception(Exception::class.java) { e, ctx ->
            val api = ErrorTranslator.fromThrowable(e)
            val correlation = java.util.UUID.randomUUID().toString()
            val body = ErrorResponse(
                status = api.status.code,
                error = api.status.message,
                message = api.message,
                details = api.details,
                correlationId = correlation
            )
            ctx.status(api.status).json(body)
        }
        error(HttpStatus.NOT_FOUND) { ctx -> ctx.json(mapOf("status" to 404, "error" to "Not Found")) }
    }.start(7070)

    //Registering the routes
    Routes.registerRoutes(app, registry)

    //Displaying the server started message displaying the unprotected server url for quick check
    log.info("Check out Openapi.json at http://localhost:7070/javalin/api/openapi")
    log.info("Check out ReDoc docs at http://localhost:7070/javalin/api/redoc")
    log.info("Check out Swagger UI docs at http://localhost:7070/javalin/api/swagger")
    log.info("Check out Prometheus scrap endpoint http://localhost:7070/javalin/api/prometheus")
    log.info("Check out Prometheus scrap endpoint http://localhost:7070/javalin/api/health")
    log.info("Check out Prometheus scrap endpoint http://localhost:7070/javalin/api/liveness")
    log.info("Check out Prometheus scrap endpoint http://localhost:7070/javalin/api/readiness")

}



