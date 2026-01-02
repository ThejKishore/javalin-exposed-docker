// No plugins block needed as it is applied in root build.gradle.kts

dependencies {
    // JDBI for DB access used by AppJdbi. Exposed as api because AppJdbi returns Jdbi type
    api(platform(libs.jdbiBom))
    api(libs.jdbiCore)
    api(libs.jdbiKotlin)

    // HikariCP pool used by AppJdbi
    implementation(libs.hikariCp)

    // Config models (AppConfig, DatabaseConfig) validated via valiktor
    implementation(libs.hopliteCore)
    implementation(libs.hopliteYaml)
    implementation(libs.valiktorCore)

    // KubeRoutes uses Javalin Context and Micrometer Prometheus registry
    implementation(libs.micrometerPrometheus)

    // Logging used indirectly in apps
    implementation(libs.slf4jSimple)

    testRuntimeOnly(libs.h2)
}


