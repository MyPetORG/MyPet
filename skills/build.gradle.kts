plugins {
    `java-library`
    id("io.freefair.lombok") version "9.1.0"
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")

    compileOnly("net.kyori:adventure-nbt:4.17.0")
    compileOnly("org.jetbrains:annotations:16.0.2")

    compileOnly(project(":api"))
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
    options.encoding = "UTF-8"
}