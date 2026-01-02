plugins {
    // convention plugin published from buildSrc with id "docker"
}

abstract class GenerateDockerfile : DefaultTask() {
    @get:Input
    abstract val buildBaseImage: Property<String>

    @get:Input
    abstract val runtimeBaseImage: Property<String>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    val templateFile: File = run {
        // Load resource from buildSrc classpath to a temp file to participate in up-to-date checks
        val res = javaClass.classLoader.getResource("Dockerfile.jlink")
            ?: throw IllegalStateException("Resource Dockerfile.jlink not found in buildSrc resources")
        val tmp = project.layout.buildDirectory.file("tmp/docker/Dockerfile.jlink").get().asFile
        tmp.parentFile.mkdirs()
        tmp.writeText(res.readText())
        tmp
    }

    @get:OutputFile
    val outputFile: File = project.rootProject.layout.projectDirectory.file("Dockerfile").asFile

    init {
        group = "docker"
        description = "Generate or update root Dockerfile from Dockerfile.jlink template using configured base images"
    }

    @TaskAction
    fun generate() {
        val templateText = templateFile.readText()
        val buildImg = buildBaseImage.get()
        val runImg = runtimeBaseImage.get()

        fun renderFromTemplate(text: String): String {
            var out = text
            if (out.contains("\${BUILD_BASE_IMAGE}") || out.contains("\${RUNTIME_BASE_IMAGE}")) {
                out = out.replace("\${BUILD_BASE_IMAGE}", buildImg)
                    .replace("\${RUNTIME_BASE_IMAGE}", runImg)
            } else {
                // No placeholders — replace known FROM lines by stage
                out = out.replace(Regex("^FROM\\s+.+\\s+AS\\s+build$", RegexOption.MULTILINE), "FROM ${buildImg} AS build")
                out = out.replace(Regex("^FROM\\s+.+\\s+AS\\s+jre-builder$", RegexOption.MULTILINE), "FROM ${buildImg} AS jre-builder")
                out = out.replace(Regex("^FROM\\s+.+\\s+AS\\s+runtime-base$", RegexOption.MULTILINE), "FROM ${runImg} AS runtime-base")
            }
            return out
        }

        val rendered = renderFromTemplate(templateText)

        // If file exists and appears to be in sync (FROM lines match), keep it; else write new content
        if (outputFile.exists()) {
            val current = outputFile.readText()
            val wantFroms = listOf(
                // We consider either legacy 'build' stage or the new 'jre-builder-deps' stage acceptable
                "FROM ${buildImg} AS jre-builder-deps",
                "FROM ${buildImg} AS jre-builder",
                "FROM ${runImg} AS runtime-base"
            )
            val inSync = wantFroms.all { current.contains(it) }
            if (!inSync) {
                outputFile.writeText(rendered)
                logger.lifecycle("Updated root Dockerfile with new base images: build='${buildImg}', runtime='${runImg}'")
            } else {
                // Nothing to do
                logger.info("Dockerfile already up-to-date with configured base images")
            }
        } else {
            outputFile.parentFile.mkdirs()
            outputFile.writeText(rendered)
            logger.lifecycle("Generated root Dockerfile from template using base images: build='${buildImg}', runtime='${runImg}'")
        }
    }
}

val dockerExt = extensions.create("dockerConfig", DockerConfig::class.java)

open class DockerConfig {
    var imageName: String? = null
    var imageTag: String? = null
    var registry: String? = null
}

val buildBaseImageProp = providers.gradleProperty("docker.buildBaseImage").orElse("eclipse-temurin:21-jdk-jammy")
val runtimeBaseImageProp = providers.gradleProperty("docker.runtimeBaseImage").orElse("gcr.io/distroless/base-debian12:nonroot")
val imageNameProp = providers.gradleProperty("docker.imageName").orElse(rootProject.name)
val imageTagProp = providers.gradleProperty("docker.imageTag").orElse("latest")
val registryProp = providers.gradleProperty("docker.registry").orElse("")

val dockerExecutable = providers.systemProperty("docker.executable")
    .orElse(providers.environmentVariable("DOCKER_EXECUTABLE"))
    .orElse("/usr/local/bin/docker")

val validateDocker = tasks.register<Exec>("validateDocker") {
    group = "docker"
    description = "Validate that Docker daemon/CLI is available and running"
    commandLine(dockerExecutable.get(), "info")
}

val generateDockerfile = tasks.register("generateDockerfile", GenerateDockerfile::class) {
    buildBaseImage.set(buildBaseImageProp)
    runtimeBaseImage.set(runtimeBaseImageProp)
}

val dockerBuildImage = tasks.register<Exec>("dockerBuildImage") {
    group = "docker"
    description = "Builds Docker image using the generated Dockerfile"
    // Ensure the host jar exists before building the image
    dependsOn(":bootJar")
    dependsOn(generateDockerfile)
    dependsOn(validateDocker)
    workingDir = project.rootDir
    doFirst {
        val name = imageNameProp.get()
        val tag = imageTagProp.get()
        // Verify at least one jar exists; Dockerfile expects build/libs/*.jar from host
        val libsDir = project.layout.projectDirectory.dir("build/libs").asFile
        val jars = libsDir.listFiles { f -> f.isFile && f.name.endsWith(".jar") }?.toList() ?: emptyList()
        if (jars.isEmpty()) {
            throw GradleException("No JAR found under build/libs. Run ':bootJar' first or check the build.")
        }
        commandLine(
            dockerExecutable.get(),
            "build",
            "--build-arg", "APP_JAR=build/libs/*.jar",
            "-t", "$name:$tag",
            "-f", "Dockerfile",
            "."
        )
    }
}

val dockerTagImage = tasks.register<Exec>("dockerTagImage") {
    group = "docker"
    description = "Tags the local image with the registry prefix"
    dependsOn(dockerBuildImage)
    onlyIf { registryProp.get().isNotBlank() }
    doFirst {
        val name = imageNameProp.get()
        val tag = imageTagProp.get()
        val registry = registryProp.get()
        val remote = "$registry/$name:$tag"
        commandLine(dockerExecutable.get(), "tag", "$name:$tag", remote)
        logger.lifecycle("Tagged image as $remote")
    }
}

val dockerPushImage = tasks.register<Exec>("dockerPushImage") {
    group = "docker"
    description = "Pushes the tagged image to the configured container registry"
    dependsOn(validateDocker)
    dependsOn(dockerTagImage)
    onlyIf { registryProp.get().isNotBlank() }
    doFirst {
        val name = imageNameProp.get()
        val tag = imageTagProp.get()
        val registry = registryProp.get()
        val remote = "$registry/$name:$tag"
        commandLine(dockerExecutable.get(), "push", remote)
        logger.lifecycle("Pushed image $remote")
    }
}
