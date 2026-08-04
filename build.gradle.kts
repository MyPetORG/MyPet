import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    java
    id("com.gradleup.shadow") version "9.3.1"
    id("io.freefair.lombok") version "9.1.0"
    id("io.sentry.jvm.gradle") version "5.12.2"
    id("net.minecraftforge.licenser") version "1.2.0" apply false
    id("io.github.drownek.plugwright") version "2.0.2"
    `maven-publish`
}

group = "de.keyle"

val buildType = project.findProperty("buildType")?.toString() ?: "alpha"
val buildNumber = project.findProperty("BUILD_NUMBER")?.toString() ?: "local"
val baseVersion = "4.0.1"
val versionSuffix = when (buildType) {
    "alpha", "beta" -> {
        val buildSuffix = if (buildNumber == "local") "-local" else "-${buildNumber.padStart(2, '0')}"
        "-${buildType}${buildSuffix}"
    }
    else -> ""
}
version = "$baseVersion$versionSuffix"
val minecraftVersion by extra("1.21.11")

repositories {
    mavenCentral()
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "io.freefair.lombok")
    apply(plugin = "io.sentry.jvm.gradle")
    apply(plugin = "net.minecraftforge.licenser")

    configure<net.minecraftforge.licenser.LicenseExtension> {
        setHeader(rootProject.file("HEADER.txt"))
        include("**/*.java")
        newLine.set(true)
    }

    // Licenser scans sourceSet.allSource, which overlaps with Sentry's generated
    // resources directory. Order the tasks so Gradle 9's strict validator is satisfied.
    val sentryGenerators = tasks.matching {
        it.name.startsWith("generateSentry") || it.name.startsWith("collectExternalDependenciesForSentry")
    }
    tasks.matching { it.name.startsWith("checkLicense") || it.name.startsWith("updateLicense") }.configureEach {
        mustRunAfter(sentryGenerators)
    }

    repositories {
        mavenCentral()
        maven { url = uri("https://mvn.lib.co.nz/spigot/") }
        maven { url = uri("https://repo.md-5.net/content/groups/public/") }
        maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
        maven { url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/") }
        maven { url = uri("https://oss.sonatype.org/content/groups/public/") }
        maven { url = uri("https://maven.enginehub.org/repo/") }
        maven { url = uri("https://hub.spigotmc.org/nexus/content/groups/public/") }
        maven { url = uri("https://repo.md-5.net/content/repositories/public") }
        maven { url = uri("https://dependency.download/releases") } // FactionsUUID
        maven { url = uri("https://maven.citizensnpcs.co/repo") } // Citizens
        maven { url = uri("https://nexus.neetgames.com/repository/maven-public/") } // mcMMO
        maven { url = uri("https://repo.codemc.io/repository/maven-public/") } // StackMob
        maven { url = uri("https://repo.glaremasters.me/repository/towny/") } // Towny
        maven { url = uri("https://repo.glaremasters.me/repository/public/") } // Guilds
        maven { url = uri("https://repo.roinujnosde.me/releases/") } // SimpleClans
        maven { url = uri("https://nexus.hc.to/content/repositories/pub_releases/") } // Heroes
        maven { url = uri("https://nexus.sirblobman.xyz/public/") } // CombatLogX
        maven { url = uri("https://repo.battleplugins.org/snapshots") } // BattleArena
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://repo.mypet-plugin.de/") }
        maven { url = uri("https://mvn.lumine.io/repository/maven-public/") } // MythicMobs
    }

    // Use lazy task configuration for better configuration performance
    tasks.named("processResources") { enabled = false }
    tasks.named("test") { enabled = false }
    tasks.named("compileTestJava") { enabled = false }
    tasks.named("processTestResources") { enabled = false }

    if (project.name != "api") {
        plugins.withId("maven-publish") {
            tasks.withType<PublishToMavenRepository>().configureEach { enabled = false }
            tasks.withType<PublishToMavenLocal>().configureEach { enabled = false }
        }
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }
}

val archivesBaseName = "MyPet"

