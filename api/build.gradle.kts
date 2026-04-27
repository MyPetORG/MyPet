plugins {
    `java-library`
    id("io.freefair.lombok") version "9.1.0"
    `maven-publish`
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")

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

val buildType = rootProject.findProperty("buildType")?.toString() ?: "alpha"
val baseVersion = rootProject.version.toString().split("-")[0]
val apiVersion = if (buildType == "release") baseVersion else "$baseVersion-SNAPSHOT"

publishing {
    repositories {
        maven {
            name = "UserDerezzed"
            val repoPath = if (apiVersion.endsWith("-SNAPSHOT")) "snapshots" else "releases"
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