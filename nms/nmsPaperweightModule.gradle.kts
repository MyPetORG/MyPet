// Ensure configurations like compileOnly exist
apply(plugin = "java-library")

// Per-module switch; override in gradle.properties if needed
extra["needsReobf"] = true

dependencies {
    add("compileOnly", project(":api"))
    add("compileOnly", project(":skills"))

    add("compileOnly", "com.mojang:brigadier:1.0.18")
    add("compileOnly", "com.mojang:datafixerupper:8.0.16")
    add("compileOnly", "com.mojang:javabridge:1.2.24")
    add("compileOnly", "com.mojang:authlib:3.2.38")

    add("compileOnly", "net.kyori:adventure-text-minimessage:4.17.0")

    add("compileOnly", "org.jetbrains:annotations:13.0")
    add("compileOnly", "de.keyle:knbt:0.0.5")
}

afterEvaluate {
    val reobf: Boolean =
        (findProperty("needsReobf")?.toString()?.toBoolean())
            ?: (extra["needsReobf"] as? Boolean ?: true)

    if (reobf) {
        // 1.17–1.20.4: publish reobfuscated JAR produced by paperweight
        configurations.named("runtimeElements").configure {
            outgoing.artifacts.clear()
            outgoing.artifact(tasks.named("reobfJar"))
        }
    } else {
        // 1.20.5+: publish Mojang-mapped JAR
        configurations.named("runtimeElements").configure {
            outgoing.artifacts.clear()
            outgoing.artifact(tasks.named("jar"))
        }
    }
}