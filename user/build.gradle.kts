// No plugins block needed as it is applied in root build.gradle.kts

dependencies {
    implementation(project(":shared"))

    // JDBI used directly in user routes
    implementation(platform(libs.jdbiBom))
    implementation(libs.jdbiCore)
    implementation(libs.jdbiKotlin)

    // OpenAPI plugin for route docs (annotation processor is in javalin-lib)
    implementation(libs.openapiPlugin)

    testRuntimeOnly(libs.h2)
}
