plugins {
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
}

apply(from = rootProject.file("nms/nmsPaperweightModule.gradle.kts"))

paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

dependencies {
    paperweight.paperDevBundle("1.21.3-R0.1-SNAPSHOT")
}

description = "v1_21_R2"

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
    options.encoding = "UTF-8"
}