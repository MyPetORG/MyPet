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

    implementation("org.fusesource.jansi:jansi:1.18")
    implementation("com.google.code.gson:gson:2.13.2")
    implementation("de.keyle:knbt:0.0.5")
    implementation("at.blvckbytes:RawMessage:0.2")
    implementation("org.jetbrains:annotations:16.0.2")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}