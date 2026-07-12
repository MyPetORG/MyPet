import { test, expect } from '@drownek/plugwright';
import { expectCondition } from '../lib/oracle.js';
import { msgFragment } from '../lib/locale.js';
import { removePet } from '../lib/pets.js';
import { ARENA, setupArena, spawnVictim, killTagged, equipItem, botAttack } from '../lib/world.js';

const sleep = (ms: number) => new Promise(r => setTimeout(r, ms));

/**
 * Attempts a tame; returns true if a pet was created.
 *
 * A bot swing can silently drop server-side, so a dropped swing must never
 * read as "denied". Every attempt is VERIFIED: a landed swing has exactly
 * two observable outcomes — (a) allowed: damage is cancelled and
 * Message.Leash.Add arrives, or (b) denied: the punch's damage actually
 * lands, dropping Health (or killing the victim — covered by the -9999
 * sentinel). Retries until one is observed, then the tame outcome is read
 * via a { since }-scoped /petcall probe.
 *
 * `dim` scopes the pin and Health reads with `execute in` for victims
 * outside the overworld (World flag's deny side runs in the Nether) — a
 * plain console command defaults to overworld.
 *
 * All victims use spawnVictim's default noAI:true: these flags only check
 * static entity state, not AI behavior, and NoAI removes melee-retaliation
 * risk and keeps the pin point valid across retries (no knockback).
 */
async function tameAttempt(
  server: any, player: any, mob: string, tag: string,
  dim = 'minecraft:overworld', attempts = 8,
): Promise<boolean> {
  server.execute(`give ${player.username} minecraft:lead 1`);
  await sleep(800);
  await equipItem(player, 'lead');
  const sel = `@e[tag=${tag},limit=1]`;
  server.execute('scoreboard objectives add e2e dummy');
  server.execute(`execute in ${dim} store result score lf_base e2e run data get entity ${sel} Health 100`);
  let since = player.getMessageBufferIndex();
  let landed = false;
  for (let i = 0; i < attempts && !landed; i++) {
    if (i === 2 || i === 5) {
      // Rejoin fallback (same as tameSwingExpecting's): a bot can enter a
      // session state where use-entity packets are silently dropped for the
      // rest of the session; a fresh connection (rejoins at the same
      // position) clears it. Fires early, at attempt 2 (each verified
      // failure costs ~3s), with a second rejoin at attempt 5 in case one
      // wasn't enough.
      await player.rejoin();
      await equipItem(player, 'lead');
      since = player.getMessageBufferIndex();
    }
    // Pin the victim into melee reach, dimension-scoped (console defaults to overworld).
    server.execute(`execute in ${dim} at ${player.username} run tp ${sel} ~1.5 ~ ~`);
    await sleep(400);
    try { await botAttack(player, mob); }
    catch { await sleep(500); continue; } // bot can't see the mob yet — re-pin and retry
    // Outcome (a): tame succeeded — leash success cancels the damage event,
    // so the success message is the landed-signal (sentinel below still
    // catches a missed message).
    try {
      await expect(player).toHaveReceivedMessage(msgFragment('Message.Leash.Add'), { since, timeout: 1200 });
      landed = true;
      break;
    } catch { /* no tame message (yet) — check whether the swing landed at all */ }
    // Outcome (b): swing landed and was denied — Health dropped below
    // baseline. -9999 sentinel treats "victim gone" (died, or consumed by a
    // missed tame message) as landed too; /petcall below issues the verdict.
    try {
      await expectCondition(server, player, 'if score lf_cur e2e < lf_base e2e', {
        pre: [
          'scoreboard players set lf_cur e2e -9999',
          `execute in ${dim} store result score lf_cur e2e run data get entity ${sel} Health 100`,
        ],
        timeout: 1200, interval: 500,
      });
      landed = true;
    } catch { /* swing dropped server-side — re-pin and retry */ }
  }
  if (!landed) throw new Error(`No swing ever landed on ${mob} (tag=${tag}) after ${attempts} attempts`);
  await sleep(800); // let the async pet activation settle before probing
  const sinceCall = player.getMessageBufferIndex();
  player.chat('/petcall');
  try {
    await expect(player).toHaveReceivedMessage(msgFragment('Message.No.HasPet'), { since: sinceCall, timeout: 4000 });
    return false;
  } catch { return true; }
}

