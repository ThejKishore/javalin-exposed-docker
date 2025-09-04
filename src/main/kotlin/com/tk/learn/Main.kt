package com.tk.learn

import io.javalin.Javalin
import io.javalin.http.Context

val log = org.slf4j.LoggerFactory.getLogger("Main")!!

fun main() {
    //Initializing the h2 database using exposed kotlin framework
    Db.init()

    Javalin.create{
        it.contextResolver.ip = { ctx -> ctx.header("X-Forwarded-For") ?: ctx.req().remoteAddr }
        it.router.treatMultipleSlashesAsSingleSlash = true
        it.router.ignoreTrailingSlashes = true
        it.router.contextPath = "/javalin/api"
        it.requestLogger.http { ctx, ms -> log.info("Request from Ip :${ctx.req().remoteAddr} , Method: ${ctx.method()} Url Path :${ctx.path()} took $ms ms") }
        it.useVirtualThreads = true
        it.showJavalinBanner = false
    }.start(7070)
    .get("/", ::helloWorld )
    .get("/users",::fetchUsers)
    .get("/users/{id}", ::getUserById)
    .post("/users", ::createUser)
    .put("/users/{id}", ::updateUser)
    .delete("/users/{id}", ::deleteUser)
    .get("/test/{name}",::greetUser)
}

fun helloWorld(ctx: Context) = ctx.result("Hello World")

fun greetUser(ctx: Context) {
    val name = ctx.pathParam("name")
    ctx.result("Hello $name")
}

fun fetchUsers(ctx: Context){
    ctx.json(Db.listUsers())
}

