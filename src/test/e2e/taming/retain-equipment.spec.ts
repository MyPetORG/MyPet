import { test, expect } from '@drownek/plugwright';
import { expectCondition } from '../lib/oracle.js';
import { msgFragment } from '../lib/locale.js';
import { removePet } from '../lib/pets.js';
import { ARENA, setupArena, killTagged, equipItem, tameSwingExpecting } from '../lib/world.js';
import { EQUIPMENT_CAPABLE_TYPES, parsePetConfig, setPetFlag } from '../lib/petconfig.js';

const sleep = (ms: number) => new Promise(r => setTimeout(r, ms));

// Spec: docs/superpowers/specs/2026-08-09-retain-equipment-on-tame-design.md (LOCKED 2026-08-09).
// Written from the acceptance criteria only, in parallel with the plugin change — no
// implementation was read, so a disagreement between this file and the plugin is a
// signal about the spec, not automatically a bug in either side.
//
// Fixture choices:
//
// (a) HUSK is the primary victim. It implements PetEquipment with the DEFAULT allowed-slot
//     set (HAND/OFF_HAND/HEAD/CHEST/LEGS/FEET), so both a weapon and a helmet are in scope
//     for the same mob; it is immune to daylight burning, which matters because setupArena
//     pins `time set day` on an open-air platform (a plain Zombie would ignite and die
//     mid-test); and it carries the default LeashRequirements ([LowHp]) — build.gradle.kts's
//     staged pet-config.yml overrides LeashRequirements for Pig/Sheep/Bee/Goat/Slime/Cat/
//     IronGolem/Rabbit/Donkey only, and no other spec uses a husk.
//
// (b) VINDICATOR appears once, for the allowed-slot boundary: it narrows getAllowedSlotNames()
//     to HAND/OFF_HAND, so a helmet on a vindicator is the "slot outside the pet type's set"
//     case the design section calls out. Also illager-safe in daylight.
//
// (c) Every mob is summoned with explicit `drop_chances`, and every assertion names a
//     specific item id. Vanilla can hand a husk random armor at spawn time and rolls its own
//     ~8.5% per-slot drop on death; pinning the chances for the slots under test and scoping
//     each assertion to one item id keeps that noise out of the result.
//
// (d) Each test uses its own item ids, so a stray ground item leaked by an earlier test (or
//     an earlier retry) can never satisfy a later test's drop assertion.
//
// (e) Taming is an in-place conversion, so the wild mob's scoreboard Tags survive onto the
//     pet — `@e[tag=...]` addresses the victim before the tame and the pet after it, with no
//     name-based re-tagging step.

/** `data merge`s the LowHp leash flag's precondition in, then confirms it landed. */
async function spawnArmedVictim(
  server: any, player: any, mob: string, tag: string, gearNbt: string,
): Promise<string> {
  server.execute(
    `summon minecraft:${mob} ${ARENA.x + 2} ${ARENA.y} ${ARENA.z} ` +
    `{Tags:["${tag}"],PersistenceRequired:1b,NoAI:1b,${gearNbt}}`);
  await expectCondition(server, player, `if entity @e[tag=${tag}]`);

  // Same shape as taming.spec.ts's cow: the default LeashRequirements are [LowHp]
  // ((health-incomingDamage)/maxHealth <= 0.1 at the tame hit). Dropping Health from
  // console is safe because a successful leash cancels the damage event.
  server.execute(`data merge entity @e[tag=${tag},limit=1] {Health:0.5f}`);
  await expectCondition(server, player, `if entity @e[tag=${tag},limit=1,nbt={Health:0.5f}]`);
  return `@e[tag=${tag},limit=1]`;
}

/** Gives a fresh lead and swings until Message.Leash.Add confirms the tame. */
async function tameWithLead(server: any, player: any, tag: string, mob: string): Promise<void> {
  server.execute(`give ${player.username} minecraft:lead 1`);
  await sleep(800);
  await equipItem(player, 'lead');
  await tameSwingExpecting(server, player, tag, mob, 'lead', msgFragment('Message.Leash.Add'));
}

/**
 * Asserts `itemId` came to rest as a world item near `anchor`.
 *
 * The owner bot stands in melee reach of every drop this spec produces, so the stack can be
 * auto-picked-up (10-tick vanilla pickup delay) before the ground poll lands. Either resting
 * place proves the drop happened, so both are accepted — the same dual assertion
 * systems/equipment.spec.ts uses for the sheared-off horse armor.
 */
