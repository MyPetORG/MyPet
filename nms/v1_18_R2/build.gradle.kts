apply(from = rootProject.file("nms/nmsModule.gradle"))

extra["craftbukkitVersion"] = "1.18.2-R0.1-SNAPSHOT";

description = "v1_18_R2"

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
    options.encoding = "UTF-8"
}