apply(from = rootProject.file("nms/nmsModule.gradle"))

extra["craftbukkitVersion"] = "26.2-R0.1-SNAPSHOT"
extra["remapServerCode"] = false

description = "v26_2_R1"

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
    options.encoding = "UTF-8"
}