import { test, expect } from '@drownek/plugwright';
import { expectCondition, expectConditionHolds } from '../lib/oracle.js';
import { msgFragment } from '../lib/locale.js';
import { createPet, removePet } from '../lib/pets.js';
import { setupArena } from '../lib/world.js';

const sleep = (ms: number) => new Promise(r => setTimeout(r, ms));

/**
 * Polls until <selector>'s Health equals the value captured on entry, `checks` times in a
 * row. Compares "now" against "at spawn" rather than an assumed literal max-HP value.
 */
async function expectHealthStable(
  server: any, player: any, selector: string,
  { checks = 6, interval = 700 } = {},
): Promise<void> {
  server.execute('scoreboard objectives add e2e dummy');
  server.execute(`execute store result score hp_base e2e run data get entity ${selector} Health`);
  for (let i = 0; i < checks; i++) {
    await sleep(interval);
    await expectCondition(server, player, 'if score hp_now e2e = hp_base e2e', {
      pre: [`execute store result score hp_now e2e run data get entity ${selector} Health`],
      timeout: interval + 2000,
    });
  }
}

// (a) PetZombificationListener (Hoglin/Piglin/PiglinBrute): default AllowZombification=false
//     is enforced via PetVisualSyncer setting Bukkit's setImmuneToZombification(true) at spawn
//     (not a fire-then-cancel) — vanilla's overworld timer never even increments.
//
// (b) PetLightningStrikeListener (Pig/Villager species conversion, Mooshroom variant flip,
//     Creeper-power): ALLOW_LIGHTNING_CONVERSION defaults false, cancelling the conversion
//     outright. Villager -> Witch fires EntityTransformEvent(LIGHTNING); Pig -> zombified_piglin
//     fires the separate PigZapEvent, handled by its own listener method. Tests 1 (Villager)
//     and 2 (Pig) both assert ownership survives.
//
// (c) PetEnvironmentListener has no water/drowning logic at all. The real mechanism is
//     PetSurvivalListener + the PetAquaticEntity marker: water-breathing pets take vanilla
//     DamageCause.DRYOUT out of water, not in it (a19 fix, commit 729c3fa82).
//     PREVENT_SUFFOCATION defaults true. Test 3 keeps the pet on ARENA's dry platform (no
//     water fill) since DRYOUT is an out-of-water condition.
//
// (d) PetDespawnListener: PlayerQuitEvent removes the bukkit entity and preserves the domain
//     Pet for on-rejoin respawn. Test 4 reuses rejoin()'s quit/join cycle (same pattern as
//     player-commands.spec.ts's petrespawn test) to prove the old tagged entity is gone and a
//     fresh one replaces it.

test('villager pet + lightning strike: no witch conversion, ownership survives (petcall)', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Villager', { name: 'BoltProof' });
  const sel = `@e[tag=${pet.tag},limit=1]`;
  try {
    // Vanilla Villager#thunderHit ALWAYS converts a lightning-struck villager to a witch
    // (deterministic) — this only holds because PetLightningStrikeListener cancels it (the
    // same sequence against a Pig pet fails, see the file header).
    server.execute(`execute at ${sel} run summon minecraft:lightning_bolt ~ ~ ~`);
    // Real LightningBolt strike also deals impact damage + sets the villager on fire; clear
    // the fire once the transform has resolved to avoid an unrelated burn-death.
    await sleep(300);
    server.execute(`data merge entity ${sel} {Fire:0}`);

    await expectConditionHolds(server, player,
      `if entity @e[tag=${pet.tag},type=minecraft:villager,limit=1]`, { checks: 5, interval: 600 });

    // /petcall only works through PetManager's live owner<->pet association; proves ownership
    // survived, not just that the Bukkit type still reads "villager".
    server.execute(`execute as ${sel} at @s run tp @s ~10 ~ ~`);
    const since = player.getMessageBufferIndex();
    player.chat('/petcall');
    await expect(player).toHaveReceivedMessage(msgFragment('Message.Command.Call.Success'), { since });
    await expectCondition(server, player,
      `at ${player.username} if entity @e[tag=${pet.tag},type=minecraft:villager,distance=..6]`);
  } finally {
    removePet(server, player);
    server.execute(`kill @e[tag=${pet.tag}]`);
  }
});

