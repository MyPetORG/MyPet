import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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

    val buildNumber = rootProject.findProperty("BUILD_NUMBER")?.toString() ?: "local"
    val minecraftVersion: String by rootProject.extra

    val filteringProps = mapOf(
        "buildNumber" to buildNumber,
        "gitCommit" to (System.getenv("GIT_COMMIT") ?: ""),
        "minecraft" to mapOf("version" to minecraftVersion),
        "mypetVersion" to rootProject.version,
        "timestamp" to DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(LocalDateTime.now()),
    )

    filesMatching("*.yml") { expand(filteringProps) }
}

dependencies {
    compileOnly(project(":api"))
    compileOnly(project(":skills"))

    compileOnly("com.zaxxer:HikariCP:3.4.2")
    compileOnly("net.kyori:adventure-nbt:4.17.0")
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("org.mozilla:rhino:1.8.0")
    compileOnly("com.mojang:brigadier:1.0.18")

    compileOnly("net.citizensnpcs:citizensapi:2.0.24")
    // PlotSquared V6+ (note: 6.11.2 has broken Maven deployment, use 6.11.1)
    compileOnly("com.plotsquared:PlotSquared-Core:6.11.1") {
        isTransitive = false
    }
    compileOnly("com.plotsquared:PlotSquared-Bukkit:6.11.1") {
        isTransitive = false
    }

    compileOnly("io.lumine:Mythic-Dist:5.0.1-SNAPSHOT")

    compileOnly("org.natrolite:vault-api:1.7")
    compileOnly("com.gmail.nossr50:mcMMO:2.1.0")

    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.7")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.2.20")

    compileOnly("com.mewin:WGCustomFlags:1.9") {
        exclude(group = "org.bukkit", module = "bukkit")
    }
    compileOnly("dev.kitteh:factions:4.4.0")
    compileOnly("com.garbagemule:MobArena:0.103")
    compileOnly("mc.alk:BattleArena:3.9.9.10.5")
    compileOnly("net.slipcor:pvparena:1.3.4.298")
    compileOnly("me.maker56.survivalgames:SurvivalGames:1.9.4")
    compileOnly("org.mcsg:survivalgames:0.6.7")
    compileOnly("com.bekvon.bukkit:residence:4.8.3.1")
    compileOnly("com.palmergames.bukkit:Towny:0.93.0.0")
    compileOnly("net.sacredlabyrinth.phaed.simpleclans:SimpleClans:2.7.11")
    compileOnly("br.net.fabiozumbi12.RedProtect:RedProtect:7.5.5")
    compileOnly("com.github.TechFortress:GriefPrevention:16.18")
    compileOnly("me.NoChance.PvPManager:PvPManager:3.4")
    compileOnly("org.kingdoms:kingdoms:13.3.40")
    compileOnly("com.herocraftonline.heroes:Heroes:1.5.5.7")
    compileOnly("net.dmulloy2:ProtocolLib:5.3.0")
    compileOnly("me.clip:placeholderapi:2.12.2")
    compileOnly("fr.neatmonster:nocheatplus:3.16.0")
    compileOnly("de.myzelyam:supervanish:6.1.0")
    compileOnly("com.kirelcodes.miniaturepets:miniaturepets-api:1.5.4")
    compileOnly("com.SirBlobman.combatlogx:CombatLogX-Plugin:9.7.1.2")
    compileOnly("uk.antiperson:StackMob:5.0.2")
    compileOnly("me.glaremasters:Guilds:3.3")
    compileOnly("com.github.Angeschossen:LandsAPI:4.5.2.0")
    compileOnly("de.keyle:mypet-premium-apis:1.0-SNAPSHOT")
    compileOnly("net.kyori:adventure-text-minimessage:4.17.0")
    compileOnly("net.kyori:adventure-text-serializer-legacy:4.17.0")
    compileOnly("net.kyori:adventure-text-serializer-ansi:4.17.0")

    compileOnly("org.bstats:bstats-bukkit:1.7")

    compileOnly("io.sentry:sentry:8.22.0")
    compileOnly("ch.qos.logback:logback-classic:1.2.11")
    compileOnly("org.slf4j:slf4j-api:1.7.36")
    compileOnly("org.apache.logging.log4j:log4j-core:2.17.1")

}

fun getVersionFromName(filename: String): String {
    return """\d+(\.\d+)+(-SNAPSHOT)?""".toRegex().find(filename)?.value
        ?: throw GradleException("Failed to get PE version from: '$filename'")
}
