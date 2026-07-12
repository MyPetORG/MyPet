# MyPet E2E tests (plugwright)

Real Paper server + Mineflayer bots, driven by the
`io.github.drownek.plugwright` Gradle plugin. **Local development only — not
wired into CI.**

## Requirements
- Node.js 16+ on the PATH (`npm` is invoked by the Gradle task)
- Java 21 (same as the main build)

`package.json` pins `overrides.mineflayer: "^4.37.1"` — plugwright 2.0.2's
own pinned mineflayer 4.25.0 has no MC 1.21.11 protocol support, and without
the override every bot connect fails with "Server version '1.21.11' is not
supported".

## First-time setup

None beyond the requirements. On a fresh clone (`node_modules/` is
gitignored), `plugwrightTest` detects the missing `node_modules` here and
runs `npm install` in this directory itself before compiling and running the
specs (verified in the Gradle plugin's `PlugwrightTestTask` source — this is
a separate, working code path from `plugwrightInit`'s broken install). That
install honors the committed `package.json` (including the mineflayer
override above) and runs its `postinstall` hook, which auto-applies the port
patch (`scripts/patch-port.mjs` — see Port below). Running `npm install`
here manually first is equivalent, just not required.

## Run

    ./gradlew plugwrightTest                            # full suite (~100s of tests, ~2min build per full run)
    ./gradlew plugwrightTest -PtestFiles="menus"        # one spec file (substring match on basename/path)
    ./gradlew plugwrightTest -PtestNames="petcall"      # filter by test title (substring)

No configuration-cache flag is needed: plugwright 2.0.2's task classes are
incompatible with Gradle's configuration cache (they hold `Project`
references that NPE on cache restore), so `build.gradle.kts` marks every
plugwright task `notCompatibleWithConfigurationCache` — Gradle skips
storing/reusing the cache for these runs automatically. (Historical note:
before that marker existed, bare runs could hit
`Cannot invoke "Project.getLogger()" because "this.$project" is null`.)

Do **not** run `./gradlew plugwrightInit`. It only exists to scaffold a fresh
`src/test/e2e` from nothing, its own `npm install` step is broken on Gradle 9
regardless of the config-cache flag, and re-running it clobbers
`package.json` — including the `overrides.mineflayer` pin above — with the
scaffold template. All harness files are already committed and dependency
installation is automatic (First-time setup above); if `node_modules` ever
needs a clean reinstall, run `npm install` directly in this directory.

### Port

The server binds `25599`, not plugwright's hardcoded default `25565`, so a
local run coexists with anything else already bound to 25565 (e.g. a
Velocity proxy). This is a two-part redirect and both halves must stay in
sync:
1. `build.gradle.kts`'s `writeFiles` block stages a `server.properties` with
   `server-port=25599`.
