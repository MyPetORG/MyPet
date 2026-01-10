import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.gradle.api.attributes.Usage
import java.net.URI

plugins {
    java
    id("com.gradleup.shadow") version "9.3.1"
    id("io.freefair.lombok") version "9.1.0"
    id("com.cjcrafter.polymart-release") version "1.0.1"
    id("io.papermc.hangar-publish-plugin") version "0.1.4"
    id("io.sentry.jvm.gradle") version "5.12.2"
    `maven-publish`
}

group = "de.keyle"

val buildType = project.findProperty("buildType")?.toString() ?: "local"
val minecraftVersion by extra("1.21.11")
val bukkitPackets by extra("v1_8_R3;v1_12_R1;v1_16_R3;v1_17_R1;v1_18_R1;v1_18_R2;v1_19_R2;v1_19_R3;v1_20_R1;v1_20_R2;v1_20_R3;v1_20_R4;v1_21_R1;v1_21_R2;v1_21_R3;v1_21_R4;v1_21_R5;v1_21_R6;v1_21_R7")
val specialVersions by extra("")

version = "3.14.1"

val nmsModules: List<String> = File(rootDir, "nms")
    .listFiles()
    ?.filter { it.isDirectory && it.name.matches(Regex("v[\\d_]+R\\d+")) }
    ?.map { ":nms:${it.name}" }
    ?: emptyList()

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
val versionSuffix = when (buildType) {
    "release" -> ""
    "snapshot", "dev" -> "-SNAPSHOT"
    else -> "-SNAPSHOT-local"  // local builds
}
val fullVersion = "${project.version}$versionSuffix"

val filteringProps = mapOf(
    "project" to project,
    "buildNumber" to providers.gradleProperty("BUILD_NUMBER").orElse(""),
    "gitCommit" to (System.getenv("GIT_COMMIT") ?: ""),
    "minecraft" to mapOf("version" to minecraftVersion),
    "bukkit" to mapOf("packets" to bukkitPackets),
    "special" to mapOf("versions" to specialVersions),
    "mypetVersion" to version,
    "timestamp" to DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(LocalDateTime.now()),
)

sourceSets {
    main {
        resources { srcDir("src/main/resources/") }
    }
}

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
    dependsOn(downloadVersionmatcher, downloadTranslations)
    duplicatesStrategy = DuplicatesStrategy.WARN

    filesMatching("plugin.yml") { expand(filteringProps) }
    filesMatching("*.yml") { if (name != "plugin.yml") expand(filteringProps) }
}

fun Manifest.attributesForMyPet() = attributes(
    mapOf(
        "Class-Path" to "MyPet/rhino.jar MyPet/rhino-1.7.9.jar MyPet/rhino-1.7.10.jar MyPet/rhino-1.7.15.jar ../MyPet/rhino.jar ../MyPet/rhino-1.7.9.jar ../MyPet/rhino-1.7.10.jar ../MyPet/rhino-1.7.15.jar MyPet/mongo-java-driver.jar MyPet/mongo-java-driver-3.12.11.jar",
        "Main-Class" to "de.Keyle.MyPet.Main",
        "Project-Name" to project.name,
        "Project-Version" to project.version.toString(),
        "Project-Build" to (System.getenv("BUILD_NUMBER") ?: ""),
        "Project-Type" to buildType,
        "Project-Minecraft-Version" to minecraftVersion,
        "Project-Bukkit-Packets" to bukkitPackets,
        "Special-MC-Versions" to specialVersions,
        "Git-Commit" to (System.getenv("GIT_COMMIT") ?: "")
    )
)

tasks.jar {
    archiveBaseName.set(archivesBaseName)
    archiveFileName.set("${archivesBaseName}-${fullVersion}.jar")
    archiveVersion.set(fullVersion)
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
    add("shade", "at.blvckbytes:RawMessage:0.2")
    add("shade", "org.bstats:bstats-bukkit:1.7")
    add("shade", "org.mongodb:mongodb-driver:3.12.11")
    add("shade", "de.keyle:knbt:0.0.5")
    add("shade", "com.google.code.gson:gson:2.8.9")
    add("shade", "io.sentry:sentry:8.22.0")
    add("shade", "io.sentry:sentry-logback:8.22.0")
    add("shade", "com.zaxxer:HikariCP:3.4.2")
}

// Build the shaded jar strictly from the 'shade' configuration
tasks.shadowJar {
    archiveBaseName.set(archivesBaseName)
    archiveVersion.set(fullVersion)
    archiveClassifier.set("")
    exclude("META-INF/**")
    manifest { attributesForMyPet() }

    dependsOn(shade)

    configurations = listOf(shade)

    relocate("at.blvckbytes.raw_message", "de.Keyle.MyPet.util.raw_message")
    relocate("org.bstats", "de.Keyle.MyPet.util.metrics")
    relocate("com.zaxxer.hikari", "de.Keyle.MyPet.util.hikari")
    relocate("de.keyle.knbt", "de.Keyle.MyPet.util.nbt")
    relocate("org.bson", "de.Keyle.MyPet.util.bson")
    relocate("com.google.gson", "de.Keyle.MyPet.util.gson")
    relocate("com.mongodb", "de.Keyle.MyPet.util.mongodb")
}

tasks.assemble { dependsOn(tasks.shadowJar) }
tasks.build { dependsOn(tasks.shadowJar) }

/* ---------- Sentry Configuration ---------- */

sentry {
    // Only bundle sources when auth token is available (snapshot/release builds)
    // PR builds don't have the token and don't need source bundling
    includeSourceContext = !System.getenv("SENTRY_AUTH_TOKEN").isNullOrEmpty()

    org = "mypet"
    projectName = "mypet"
    authToken = System.getenv("SENTRY_AUTH_TOKEN")
}

/* ---------- Root compilation settings (Java 8 output) ---------- */

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}
tasks.withType<JavaCompile>().configureEach {
    options.release.set(8)
    options.encoding = "UTF-8"
}

/* ---------- Polymart Release ---------- */

polymart {
    val polymartVersion = providers.gradleProperty("POLYMART_VERSION").orNull
        ?: project.version.toString()
    val polymartFile = providers.gradleProperty("POLYMART_FILE").orNull
        ?: "build/libs/MyPet-${fullVersion}.jar"
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
            ?: "build/libs/MyPet-${fullVersion}.jar"
        val hangarChangelog = providers.gradleProperty("HANGAR_CHANGELOG").orNull
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
                platformVersions.set(listOf(
                    "1.8.8", "1.12.2", "1.16.5", "1.17.1", "1.18.2",
                    "1.19.x", "1.20.x", "1.21.x"
                ))
            }
        }
    }
}