async function expectDropped(
  server: any, player: any, anchor: string, itemId: string,
): Promise<void> {
  try {
    await expectCondition(server, player,
      `${anchor} if entity @e[type=minecraft:item,distance=..8,nbt={Item:{id:"minecraft:${itemId}"}}]`,
      { timeout: 6000 });
  } catch {
    await expect(player).toContainItem(itemId, { timeout: 6000 });
  }
}

/** `unless`-form of the equipment read, for the "pet came out bare" assertions. */
const wearing = (tag: string, slot: string, itemId: string) =>
  `entity @e[tag=${tag},limit=1,nbt={equipment:{${slot}:{id:"minecraft:${itemId}"}}}]`;

/**
 * Re-grants op after a mid-test `deOp()`.
 *
 * `makeOp()`'s own poll scans the whole message buffer, so a stale "Made X a server
 * operator" line from an earlier op satisfies it instantly — it can return before the
 * grant has actually landed. Assert a `{since}`-scoped confirmation instead, then give
 * the client's command tree a moment to resync before the next admin-gated command
 * (`/mypet reload config` in the restore step below). economy/fees.spec.ts hits and
 * documents the same two races.
 */
async function restoreOp(player: any): Promise<void> {
  const since = player.getMessageBufferIndex();
  await player.makeOp();
  await expect(player).toHaveReceivedMessage('server operator', { since, timeout: 5000 });
  await sleep(500);
}

function cleanup(server: any, player: any, tag: string, entityType: string): void {
  removePet(server, player);
  killTagged(server, tag);
  server.execute(`kill @e[type=minecraft:${entityType}]`);
  server.execute('kill @e[type=minecraft:item]');
  server.execute(`clear ${player.username}`);
}

// ---------------------------------------------------------------------------
// Criterion 1 — key registration
// ---------------------------------------------------------------------------

// Reads the live pet-config.yml rather than a staged fixture: build.gradle.kts stages only a
// partial pet-config.yml (LeashRequirements for nine types), and ConfigurationLoader.setDefault
// fills every other row on first boot — so the file on disk IS the fresh-boot output for this key.
//
// Declared first in the file on purpose: the later tests flip RetainEquipmentOnTame on Husk and
// Vindicator and restore it in `finally`, and plugwright runs a file's tests in declaration
// order. A failure here after a crashed later test is still a true failure (the flag was left
// flipped), just with a different root cause.
test('fresh boot writes RetainEquipmentOnTame: true under every equipment-capable type, and no other type', async () => {
  const types = parsePetConfig();

  // Guard against a vacuous pass: the file must actually enumerate pet types, including
  // types that are NOT equipment-capable (otherwise the negative half below proves nothing).
  const nonEquipmentSamples = ['Cow', 'Pig', 'Sheep', 'Wolf', 'Creeper', 'Chicken'];
  const missingSamples = nonEquipmentSamples.filter(t => !types.has(t));
  if (missingSamples.length) {
    throw new Error(`pet-config.yml has no block for ${missingSamples.join(', ')} — `
      + `parse or boot problem, not a flag problem (${types.size} type blocks seen)`);
  }

  // PetType is auto-discovered from Bukkit's EntityType enum, so equipment-capable types
  // introduced in a newer Minecraft than the harness server (CamelHusk, Nautilus, Parched,
  // ZombieNautilus at time of writing) legitimately have no block at all. Assert over the
  // intersection, with a floor so a broken parse can't shrink it to nothing.
  const present = EQUIPMENT_CAPABLE_TYPES.filter(t => types.has(t));
  if (present.length < 20) {
    throw new Error(`only ${present.length} equipment-capable types found in pet-config.yml `
      + `(expected most of ${EQUIPMENT_CAPABLE_TYPES.length}) — parse or registration problem`);
  }

  const wrong = present
    .map(t => [t, types.get(t)!.get('RetainEquipmentOnTame')] as const)
    .filter(([, value]) => value !== 'true');
  if (wrong.length) {
    throw new Error('RetainEquipmentOnTame must default to true for every equipment-capable type; '
      + `wrong or missing on: ${wrong.map(([t, v]) => `${t}=${v ?? '<absent>'}`).join(', ')}`);
  }

  const leaked = [...types.entries()]
    .filter(([name, keys]) => keys.has('RetainEquipmentOnTame')
      && !EQUIPMENT_CAPABLE_TYPES.includes(name))
    .map(([name]) => name);
  if (leaked.length) {
    throw new Error(`RetainEquipmentOnTame registered for non-equipment types: ${leaked.join(', ')}`);
  }
});

