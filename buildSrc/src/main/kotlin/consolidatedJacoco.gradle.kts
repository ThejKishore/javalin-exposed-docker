plugins {
    id("io.freefair.aggregate-jacoco-report")
}

// Ensure project tests run before generating reports in this project
tasks.named("jacocoTestReport") {
    dependsOn(tasks.named("test"))
}

// Configure the aggregate report produced by FreeFair plugin
tasks.named<JacocoReport>("aggregateJacocoReport") {
    group = "Reporting"
    description = "Generates an aggregate jacoco report from all subprojects"

    // Run all subproject tests before aggregating coverage
    dependsOn(subprojects.map { it.tasks.withType<Test>() })

    reports {
        html.required.set(true)
        xml.required.set(true)
        csv.required.set(false)

        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/aggregate/html"))
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/aggregate/jacoco.xml"))
    }
}

// Aggregate test report (HTML) across modules
tasks.register("aggregateTestReport", TestReport::class) {
    group = "Reporting"
    description = "Generates an aggregate test report from all subprojects and parent project"

    destinationDirectory.set(layout.buildDirectory.dir("reports/tests/aggregate"))
    testResults.from(subprojects.map { it.tasks.withType<Test>() })
}

// Coverage verification — depends on the aggregate report
tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    enabled = true
    dependsOn(tasks.named("aggregateJacocoReport"))
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal() // 80% line coverage
            }
        }
        rule {
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal() // 80% conditional (branch) coverage
            }
        }
    }
}


tasks.named("check") {
    dependsOn(tasks.named("sonarlintAll"))
    dependsOn(tasks.named("aggregateTestReport"))
    dependsOn(tasks.named("jacocoTestCoverageVerification"))
}
