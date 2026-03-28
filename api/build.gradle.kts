plugins {
    `java-library`
    id("io.freefair.lombok") version "9.1.0"
    `maven-publish`
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")

    compileOnly("net.kyori:adventure-nbt:4.17.0")
    compileOnly("org.jetbrains:annotations:16.0.2")
    compileOnly("net.kyori:adventure-text-minimessage:4.17.0")
    compileOnly("net.kyori:adventure-text-serializer-ansi:4.17.0")
    compileOnly("net.kyori:adventure-text-serializer-legacy:4.17.0")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
    options.encoding = "UTF-8"
}

val buildType = rootProject.findProperty("buildType")?.toString() ?: "local"
val versionSuffix = when (buildType) {
    "release" -> ""
    "snapshot", "dev" -> "-SNAPSHOT"
    else -> "-SNAPSHOT-local"
}
val apiVersion = "${rootProject.version}$versionSuffix"

publishing {
    repositories {
        maven {
            name = "UserDerezzed"
            val repoPath = if (apiVersion.endsWith("-SNAPSHOT") || apiVersion.endsWith("-SNAPSHOT-local")) "snapshots" else "releases"
            url = uri("https://repo.userderezzed.dev/$repoPath")
            credentials {
                username = "MyPetORG"
                password = providers.gradleProperty("reposiliteToken").orNull
                    ?: System.getenv("REPOSILITE_TOKEN")
            }
        }
    }

    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            groupId = "de.keyle"
            artifactId = "mypet-api"
            version = apiVersion
        }
    }
}