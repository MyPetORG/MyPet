import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    java
    id("com.gradleup.shadow") version "9.2.2"
    id("io.freefair.lombok") version "9.0.0"
    `maven-publish`
}

group = "de.keyle"
version = "3.14-SNAPSHOT"

val minecraftVersion by extra("1.21.9")
val bukkitPackets by extra("v1_8_R3;v1_12_R1;v1_16_R3;v1_17_R1;v1_18_R1;v1_18_R2;v1_19_R1;v1_19_R2;v1_19_R3;v1_20_R1;v1_20_R2;v1_20_R3;v1_20_R4;v1_21_R1;v1_21_R2;v1_21_R3;v1_21_R4;v1_21_R5;v1_21_R6")
val specialVersions by extra("")

val nmsModules: List<String> = File(rootDir, "nms")
    .listFiles()
    ?.filter { it.isDirectory && it.name.matches(Regex("v[\\d_]+R\\d+")) }
    ?.map { ":nms:${it.name}" }
    ?: emptyList()

repositories {
    mavenLocal()
    mavenCentral()
    /*maven {
        url = uri("https://maven.pkg.github.com/MyPetORG/MyPet")
        credentials {
            username = System.getenv("GITHUB_ACTOR") ?: System.getenv("GITHUB_USERNAME")
            password = System.getenv("GITHUB_TOKEN") ?: System.getenv("GITHUB_PACKAGES_TOKEN")
        }
    }*/
}

subprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        maven { url = uri("https://mvn.lib.co.nz/spigot/") }
        maven { url = uri("https://repo.md-5.net/content/groups/public/") }
        maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
        maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }
        maven { url = uri("https://repo.codemc.io/repository/maven-snapshots/") }
        maven { url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/") }
        maven { url = uri("https://oss.sonatype.org/content/groups/public/") }
        maven { url = uri("https://maven.pkg.github.com/MyPetORG/*") }
        maven { url = uri("https://repo.dmulloy2.net/nexus/repository/public/") }
        maven { url = uri("https://maven.enginehub.org/repo/") }
        maven { url = uri("https://hub.spigotmc.org/nexus/content/groups/public/") }
        maven { url = uri("https://repo.md-5.net/content/repositories/public") }
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://repo.mypet-plugin.de/") }
    }

    apply(plugin = "java-library")
    apply(plugin = "io.freefair.lombok")

    java {
        toolchain { languageVersion = JavaLanguageVersion.of(21) }
        disableAutoTargetJvm()
    }

    // prevent accidental publish from submodules
    plugins.withId("maven-publish") {
        tasks.withType<PublishToMavenRepository>().configureEach { enabled = false }
        tasks.withType<PublishToMavenLocal>().configureEach { enabled = false }
    }
}

val archivesBaseName = "MyPet"

val filteringProps = mapOf(
    "project" to project,
    "BUILD_NUMBER" to (System.getenv("BUILD_NUMBER") ?: ""),
    "GIT_COMMIT" to (System.getenv("GIT_COMMIT") ?: ""),
    "minecraft" to mapOf("version" to minecraftVersion),
    "bukkit" to mapOf("packets" to bukkitPackets),
    "special" to mapOf("versions" to specialVersions),
    "mypetVersion" to project(":plugin").version,
    "timestamp" to DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(LocalDateTime.now()),
    "buildNumber" to System.getProperty("build.number", "unknown")
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
        val url = URL("https://raw.githubusercontent.com/MyPetORG/MyPet/versionmatcher/versionmatcher.csv")
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
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    dependsOn(downloadVersionmatcher, downloadTranslations)
    filesMatching("plugin.yml") {
        expand(
            filteringProps
        )
    }
    filesMatching("**/*.yml") {
        if (name != "plugin.yml") expand(filteringProps)
    }
}

fun Manifest.attributesForMyPet() = attributes(
    mapOf(
        "Class-Path" to "MyPet/rhino.jar MyPet/rhino-1.7.9.jar MyPet/rhino-1.7.10.jar MyPet/rhino-1.7.15.jar ../MyPet/rhino.jar ../MyPet/rhino-1.7.9.jar ../MyPet/rhino-1.7.10.jar ../MyPet/rhino-1.7.15.jar MyPet/mongo-java-driver.jar MyPet/mongo-java-driver-3.12.11.jar",
        "Main-Class" to "de.Keyle.MyPet.skilltreecreator.Main",
        "Project-Author" to "Keyle",
        "Project-Name" to project.name,
        "Project-Version" to project.version.toString(),
        "Project-Build" to (System.getenv("BUILD_NUMBER") ?: ""),
        "Project-Minecraft-Version" to minecraftVersion,
        "Project-Bukkit-Packets" to bukkitPackets,
        "Special-MC-Versions" to specialVersions,
        "Git-Commit" to (System.getenv("GIT_COMMIT") ?: "")
    )
)

tasks.jar {
    archiveBaseName.set(archivesBaseName)
    archiveFileName.set("${archivesBaseName}-${project.version}.jar")
    archiveVersion.set(project.version.toString())
    manifest { attributesForMyPet() }
}

tasks.shadowJar {
    archiveBaseName.set(archivesBaseName)
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")
    exclude("META-INF/**")
    manifest { attributesForMyPet() }

    dependsOn(":plugin:classes", ":api:classes", ":skills:classes")
    nmsModules.forEach { dependsOn("$it:classes") }

    // avoid runtime conflicts
    relocate("at.blvckbytes.raw_message", "de.Keyle.MyPet.util.raw_message")
    relocate("org.bstats", "de.Keyle.MyPet.util.metrics")
    relocate("com.google.gson", "de.Keyle.MyPet.util.gson")
    relocate("com.zaxxer.hikari", "de.Keyle.MyPet.util.hikari")
    relocate("io.sentry", "de.Keyle.MyPet.util.sentry")
    relocate("de.keyle.knbt", "de.Keyle.MyPet.util.nbt")
    relocate("org.bson", "de.Keyle.MyPet.util.bson")
    relocate("com.mongodb", "de.Keyle.MyPet.util.mongodb")
}

tasks.assemble { dependsOn(tasks.shadowJar) }
tasks.build { dependsOn(tasks.shadowJar) }

dependencies {
    implementation(project(":plugin")) { isTransitive = false }
    implementation(project(":api")) { isTransitive = false }
    implementation(project(":skills")) { isTransitive = false }
    nmsModules.forEach {
        implementation(project(it)) { isTransitive = false }
    }

    implementation("at.blvckbytes:RawMessage:0.2")
    implementation("org.bstats:bstats-bukkit:1.7")
    implementation("org.mongodb:mongodb-driver:3.12.11")
    implementation("de.keyle:knbt:0.0.5")
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("io.sentry:sentry:1.7.30")
    implementation("com.zaxxer:HikariCP:3.4.2")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}