// ---------------------------------------------------------------------------
// Criterion 2 — flag true keeps the gear on the pet
// ---------------------------------------------------------------------------

// The criterion's second half ("readable through PetEquipment.getEquipment(slot)") has no
// direct e2e oracle: no command, GUI or placeholder exposes the pet's domain-side equipment
// map, and a despawn/recall round-trip is not a proof either (recall restores the mob from its
// captured vanilla NBT snapshot, so gear merely left ON the entity would survive that too).
// The two drop tests below ARE that proof — PetImpl#dropEquipment iterates the domain map and
// nothing else, so gear that was never registered as MyPet equipment drops nothing.
test('flag true: taming an armed husk leaves its gear on the pet', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const tag = 'req_keep';
  try {
    removePet(server, player);
    await setPetFlag(player, 'Husk', 'RetainEquipmentOnTame', true);
    await spawnArmedVictim(server, player, 'husk', tag,
      'equipment:{mainhand:{id:"minecraft:iron_sword",count:1},head:{id:"minecraft:iron_helmet",count:1}},'
      + 'drop_chances:{mainhand:0.0f,head:0.0f}');

    await tameWithLead(server, player, tag, 'husk');

    // Same entity, same tag: the gear must still be visible on it after the conversion.
    await expectCondition(server, player, `if ${wearing(tag, 'mainhand', 'iron_sword')}`);
    await expectCondition(server, player, `if ${wearing(tag, 'head', 'iron_helmet')}`);

    // And it really is a MyPet pet, not just a leashed mob (taming.spec.ts's probe).
    player.chat('/petcall');
    await expectCondition(server, player,
      `at ${player.username} if entity @e[type=minecraft:husk,distance=..10]`);
  } finally {
    cleanup(server, player, tag, 'husk');
  }
});

// ---------------------------------------------------------------------------
// Criterion 3 — the retained gear drops on release and on release-on-death
// ---------------------------------------------------------------------------

// RemoveAfterRelease is pinned true for the duration of this test. See the SPEC QUESTION in
// the handoff: on the default RemoveAfterRelease=false, release converts the pet back into a
// live wild mob and the gear rides out on that mob instead of hitting the ground, so "drops
// the retained gear as items" is only observable on the remove-after-release path.
test('flag true: releasing the pet drops the retained gear as items', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const tag = 'req_release';
  try {
    removePet(server, player);
    server.execute(`clear ${player.username}`);
    server.execute('kill @e[type=minecraft:item]');
    await setPetFlag(player, 'Husk', 'RetainEquipmentOnTame', true);
    await setPetFlag(player, 'Husk', 'RemoveAfterRelease', true);
    await spawnArmedVictim(server, player, 'husk', tag,
      'equipment:{mainhand:{id:"minecraft:golden_sword",count:1},head:{id:"minecraft:golden_helmet",count:1}},'
      + 'drop_chances:{mainhand:0.0f,head:0.0f}');

    await tameWithLead(server, player, tag, 'husk');
    // Baseline — without this a "nothing was ever equipped" bug would look like a drop bug.
    await expectCondition(server, player, `if ${wearing(tag, 'mainhand', 'golden_sword')}`);

    // `/petrelease <name>` skips the click-to-confirm chat component a bot cannot click
    // (commands/misc-commands.spec.ts). Rename first so the name is known and locale-independent
    // — an untouched tamed pet is named from the client's vanilla entity translation.
    player.chat(`/petadmin name ${player.username} ReqRelease`);
    await sleep(1000);
    const since = player.getMessageBufferIndex();
    player.chat('/petrelease ReqRelease');
    await expect(player).toHaveReceivedMessage(msgFragment('Message.Command.Release.Success'), { since });

    // The pet entity is gone on this path, so anchor the ground search at the owner — the pet
    // was standing in melee reach when it was released.
    await expectDropped(server, player, `at ${player.username}`, 'golden_sword');
    await expectDropped(server, player, `at ${player.username}`, 'golden_helmet');
  } finally {
    await setPetFlag(player, 'Husk', 'RemoveAfterRelease', false).catch((e: unknown) => console.warn('[retain-equipment] failed to restore pet-config flag:', e));
    cleanup(server, player, tag, 'husk');
  }
});

