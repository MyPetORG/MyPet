plugins {
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
}

extra["needsReobf"] = true  // Pre-1.20.5 needs Spigot mappings at runtime

apply(from = rootProject.file("nms/nmsPaperweightModule.gradle"))

dependencies {
    paperweight.paperDevBundle("1.19.3-R0.1-SNAPSHOT")
}

description = "v1_19_R2"

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
    options.encoding = "UTF-8"
}