test('pig pet + lightning strike: no zombified-piglin conversion, ownership survives (petcall)', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Pig', { name: 'BoltProof' });
  const sel = `@e[tag=${pet.tag},limit=1]`;
  try {
    // Vanilla ALWAYS converts a lightning-struck pig to a zombified_piglin (via PigZapEvent,
    // not EntityTransformEvent); this only holds because PetLightningStrikeListener handles
    // PigZapEvent.
    server.execute(`execute at ${sel} run summon minecraft:lightning_bolt ~ ~ ~`);
    await sleep(300);
    server.execute(`data merge entity ${sel} {Fire:0}`);

    await expectConditionHolds(server, player,
      `if entity @e[tag=${pet.tag},type=minecraft:pig,limit=1]`, { checks: 5, interval: 600 });

    server.execute(`execute as ${sel} at @s run tp @s ~10 ~ ~`);
    const since = player.getMessageBufferIndex();
    player.chat('/petcall');
    await expect(player).toHaveReceivedMessage(msgFragment('Message.Command.Call.Success'), { since });
    await expectCondition(server, player,
      `at ${player.username} if entity @e[tag=${pet.tag},type=minecraft:pig,distance=..6]`);
  } finally {
    removePet(server, player);
    server.execute(`kill @e[tag=${pet.tag}]`);
  }
});

test('piglin pet in the overworld never zombifies', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Piglin', { name: 'NoZombify' });
  try {
    // Suppression is Bukkit's own immunity flag (PiglinAbstract#setImmuneToZombification),
    // applied at spawn by PetVisualSyncer — a vanilla piglin never carries this NBT tag.
    await expectCondition(server, player,
      `if entity @e[tag=${pet.tag},type=minecraft:piglin,nbt={IsImmuneToZombification:1b}]`);

    // Force vanilla's overworld-exposure counter far past threshold and hold; a non-immune
    // piglin would flip to zombified_piglin well within this window.
    server.execute(`data merge entity @e[tag=${pet.tag},limit=1] {TimeInOverworld:100000}`);
    await expectConditionHolds(server, player,
      `if entity @e[tag=${pet.tag},type=minecraft:piglin,limit=1]`, { checks: 5, interval: 600 });
  } finally {
    removePet(server, player);
    server.execute(`kill @e[tag=${pet.tag}]`);
  }
});

test('an aquatic pet out of water takes no dry-out damage (PreventSuffocation)', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  // Cod implements PetAquaticEntity; PREVENT_SUFFOCATION defaults true. ARENA's platform is
  // dry open air by construction, so a Cod spawned there is immediately out of water.
  const pet = await createPet(server, player, 'Cod', { name: 'DryOutProof' });
  const sel = `@e[tag=${pet.tag},limit=1]`;
  try {
    await expectCondition(server, player, `if entity ${sel}`); // baseline: pet exists, out of water

    // A vanilla Cod out of water takes periodic DRYOUT damage; several ticks land inside this
    // ~4s window, so health staying pinned proves PetSurvivalListener cancels DRYOUT here.
    await expectHealthStable(server, player, sel, { checks: 6, interval: 700 });
  } finally {
    removePet(server, player);
    server.execute(`kill @e[tag=${pet.tag}]`);
  }
});

test('owner logout despawns the pet; rejoin proves a fresh pet replaces the old tagged entity', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Cow', { name: 'LogoutProof' });
  try {
    // rejoin() disconnects and reconnects under the same username — a genuine
    // PlayerQuitEvent -> PlayerJoinEvent cycle. PetDespawnListener#onPlayerQuit is what
    // removes the bukkit entity on disconnect.
    await player.rejoin();

    // Without PetDespawnListener the old tagged entity would still be standing there.
    await expectCondition(server, player, `unless entity @e[tag=${pet.tag}]`);
    // A fresh, untagged Cow respawned at the (rejoined) owner proves the normal
    // join-time respawn path, not the old entity merely wandering off.
    await expectCondition(server, player,
      `at ${player.username} if entity @e[type=minecraft:cow,distance=..16]`);
  } finally {
    removePet(server, player);
  }
});
