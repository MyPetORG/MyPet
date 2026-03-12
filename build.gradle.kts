import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    java
    id("com.gradleup.shadow") version "9.3.1"
    id("io.freefair.lombok") version "9.1.0"
    id("io.sentry.jvm.gradle") version "5.12.2"
    id("com.cjcrafter.polymart-release") version "1.0.1"
    id("io.papermc.hangar-publish-plugin") version "0.1.4"
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
val minecraftVersion by extra("1.21.11")

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
        maven { url = uri("https://dependency.download/releases") } // FactionsUUID
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://repo.mypet-plugin.de/") }
        maven { url = uri("https://mvn.intellectualsites.com/content/repositories/releases/") } // PlotSquared V6+
    }

    // Use lazy task configuration for better configuration performance
    tasks.named("processResources") { enabled = false }
    tasks.named("test") { enabled = false }
    tasks.named("compileTestJava") { enabled = false }
    tasks.named("processTestResources") { enabled = false }

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

val downloadTranslations by tasks.register<Exec>("downloadTranslations") {
    group = "resources"
    description = "Downloads MyPet translations into build/resources/main/locale"
    val targetDir = layout.buildDirectory.dir("resources/main/locale").get().asFile
    outputs.dir(targetDir)

    // Skip download if translations are less than 12 hours old
    val maxAgeMillis = 12 * 60 * 60 * 1000L // 12 hours in milliseconds
    onlyIf {
        if (!targetDir.exists()) {
            true
        } else {
            val ageMillis = System.currentTimeMillis() - targetDir.lastModified()
            val shouldDownload = ageMillis > maxAgeMillis
            if (!shouldDownload) {
                logger.lifecycle("Skipping translation download - last updated ${ageMillis / (60 * 60 * 1000)} hours ago (max: 12 hours)")
            }
            shouldDownload
        }
    }

    doFirst {
        if (targetDir.exists()) targetDir.deleteRecursively()
        targetDir.mkdirs()
    }
    commandLine(
        "git", "clone", "--depth", "1", "--single-branch",
        "https://github.com/MyPetORG/MyPet-Translations.git", targetDir
    )
    doLast {
        // Clean up files we don't need in the final JAR
        // Use direct File operations for configuration cache compatibility
        File(targetDir, ".git").deleteRecursively()
        File(targetDir, ".gitignore").delete()
        File(targetDir, "README.md").delete()
        File(targetDir, "exclude").deleteRecursively()
    }
}

tasks.processResources {
    dependsOn(downloadTranslations)
    duplicatesStrategy = DuplicatesStrategy.WARN

    // Define filtering properties inline for configuration cache compatibility
    val filteringProps = mapOf(
        "buildNumber" to buildNumber,
        "gitCommit" to (System.getenv("GIT_COMMIT") ?: ""),
        "minecraft" to mapOf("version" to minecraftVersion),
        "bukkit" to mapOf("packets" to bukkitPackets),
        "mypetVersion" to version,
        "timestamp" to DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(LocalDateTime.now()),
    )

    filesMatching("plugin.yml") { expand(filteringProps) }
    filesMatching("*.yml") { if (name != "plugin.yml") expand(filteringProps) }
}

fun Manifest.attributesForMyPet() = attributes(
    mapOf(
        "Class-Path" to "MyPet/rhino.jar MyPet/rhino-1.7.9.jar MyPet/rhino-1.7.10.jar MyPet/rhino-1.7.15.jar ../MyPet/rhino.jar ../MyPet/rhino-1.7.9.jar ../MyPet/rhino-1.7.10.jar ../MyPet/rhino-1.7.15.jar MyPet/mongo-java-driver.jar MyPet/mongo-java-driver-3.12.11.jar",
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
    add("shade", project(path = ":api", configuration = "runtimeElements"))
    add("shade", project(path = ":skills", configuration = "runtimeElements"))
    nmsModules.forEach { add("shade", project(path = it, configuration = "runtimeElements")) }

    // External libs to be shaded
    add("shade", "org.bstats:bstats-bukkit:1.7")
    add("shade", "net.kyori:adventure-nbt:4.17.0")
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
        exclude(dependency("io.sentry:.*:.*"))
        exclude(dependency("org.bstats:.*:.*"))
        // Adventure API uses reflection for serializers - must exclude to prevent runtime errors
        exclude(dependency("net.kyori:.*:.*"))
        exclude(project(":plugin"))
        exclude(project(":api"))
        exclude(project(":skills"))
        nmsModules.forEach { exclude(project(it)) }
    }

    relocate("org.bstats", "de.Keyle.MyPet.util.metrics")
}

tasks.assemble { dependsOn(tasks.shadowJar) }
tasks.build { dependsOn(tasks.shadowJar) }

sentry {
    // Only bundle sources when auth token is available (snapshot/release builds)
    // PR builds don't have the token and don't need source bundling
    includeSourceContext = !System.getenv("SENTRY_AUTH_TOKEN").isNullOrEmpty()

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

/* ---------- Polymart Release ---------- */

polymart {
    val polymartVersion = providers.gradleProperty("POLYMART_VERSION").orNull
        ?: project.version.toString()
    val polymartFile = providers.gradleProperty("POLYMART_FILE").orNull
        ?: "build/libs/MyPet-${project.version}.jar"
    apiKey = providers.gradleProperty("POLYMART_TOKEN").orNull
        ?: System.getenv("POLYMART_TOKEN")
        ?: ""
    resourceId = 8915
    version = polymartVersion
    title = "MyPet $polymartVersion"
    message = "View the full changelog on Modrinth: https://modrinth.com/plugin/mypet/version/$polymartVersion"
    file.set(file(polymartFile))
    beta = buildType != "release"
}

/* ---------- Hangar Release ---------- */

hangarPublish {
    publications.register("plugin") {
        val hangarVersion = providers.gradleProperty("HANGAR_VERSION").orNull
            ?: project.version.toString()
        val hangarFile = providers.gradleProperty("HANGAR_FILE").orNull
            ?: "build/libs/MyPet-${project.version}.jar"
        val hangarChangelog = providers.gradleProperty("HANGAR_CHANGELOG").orNull
            ?: System.getenv("HANGAR_CHANGELOG")
            ?: "View the full changelog on Modrinth: https://modrinth.com/plugin/mypet/version/$hangarVersion"

        version.set(hangarVersion)
        id.set("MyPet")
        channel.set(if (buildType == "release") "Release" else "Snapshot")
        changelog.set(hangarChangelog)
        apiKey.set(
            providers.gradleProperty("HANGAR_TOKEN").orNull
                ?: System.getenv("HANGAR_TOKEN")
                ?: ""
        )

        platforms {
            paper {
                jar.set(file(hangarFile))
                val gameVersions = System.getenv("GAME_VERSIONS")?.split("\n")?.filter { it.isNotBlank() } ?: listOf()
                platformVersions.set(gameVersions)
            }
        }
    }
}