test('Adult flag: adult pig tames, baby pig does not', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  try {
    // AdultFlag.check: Ageable must be isAdult(). Default summon age (no Age tag) is adult.
    await spawnVictim(server, player, 'pig', 'lf_adult');
    if (!(await tameAttempt(server, player, 'pig', 'lf_adult'))) throw new Error('adult pig should tame');
    removePet(server, player);
    server.execute(`summon minecraft:pig ${ARENA.x + 2} ${ARENA.y} ${ARENA.z} {Tags:["lf_baby"],Age:-24000,PersistenceRequired:1b,NoAI:1b}`);
    await expectCondition(server, player, `if entity @e[tag=lf_baby]`);
    if (await tameAttempt(server, player, 'pig', 'lf_baby')) throw new Error('baby pig must NOT tame under Adult flag');
  } finally {
    killTagged(server, 'lf_adult'); killTagged(server, 'lf_baby'); removePet(server, player);
    server.execute('kill @e[type=minecraft:pig]');
  }
});

test('Baby flag: baby sheep tames, adult sheep does not', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  try {
    // BabyFlag.check: Ageable entities must NOT be isAdult().
    server.execute(`summon minecraft:sheep ${ARENA.x + 2} ${ARENA.y} ${ARENA.z} {Tags:["lf_baby_ok"],Age:-24000,PersistenceRequired:1b,NoAI:1b}`);
    await expectCondition(server, player, `if entity @e[tag=lf_baby_ok]`);
    if (!(await tameAttempt(server, player, 'sheep', 'lf_baby_ok'))) throw new Error('baby sheep should tame');
    removePet(server, player);
    await spawnVictim(server, player, 'sheep', 'lf_adult_sheep');
    if (await tameAttempt(server, player, 'sheep', 'lf_adult_sheep')) throw new Error('adult sheep must NOT tame under Baby flag');
  } finally {
    killTagged(server, 'lf_baby_ok'); killTagged(server, 'lf_adult_sheep'); removePet(server, player);
    server.execute('kill @e[type=minecraft:sheep]');
  }
});

test('Angry flag: angry bee tames, calm bee does not', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  try {
    // AngryFlag.check -> PetBee.ANGER_CHECK: bee.getAnger() > 0, backed by NBT
    // "anger_end_time" (a target gametime, not a tick counter) -- not "Anger".
    // A huge absolute value stays "in the future" for the whole test.
    server.execute(`summon minecraft:bee ${ARENA.x + 2} ${ARENA.y} ${ARENA.z} {Tags:["lf_angry"],anger_end_time:2000000000L,PersistenceRequired:1b,NoAI:1b}`);
    await expectCondition(server, player, `if entity @e[tag=lf_angry]`);
    if (!(await tameAttempt(server, player, 'bee', 'lf_angry'))) throw new Error('angry bee should tame');
    removePet(server, player);
    await spawnVictim(server, player, 'bee', 'lf_calm');
    if (await tameAttempt(server, player, 'bee', 'lf_calm')) throw new Error('calm bee must NOT tame under Angry flag');
  } finally {
    killTagged(server, 'lf_angry'); killTagged(server, 'lf_calm'); removePet(server, player);
    server.execute('kill @e[type=minecraft:bee]');
  }
});

