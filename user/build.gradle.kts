plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.kapt)
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

    implementation(libs.javalin)

    // JDBI used directly in user routes
    implementation(platform(libs.jdbiBom))
    implementation(libs.jdbiCore)
    implementation(libs.jdbiKotlin)

    // JSON
    implementation(libs.jacksonDatabind)
    implementation(libs.jacksonKotlin)

    // OpenAPI annotations for route docs
    kapt(libs.openapiApt)
    implementation(libs.openapiPlugin)

    testImplementation(platform(libs.junitBom))
    testImplementation(libs.junitJupiter)
    testRuntimeOnly(libs.h2)
}

tasks.test {
    useJUnitPlatform()
}