val downloadTranslations by tasks.register<Exec>("downloadTranslations") {
    group = "resources"
    description = "Downloads MyPet translations into plugin module resources"
    val targetDir = project(":plugin").layout.buildDirectory.dir("resources/main/locale").get().asFile
    outputs.dir(targetDir)

    // Skip download if translations are less than 12 hours old
    val maxAgeMillis = 12 * 60 * 60 * 1000L // 12 hours in milliseconds
    onlyIf {
        if (!targetDir.exists()) {
            true
        } else {
            val ageMillis = System.currentTimeMillis() - targetDir.lastModified()
            val shouldDownload = ageMillis > maxAgeMillis
            if (!shouldDownload) {
                logger.lifecycle("Skipping translation download - last updated ${ageMillis / (60 * 60 * 1000)} hours ago (max: 12 hours)")
            }
            shouldDownload
        }
    }

    doFirst {
        if (targetDir.exists()) targetDir.deleteRecursively()
        targetDir.mkdirs()
    }
    commandLine(
        "git", "clone", "--depth", "1", "--single-branch",
        "https://github.com/MyPetORG/MyPet-Translations.git", targetDir
    )
    doLast {
        // Clean up files we don't need in the final JAR
        // Use direct File operations for configuration cache compatibility
        File(targetDir, ".git").deleteRecursively()
        File(targetDir, ".gitignore").delete()
        File(targetDir, "README.md").delete()
        File(targetDir, "exclude").deleteRecursively()
    }
}

// Root project no longer has src/main — resources are in :plugin module
tasks.processResources { enabled = false }

// Only the official CI has this secret. Forks and self-compiled builds get an empty DSN, so
// SentryErrorReporter stays disabled instead of silently reporting a fork's errors to our project.
val sentryDsn = System.getenv("SENTRY_DSN") ?: ""

fun Manifest.attributesForMyPet() = attributes(
    mapOf(
        "Main-Class" to "de.Keyle.MyPet.Main",
        "Project-Name" to project.name,
        "Project-Version" to version,
        "Project-Build" to buildNumber,
        "Project-Type" to buildType,
        "Sentry-Dsn" to sentryDsn
    )
)

tasks.jar {
    archiveBaseName.set(archivesBaseName)
    archiveFileName.set("${archivesBaseName}-${version}.jar")
    archiveVersion.set(project.version.toString())
    manifest { attributesForMyPet() }
}

/* ---------- Shading without JVM attribute conflicts ---------- */

// Create a resolvable-only configuration to collect jars to shade.
val shade by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
    // Mark as runtime usage and leave TargetJvmVersion unset.
    attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
}

dependencies {
    // Pull submodules as built jars (regardless of their target JVM)
    add("shade", project(path = ":plugin", configuration = "runtimeElements"))
    add("shade", project(path = ":api", configuration = "runtimeElements"))
    add("shade", project(path = ":skills", configuration = "runtimeElements"))

    // External libs to be shaded
    add("shade", "org.bstats:bstats-bukkit:3.2.1")
    add("shade", "net.kyori:adventure-nbt:4.17.0")
}

// Build the shaded jar strictly from the 'shade' configuration
tasks.shadowJar {
    archiveBaseName.set(archivesBaseName)
    archiveFileName.set("${archivesBaseName}-${project.version}.jar")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")
    exclude("META-INF/**")
    manifest { attributesForMyPet() }

    dependsOn(shade)

    configurations = listOf(shade)

    relocate("org.bstats", "de.Keyle.MyPet.util.metrics")
}

tasks.assemble { dependsOn(tasks.shadowJar) }
tasks.build { dependsOn(tasks.shadowJar) }

sentry {
    // Only bundle sources when auth token is available (snapshot/release builds)
    // PR builds don't have the token and don't need source bundling
    includeSourceContext = !System.getenv("SENTRY_AUTH_TOKEN").isNullOrEmpty()

    org = "mypet"
    projectName = "mypet"
    authToken = System.getenv("SENTRY_AUTH_TOKEN")
}

/* ---------- E2E tests (plugwright) ---------- */

