plugins {
    alias(libs.plugins.kotlin.jvm)
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
    // JDBI for DB access used by AppJdbi. Exposed as api because AppJdbi returns Jdbi type
    api(platform(libs.jdbiBom))
    api(libs.jdbiCore)
    api(libs.jdbiKotlin)

    // Config models (AppConfig, DatabaseConfig) validated via valiktor
    implementation(libs.hopliteCore)
    implementation(libs.hopliteYaml)
    implementation(libs.valiktorCore)

    // KubeRoutes uses Javalin Context and Micrometer Prometheus registry
    implementation(libs.javalin)
    implementation(libs.micrometerPrometheus)

    // Logging used indirectly in apps
    implementation(libs.slf4jSimple)

    testImplementation(platform(libs.junitBom))
    testImplementation(libs.junitJupiter)
    testImplementation(libs.mockk)
    testRuntimeOnly(libs.h2)
}

tasks.test {
    useJUnitPlatform()
}
