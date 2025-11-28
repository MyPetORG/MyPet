import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.gradle.api.attributes.Usage
import java.net.URI

plugins {
    java
    id("com.gradleup.shadow") version "9.2.2"
    id("io.freefair.lombok") version "9.0.0"
    id("io.typst.gradlesource.spigot") version "2.0.0" apply false
    id ("io.sentry.jvm.gradle") version "5.12.2"
    `maven-publish`
}

group = "de.keyle"

val buildType = project.findProperty("buildType")?.toString() ?: "dev"
val buildNumber = project.findProperty("BUILD_NUMBER")?.toString() ?: "local"
val baseVersion = "4.0.0"
val versionSuffix = if (buildType == "dev") {
    if (buildNumber == "local") "-SNAPSHOT-local" else "-SNAPSHOT-b${buildNumber}"
} else ""
version = "$baseVersion$versionSuffix"
val minecraftVersion by extra("1.21.10")

val nmsModules: List<String> = File(rootDir, "nms")
    .listFiles()
    ?.filter { it.isDirectory && it.name.matches(Regex("v[\\d_]+R\\d+")) }
    ?.map { ":nms:${it.name}" }
    ?: emptyList()

val bukkitPackets by extra(
    File(rootDir, "nms")
        .listFiles()
        ?.filter { it.isDirectory && it.name.matches(Regex("v[\\d_]+R\\d+")) }
        ?.map { it.name }
        ?.sorted()
        ?.joinToString(";")
        ?: ""
)

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://hub.spigotmc.org/nexus/content/groups/public/")
    maven("https://repo.mypet-plugin.de/")
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "io.freefair.lombok")
    apply(plugin = "io.sentry.jvm.gradle")

    repositories {
        mavenCentral()
        mavenLocal()
        maven { url = uri("https://mvn.lib.co.nz/spigot/") }
        maven { url = uri("https://repo.md-5.net/content/groups/public/") }
        maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
        maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }
        maven { url = uri("https://repo.codemc.io/repository/maven-snapshots/") }
        maven { url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/") }
        maven { url = uri("https://oss.sonatype.org/content/groups/public/") }
        maven { url = uri("https://maven.enginehub.org/repo/") }
        maven { url = uri("https://hub.spigotmc.org/nexus/content/groups/public/") }
        maven { url = uri("https://repo.md-5.net/content/repositories/public") }
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://repo.mypet-plugin.de/") }
    }

    tasks.processResources { enabled = false }
    tasks.test { enabled = false }
    tasks.compileTestJava { enabled = false }
    tasks.processTestResources { enabled = false }

    plugins.withId("maven-publish") {
        tasks.withType<PublishToMavenRepository>().configureEach { enabled = false }
        tasks.withType<PublishToMavenLocal>().configureEach { enabled = false }
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }
}

val archivesBaseName = "MyPet"

val filteringProps = mapOf(
    "project" to project,
    "buildNumber" to buildNumber,
    "gitCommit" to (System.getenv("GIT_COMMIT") ?: ""),
    "minecraft" to mapOf("version" to minecraftVersion),
    "bukkit" to mapOf("packets" to bukkitPackets),
    "mypetVersion" to version,
    "timestamp" to DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(LocalDateTime.now()),
)


val downloadVersionmatcher by tasks.register("downloadVersionmatcher") {
    val dest = layout.projectDirectory.file("src/main/resources/versionmatcher.csv")
    outputs.file(dest)
    doLast {
        dest.asFile.parentFile.mkdirs()
        val url = URI("https://raw.githubusercontent.com/MyPetORG/MyPet/versionmatcher/versionmatcher.csv").toURL()
        url.openStream().use { input ->
            dest.asFile.outputStream().use { out -> input.copyTo(out) }
        }
    }
}

val downloadTranslations by tasks.register<Exec>("downloadTranslations") {
    group = "resources"
    description = "Downloads MyPet translations into build/resources/main/locale"
    val targetDir = layout.buildDirectory.dir("resources/main/locale").get().asFile
    outputs.dir(targetDir)
    doFirst {
        if (targetDir.exists()) targetDir.deleteRecursively()
        targetDir.mkdirs()
    }
    commandLine(
        "git", "clone", "--depth", "1", "--single-branch",
        "https://github.com/MyPetORG/MyPet-Translations.git", targetDir
    )
    delete(fileTree("build/resources/main/locale") {
        include(
            "exclude",
            ".git",
            ".gitignore",
            "README.md"
        )
    })
}

