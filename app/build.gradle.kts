import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java")
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.spring.dependency.management)
}

group = "com.tk.learn"
version = "1.0-SNAPSHOT"

repositories {
    maven { url = uri(".maven-repo") }
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    jvmToolchain(21)
}


dependencies {
    implementation(project(":shared"))
    implementation(project(":user"))

    implementation(libs.javalin)
    implementation(libs.slf4jSimple)

    // Kotlin stdlib (explicit)
    implementation(libs.kotlinStdlib)

    // H2 Database (runtime for app)
    runtimeOnly(libs.h2)

    // Config from hoplite
    implementation(libs.hopliteCore)
    implementation(libs.hopliteYaml)

    // Validator
    implementation(libs.valiktorCore)

    // OpenApi3
    kapt(libs.openapiApt)
    implementation(libs.openapiPlugin) // for /openapi route with JSON scheme
    implementation(libs.openapiSwagger) // for Swagger UI
    implementation(libs.openapiRedoc)

    // Micrometer and prometheus
    implementation(libs.javalinMicrometer)
    implementation(libs.micrometerPrometheus)

    // JSON
    implementation(libs.jacksonDatabind)
    implementation(libs.jacksonKotlin)

    // Test
    testImplementation(platform(libs.junitBom))
    testImplementation(libs.junitJupiter)
    testImplementation(libs.mockk)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
