import { test } from '@drownek/plugwright';
import { expectCondition, expectConditionHolds, expectScore } from '../lib/oracle.js';
import { createPet, removePet } from '../lib/pets.js';
import { setupArena, spawnVictim, killTagged } from '../lib/world.js';
import { secondBot } from '../lib/players.js';
import { setDisablePetVersusPlayer } from '../lib/config.js';

const sleep = (ms: number) => new Promise(r => setTimeout(r, ms));

// PetPvPListener only gates player-on-pet damage; pet-on-PLAYER is gated earlier at
// target acquisition (PetAggressiveTargetGoal/PetHurtByTargetGoal/PetOwnerHurtByTargetGoal
// call HookHelper.canHurt(owner, targetPlayer, true) before pet.setTarget(...)), controlled
// by MyPetGlobal.Misc.DISABLE_PET_VS_PLAYER (default false = allowed). A raw `/damage`
// command bypasses target acquisition entirely, so these tests must let a real goal
// (Aggressive's scan, Raid's owner-hurt-by-a-player retaliation) actually run.
//
// The suite stages the deny default (config.yml DisablePetVersusPlayer: true) so tests
// are opt-in; `/mypet reload config` hot-reloads it, letting the "PvP-on" tests flip it
// live and restore it in `finally`.

async function expectFullHealthHolds(server: any, player: any, username: string, checks = 4, interval = 800): Promise<void> {
  for (let i = 0; i < checks; i++) {
    await expectScore(server, player, `data get entity @a[name=${username},limit=1] Health`, '20..20', { timeout: interval + 2500 });
    await sleep(interval);
  }
}

test('PvP-off (default): an aggressive pet never damages the second bot, even while it kills a nearby husk', async ({ player, server, createPlayer }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Cow', { skilltree: 'test-behavior-modes' });
  const bystander = await secondBot(createPlayer, server, player, { username: 'PvPOffBot' });

  try {
    player.chat('/petbehavior aggressive');
    // Park the pet on the bystander (inside Aggressive's ~9.5-block scan radius) so the
    // scan and melee goal engage every tick instead of just idling.
    server.execute(`execute at ${bystander.bot.username} run tp @e[tag=${pet.tag},limit=1] ~ ~ ~`);
    // A husk in the same scan radius proves the attack goal is genuinely live (not idle
    // for lack of any target) while the bystander stays exempt throughout.
    const victim = await spawnVictim(server, player, 'husk', 'v_pvpoff', { dx: 0, dz: 4 });

    await expectFullHealthHolds(server, player, bystander.bot.username);
    await expectCondition(server, player, `unless entity ${victim}`, { timeout: 20000 });
    await expectFullHealthHolds(server, player, bystander.bot.username);
  } finally {
    killTagged(server, 'v_pvpoff');
    removePet(server, player);
    await bystander.dispose();
  }
});

test('PvP-on (runtime flip): the same aggressive pet damages the second bot once DisablePetVersusPlayer reloads to false', async ({ player, server, createPlayer }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Cow', { skilltree: 'test-behavior-modes' });
  const target = await secondBot(createPlayer, server, player, { username: 'PvPOnBot' });

  try {
    await setDisablePetVersusPlayer(server, player, false);

    player.chat('/petbehavior aggressive');
    server.execute(`execute at ${target.bot.username} run tp @e[tag=${pet.tag},limit=1] ~ ~ ~`);

    // Positive control for the "PvP-off" test: same mechanism, only the config key flipped.
    // Damage+50 one-shots the player and auto-respawn is near-instant, so a Health poll
    // races the respawn and can miss it; deathCount is a durable, respawn-proof record.
    server.execute(`scoreboard objectives add pvpon_deaths deathCount`);
    await expectCondition(server, player,
      `if score ${target.bot.username} pvpon_deaths matches 1..`, { timeout: 15000 });
  } finally {
    // Entity cleanup first (non-throwing); config restore last and guarded, so a restore
    // failure logs instead of masking the passed test or skipping cleanup above.
    removePet(server, player);
    await target.dispose();
    try {
      await setDisablePetVersusPlayer(server, player, true);
    } catch (err) {
      console.error('WARNING: failed to restore DisablePetVersusPlayer=true — later tests may inherit an open pet-vs-player gate:', err);
    }
  }
});

test('Raid mode never targets a player, even one who provoked it, but still kills a wild mob', async ({ player, server, createPlayer }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Cow', { skilltree: 'test-behavior-modes' });
  const attacker = await secondBot(createPlayer, server, player, { username: 'PvPRaidBot' });

  try {
    // PetOwnerHurtByTargetGoal checks the general canHurt(owner, targetPlayer, true) gate
    // before its Raid-specific player-exclusion, so with the suite's staged deny default
    // still active a provocation would be rejected mode-independently, not by Raid's own
    // logic. Flip the gate open so only Raid's player-exclusion is under test.
    await setDisablePetVersusPlayer(server, player, false);

    player.chat('/petbehavior raid');
    server.execute(`execute at ${attacker.bot.username} run tp @e[tag=${pet.tag},limit=1] ~ ~ ~`);

    // Attribute damage on the owner to the attacker so PetDamageTracker records a real
    // Player as last attacker -- the input Raid's exclusion branch inspects.
    server.execute(`damage ${player.username} 1 minecraft:mob_attack by @a[name=${attacker.bot.username},limit=1]`);

    await expectFullHealthHolds(server, player, attacker.bot.username);
    await expectConditionHolds(server, player,
      `at ${attacker.bot.username} unless entity @e[tag=${pet.tag},distance=..2]`, { checks: 4, interval: 800 });

    // Same conditions, wild-mob retaliation: proves the goal is genuinely live, not idle.
    const victim = await spawnVictim(server, player, 'husk', 'v_pvpraid', { dx: 0, dz: 4 });
    server.execute(`damage ${pet.selector} 1 minecraft:mob_attack by ${victim}`);
    await expectCondition(server, player, `unless entity ${victim}`, { timeout: 15000 });
  } finally {
    // Same ordering as the PvP-on test: entity cleanup first, config restore last.
    killTagged(server, 'v_pvpraid');
    removePet(server, player);
    await attacker.dispose();
    try {
      await setDisablePetVersusPlayer(server, player, true);
    } catch (err) {
      console.error('WARNING: failed to restore DisablePetVersusPlayer=true — later tests may inherit an open pet-vs-player gate:', err);
    }
  }
});
