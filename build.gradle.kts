import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("javalin-lib")
    id("consolidatedJacoco")
    alias(libs.plugins.shadow)
}

group = "com.tk.learn"
version = "1.0-SNAPSHOT"

repositories {
    maven { url = uri(".maven-repo") }
    mavenCentral()
}

allprojects {
    group = "com.tk.learn"
    version = "1.0-SNAPSHOT"

    repositories {
        maven { url = uri(".maven-repo") }
        maven { url = uri("https://repo.maven.apache.org/maven2") }
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "javalin-lib")
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":user"))

    implementation(libs.slf4jSimple)

    // HikariCP Connection Pool
    implementation(libs.hikariCp)

    // H2 Database (runtime for app)
    runtimeOnly(libs.h2)

    // Config from hoplite
    implementation(libs.hopliteCore)
    implementation(libs.hopliteYaml)

    // Validator
    implementation(libs.valiktorCore)

    // OpenApi3 plugins (annotations/processor in javalin-lib)
    implementation(libs.openapiPlugin) // for /openapi route with JSON scheme
    implementation(libs.openapiSwagger) // for Swagger UI
    implementation(libs.openapiRedoc)

    // Micrometer and prometheus
    implementation(libs.javalinMicrometer)
    implementation(libs.micrometerPrometheus)
}

tasks.test {
    useJUnitPlatform()
}

// Configure ShadowJar to build a fat jar including all dependencies
tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("all")
    mergeServiceFiles()
    manifest {
        attributes(
            mapOf(
                "Main-Class" to "com.tk.learn.bootstrap.MainKt"
            )
        )
    }
}

// Optionally, make the standard build also assemble the shadow jar
tasks.named("build") {
    dependsOn(tasks.named("shadowJar"))
}

// Removed duplicate blocks
