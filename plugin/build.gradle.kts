plugins {
    id("io.freefair.lombok") version "9.0.0"
    `maven-publish`
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(8)
    options.encoding = "UTF-8"
}

dependencies {
    compileOnly(project(":api"))
    compileOnly(project(":skills"))

    compileOnly("com.zaxxer:HikariCP:3.4.2")
    compileOnly("de.keyle:knbt:0.0.5")
    compileOnly("org.spigotmc:spigot-api:1.17.1-R0.1-SNAPSHOT")
    compileOnly("org.mozilla:rhino:1.7.12")

    compileOnly("org.slf4j:slf4j-api:1.7.30")
    compileOnly("org.slf4j:slf4j-nop:1.7.30")

    compileOnly("org.apache.logging.log4j:log4j-core:2.17.1")

    compileOnly("at.blvckbytes:RawMessage:0.2")

    compileOnly("net.citizensnpcs:citizensapi:2.0.24")
    compileOnly("br.net.fabiozumbi12:PvPDiffTimer:1.4.4")
    compileOnly("au.com.mineauz:Minigames:1.12.0")
    compileOnly("com.plotsquared.bukkit:PlotSquared:3.4.4")

    compileOnly("com.github.intellectualsites:plotsquared:4.453") {
        exclude(group = "org.bstats", module = "bstats-bukkit")
        exclude(group = "com.destroystokyo.paper", module = "paper-api")
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
    compileOnly("net.kyori:adventure-api:4.25.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.25.0")
    compileOnly("net.kyori:adventure-platform-bukkit:4.4.2-SNAPSHOT")

    compileOnly("org.bstats:bstats-bukkit:1.7")
    compileOnly("org.mongodb:mongodb-driver:3.12.11")
    compileOnly("de.keyle:knbt:0.0.5")
    compileOnly("com.google.code.gson:gson:2.8.9")

    // Prefer Lombok as compileOnly + annotationProcessor in Gradle:
    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")

}

fun getVersionFromName(filename: String): String {
    return """\d+(\.\d+)+(-SNAPSHOT)?""".toRegex().find(filename)?.value
        ?: throw GradleException("Failed to get PE version from: '$filename'")
}

publishing {
    repositories {
        if (System.getenv("MVN_USER") != null) {
            maven {
                val repoType = if (project.version.toString().endsWith("-SNAPSHOT")) "snapshots" else "releases"
                // Eg: https://mvn.lib.co.nz/repositories/maven-%branch%/
                url = uri(System.getenv("MVN_PATH").replace("%branch%", repoType))

                credentials {
                    username = System.getenv("MVN_USER")
                    password = System.getenv("MVN_PASS")
                }
            }
        } else {
            mavenLocal()
        }
    }

    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            artifactId = "mypet"
        }
    }
}
