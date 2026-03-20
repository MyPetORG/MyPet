// Ensure configurations like compileOnly exist
apply(plugin = "java-library")

dependencies {
    add("compileOnly", project(":api"))
    add("compileOnly", project(":skills"))

    add("compileOnly", "com.mojang:brigadier:1.0.18")
    add("compileOnly", "com.mojang:datafixerupper:8.0.16")
    add("compileOnly", "com.mojang:javabridge:1.2.24")
    add("compileOnly", "com.mojang:authlib:3.2.38")

    add("compileOnly", "net.kyori:adventure-text-minimessage:4.17.0")

    add("compileOnly", "org.jetbrains:annotations:13.0")
    add("compileOnly", "net.kyori:adventure-nbt:4.17.0")
}

afterEvaluate {
    configurations.named("runtimeElements").configure {
        outgoing.artifacts.clear()
        outgoing.artifact(tasks.named("jar"))
    }
}