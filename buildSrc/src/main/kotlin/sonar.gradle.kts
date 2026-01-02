plugins{
    id("name.remal.sonarlint")
}

// Ensure SonarLint runs as part of the standard verification lifecycle
// Defer wiring until a 'check' task is available (added by 'base' / 'java' plugins)
pluginManager.withPlugin("base") {
    // Only wire dependency if SonarLint tasks exist in this project
    val wire: () -> Unit = {
        tasks.named("check").configure {
            // Depend on all SonarLint tasks (e.g., sonarlint, sonarlintMain, sonarlintTest, etc.)
            tasks.matching { it.name.startsWith("sonarlint") }.forEach { dependsOn(it) }
        }
    }
    // If sonarlint plugin is already applied, wire immediately; otherwise wait for it
    if (pluginManager.hasPlugin("name.remal.sonarlint")) {
        wire()
    } else {
        pluginManager.withPlugin("name.remal.sonarlint") { wire() }
    }
}

