import org.gradle.api.attributes.java.TargetJvmVersion

plugins {
    id("io.freefair.lombok")
    `maven-publish`
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(8)
    options.encoding = "UTF-8"
}

// Disable JVM version checking for compileClasspath to allow PlotSquared V6 (Java 17+) dependencies
configurations.compileClasspath {
    attributes {
        // Request Java 17 runtime to match PlotSquared V6 requirements
        // The actual compilation still targets Java 8 via options.release.set(8)
        attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 17)
    }
}

dependencies {
    compileOnly(project(":api"))
    compileOnly(project(":skills"))

    compileOnly("com.zaxxer:HikariCP:3.4.2")
    compileOnly("de.keyle:knbt:0.0.5")
    compileOnly("org.spigotmc:spigot-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("org.mozilla:rhino:1.7.15")

    compileOnly("org.slf4j:slf4j-api:1.7.30")
    compileOnly("org.slf4j:slf4j-nop:1.7.30")

    compileOnly("org.apache.logging.log4j:log4j-core:2.17.1")

    compileOnly("net.citizensnpcs:citizensapi:2.0.24")
    compileOnly("br.net.fabiozumbi12:PvPDiffTimer:1.4.4")
    compileOnly("au.com.mineauz:Minigames:1.12.0")
    compileOnly("com.plotsquared.bukkit:PlotSquared:3.4.4")

    compileOnly("com.github.intellectualsites:plotsquared:4.453") {
        exclude(group = "org.bstats", module = "bstats-bukkit")
        exclude(group = "com.destroystokyo.paper", module = "paper-api")
    }

    // PlotSquared V6+ (note: 6.11.2 has broken Maven deployment, use 6.11.1)
    // These require Java 17+ but we only use them for compile-time type checking
    compileOnly("com.plotsquared:PlotSquared-Core:6.11.1") {
        isTransitive = false
    }
    compileOnly("com.plotsquared:PlotSquared-Bukkit:6.11.1") {
        isTransitive = false
    }

    compileOnly("io.lumine.xikage:MythicMobs:4.9.1")
    compileOnly("io.lumine:Mythic-Dist:5.0.1-SNAPSHOT")

    compileOnly("org.natrolite:vault-api:1.7")
    compileOnly("com.gmail.nossr50:mcMMO:2.1.0")

    compileOnly("com.sk89q.worldguard:worldguard-legacy:7.0.0")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.5")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.2.5")

    compileOnly("com.mewin:WGCustomFlags:1.9")
    compileOnly("com.ancientshores:ancient:1.1.0")
    compileOnly("com.massivecraft:Factions:2.14.0")
    compileOnly("com.massivecraft:MassiveCore:2.14.0")
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
    compileOnly("net.kaikk.mc:GriefPreventionPlus:12.28")
    compileOnly("me.NoChance.PvPManager:PvPManager:3.4")
    compileOnly("org.kingdoms:kingdoms:13.3.40")
    compileOnly("com.herocraftonline.heroes:Heroes:1.5.5.7")
    compileOnly("com.sucy.skill:SkillAPI:3.108")
    compileOnly("com.nisovin.magicspells:MagicSpells:3.4.2")
    compileOnly("net.dmulloy2:ProtocolLib:5.3.0")
    compileOnly("me.clip:placeholderapi:2.9.2")
    compileOnly("fr.neatmonster:nocheatplus:3.16.0")
    compileOnly("de.myzelyam:supervanish:6.1.0")
    compileOnly("net.sacredlabyrinth.Phaed:PreciousStones:10.7.2")
    compileOnly("com.kirelcodes.miniaturepets:miniaturepets-api:1.5.4")
    compileOnly("com.SirBlobman.combatlogx:CombatLogX-Plugin:9.7.1.2")
    compileOnly("me.jet315:MobStacker:1.7")
    compileOnly("com.kiwifisher:mobstacker:2.0.0")
    compileOnly("uk.antiperson:StackMob:5.0.2")
    compileOnly("me.glaremasters:Guilds:3.3")
    compileOnly("com.github.Angeschossen:LandsAPI:4.5.2.0")
    compileOnly("de.keyle:mypet-premium-apis:1.0-SNAPSHOT")

    compileOnly("org.bstats:bstats-bukkit:1.7")
    compileOnly("org.mongodb:mongodb-driver:3.12.11")
    compileOnly("de.keyle:knbt:0.0.5")

    compileOnly("io.sentry:sentry:8.22.0")

}

fun getVersionFromName(filename: String): String {
    return """\d+(\.\d+)+(-SNAPSHOT)?""".toRegex().find(filename)?.value
        ?: throw GradleException("Failed to get PE version from: '$filename'")
}
