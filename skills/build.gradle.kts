plugins {
    `java-library`
    id("io.freefair.lombok")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")

    compileOnly("com.google.code.gson:gson:2.8.9")
    compileOnly("de.keyle:knbt:0.0.5")
    compileOnly("org.jetbrains:annotations:16.0.2")

    compileOnly(project(":api"))
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(8)
    options.encoding = "UTF-8"
}