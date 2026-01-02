import java.io.File
import java.util.Properties
import org.gradle.api.GradleException
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider

plugins {
    // no external plugins needed; this is a precompiled script plugin
}

// Utilities for semantic version handling
data class SemVer(var major: Int, var minor: Int, var patch: Int, var snapshot: Boolean) {
    override fun toString(): String = buildString {
        append("$major.$minor.$patch")
        if (snapshot) append("-SNAPSHOT")
    }
}

fun parseVersion(raw: String?): SemVer {
    val v = raw?.trim().orEmpty().ifEmpty { "0.1.0-SNAPSHOT" }
    val snapshot = v.endsWith("-SNAPSHOT", ignoreCase = true)
    val core = if (snapshot) v.removeSuffix("-SNAPSHOT") else v
    val parts = core.split('.').mapNotNull { it.toIntOrNull() }
    val (maj, min, pat) = when (parts.size) {
        3 -> Triple(parts[0], parts[1], parts[2])
        2 -> Triple(parts[0], parts[1], 0)
        1 -> Triple(parts[0], 0, 0)
        else -> Triple(0, 1, 0)
    }
    return SemVer(maj, min, pat, snapshot)
}

fun readRootProperties(root: Project): Pair<File, Properties> {
    val propsFile = root.rootDir.resolve("gradle.properties")
    val props = Properties()
    if (propsFile.exists()) propsFile.inputStream().use { props.load(it) }
    return propsFile to props
}

fun writeProperties(file: File, props: Properties) {
    file.outputStream().use { out ->
        props.store(out, "Updated by versions plugin")
    }
}

fun detectBranch(project: Project): String {
    // Priority: -PbranchName, env vars, git command, fallback
    project.findProperty("branchName")?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    val env = System.getenv()
    listOf(
        env["GITHUB_REF_NAME"],
        env["BRANCH_NAME"],
        env["CI_COMMIT_REF_NAME"],
        env["GIT_BRANCH"],
        env["BUILD_SOURCEBRANCHNAME"]
    ).firstOrNull { !it.isNullOrBlank() }?.let { return it }
    return try {
        val proc = ProcessBuilder("git", "rev-parse", "--abbrev-ref", "HEAD")
            .directory(project.rootDir)
            .redirectErrorStream(true)
            .start()
        val out = proc.inputStream.bufferedReader().readText().trim()
        if (proc.waitFor() == 0 && out.isNotBlank()) out else "develop"
    } catch (e: Exception) {
        "develop"
    }
}

fun incrementPatch(v: SemVer, snapshot: Boolean? = null): SemVer = v.copy(patch = v.patch + 1, snapshot = snapshot ?: v.snapshot)
fun incrementMinor(v: SemVer, snapshot: Boolean? = null): SemVer = v.copy(minor = v.minor + 1, patch = 0, snapshot = snapshot ?: v.snapshot)
fun incrementMajor(v: SemVer, snapshot: Boolean? = null): SemVer = v.copy(major = v.major + 1, minor = 0, patch = 0, snapshot = snapshot ?: v.snapshot)

// Writes computed version to gradle.properties under the key 'version'
fun setRootVersion(project: Project, newVersion: String, dryRun: Boolean = false) {
    val (propsFile, props) = readRootProperties(project.rootProject)
    val old = props.getProperty("version")
    props.setProperty("version", newVersion)
    if (dryRun) {
        project.logger.lifecycle("[versions] DRY-RUN: version would change from '${old ?: "<none>"}' to '$newVersion'")
        return
    }
    writeProperties(propsFile, props)
    project.logger.lifecycle("[versions] Updated version: ${old ?: "<none>"} -> $newVersion (${propsFile.relativeTo(project.rootDir)})")
}

val printVersion by tasks.registering {
    group = "versioning"
    description = "Print the current project version from gradle.properties"
    doLast {
        val (_, props) = readRootProperties(project.rootProject)
        val current = props.getProperty("version") ?: "<not set>"
        println(current)
    }
}

