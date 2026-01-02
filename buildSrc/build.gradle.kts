plugins{
    `kotlin-dsl`
}

repositories{
    gradlePluginPortal()
    mavenCentral()
}


dependencies{
    implementation(libs.sonarlint.plugin)
    implementation(libs.freefair.aggregate.jacoco.plugin)
    implementation(libs.palantir.checkstyle.plugin)
    implementation(libs.palantir.idea.plugin)
    implementation(libs.graalVm.plugin)

    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
    implementation("io.spring.gradle:dependency-management-plugin:${libs.versions.springDepMgmt.get()}")

    // JGit for Git operations from versioning plugin
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.10.0.202406032230-r")
}