# Javalin Example (Kotlin, JDK 21)

A multi-module Kotlin project demonstrating a production-ready Javalin HTTP API with:
- Modular architecture (app, shared, user)
- OpenAPI JSON + Swagger UI + ReDoc
- Health/liveness/readiness endpoints for Kubernetes
- Micrometer + Prometheus metrics
- Structured error handling and validation
- Simple in-memory H2 database setup via JDBI
- Dockerfile (multi-stage) and docker-compose for local runs


## Modules Overview

This Gradle multi-module project includes three modules:

- app
  - Bootstraps the Javalin server and config
  - Registers all routes and plugins (OpenAPI, Swagger, ReDoc, Micrometer)
  - Sets global exception handling and logging
  - Entry point: com.tk.learn.bootstrap.MainKt
  - Port: 7070 (configured in code); contextPath: /javalin/api

- shared
  - Cross-cutting infrastructure and domain types
  - com.tk.learn.infrastructure.AppJdbi: Inits H2 DB and schema using JDBI
  - com.tk.learn.shared.*: Error models, API exception translation, common models
  - com.tk.learn.kube.KubeRoutes: Kubernetes health/metrics routes

- user
  - Feature module holding user-facing routes and handlers
  - com.tk.learn.users.UserRoutes: CRUD-style endpoints

Gradle settings: see settings.gradle.kts where modules are included: :app, :shared, :user.


## How it works

- Application startup
  - Main.kt loads configuration from application.yaml using Hoplite
  - AppJDBi.init + createSchema set up an in-memory H2 DB
  - Micrometer Prometheus registry is created and registered
  - Javalin is created with:
    - contextPath = /javalin/api
    - OpenAPI, Swagger, ReDoc plugins
    - Micrometer metrics plugin
    - Request logging and virtual threads enabled
  - Routes.registerRoutes wires feature routes (user, kube) and a root handler

- Routing
  - Base URL: http://localhost:7070/javalin/api
  - User endpoints (from user module):
    - GET /users — list all users
    - GET /users/{id} — fetch user by id
    - POST /users — create user
    - PUT /users/{id} — update user
    - DELETE /users/{id} — delete user
  - Kubernetes/ops endpoints (from shared module via KubeRoutes):
    - GET /prometheus — Prometheus metrics scrape
    - GET /health — health check
    - GET /liveness — liveness probe
    - GET /readiness — readiness probe
  - Other:
    - GET / — simple Hello World (on the same contextPath)

- Observability and docs
  - OpenAPI JSON: GET /openapi
  - Swagger UI: GET /swagger
  - ReDoc: GET /redoc
  - Prometheus: GET /prometheus

- Error handling
  - ApiException and validation errors are translated into consistent JSON via ErrorResponse
  - Unhandled exceptions are mapped to a standard structure and status codes


## Build and Run

Prerequisites: JDK 21 (Temurin recommended). The project uses the Gradle wrapper.

- Build all modules and run tests:
  - macOS/Linux: ./gradlew clean build
  - Windows: gradlew.bat clean build

- Run locally (fat jar):
  1) Build the shadow jar (done as part of build):
     - ./gradlew :app:shadowJar
  2) Run:
     - java -jar app/build/libs/app-*-all.jar
  3) Access the API at http://localhost:7070/javalin/api

- Docker (multi-stage, JDK 21 -> JRE):
  - Build: docker build -t javalin-example:0.0.3 .
  - Run: docker run --rm -p 7070:7070 javalin-example:0.0.3

- docker-compose (recommended for local):
  - docker compose up --build
  - Access at http://localhost:7070/javalin/api


## Endpoints quick reference

Base path: http://localhost:7070/javalin/api

- Health and metrics
  - GET /health
  - GET /liveness
  - GET /readiness
  - GET /prometheus

- Users
  - GET /users
  - GET /users/{id}
  - POST /users
  - PUT /users/{id}
  - DELETE /users/{id}

- Docs
  - GET /openapi
  - GET /swagger
  - GET /redoc


## Configuration

- app/src/main/resources/application.yaml is loaded by Hoplite. You can add DB or other configs here.
- docker-compose sets JAVA_TOOL_OPTIONS to tune container memory behavior.


## Project structure (simplified)

- app
  - src/main/kotlin/com/tk/learn/bootstrap/Main.kt — application entrypoint
  - src/main/kotlin/com/tk/learn/bootstrap/Routes.kt — route wiring
  - src/main/resources/application.yaml — config (optional)
- shared
  - src/main/kotlin/com/tk/learn/infrastructure/AppJdbi.kt — H2 + JDBI bootstrapping
  - src/main/kotlin/com/tk/learn/kube/KubeRoutes.kt — ops endpoints
  - src/main/kotlin/com/tk/learn/shared/* — errors and shared models
- user
  - src/main/kotlin/com/tk/learn/users/UserRoutes.kt — user API handlers


## Testing

- Run all tests: ./gradlew test
- Sample tests live across app/shared/user module test directories.


## Notes and Tips

- If the server is running but health check fails in compose, ensure port 7070 is free.
- The compose healthcheck probes GET /javalin/api/. You should see a 200 response line.
- When changing dependencies, prefer editing gradle/libs.versions.toml and the module build.gradle.kts.


## License

This project is for learning/demo purposes.


### Snowflake with hikari-cp

* (https://docs.snowflake.com/en/user-guide/data-integration/openflow/controllers/hikaricpconnectionpool)[Snowflake docs]
* (https://medium.com/@gudise.ashok/integrating-hikari-connection-pool-for-reliable-snowflake-database-connections-7e806fae95be)[Docreference]