// plugwright 2.0.2 holds Project references in its task classes, which NPE on a
// configuration-cache restore ("this.$project" is null). Declaring the tasks
// incompatible makes Gradle skip the cache for builds that run them, so plain
// `./gradlew plugwrightTest` works without remembering --no-configuration-cache.
tasks.matching { it.name.startsWith("plugwright") }.configureEach {
    notCompatibleWithConfigurationCache("plugwright 2.0.2 task classes reference Project at execution time")
}

plugwright {
    minecraftVersion.set(project.extra["minecraftVersion"] as String)
    testsDir.set(file("src/test/e2e"))
    runDir.set(file("build/plugwright-run"))
    acceptEula.set(true)

    // The economy suite (economy/*.spec.ts) needs a Vault economy provider for
    // /petshop, /petchooseskilltree's switch fee, and /petrespawn pay/auto
    // (both jars confirmed 1.21.11-compatible via Modrinth's version API):
    //  - VaultUnlocked 2.20.2 — maintained MilkBowl/Vault fork, same
    //    net.milkbowl.vault Economy/Permission API MyPet's VaultHook.java targets.
    //  - PlayerPoints 3.3.5 — the economy provider (/points, integer "Points"
    //    currency); Vault hook enabled via its own config.yml below.
    // PlaceholderAPI 2.12.2 (placeholder/*.spec.ts) — matches the compileOnly
    // version in plugin/build.gradle.kts; MyPet auto-registers its expansion when
    // present, and the suite reads placeholders back via `/papi parse`. Safe for
    // all suites: it registers only `/papi`, shadowing no vanilla command.
    downloadPlugins {
        url("https://cdn.modrinth.com/data/ayRaM8J7/versions/cLNipSgw/VaultUnlocked-2.20.2.jar")
        url("https://cdn.modrinth.com/data/bPX4jcVd/versions/Jgohk2ua/PlayerPoints-3.3.5.jar")
        url("https://cdn.modrinth.com/data/lKEzGugV/versions/UmbIiI5H/PlaceholderAPI-2.12.2.jar")
        // WorldEdit is a hard dependency of WorldGuard; both must support MC 1.21.11.
        // WorldGuard 7.0.18 dropped 1.21.11 — do not bump past 7.0.17 without rechecking.
        url("https://cdn.modrinth.com/data/1u6JkXh5/versions/qNuPcliz/worldedit-bukkit-7.4.4.jar")
        url("https://cdn.modrinth.com/data/DKY9btbd/versions/pI4UHLJL/worldguard-bukkit-7.0.17.jar")
    }

    writeFiles {
        // Custom port so the test server coexists with a local Velocity proxy on 25565;
        // src/test/e2e/scripts/patch-port.mjs (npm postinstall) patches the bot side to match.
        // level-seed pin: a random seed can spawn far from the origin, turning
        // setupArena's teleport (login -> ARENA at 0,200,0) into a long-range
        // tp that triggers the degraded-session/dropped-packet bug documented
        // in the suite's helper comments (a bad seed failed 15 bot-interaction
        // tests). This seed's spawn lands near (0,~65,0), keeping that hop short.
        file("server.properties", """
            server-port=25599
            online-mode=false
            connection-throttle=0
            spawn-protection=0
            level-seed=8678942899319966093
        """.trimIndent())

        // DropWhenOwnerDies: true — ai/regressions.spec.ts's backpack-dupe test
        // needs it on. YAML path is "MyPet.Skill.Backpack.*", NOT
        // "MyPet.Skilltree.Skill.Backpack.*" despite the Java nesting (same
        // pattern as CONTROL_ITEM -> "MyPet.Skill.Control.Item"). MyPet's
        // loader fills in defaults for missing keys, so a partial file is safe.
        //
        // MYPET_TEST_MYSQL=host:port/db:user:pass opts the run into the MySQL
        // repository instead of SQLite; both branches share ONE file() call
        // since writeFiles keeps only the last file() registered per path (a
        // second call for MySQL would discard the DropWhenOwnerDies staging).
        val mysqlSpec = System.getenv("MYPET_TEST_MYSQL")
        val mysqlMatch = mysqlSpec?.let {
            Regex("([^:]+):(\\d+)/([^:]+):([^:]*):(.*)").matchEntire(it)
                ?: error("MYPET_TEST_MYSQL must be host:port/db:user:pass")
        }

        file("plugins/MyPet/config.yml", buildString {
            appendLine("MyPet:")
            appendLine("  Skill:")
            appendLine("    Backpack:")
            appendLine("      DropWhenOwnerDies: true")
            // HungerSystem.Time: 4 — systems/feeding.spec.ts's hunger-decay test
            // needs several decrements inside the 30s per-test cap; default 60
            // is far too slow. Not lower: Time=2 decremented ~1.05s after spawn,
            // inside the refusal test's setup, causing a flake (pet already
            // below full saturation at feed time). feeding.spec.ts verifies
            // "full" via /petinfo right before the refusal feed regardless, so
            // this is margin, not a correctness dependency. Safe suite-wide:
            // every spec's pet is fresh (saturation 100) and no test runs
            // anywhere near the ~396s needed to reach starvation.
            appendLine("  HungerSystem:")
            appendLine("    Time: 4")
            // DisablePetVersusPlayer: true — default is false (PvP allowed);
            // multiplayer/pvp.spec.ts needs a genuine deny baseline. The spec
            // proves the gate is runtime-flippable by editing this file and
            // running `/mypet reload config` mid-run, restoring true after. No
            // other spec touches this key.
            appendLine("  DisablePetVersusPlayer: true")
            // SwitchFee Fixed=25/Percent=0 — economy/fees.spec.ts. Despite the
            // name, CommandChooseSkilltree.applySkilltree charges this as an
            // EXPERIENCE penalty on the pet, not a Vault currency charge.
            // Percent=0 makes the deduction exactly 25 regardless of seeded exp.
            appendLine("  Skilltree:")
            appendLine("    SwitchFee:")
            appendLine("      Fixed: 25")
            appendLine("      Percent: 0")
            // Respawn Time Factor=0/Fixed=10, EconomyCost Factor=0/Fixed=50 —
            // economy/respawn-cost.spec.ts. Zeroing both TIME_*_FACTOR keys
            // makes the respawn timer a flat 10s regardless of death cause or
            // pet level. Cost must come from Fixed, not Factor: respawnTime
            // ticks down 1/sec, so a Factor-based cost decays while the test
            // runs (observed: `/petrespawn show` printed 50 but paying 2s
            // later charged 40). Fixed=50 keeps the charge countdown-immune.
            appendLine("  Respawn:")
            appendLine("    Time:")
            appendLine("      Default:")
            appendLine("        Factor: 0")
            appendLine("        Fixed: 10")
            appendLine("      Player:")
            appendLine("        Factor: 0")
            appendLine("        Fixed: 10")
            appendLine("    EconomyCost:")
            appendLine("      Factor: 0")
            appendLine("      Fixed: 50")
            if (mysqlMatch != null) {
                val (host, port, db, user, pass) = mysqlMatch.destructured
                appendLine("  Repository:")
                appendLine("    Type: MySQL")
                appendLine("    MySQL:")
                appendLine("      Host: $host")
                appendLine("      Port: $port")
                appendLine("      Database: $db")
                appendLine("      User: $user")
                appendLine("      Password: \"$pass\"")
            }
        }.trimEnd())

        (projectDir.resolve("src/test/e2e/testdata/skilltrees").listFiles()
            ?: error("src/test/e2e/testdata/skilltrees missing — 21 skill specs depend on it"))
            .filter { it.name.endsWith(".st.json") }
            .forEach { f -> file("plugins/MyPet/skilltrees/${f.name}", f) }

        // economy/shop.spec.ts: no pet-shops.yml ships with the plugin, so one
        // is staged here (reduced from MyPet-Configurator's example workspace
        // schema) with one cheap pet (100) and one expensive pet (1000000).
        // Balance.Type stays default (None) — the suite only exercises the
        // buyer-side withdraw, not the Bank/Player/Private payout path.
        file("plugins/MyPet/pet-shops.yml", """
            Shops:
              e2e:
                Name: E2E Shop
                Default: true
                Position: 0
                Pets:
                  cheap:
                    Name: ShopCheap
                    Description:
                      - A cheap test pet.
                    PetType: WOLF
                    EXP: 0.0
                    Price: 100.0
                  expensive:
                    Name: ShopExpensive
                    Description:
                      - An expensive test pet.
                    PetType: OCELOT
                    EXP: 0.0
                    Price: 1000000.0
        """.trimIndent())

        // PlayerPoints config.yml: `vault: true` registers it as the Vault
        // currency manager (OFF by default). Other keys stay at plugin
        // defaults (missing keys are filled in on first boot). Balances start
        // at 0 — economy/*.spec.ts's exact-delta assertions rely on that plus
        // explicit `points set` pins.
        file("plugins/PlayerPoints/config.yml", """
            vault: true
        """.trimIndent())

        // Leash-flag matrix (taming/leash-flags.spec.ts): one flag per type,
        // chosen to never collide with the base taming suite (Cow/Wolf untouched).
        //
        // YAML key is "LeashRequirements" (a string list), NOT "LeashFlags";
        // each entry is "FlagName" or "FlagName:arg1:arg2". Types not listed
        // here keep their normal @DefaultInfo.leashFlags() default.
        //
        // Two deliberate deviations from the original draft (WildFlag/
        // UserCreatedFlag only branch on Tameable/IronGolem, else always-pass
        // vacuously — moved off Mooshroom/Chicken to Cat/IronGolem). Size's
        // positional token is a MINIMUM threshold, not an exact match.
        file("plugins/MyPet/pet-config.yml", """
            MyPet:
              Pets:
                Pig:
                  LeashRequirements: [Adult]
                Sheep:
                  LeashRequirements: [Baby]
                Bee:
                  LeashRequirements: [Angry]
                Goat:
                  LeashRequirements: [Screaming]
                Slime:
                  LeashRequirements: [Size:2]
                Cat:
                  LeashRequirements: [Wild]
                IronGolem:
                  LeashRequirements: [UserCreated]
                Rabbit:
                  LeashRequirements: [World:world]
                Donkey:
                  LeashRequirements: [Impossible]
        """.trimIndent())

        // migration/mypet3.spec.ts: a pre-built MyPet 3.x-shaped SQLite
        // database (testdata/legacy/pets.db, built by generate_mypet3_db.py).
        // Staged directly as the plugin's own pets.db, not via a config
        // option: MyPet.Repository.ConvertFrom only converts between
        // same-schema backends (SQLite<->MySQL), not 3.x upgrades. The real
        // v3->v4 path is MigrationService, which auto-detects a legacy
        // install by probing the active repository's schema at boot and
        // migrates in-place — no config flag involved. persistence.spec.ts's
        // random per-test usernames don't collide with LegacyOwner's fixed rows.
        file("plugins/MyPet/pets.db",
            projectDir.resolve("src/test/e2e/testdata/legacy/pets.db").also {
                if (!it.exists()) error("src/test/e2e/testdata/legacy/pets.db missing — " +
                    "run generate_mypet3_db.py (see migration/mypet3.spec.ts)")
            })

        // Region for systems/worldguard-spawn.spec.ts. Deliberately far from ARENA
        // (0,200,0) on general principle: `mob-spawning: deny` only gates a
        // RELEASED pet (a genuine wild mob at that point) - calling/recalling a
        // pet is always exempt - but there's no upside to overlapping ARENA
        // anyway. The spec builds its own platform inside.
        file("plugins/WorldGuard/worlds/world/regions.yml", """
            regions:
              no-mobs:
                type: cuboid
                min: {x: 992, y: 0, z: 992}
                max: {x: 1056, y: 320, z: 1056}
                priority: 10
                flags:
                  mob-spawning: deny
        """.trimIndent())
    }
}

/* ---------- Root project has no source files — compilation is in submodules ---------- */
