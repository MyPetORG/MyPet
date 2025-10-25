apply(from = rootProject.file("nms/nmsModule.gradle"))

extra["craftbukkitVersion"] = "1.21.8-R0.1-SNAPSHOT";

description = "v1_21_R5"

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
    options.encoding = "UTF-8"
}