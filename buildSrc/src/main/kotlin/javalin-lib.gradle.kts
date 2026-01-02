plugins {
    java
    `java-library`
    jacoco
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.kapt")
    id("io.spring.dependency-management")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    jvmToolchain(21)
}

// Access the version catalog
val libs = project.rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    implementation(libs.findLibrary("javalin").get())
    implementation(libs.findLibrary("koinCore").get())
    implementation(libs.findLibrary("koinLoggerSlf4j").get())
    implementation(libs.findLibrary("kotlinStdlib").get())

    kapt(libs.findLibrary("openapiApt").get())

    // JSON
    implementation(libs.findLibrary("jacksonDatabind").get())
    implementation(libs.findLibrary("jacksonKotlin").get())

    // Test
    testImplementation(platform(libs.findLibrary("junitBom").get()))
    testImplementation(libs.findLibrary("junitJupiter").get())
    testImplementation(libs.findLibrary("mockk").get())
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