val bumpPatchTask by tasks.registering {
    group = "versioning"
    description = "Increment patch version (x.y.z -> x.y.(z+1)); preserves -SNAPSHOT unless overridden by -Psnapshot=[true|false]"
    doLast {
        val branch = detectBranch(project)
        val (_, props) = readRootProperties(project.rootProject)
        val current = parseVersion(props.getProperty("version"))
        val snapOverride = project.findProperty("snapshot")?.toString()?.toBooleanStrictOrNull()
        val next = incrementPatch(current, snapOverride)
        val dry = project.hasProperty("dryRun")
        if (next.toString() != current.toString()) {
            setRootVersion(project, next.toString(), dry)
            gitVersionFlow(project, branch, next.toString(), dry)
        } else {
            project.logger.lifecycle("[versions] Version unchanged (${current}); skipping Git commit/push")
        }
    }
}

val bumpMinorTask by tasks.registering {
    group = "versioning"
    description = "Increment minor version (x.y.z -> x.(y+1).0); preserves -SNAPSHOT unless overridden by -Psnapshot=[true|false]"
    doLast {
        val branch = detectBranch(project)
        val (_, props) = readRootProperties(project.rootProject)
        val current = parseVersion(props.getProperty("version"))
        val snapOverride = project.findProperty("snapshot")?.toString()?.toBooleanStrictOrNull()
        val next = incrementMinor(current, snapOverride)
        val dry = project.hasProperty("dryRun")
        if (next.toString() != current.toString()) {
            setRootVersion(project, next.toString(), dry)
            gitVersionFlow(project, branch, next.toString(), dry)
        } else {
            project.logger.lifecycle("[versions] Version unchanged (${current}); skipping Git commit/push")
        }
    }
}

val bumpMajorTask by tasks.registering {
    group = "versioning"
    description = "Increment major version ((x+1).0.0); preserves -SNAPSHOT unless overridden by -Psnapshot=[true|false]"
    doLast {
        val branch = detectBranch(project)
        val (_, props) = readRootProperties(project.rootProject)
        val current = parseVersion(props.getProperty("version"))
        val snapOverride = project.findProperty("snapshot")?.toString()?.toBooleanStrictOrNull()
        val next = incrementMajor(current, snapOverride)
        val dry = project.hasProperty("dryRun")
        if (next.toString() != current.toString()) {
            setRootVersion(project, next.toString(), dry)
            gitVersionFlow(project, branch, next.toString(), dry)
        } else {
            project.logger.lifecycle("[versions] Version unchanged (${current}); skipping Git commit/push")
        }
    }
}

val updateVersion by tasks.registering {
    group = "versioning"
    description = "Branch-aware version bump: feature/* -> patch+SNAPSHOT; develop -> patch (no SNAPSHOT); master/main -> minor (no SNAPSHOT)"
    doLast {
        val branch = detectBranch(project)
        val (_, props) = readRootProperties(project.rootProject)
        val current = parseVersion(props.getProperty("version"))
        val next = when {
            branch.startsWith("feature/") || branch.equals("feature", ignoreCase = true) -> incrementPatch(current, true)
            branch.equals("develop", ignoreCase = true) -> incrementPatch(current, false)
            branch.equals("master", ignoreCase = true) || branch.equals("main", ignoreCase = true) -> incrementMinor(current, false)
            else -> {
                // default behavior: conservative patch bump, preserve snapshot
                project.logger.lifecycle("[versions] Unrecognized branch '$branch'; defaulting to patch bump (preserve snapshot)")
                incrementPatch(current, current.snapshot)
            }
        }
        val dry = project.hasProperty("dryRun")
        if (next.toString() != current.toString()) {
            setRootVersion(project, next.toString(), dry)
            gitVersionFlow(project, branch, next.toString(), dry)
        } else {
            project.logger.lifecycle("[versions] Version unchanged (${current}); skipping Git commit/push")
        }
    }
}

// Short, user-friendly task names
val bumpPatch by tasks.registering {
    group = "versioning"
    description = (bumpPatchTask.get().description ?: "")
    dependsOn(bumpPatchTask)
}
val bumpMinor by tasks.registering {
    group = "versioning"
    description = (bumpMinorTask.get().description ?: "")
    dependsOn(bumpMinorTask)
}
val bumpMajor by tasks.registering {
    group = "versioning"
    description = (bumpMajorTask.get().description ?: "")
    dependsOn(bumpMajorTask)
}

// --- Git/JGit helpers for committing and pushing version changes ---

fun Project.isTrueProp(name: String): Boolean =
    (findProperty(name)?.toString()?.equals("true", ignoreCase = true) == true)

