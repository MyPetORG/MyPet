import { test } from '@drownek/plugwright';
import { expectCondition, expectConditionHolds } from '../lib/oracle.js';
import { createPet, removePet } from '../lib/pets.js';
import { setupArena, spawnVictim, killTagged, attackPinned } from '../lib/world.js';

// `/petbehavior <mode>` refuses any mode not flagged usable on the pet's
// skilltree (BehaviorImpl.setBehavior falls back to Normal). Neither Friend
// nor Raid is enabled by any existing fixture, so a new skilltree
// (test-behavior-modes.st.json: Aggro+Friend+Raid true, Duel+Farm false,
// Damage +50) covers this file's Friendly/Aggressive/Raid tests; Normal
// reuses the existing test-behavior fixture (always the default mode).

// Raid has no unprovoked-scan goal (unlike Aggressive's
// PetAggressiveTargetGoal) -- it only widens the ordinary retaliation goals
// to also allow retaliating against wild mobs; only Friendly excludes
// retaliation entirely. So the proof damages the PET directly (not the
// owner), confirming retaliation without touching the owner's own
// PetDamageTracker entry.

test('test-behavior-modes: Friendly pet never engages even when the owner is attacked', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Cow', { skilltree: 'test-behavior-modes' });

  try {
    player.chat('/petbehavior friendly');
    const victim = await spawnVictim(server, player, 'husk', 'v_friendly', { dx: 4 });

    // Real provocation (owner actually damaged), the exact trigger
    // PetOwnerHurtByTargetGoal reacts to on every other mode -- proves
    // Friendly's unconditional short-circuit, not mere inaction.
    server.execute(`damage ${player.username} 1 minecraft:mob_attack by ${victim}`);

    // "at <victim>" also fails loudly if the husk ever died (no entity to run "at" on).
    await expectConditionHolds(server, player,
      `at ${victim} unless entity @e[tag=${pet.tag},distance=..2]`);
    await expectCondition(server, player, `if entity ${victim}`);
  } finally {
    killTagged(server, 'v_friendly');
    removePet(server, player);
  }
});

test('test-behavior-modes: Normal pet ignores an unprovoked husk but kills once the owner is attacked', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  // Normal is always the default mode -- reuse the locked test-behavior
  // fixture (Aggro+Duel+Damage+50).
  const pet = await createPet(server, player, 'Cow', { skilltree: 'test-behavior' });

  try {
    const victim = await spawnVictim(server, player, 'husk', 'v_normal', { dx: 4, noAI: false });

    // Unprovoked hold: Normal has no unprovoked-scan goal, only retaliation goals.
    await expectConditionHolds(server, player,
      `at ${victim} unless entity @e[tag=${pet.tag},distance=..2]`, { checks: 3 });

    // Provoke: owner swings first, husk's vanilla AI retaliates,
    // PetOwnerHurtByTargetGoal picks it up next tick. Retried since both the
    // swing and the retaliation hit can silently drop.
    let dead = false;
    for (let i = 0; i < 4 && !dead; i++) {
      await attackPinned(server, player, 'v_normal', 'husk');
      try {
        await expectCondition(server, player, `unless entity ${victim}`, { timeout: 4000 });
        dead = true;
      } catch { /* swing or retaliation dropped — re-pin and re-provoke */ }
    }
    if (!dead) throw new Error('husk never died after 4 provocation attempts');
  } finally {
    killTagged(server, 'v_normal');
    removePet(server, player);
  }
});

// Same fixture/species/husk placement as the Friendly test above, only the
// mode differs, for a direct contrast (distinct from interaction.spec.ts's
// existing aggressive test, which uses a different fixture/species).
test('test-behavior-modes: Aggressive contrast -- same setup as Friendly, but the husk dies unprovoked', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Cow', { skilltree: 'test-behavior-modes' });

  try {
    player.chat('/petbehavior aggressive');
    const victim = await spawnVictim(server, player, 'husk', 'v_aggro', { dx: 4 });

    await expectCondition(server, player, `unless entity ${victim}`, { timeout: 20000 });
  } finally {
    killTagged(server, 'v_aggro');
    removePet(server, player);
  }
});

test('test-behavior-modes: Raid pet retaliates against a wild mob that hurts it, without owner involvement', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Cow', { skilltree: 'test-behavior-modes' });

  try {
    player.chat('/petbehavior raid');
    const victim = await spawnVictim(server, player, 'husk', 'v_raid', { dx: 4 });

    // Provoke the PET directly, not the owner, so the kill can only be
    // explained by the pet's own retaliation goal.
    server.execute(`damage ${pet.selector} 1 minecraft:mob_attack by ${victim}`);

    await expectCondition(server, player, `unless entity ${victim}`, { timeout: 15000 });
  } finally {
    killTagged(server, 'v_raid');
    removePet(server, player);
  }
});