test('flag true: a pet killed with ReleaseOnDeath drops the retained gear as items', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const tag = 'req_death';
  try {
    removePet(server, player);
    server.execute(`clear ${player.username}`);
    server.execute('kill @e[type=minecraft:item]');
    await setPetFlag(player, 'Husk', 'RetainEquipmentOnTame', true);
    await setPetFlag(player, 'Husk', 'ReleaseOnDeath', true);
    await spawnArmedVictim(server, player, 'husk', tag,
      'equipment:{mainhand:{id:"minecraft:diamond_sword",count:1},head:{id:"minecraft:diamond_helmet",count:1}},'
      + 'drop_chances:{mainhand:0.0f,head:0.0f}');

    await tameWithLead(server, player, tag, 'husk');
    await expectCondition(server, player, `if ${wearing(tag, 'mainhand', 'diamond_sword')}`);

    // DEOP BEFORE THE KILL — do not "restore" makeOp() here, the test cannot pass with it.
    // Release-on-death is gated on !Permissions.has(owner, AdminPermissions.BYPASS_DEATH)
    // (PetDeathListener), Permissions.has is `isOp() || hasPermission(node)`, and
    // MyPet.bypass.death is `default: op` (plugin.yml) — so an opped owner is doubly exempt
    // and the pet takes the ordinary respawn timer ("<name> will respawn in 10 sec.") instead
    // of the release branch this test asserts. The bot must be a plain player at the moment
    // of the kill; op is restored in the `finally` for the config-restore step.
    await player.deOp();

    // drop_chances 0 on both slots means vanilla's own death drop cannot produce these two
    // ids — anything on the ground below came from MyPet's dropEquipment().
    const since = player.getMessageBufferIndex();
    server.execute(`kill @e[tag=${tag}]`);
    // Release.Dead (not Release.Success) is the ReleaseOnDeath branch's own message: it proves
    // the pet was permanently released rather than sent into the ordinary respawn timer.
    await expect(player).toHaveReceivedMessage(msgFragment('Message.Command.Release.Dead'), { since });

    await expectDropped(server, player, `at ${player.username}`, 'diamond_sword');
    await expectDropped(server, player, `at ${player.username}`, 'diamond_helmet');
  } finally {
    // Undo the deop first — `/mypet reload config` inside setPetFlag is admin-gated, so the
    // flag restore below silently no-ops on a deopped bot and leaves ReleaseOnDeath: true in
    // the run's pet-config.yml for every later test. Swallowed so a restore failure cannot
    // mask a real assertion error. (Only this test needs it: the runner hands every test a
    // fresh Test_<uuid> bot, so the deop cannot leak into the next one.)
    await restoreOp(player).catch((e: unknown) => console.warn('[retain-equipment] failed to restore op:', e));
    await setPetFlag(player, 'Husk', 'ReleaseOnDeath', false).catch((e: unknown) => console.warn('[retain-equipment] failed to restore pet-config flag:', e));
    cleanup(server, player, tag, 'husk');
  }
});

// ---------------------------------------------------------------------------
// Criterion 4 — flag false strips the gear and drops it at the tame location
// ---------------------------------------------------------------------------

test('flag false: taming the same husk leaves the pet bare and drops the gear at the tame location', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const tag = 'req_strip';
  try {
    removePet(server, player);
    server.execute(`clear ${player.username}`);
    server.execute('kill @e[type=minecraft:item]');
    await setPetFlag(player, 'Husk', 'RetainEquipmentOnTame', false);
    await spawnArmedVictim(server, player, 'husk', tag,
      'equipment:{mainhand:{id:"minecraft:netherite_sword",count:1},head:{id:"minecraft:netherite_helmet",count:1}},'
      + 'drop_chances:{mainhand:0.0f,head:0.0f}');

    await tameWithLead(server, player, tag, 'husk');

    // Bare: the slots the pet type allows were cleared on the live entity.
    await expectCondition(server, player, `unless ${wearing(tag, 'mainhand', 'netherite_sword')}`);
    await expectCondition(server, player, `unless ${wearing(tag, 'head', 'netherite_helmet')}`);

    // Not destroyed — dropped as world items where the mob was standing. The pet entity is
    // still the tamed mob here (release-on-tame is not in play), so anchor on it.
    await expectDropped(server, player, `at @e[tag=${tag},limit=1]`, 'netherite_sword');
    await expectDropped(server, player, `at @e[tag=${tag},limit=1]`, 'netherite_helmet');
  } finally {
    await setPetFlag(player, 'Husk', 'RetainEquipmentOnTame', true).catch((e: unknown) => console.warn('[retain-equipment] failed to restore pet-config flag:', e));
    cleanup(server, player, tag, 'husk');
  }
});

// ---------------------------------------------------------------------------
// Criterion 5 — vanilla drop chances are ignored entirely
// ---------------------------------------------------------------------------

