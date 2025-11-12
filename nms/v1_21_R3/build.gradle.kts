plugins {
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
}

extra["needsReobf"] = false  // Paper 1.20.5+ uses Mojang mappings at runtime

apply(from = rootProject.file("nms/nmsPaperweightModule.gradle"))

dependencies {
    paperweight.paperDevBundle("1.21.4-R0.1-SNAPSHOT")
}

description = "v1_21_R3"

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
    options.encoding = "UTF-8"
}