test('Screaming flag: screaming goat tames, normal goat does not', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  try {
    // ScreamingFlag.check: Goat#isScreaming() (vanilla "IsScreamingGoat" NBT).
    server.execute(`summon minecraft:goat ${ARENA.x + 2} ${ARENA.y} ${ARENA.z} {Tags:["lf_scream"],IsScreamingGoat:1b,PersistenceRequired:1b,NoAI:1b}`);
    await expectCondition(server, player, `if entity @e[tag=lf_scream]`);
    if (!(await tameAttempt(server, player, 'goat', 'lf_scream'))) throw new Error('screaming goat should tame');
    removePet(server, player);
    await spawnVictim(server, player, 'goat', 'lf_normal_goat');
    if (await tameAttempt(server, player, 'goat', 'lf_normal_goat')) throw new Error('normal goat must NOT tame under Screaming flag');
  } finally {
    killTagged(server, 'lf_scream'); killTagged(server, 'lf_normal_goat'); removePet(server, player);
    server.execute('kill @e[type=minecraft:goat]');
  }
});

test('Size flag: large slime (>=2) tames, tiny slime does not', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  try {
    // SizeFlag.check: the bare positional token ("Size:2") is a MINIMUM
    // (slime.getSize() >= token), not an exact match. Size NBT 10 is well
    // above threshold, 0 well below.
    server.execute(`summon minecraft:slime ${ARENA.x + 2} ${ARENA.y} ${ARENA.z} {Tags:["lf_big_slime"],Size:10,PersistenceRequired:1b,NoAI:1b}`);
    await expectCondition(server, player, `if entity @e[tag=lf_big_slime]`);
    if (!(await tameAttempt(server, player, 'slime', 'lf_big_slime'))) throw new Error('large slime should tame');
    removePet(server, player);
    server.execute(`summon minecraft:slime ${ARENA.x + 2} ${ARENA.y} ${ARENA.z} {Tags:["lf_tiny_slime"],Size:0,PersistenceRequired:1b,NoAI:1b}`);
    await expectCondition(server, player, `if entity @e[tag=lf_tiny_slime]`);
    if (await tameAttempt(server, player, 'slime', 'lf_tiny_slime')) throw new Error('tiny slime must NOT tame under Size:2 flag');
  } finally {
    killTagged(server, 'lf_big_slime'); killTagged(server, 'lf_tiny_slime'); removePet(server, player);
    server.execute('kill @e[type=minecraft:slime]');
  }
});

test('Wild flag: untamed cat tames, already-tamed cat does not', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  try {
    // WildFlag.check: Tameable must NOT already be isTamed(). Mooshroom (the
    // original draft target) is neither IronGolem nor Tameable (vacuous
    // always-pass) — moved to Cat, the flag's real Tameable branch.
    await spawnVictim(server, player, 'cat', 'lf_wild');
    if (!(await tameAttempt(server, player, 'cat', 'lf_wild'))) throw new Error('untamed cat should tame');
    removePet(server, player);
    // A UUID-typed "Owner" tag makes TamableAnimal set isTamed() on load
    // (same trick as taming.spec.ts's wolf test); WildFlag only checks
    // isTamed(), not owner identity, so any well-formed UUID array works.
    server.execute(`summon minecraft:cat ${ARENA.x + 2} ${ARENA.y} ${ARENA.z} {Tags:["lf_tamed_cat"],PersistenceRequired:1b,NoAI:1b,Owner:[I;0,0,0,1]}`);
    await expectCondition(server, player, `if entity @e[tag=lf_tamed_cat,nbt={Owner:[I;0,0,0,1]}]`);
    if (await tameAttempt(server, player, 'cat', 'lf_tamed_cat')) throw new Error('already-tamed cat must NOT tame under Wild flag');
  } finally {
    killTagged(server, 'lf_wild'); killTagged(server, 'lf_tamed_cat'); removePet(server, player);
    server.execute('kill @e[type=minecraft:cat]');
  }
});