// Decision 4 of the locked spec is two-sided: "no drop-chance roll, no drop-chance zeroing".
// A 1% mainhand chance is the rare-gear case; asserting the SAME value before and after the
// tame covers both sides at once (a roll would sometimes leave the mob bare, a zeroing pass
// would rewrite 0.01 to 0). The pre-tame assertion doubles as a setup guard — if this
// server's NBT shape for per-slot drop chances is not `drop_chances`, it fails here with a
// clear message instead of making the post-tame assertion vacuous.
test('flag true: rare gear is retained and its vanilla drop chance is left untouched', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const tag = 'req_rare';
  try {
    removePet(server, player);
    await setPetFlag(player, 'Husk', 'RetainEquipmentOnTame', true);
    await spawnArmedVictim(server, player, 'husk', tag,
      'equipment:{mainhand:{id:"minecraft:trident",count:1}},drop_chances:{mainhand:0.01f}');
    await expectCondition(server, player,
      `if entity @e[tag=${tag},limit=1,nbt={drop_chances:{mainhand:0.01f}}]`);

    await tameWithLead(server, player, tag, 'husk');

    await expectCondition(server, player, `if ${wearing(tag, 'mainhand', 'trident')}`);
    await expectCondition(server, player,
      `if entity @e[tag=${tag},limit=1,nbt={drop_chances:{mainhand:0.01f}}]`);
  } finally {
    cleanup(server, player, tag, 'husk');
  }
});

// ---------------------------------------------------------------------------
// Design section — slots outside getAllowedSlotNames() are left exactly as they are
// ---------------------------------------------------------------------------

// Not one of the six numbered criteria, but stated explicitly in the design: "Slots outside
// getAllowedSlotNames() for that pet type are left exactly as they are today — neither
// imported nor stripped." PetVindicator narrows the set to HAND/OFF_HAND, so a helmet on a
// vindicator is out of scope for the flag in BOTH directions; the false side is the one that
// can be observed, since stripping is a visible state change.
test('flag false: a slot outside the pet type\'s allowed set is left alone', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const tag = 'req_slots';
  try {
    removePet(server, player);
    server.execute(`clear ${player.username}`);
    server.execute('kill @e[type=minecraft:item]');
    await setPetFlag(player, 'Vindicator', 'RetainEquipmentOnTame', false);
    await spawnArmedVictim(server, player, 'vindicator', tag,
      'equipment:{mainhand:{id:"minecraft:iron_axe",count:1},head:{id:"minecraft:chainmail_helmet",count:1}},'
      + 'drop_chances:{mainhand:0.0f,head:0.0f}');

    await tameWithLead(server, player, tag, 'vindicator');

    // HAND is allowed -> stripped and dropped.
    await expectCondition(server, player, `unless ${wearing(tag, 'mainhand', 'iron_axe')}`);
    await expectDropped(server, player, `at @e[tag=${tag},limit=1]`, 'iron_axe');

    // HEAD is not in PetVindicator.getAllowedSlotNames() -> untouched, still worn.
    await expectCondition(server, player, `if ${wearing(tag, 'head', 'chainmail_helmet')}`);
  } finally {
    await setPetFlag(player, 'Vindicator', 'RetainEquipmentOnTame', true).catch((e: unknown) => console.warn('[retain-equipment] failed to restore pet-config flag:', e));
    cleanup(server, player, tag, 'vindicator');
  }
});

// ---------------------------------------------------------------------------
// Criterion 6 — source-plugin-created pets are unaffected by the flag: NOT COVERED
// ---------------------------------------------------------------------------
//
// The adoption path the criterion is about (a source plugin spawns an already-armed creature
// and MyPet adopts it in place) cannot be reached from this harness. Custom creature types
// ARE reachable — systems/custom-pets.spec.ts registers them by writing a `Host:` block into
// pet-config.yml and running `petadmin create -f <player> mypet:<type>` — but MyPet spawns
// that host mob itself, so it has no pre-existing gear to retain or strip and the flag has
// nothing to act on. Producing a genuine armed source-spawned creature needs a real source
// plugin (MythicMobs or an API test plugin) in build.gradle.kts's downloadPlugins block,
// which this task explicitly excludes. Left as a gap, reported in the handoff.
//
// Partial cover that IS in this file: the criterion-1 test asserts RetainEquipmentOnTame is
// absent for every non-equipment-capable type, which includes custom creature types (they all
// share the ModelPet class, which does not implement PetEquipment).
