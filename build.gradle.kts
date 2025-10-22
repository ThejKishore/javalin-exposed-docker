// Root aggregator for multi-module build

allprojects {
    group = "com.tk.learn"
    version = "1.0-SNAPSHOT"

    repositories {
        maven { url = uri(".maven-repo") }
        mavenCentral()
    }
}

// No plugins or source code in root project; see subprojects :app, :shared, :user