apply(from = rootProject.file("nms/nmsModule.gradle"))

extra["craftbukkitVersion"] = "1.21.3-R0.1-SNAPSHOT";

description = "v1_21_R2"

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
    options.encoding = "UTF-8"
}