tasks.processResources {
    dependsOn(downloadTranslations)

    filesMatching("plugin.yml") { expand(filteringProps) }
    filesMatching("*.yml") { if (name != "plugin.yml") expand(filteringProps) }
}

fun Manifest.attributesForMyPet() = attributes(
    mapOf(
        "Main-Class" to "de.Keyle.MyPet.Main",
        "Project-Name" to project.name,
        "Project-Version" to version,
        "Project-Build" to buildNumber,
        "Project-Type" to buildType,
        "Project-Minecraft-Version" to minecraftVersion,
        "Project-Bukkit-Packets" to bukkitPackets,
        "Git-Commit" to (System.getenv("GIT_COMMIT") ?: "")
    )
)

tasks.jar {
    archiveBaseName.set(archivesBaseName)
    archiveFileName.set("${archivesBaseName}-${version}.jar")
    archiveVersion.set(project.version.toString())
    manifest { attributesForMyPet() }
}

/* ---------- Shading without JVM attribute conflicts ---------- */

// Create a resolvable-only configuration to collect jars to shade.
val shade by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
    // Mark as runtime usage and leave TargetJvmVersion unset.
    attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
}

dependencies {
    // Pull submodules as built jars (regardless of their target JVM)
    add("shade", project(path = ":plugin", configuration = "runtimeElements"))
    add("shade", project(path = ":api",    configuration = "runtimeElements"))
    add("shade", project(path = ":skills", configuration = "runtimeElements"))
    nmsModules.forEach { add("shade", project(path = it, configuration = "runtimeElements")) }

    // External libs to be shaded
    add("shade", "org.bstats:bstats-bukkit:1.7")
    add("shade", "org.mongodb:mongodb-driver:3.12.11")
    add("shade", "de.keyle:knbt:0.0.5")
    add("shade", "com.zaxxer:HikariCP:3.4.2")
    add("shade", "net.kyori:adventure-text-minimessage:4.17.0")
    add("shade", "net.kyori:adventure-text-serializer-ansi:4.17.0")
    add("shade", "net.kyori:adventure-text-serializer-legacy:4.17.0")
    add("shade", "io.sentry:sentry:8.22.0")
    add("shade", "io.sentry:sentry-logback:8.22.0")

}

// Build the shaded jar strictly from the 'shade' configuration
tasks.shadowJar {
    archiveBaseName.set(archivesBaseName)
    archiveFileName.set("${archivesBaseName}-${project.version}.jar")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")
    exclude("META-INF/**")
    manifest { attributesForMyPet() }

    dependsOn(shade)

    configurations = listOf(shade)

    // Remove unused classes from shaded dependencies
    minimize {
        // Exclude packages that use reflection or are loaded dynamically
        exclude(dependency("com.mongodb:.*:.*"))
        exclude(dependency("io.sentry:.*:.*"))
        exclude(dependency("org.bstats:.*:.*"))
        exclude(dependency("de.keyle:knbt:.*"))
        exclude(project(":plugin"))
        exclude(project(":api"))
        exclude(project(":skills"))
        nmsModules.forEach { exclude(project(it)) }
    }

    relocate("org.bstats", "de.Keyle.MyPet.util.metrics")
    relocate("com.zaxxer.hikari", "de.Keyle.MyPet.util.hikari")
    relocate("de.keyle.knbt", "de.Keyle.MyPet.util.nbt")
    relocate("com.mongodb", "de.Keyle.MyPet.util.mongodb")
}

tasks.assemble { dependsOn(tasks.shadowJar) }
tasks.build { dependsOn(tasks.shadowJar) }

sentry {
    includeSourceContext = true

    org = "mypet"
    projectName = "mypet"
    authToken = System.getenv("SENTRY_AUTH_TOKEN")
}

/* ---------- Root compilation settings (Java 17 output) ---------- */

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}
tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
    options.encoding = "UTF-8"
}