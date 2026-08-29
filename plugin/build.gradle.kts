import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    id("io.freefair.lombok") version "9.1.0"
    `maven-publish`
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
    options.encoding = "UTF-8"
}

// Re-enable processResources (disabled in root subprojects block)
tasks.processResources {
    enabled = true
    duplicatesStrategy = DuplicatesStrategy.WARN
    dependsOn(rootProject.tasks.named("downloadTranslations"))

    // plugin.yml is the only *.yml here, and ${mypetVersion} is the only token it uses.
    // Capture the value at configuration time — the filesMatching action runs at
    // execution time, where the configuration cache forbids touching Project.
    val mypetVersion = rootProject.version.toString()

    filesMatching("*.yml") { expand(mapOf("mypetVersion" to mypetVersion)) }

    // Injects the CI-only BuiltByBit Shared API token; empty for local/PR builds that
    // don't have the secret, which BuiltByBitInfo treats as "no token bundled".
    filesMatching("builtbybit.properties") {
        filter<ReplaceTokens>("tokens" to mapOf("BBB_SHARED_TOKEN" to (System.getenv("BBB_SHARED_TOKEN") ?: "")))
    }
}

dependencies {
    compileOnly(project(":api"))
    compileOnly(project(":skills"))

    // Declared first so the real gson wins classpath order over the OLD gson that the
    // Guilds plugin jar bundles unrelocated (which otherwise shadows it and breaks JsonObject.keySet() etc.)
    compileOnly("com.google.code.gson:gson:2.11.0")

    compileOnly("com.zaxxer:HikariCP:3.4.2")
    compileOnly("net.kyori:adventure-nbt:4.17.0")
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("com.mojang:brigadier:1.0.18")

    compileOnly("net.citizensnpcs:citizensapi:2.0.42-SNAPSHOT")
    // PlotSquared V6+ (note: 6.11.2 has broken Maven deployment, use 6.11.1)
    compileOnly("com.plotsquared:PlotSquared-Core:6.11.1") {
        isTransitive = false
    }
    compileOnly("com.plotsquared:PlotSquared-Bukkit:6.11.1") {
        isTransitive = false
    }

    compileOnly("io.lumine:Mythic-Dist:5.12.1")

    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        isTransitive = false
    }
    compileOnly("com.gmail.nossr50.mcMMO:mcMMO:2.2.052-SNAPSHOT") {
        isTransitive = false // don't drag in worldguard-core 7.0.12 etc.
    }

    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.16")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.2.20")

    compileOnly("dev.kitteh:factions:4.4.0")
    // @jar: jitpack's Gradle module metadata has empty apiElements/runtimeElements variants
    // (the jar is only in shadowRuntimeElements); artifact-only notation fetches the jar directly.
    compileOnly("com.github.garbagemule:MobArena:0.109@jar")
    compileOnly("org.battleplugins:arena:4.0.3-SNAPSHOT")
    compileOnly("com.github.Zrips:Residence:6.0.0.1") {
        isTransitive = false // don't drag in org.dynmap:dynmap-api (no public host)
    }
    compileOnly("com.palmergames.bukkit.towny:towny:0.103.0.2")
    compileOnly("net.sacredlabyrinth.phaed.simpleclans:SimpleClans:2.19.2")
    compileOnly("io.github.fabiozumbi12.RedProtect:RedProtect-Spigot:8.1.2") {
        isTransitive = false // don't drag in spigot-api/UltimateFancy; Core added explicitly below
    }
    compileOnly("io.github.fabiozumbi12.RedProtect:RedProtect-Core:8.1.2") {
        isTransitive = false
    }
    compileOnly("com.github.TechFortress:GriefPrevention:16.18")
    compileOnly("me.NoChance.PvPManager:pvpmanager:3.19.10")
    compileOnly("com.github.cryptomorin:kingdoms:1.17.18.1-BETA") {
        isTransitive = false
    }
    compileOnly("com.herocraftonline.heroes:Heroes:1.10.7-RELEASE")
    compileOnly("me.clip:placeholderapi:2.12.2")
    compileOnly("com.github.LeonMangler:SuperVanish:6.2.18-3")
    compileOnly("com.ticxo.modelengine:ModelEngine:R4.0.4")
    // BetterModel is driven via reflection (its 3.x API is Java-25 bytecode, uncompilable on this
    // Java-21 build) — no compileOnly dependency. See BetterModelHook. Runtime support is Java-25 servers only.
    compileOnly("beer.devs:itemsadder-api:4.0.17")
    compileOnly("com.github.sirblobman.combatlogx:api:11.7-SNAPSHOT")
    compileOnly("com.github.sirblobman.api:core:2.9-SNAPSHOT") // BlueSlimeCore
    compileOnly("uk.antiperson.stackmob:StackMob:5.10.3") // 5.10.4+ are built for Java 25; 5.10.3 is Java 17
    compileOnly("me.glaremasters:guilds:3.5.7.0")
    compileOnly("com.github.Angeschossen:LandsAPI:4.5.2.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.17.0")
    compileOnly("net.kyori:adventure-text-serializer-legacy:4.17.0")
    compileOnly("net.kyori:adventure-text-serializer-ansi:4.17.0")

    compileOnly("com.github.retrooper:packetevents-spigot:2.7.0")
    compileOnly("net.dmulloy2:ProtocolLib:5.3.0")

    compileOnly("org.bstats:bstats-bukkit:3.2.1")

    compileOnly("io.sentry:sentry:8.22.0")
    compileOnly("org.apache.logging.log4j:log4j-core:2.17.1")

}

fun getVersionFromName(filename: String): String {
    return """\d+(\.\d+)+(-SNAPSHOT)?""".toRegex().find(filename)?.value
        ?: throw GradleException("Failed to get PE version from: '$filename'")
}
