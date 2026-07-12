import { test } from '@drownek/plugwright';
import { expectCondition, expectScore } from '../lib/oracle.js';
import { createPet, removePet } from '../lib/pets.js';
import { setupArena, spawnVictim, killTagged, ARENA } from '../lib/world.js';

const sleep = (ms: number) => new Promise(r => setTimeout(r, ms));

// Victims are pigs (10 HP, no sunburn/immunities): zombies sun-ignite even with NoAI:1b,
// burning to death and randomly eating trigger commands via burn i-frames.

test('test-knockback: victim is hurled away from its pinned spot', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Cow', { skilltree: 'test-knockback' });

  try {
    // A NoAI mob never integrates setVelocity() into its position (Paper gates physics
    // behind isEffectiveAi()), so the victim needs AI during the throw and is re-frozen with
    // NoAI:1b afterward so the displaced position holds for the assertion.
    // KnockbackImpl.apply() derives push direction from the pet's current yaw, which its AI
    // look-controller can rotate unpredictably — so the assertion is direction-agnostic:
    // pin a known start point and require >4-block displacement from it (the skill's ~1.0
    // horizontal velocity carries ~6-7 blocks total; vanilla mob_attack knockback + panic
    // movement alone stays well under 3).
    await spawnVictim(server, player, 'pig', 'v_kb', { dx: 2, noAI: false });
    server.execute(`tp ${pet.selector} ${ARENA.x} ${ARENA.y} ${ARENA.z} 270 0`);
    server.execute(`tp @e[tag=v_kb,limit=1] ${ARENA.x + 2} ${ARENA.y} ${ARENA.z}`); // pin the start point
    server.execute(`damage @e[tag=v_kb,limit=1] 1 minecraft:mob_attack by ${pet.selector}`);
    await sleep(600); // let the knockback velocity carry it out before re-freezing
    server.execute('data merge entity @e[tag=v_kb,limit=1] {NoAI:1b}');
    await expectCondition(server, player,
      `positioned ${ARENA.x + 2} ${ARENA.y} ${ARENA.z} if entity @e[tag=v_kb] unless entity @e[tag=v_kb,distance=..4]`,
      { timeout: 15000 });
  } finally {
    killTagged(server, 'v_kb');
    removePet(server, player);
  }
});

// Stomp is not actually a fall-landing trigger: StompImpl is a generic OnHitSkill dispatched
// from PetSkillTriggerListener#onPetDealsDamage like every other on-hit skill — no dedicated
// fall listener exists. The tp-from-height puts the pet within Stomp's 2.5-block AoE radius
// (matching its real use case), but the trigger itself is the same /damage attribution used
// throughout this file.
test('test-stomp: pet landing next to a victim triggers the AoE that kills it', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Cow', { skilltree: 'test-stomp' });

  try {
    const victim = await spawnVictim(server, player, 'pig', 'v_stomp', { dx: 3 });
    server.execute(`tp ${pet.selector} ${ARENA.x + 3} ${ARENA.y + 8} ${ARENA.z}`); // fall onto the victim
    await sleep(1500); // let the fall complete so the pet is within AoE range on landing
    server.execute(`damage ${victim} 1 minecraft:mob_attack by ${pet.selector}`);
    // +50 AoE damage kills the 10 HP pig outright; the 1-damage trigger alone cannot.
    await expectCondition(server, player, `unless entity ${victim}`, { timeout: 15000 });
  } finally {
    killTagged(server, 'v_stomp');
    removePet(server, player);
  }
});

test('test-thorns: attacker takes reflected damage', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Cow', { skilltree: 'test-thorns' });

  try {
    const victim = await spawnVictim(server, player, 'pig', 'v_thorn', { dx: 2 });
    // ThornsImpl is an OnDamageByEntitySkill dispatched whenever the pet takes damage from
    // any LivingEntity. Reflection is 100%, so the pig takes the full 2 back (10 -> 8).
    server.execute(`damage ${pet.selector} 2 minecraft:mob_attack by ${victim}`);
    await expectScore(server, player, `data get entity ${victim} Health`, '..9', { timeout: 20000 });
  } finally {
    killTagged(server, 'v_thorn');
    removePet(server, player);
  }
});

test('test-ranged: pet shoots a distant aggressor dead', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  await createPet(server, player, 'Cow', { skilltree: 'test-ranged' });

  try {
    // Husk, not pig or zombie: a zombie sun-burns to death; a pig's 0.94-block hitbox is too
    // small for PetRangedAttackGoal's arc at this range (overshoots). A husk has the
    // zombie's 1.95-block hitbox but doesn't burn, so death can only come from the arrows.
    const victim = await spawnVictim(server, player, 'husk', 'v_rng', { dx: 14 });
    // Owner-hurt-by-target AI: damage the owner attributed to the distant husk. Ranged has
    // its own +50 Damage gate independent of melee, so one arrow kills the 20 HP husk.
    server.execute(`damage ${player.username} 1 minecraft:mob_attack by ${victim}`);
    await expectCondition(server, player, `unless entity ${victim}`, { timeout: 25000 });
  } finally {
    killTagged(server, 'v_rng');
    removePet(server, player);
  }
});
