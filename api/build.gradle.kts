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
    compileOnly("org.bukkit:bukkit:1.15.2-R0.1-SNAPSHOT")

    compileOnly("org.fusesource.jansi:jansi:1.18")
    compileOnly("com.google.code.gson:gson:2.13.2")
    compileOnly("de.keyle:knbt:0.0.5")
    compileOnly("at.blvckbytes:RawMessage:0.2")
    compileOnly("org.jetbrains:annotations:16.0.2")

    compileOnly("net.kyori:adventure-api:4.25.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.25.0")
    compileOnly("net.kyori:adventure-platform-bukkit:4.3.3")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(8)
    options.encoding = "UTF-8"
}