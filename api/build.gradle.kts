plugins {
    `java-library`
    id("io.freefair.lombok") version "9.1.0"
}

repositories {
    mavenCentral()

    maven("https://hub.spigotmc.org/nexus/content/groups/public/")
    maven("https://repo.md-5.net/content/repositories/public/")
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