apply(from = rootProject.file("nms/nmsModule.gradle"))

extra["craftbukkitVersion"] = "1.12-R0.1-SNAPSHOT"
extra["remapServerCode"] = false

description = "v1_12_R1"

tasks.withType<JavaCompile>().configureEach {
    options.release.set(8)
    options.encoding = "UTF-8"
}