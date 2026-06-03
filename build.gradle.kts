import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    java
    id("com.gradleup.shadow") version "9.3.1"
    id("io.freefair.lombok") version "9.1.0"
    id("io.sentry.jvm.gradle") version "5.12.2"
    id("io.papermc.hangar-publish-plugin") version "0.1.4"
    id("net.minecraftforge.licenser") version "1.2.0" apply false
    `maven-publish`
}

group = "de.keyle"

val buildType = project.findProperty("buildType")?.toString() ?: "alpha"
val buildNumber = project.findProperty("BUILD_NUMBER")?.toString() ?: "local"
val baseVersion = "4.0.0"
val versionSuffix = when (buildType) {
    "alpha", "beta" -> {
        val buildSuffix = if (buildNumber == "local") "-local" else "-${buildNumber.padStart(2, '0')}"
        "-${buildType}${buildSuffix}"
    }
    else -> ""
}
version = "$baseVersion$versionSuffix"
val minecraftVersion by extra("1.21.11")

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/groups/public/")
    maven("https://repo.mypet-plugin.de/")
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "io.freefair.lombok")
    apply(plugin = "io.sentry.jvm.gradle")
    apply(plugin = "net.minecraftforge.licenser")

    configure<net.minecraftforge.licenser.LicenseExtension> {
        setHeader(rootProject.file("HEADER.txt"))
        include("**/*.java")
        newLine.set(true)
    }

    // Licenser scans sourceSet.allSource, which overlaps with Sentry's generated
    // resources directory. Order the tasks so Gradle 9's strict validator is satisfied.
    val sentryGenerators = tasks.matching {
        it.name.startsWith("generateSentry") || it.name.startsWith("collectExternalDependenciesForSentry")
    }
    tasks.matching { it.name.startsWith("checkLicense") || it.name.startsWith("updateLicense") }.configureEach {
        mustRunAfter(sentryGenerators)
    }

    repositories {
        mavenCentral()
        maven { url = uri("https://mvn.lib.co.nz/spigot/") }
        maven { url = uri("https://repo.md-5.net/content/groups/public/") }
        maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
        maven { url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/") }
        maven { url = uri("https://oss.sonatype.org/content/groups/public/") }
        maven { url = uri("https://maven.enginehub.org/repo/") }
        maven { url = uri("https://hub.spigotmc.org/nexus/content/groups/public/") }
        maven { url = uri("https://repo.md-5.net/content/repositories/public") }
        maven { url = uri("https://dependency.download/releases") } // FactionsUUID
        maven { url = uri("https://maven.citizensnpcs.co/repo") } // Citizens
        maven { url = uri("https://nexus.neetgames.com/repository/maven-public/") } // mcMMO
        maven { url = uri("https://repo.codemc.io/repository/maven-public/") } // StackMob
        maven { url = uri("https://repo.glaremasters.me/repository/towny/") } // Towny
        maven { url = uri("https://repo.glaremasters.me/repository/public/") } // Guilds
        maven { url = uri("https://repo.roinujnosde.me/releases/") } // SimpleClans
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://repo.mypet-plugin.de/") }
        maven { url = uri("https://mvn.lumine.io/repository/maven-public/") } // MythicMobs
    }

    // Use lazy task configuration for better configuration performance
    tasks.named("processResources") { enabled = false }
    tasks.named("test") { enabled = false }
    tasks.named("compileTestJava") { enabled = false }
    tasks.named("processTestResources") { enabled = false }

    if (project.name != "api") {
        plugins.withId("maven-publish") {
            tasks.withType<PublishToMavenRepository>().configureEach { enabled = false }
            tasks.withType<PublishToMavenLocal>().configureEach { enabled = false }
        }
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
    description = "Downloads MyPet translations into plugin module resources"
    val targetDir = project(":plugin").layout.buildDirectory.dir("resources/main/locale").get().asFile
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

// Root project no longer has src/main — resources are in :plugin module
tasks.processResources { enabled = false }

fun Manifest.attributesForMyPet() = attributes(
    mapOf(
        "Main-Class" to "de.Keyle.MyPet.Main",
        "Project-Name" to project.name,
        "Project-Version" to version,
        "Project-Build" to buildNumber,
        "Project-Type" to buildType,
        "Project-Minecraft-Version" to minecraftVersion,
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

    // External libs to be shaded
    add("shade", "org.bstats:bstats-bukkit:3.2.1")
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

/* ---------- Root project has no source files — compilation is in submodules ---------- */

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
        channel.set(when (buildType) {
                "release" -> "Release"
                "beta" -> "Beta"
                else -> "Alpha"
            })
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