2. `scripts/patch-port.mjs` (this package's `postinstall`) rewrites the two
   hardcoded `port: 25565` literals inside the installed
   `@drownek/plugwright` runner. It's idempotent and fails loudly if a
   plugwright upgrade changes `runner.js` in a way it no longer recognizes,
   so the redirect can't silently stop applying. It re-runs automatically
   after every `npm install`.

Before a run, confirm the port is actually free (`lsof -i :25599`) — a
crashed previous run can leak its Paper server process holding the port,
which fails the next run with "FAILED TO BIND TO PORT". Only kill processes
you've confirmed via `lsof`/`ps` are this project's `plugwright-run` server;
never touch anything on 25565.

**Hazard — concurrent Gradle builds:** a second Gradle daemon building this
repo at the same time (e.g. an IDE build or a `clean` build in another
terminal) can delete `build/plugwright-run` out from under a live test run,
producing `SQLITE_READONLY_DBMOVED` / missing-file errors that look like a
test bug but aren't. Check for other sessions' builds before a long run.

### World seed (pinned)

The staged `server.properties` also pins `level-seed`: each `plugwrightTest`
invocation generates a fresh world, and on a random seed whose world spawn
lands hundreds of blocks from the origin, every bot's login->ARENA teleport
becomes a long-range tp — the known trigger for degraded bot sessions whose
melee/interact packets are silently dropped (one such seed failed 15
bot-interaction tests in an otherwise-green suite). The pinned seed spawns
bots ~25 blocks from the arena. Don't remove it.

## Layout

    lib/          harness: oracle.ts (tellraw-condition polling assertions),
                  locale.ts (Message.* key -> English fragment, resolved from
                  the compiled plugin's locale bundle), pets.ts (createPet /
                  removePet / petadmin wrappers), world.ts (arena setup,
                  victim spawn/cleanup, bot melee), gui.ts (pet-menu /
                  release-confirm / trade navigation), players.ts (secondBot —
                  spawns + ops + arena-teleports an extra bot for multiplayer
                  specs, with a dispose() disconnect), config.ts (live
                  config.yml flip + /mypet reload), economy.ts (fundBot via
                  PlayerPoints /points, expectBalanceReply via a {since}-scoped
                  /points me)
    testdata/skilltrees/
                  deterministic *.st.json fixtures, one per skill plus
                  variants (all chances 100%, huge magnitudes) — staged into
                  the run directory by the Gradle writeFiles block so every
                  skill test exercises a real skilltree load, not a
                  hand-built in-memory one
    testdata/legacy/
                  pre-built MyPet 3.x SQLite pets.db (generate_mypet3_db.py)
                  staged as the plugin's own DB — every run boots "just
                  upgraded from 3.x" (migration/ proves the conversion)
    scripts/patch-port.mjs
                  postinstall port redirect (see above)
    smoke.spec.ts             MyPet enables; a pet can be created; its menu opens
    taming/                   vanilla-taming gate (cow+lead, wolf pre-tame requirement),
                               leash-flags.spec.ts (per-flag LeashRequirements matrix)
    commands/                 call, player-commands, admin-commands, misc-commands
    gui/                      menus.spec.ts (pet hub, Stay toggle, pet-selection,
                               choose-skilltree, release, backpack),
                               settings-menus.spec.ts (healthbar, pet-volume)
    skills/                   damage, on-hit (Fire/Poison/Slow/Wither/Bleed/Lightning),
                               combat-misc (Knockback/Stomp/Thorns/Ranged),
                               defense (Shield/Heal/Life/Sprint),
                               interaction (Ride/Control/Pickup/Beacon/Behavior),
                               ride-flight (fly + fuel window)
    ai/                       follow.spec.ts, behavior-modes.spec.ts (Friendly/
                               Normal/Aggressive/Raid), regressions.spec.ts
                               (bat-stay, released-speed, backpack-dupe)
    systems/                  leveling, feeding, equipment, environment
    multiplayer/              trade, ownership, pvp, beacon-party (second bot
                               via lib/players.ts's secondBot fixture)
    economy/                  shop (buy + denial), fees (skilltree switch fee —
                               an EXP penalty, not a currency charge),
                               respawn-cost (/petrespawn show/pay + denial)
    migration/                mypet3.spec.ts (v3 -> v4 DB auto-migration)
    persistence/              store/switch round-trip (exp + skilltree survive)

101 tests across 31 spec files, green in the last full-suite flake gate
(2 consecutive runs, ~6min per run including the Gradle build).

## Economy plugins (downloaded, not committed)

`build.gradle.kts`'s `downloadPlugins` block pins two extra jars into the
run directory's `plugins/` for the `economy/` suite (and the parts of
`/petrespawn` + `/petshop` that need a live economy):

- **VaultUnlocked 2.20.2** — maintained drop-in fork of classic Vault;
  keeps the `net.milkbowl.vault` service API MyPet's `VaultHook` targets
  (boot log shows `[MyPet] Vault (2.20.2) (Economy: ...) hook activated.`).
- **PlayerPoints 3.3.5** — the Vault economy provider (`points give|set
  <player> <amount>` from console, `/points me` for the balance reply;
  integer "Points" currency). Its `plugins/PlayerPoints/config.yml` is
  staged with `vault: true` (the hook is off by default). Chosen for its
  single root command: it shadows NO vanilla command names. EssentialsX
  was tried first and rejected — it intercepts `give`/`kill`/`tp`/`time`/
  `clear`/`xp`, and even with all of them in its `disabled-commands` list
  its pass-through re-dispatch drops the `execute` source context, so
  every `execute at <player> run tp/kill ... ~ ~ ~` in the suite silently
  re-anchored to world spawn (~15 base tests broke). Do not swap a
  kitchen-sink plugin in here; any candidate must not register vanilla
  command names.

The `MYPET_TEST_MYSQL` opt-in below is unaffected — both plugins are
repository-agnostic, and the config.yml staging keeps every always-on key
(including the economy fee/cost pins) in the same single `file()` call as
the conditional MySQL block.

## MySQL backend (optional, opt-in)

By default the suite runs on the plugin's default SQLite repository — no
setup needed. To exercise the MySQL repository instead, set
`MYPET_TEST_MYSQL` before invoking Gradle:

    MYPET_TEST_MYSQL=localhost:3306/mypet:root:root ./gradlew plugwrightTest

Format: `host:port/database:user:password` (password may be empty). When
set, `build.gradle.kts`'s `writeFiles` block stages
`MyPet.Repository.Type: MySQL` plus the matching `MyPet.Repository.MySQL.*`
connection keys in the run's `config.yml`, in addition to (not instead of)
the `DropWhenOwnerDies` key the regression suite needs — both are written by
a single `file()` call so neither staging clobbers the other. When unset,
the run stays on SQLite and only `DropWhenOwnerDies` is staged. This switches
the **whole run's** repository backend (every spec, not just persistence
ones) since there's no way to swap the backend mid-server-lifetime; there is
no dedicated MySQL-only spec directory — the existing suite (particularly
`persistence/persistence.spec.ts`) already exercises the repository, so
running the full suite (or `-PtestFiles="persistence"`) with the env var set
is the intended way to validate the MySQL path.

You need a MySQL server reachable at that host:port with the database
already created (the plugin does not create the database itself, only its
tables). Not exercised by an automated flake gate in this repo — treat it as
untested-live until you've run it once against a real MySQL instance.

## Rules

- Assert player-facing messages via `msgFragment('Message.X.Y')`
  (`lib/locale.ts`), never a hardcoded English string — except `/petadmin`
  admin-command replies, which really are hardcoded English in the plugin
  and are asserted literally.
- `/petadmin` subcommand replies go to whoever issued the command. Driving
  them via `server.execute(...)` (console) makes the reply invisible to the
  player bot — use `player.chat('/petadmin ...')` instead whenever a spec
  needs to assert the confirmation message.
- No dice rolls: probabilistic skills are forced to 100% via the
  `testdata/` skilltree fixtures.
- Every spec cleans up its own pets/victims (`removePet`, `killTagged`) in a
  `finally` block, since world state is **not** reset between tests within
  one `plugwrightTest` invocation (only the bot's connection is fresh).
- Server-side state a bot can't observe directly (mob NBT, entity presence)
  → `expectCondition(server, player, '<vanilla /execute condition>')`
  (`lib/oracle.ts`) — it polls the condition and reports back via tellraw,
  working around `server.execute` having no way to return output.
