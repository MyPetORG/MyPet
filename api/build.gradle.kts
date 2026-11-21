plugins {
    `java-library`
    id("io.freefair.lombok")
    `maven-publish`
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")

    compileOnly("com.google.code.gson:gson:2.8.9")
    compileOnly("de.keyle:knbt:0.0.6")
    compileOnly("org.jetbrains:annotations:16.0.2")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(8)
    options.encoding = "UTF-8"
}

// Pure-logic unit tests (no Bukkit or NMS classes at runtime). The root build turns tests
// off for every module; this module turns them back on.
dependencies {
    testImplementation("de.keyle:knbt:0.0.6")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named("compileTestJava") { enabled = true }
tasks.named("processTestResources") { enabled = true }
tasks.named<Test>("test") {
    enabled = true
    useJUnitPlatform()
}

val buildType = rootProject.findProperty("buildType")?.toString() ?: "local"
val versionSuffix = when (buildType) {
    "release" -> ""
    "snapshot", "dev" -> "-SNAPSHOT"
    else -> "-SNAPSHOT-local"
}
val apiVersion = "${rootProject.version}$versionSuffix"

publishing {
    repositories {
        maven {
            name = "UserDerezzed"
            val repoPath = if (apiVersion.endsWith("-SNAPSHOT") || apiVersion.endsWith("-SNAPSHOT-local")) "snapshots" else "releases"
            url = uri("https://repo.userderezzed.dev/$repoPath")
            credentials {
                username = "MyPetORG"
                password = providers.gradleProperty("reposiliteToken").orNull
                    ?: System.getenv("REPOSILITE_TOKEN")
            }
        }
    }

    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            groupId = "de.keyle"
            artifactId = "mypet-api"
            version = apiVersion
        }
    }
}