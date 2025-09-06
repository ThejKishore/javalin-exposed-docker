package com.tk.learn

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addResourceSource
import io.javalin.Javalin
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.openapi.*
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
    Db.init(appConfig.database)

    //Registering the promethus and setting it in MicrometerPlugin
    val registry = MetricsConfig.registerPrometheus()
    val micrometerPlugin = MetricsConfig.registerMetrics(registry)

    Javalin.create { config ->
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
        exception(Exception::class.java) { e, ctx -> e.printStackTrace() }
        error(HttpStatus.NOT_FOUND) { ctx -> ctx.json("not found") }
    }.start(7070)
        .get("/"){ctx -> ctx.result("Hello World")}
        .get("/users", ::fetchUsers)
        .get("/users/{id}", ::getUserById)
        .post("/users", ::createUser)
        .put("/users/{id}", ::updateUser)
        .delete("/users/{id}", ::deleteUser)
        .get("/test/{name}"){ ctx ->
            val name = ctx.pathParam("name")
            ctx.result("Hello $name")
        }
        .get("/prometheus"){
            ctx -> ctx.contentType("text/plain; version=0.0.4; charset=utf-8")
                .result(registry.scrape())
        }

    log.info("Check out ReDoc docs at http://localhost:7070/javalin/api/redoc")
    log.info("Check out Swagger UI docs at http://localhost:7070/javalin/api/swagger")
    log.info("Check out Prometheus scrap endpoint http://localhost:7070/javalin/api/prometheus")

}