fun openGit(project: Project): Git? = try {
    val repo = FileRepositoryBuilder()
        .findGitDir(project.rootDir)
        .build()
    Git(repo)
} catch (e: Exception) {
    project.logger.warn("[versions] Git repository not found: ${e.message}")
    null
}

fun ensureOnBranch(git: Git, branch: String, logger: org.gradle.api.logging.Logger) {
    val repo = git.repository
    val current = repo.branch
    if (current == branch) return

    val ref = repo.findRef("refs/heads/$branch")
    if (ref == null) {
        // try to create local branch from origin if exists; otherwise create new empty branch at HEAD
        try {
            git.checkout()
                .setCreateBranch(true)
                .setName(branch)
                .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
                .setStartPoint("origin/$branch")
                .call()
            logger.lifecycle("[versions] Checked out new local branch '$branch' tracking 'origin/$branch'")
            return
        } catch (_: Exception) {
            // fallback: create from current HEAD without upstream
            git.checkout()
                .setCreateBranch(true)
                .setName(branch)
                .call()
            logger.lifecycle("[versions] Created and switched to new local branch '$branch'")
            return
        }
    } else {
        git.checkout().setName(branch).call()
        logger.lifecycle("[versions] Switched branch: $current -> $branch")
    }
}

fun stageCommitPush(
    project: Project,
    git: Git,
    files: List<String>,
    newVersion: String,
) {
    val logger = project.logger

    // Status check: allow uncommitted only for the files we will commit if flag not set
    val allowUncommitted = project.isTrueProp("allowUncommitted")
    val status = git.status().call()
    if (!allowUncommitted) {
        val dirtyOtherThanTarget = (status.uncommittedChanges + status.untracked).filter { it !in files }
        if (dirtyOtherThanTarget.isNotEmpty()) {
            throw GradleException("Working tree is dirty. Commit or stash changes before bumping version. Dirty files: ${dirtyOtherThanTarget.joinToString()}")
        }
    }

    // add target files
    val add = git.add()
    files.forEach { add.addFilepattern(it) }
    add.call()

    val authorName = (project.findProperty("gitAuthorName")?.toString())
        ?: System.getenv("GIT_AUTHOR_NAME")
        ?: System.getenv("GIT_COMMITTER_NAME")
        ?: "Automation Bot"
    val authorEmail = (project.findProperty("gitAuthorEmail")?.toString())
        ?: System.getenv("GIT_AUTHOR_EMAIL")
        ?: System.getenv("GIT_COMMITTER_EMAIL")
        ?: "automation@example.com"

    git.commit()
        .setMessage("chore(version): bump to $newVersion")
        .setAuthor(PersonIdent(authorName, authorEmail))
        .call()
    logger.lifecycle("[versions] Committed version change to $newVersion")

    // push
    val token = (project.findProperty("gitToken")?.toString()) ?: System.getenv("GIT_TOKEN")
    val user = (project.findProperty("gitUsername")?.toString()) ?: System.getenv("GIT_USERNAME") ?: "git"
    val pass = (project.findProperty("gitPassword")?.toString()) ?: System.getenv("GIT_PASSWORD")
    val provider = when {
        token != null -> UsernamePasswordCredentialsProvider(user, token)
        pass != null -> UsernamePasswordCredentialsProvider(user, pass)
        else -> null
    }

    val pushCommand = git.push().setPushAll()
    if (provider != null) pushCommand.setCredentialsProvider(provider)
    pushCommand.call()
    logger.lifecycle("[versions] Pushed version change to remote")
}

fun gitVersionFlow(
    project: Project,
    branch: String,
    newVersion: String,
    dryRun: Boolean
) {
    val logger = project.logger
    if (dryRun) {
        logger.lifecycle("[versions] DRY-RUN: would checkout '$branch', commit gradle.properties, and push new version $newVersion")
        return
    }
    if (project.isTrueProp("noGit")) {
        logger.lifecycle("[versions] Git operations disabled (-PnoGit=true)")
        return
    }

    val git = openGit(project) ?: run {
        logger.warn("[versions] Skipping Git operations — repository not found")
        return
    }
    
    git.use {
        ensureOnBranch(it, branch, logger)
        stageCommitPush(project, it, listOf("gradle.properties"), newVersion)
    }
}
