plugins {
    `java-library`
    id("io.freefair.lombok") version "9.0.0"
}

repositories {
    mavenCentral()

    maven("https://hub.spigotmc.org/nexus/content/groups/public/")
    maven("https://repo.md-5.net/content/repositories/public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.17.1-R0.1-SNAPSHOT")

    compileOnly("de.keyle:knbt:0.0.5")
    compileOnly("at.blvckbytes:RawMessage:0.2")
    compileOnly("org.jetbrains:annotations:16.0.2")
    compileOnly("net.kyori:adventure-text-minimessage:4.25.0")
    compileOnly("net.kyori:adventure-text-serializer-ansi:4.25.0")
    compileOnly("net.kyori:adventure-text-serializer-legacy:4.25.0")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(16)
    options.encoding = "UTF-8"
}