test('UserCreated flag: player-made iron golem tames, natural one does not', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  try {
    // UserCreatedFlag.check: IronGolem must be isPlayerCreated() ("PlayerCreated"
    // NBT). Chicken (the original draft target) has no IronGolem branch
    // (vacuous always-pass) — moved to IronGolem itself.
    server.execute(`summon minecraft:iron_golem ${ARENA.x + 2} ${ARENA.y} ${ARENA.z} {Tags:["lf_player_golem"],PlayerCreated:1b,PersistenceRequired:1b,NoAI:1b}`);
    await expectCondition(server, player, `if entity @e[tag=lf_player_golem]`);
    if (!(await tameAttempt(server, player, 'iron_golem', 'lf_player_golem'))) throw new Error('player-created golem should tame');
    removePet(server, player);
    await spawnVictim(server, player, 'iron_golem', 'lf_natural_golem');
    if (await tameAttempt(server, player, 'iron_golem', 'lf_natural_golem')) throw new Error('natural golem must NOT tame under UserCreated flag');
  } finally {
    killTagged(server, 'lf_player_golem'); killTagged(server, 'lf_natural_golem'); removePet(server, player);
    server.execute('kill @e[type=minecraft:iron_golem]');
  }
});

test('World flag: rabbit tames in the configured world, not in another one', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const NETHER = 'minecraft:the_nether';
  try {
    // WorldFlag.check matches a positional world-name token against
    // getWorld().getName(); "world" is this server's real level-name (Paper
    // default, unstaged in server.properties). The arena already lives here.
    await spawnVictim(server, player, 'rabbit', 'lf_world_ok');
    if (!(await tameAttempt(server, player, 'rabbit', 'lf_world_ok'))) throw new Error('rabbit in the configured world should tame');
    removePet(server, player);

    // World flag denies in a non-configured world. Summon/pin/hit the rabbit entirely inside
    // `execute in minecraft:the_nether` on a forceloaded platform (bare @e isn't dimension-scoped);
    // assert a landed hit (health drop) then no-tame, so a deleted WorldFlag would fail this.
    for (const cmd of [
      `execute in ${NETHER} run forceload add -10 -10 10 10`,
      `execute in ${NETHER} run fill -10 199 -10 10 199 10 minecraft:sea_lantern`,
      `execute in ${NETHER} run summon minecraft:rabbit 2 200 0 {Tags:["lf_world_bad"],PersistenceRequired:1b,NoAI:1b}`,
    ]) server.execute(cmd);
    await expectCondition(server, player,
      `in ${NETHER} positioned 2 200 0 if entity @e[tag=lf_world_bad,distance=..2]`);
    server.execute(`execute in ${NETHER} run tp ${player.username} 0 200 0`);
    await expectCondition(server, player,
      `in ${NETHER} positioned 0 200 0 if entity @a[name=${player.username},distance=..4]`);
    if (await tameAttempt(server, player, 'rabbit', 'lf_world_bad', NETHER)) throw new Error('rabbit outside the configured world must NOT tame');
  } finally {
    server.execute(`execute in ${NETHER} run kill @e[tag=lf_world_bad]`);
    killTagged(server, 'lf_world_ok');
    removePet(server, player);
    server.execute(`execute in minecraft:overworld run tp ${player.username} ${ARENA.x} ${ARENA.y} ${ARENA.z}`);
    server.execute('kill @e[type=minecraft:rabbit]');
  }
});

test('Impossible flag: donkey never tames', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  try {
    // ImpossibleFlag.check unconditionally returns false — deny-only by
    // design, no allow side exists to test.
    await spawnVictim(server, player, 'donkey', 'lf_impossible');
    if (await tameAttempt(server, player, 'donkey', 'lf_impossible')) throw new Error('donkey must NEVER tame under Impossible flag');
  } finally {
    killTagged(server, 'lf_impossible'); removePet(server, player);
    server.execute('kill @e[type=minecraft:donkey]');
  }
});
