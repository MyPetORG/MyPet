import { test, sleep } from '@drownek/plugwright';
import { expectCondition, expectScore } from '../lib/oracle.js';
import { createPet, removePet } from '../lib/pets.js';
import { setupArena } from '../lib/world.js';
import { setPetBrainDisabled } from '../lib/config.js';

// The four brain-driven nether pets used to hunt their own owner.
//
// PetGoalInstaller neutralises vanilla AI with
// Bukkit.getMobGoals().removeAllGoals(mob), which clears only the GoalSelector.
// PiglinBrute, Piglin, Hoglin and Zoglin are brain-driven: their *Ai classes
// install StartAttacking (writes ATTACK_TARGET), MeleeAttack and
// SetWalkTargetFromAttackTargetIfTargetOutOfReach onto the Brain, which the
// goal sweep cannot see. Verified present on all four in Paper 1.21.11 (this
// suite's minecraftVersion) and 26.2. None of the four declared anything under
// the old hardcoded brain-behavior-removal mechanism, so PetGoalInstaller
// passed an empty strip set.
//
// MyPet's pet-vs-player gate lives in the target GOALS (HookHelper.canHurt
// before pet.setTarget), and PetPvPListener only guards damage TO a pet — so
// nothing downstream stopped a brain-driven pet from hitting its owner.
//
// Fix: a per-pet `Brain.Disabled` list in pet-config.yml (declared on each
// pet's PetXxx class), applied at spawn by BrainDisableSpec via
// PetGoalInstaller. All four failed here before it and pass after.
//
// Oracle is the owner's health: ATTACK_TARGET is a non-persistent brain memory
// and never shows up in /data get, so damage taken is the only thing a vanilla
// command can observe.

/**
 * Parks the pet on top of the owner and asserts the owner stays at full health.
 *
 * Teleport-onto-owner is load-bearing: a hostile pet merely following from
 * across the arena looks identical to a peaceful one, so without the pin a
 * broken build would pass. Re-pinned every poll because the brute's own
 * knockback shoves the owner out of melee reach after the first hit.
 */
async function expectPetNeverHurtsOwner(
  server: any, player: any, petTag: string, checks = 6,
): Promise<void> {
  for (let i = 0; i < checks; i++) {
    server.execute(`execute at ${player.username} run tp @e[tag=${petTag},limit=1] ~ ~ ~`);
    await expectScore(server, player,
      `data get entity @a[name=${player.username},limit=1] Health`, '20..20',
      { timeout: 3300 });
    await sleep(800);
  }
  // Fails loudly if the pet despawned or died mid-hold, which would make an
  // untouched health bar prove nothing.
  await expectCondition(server, player,
    `at ${player.username} if entity @e[tag=${petTag},distance=..8]`);
}

// Control: Cow is goal-driven, so removeAllGoals genuinely disarms it. If this
// one ever fails alongside the others, the harness is at fault (arena fall
// damage, starvation, a stray mob) and the four failures below prove nothing.
test('control: a cow pet never attacks its owner', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Cow', { name: 'CtlCheck' });
  try {
    await expectPetNeverHurtsOwner(server, player, pet.tag);
  } finally {
    removePet(server, player);
  }
});

for (const petType of ['PiglinBrute', 'Piglin', 'Hoglin', 'Zoglin']) {
  test(`a ${petType} pet never attacks its owner`, async ({ player, server }) => {
    await player.makeOp();
    await setupArena(server, player);
    // No gold armour on the bot, so Piglin's isWearingGold exemption never
    // applies and it is hostile like the other three. Each of these four
    // ships a Brain.Disabled default (see the pet's PetXxx class) that
    // BrainDisableSpec applies at spawn — this loop proves that shipped
    // default actually disarms them.
    const pet = await createPet(server, player, petType, { name: `${petType}Chk` });
    try {
      await expectPetNeverHurtsOwner(server, player, pet.tag);
    } finally {
      removePet(server, player);
    }
  });
}

// The point of the config key: an admin who WANTS vanilla hostility can have it.
// Also the only test that proves the shipped default is doing the work, rather
// than something else incidentally keeping the brute peaceful.
test('Brain.Disabled: emptying the list restores vanilla owner-hunting', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);

  try {
    await setPetBrainDisabled(server, player, 'PiglinBrute', []);
    // Create AFTER the reload: PetGoalInstaller applies the strip at spawn, so a
    // pet created earlier would still carry the old, stripped brain.
    const pet = await createPet(server, player, 'PiglinBrute', { name: 'VanillaChk' });

    let damaged = false;
    for (let i = 0; i < 8 && !damaged; i++) {
      server.execute(`execute at ${player.username} run tp @e[tag=${pet.tag},limit=1] ~ ~ ~`);
      await sleep(800);
      try {
        await expectScore(server, player,
          `data get entity @a[name=${player.username},limit=1] Health`, '..19',
          { timeout: 2500 });
        damaged = true;
      } catch { /* not hit yet — re-pin and wait */ }
    }
    if (!damaged) throw new Error('brute never damaged its owner with Brain.Disabled emptied');
  } finally {
    removePet(server, player);
    await setPetBrainDisabled(server, player, 'PiglinBrute', ['activity:idle', 'activity:fight']);
  }
});

// Task 3 made stripFromComposite return a count so behaviors nested inside a
// RunOne (CamelAi$RandomSitting) are not miscounted as "matched nothing". Every
// other test here uses activity: entries, so without this the behavior: path —
// and that counting fix — is never executed. The assertion is the absence of a
// warning in the server log, checked after the run; spawning is the point.
test('behavior:-driven pets spawn without a Brain.Disabled warning', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  for (const type of ['Camel', 'Villager', 'CopperGolem']) {
    try {
      await createPet(server, player, type, { name: `Bd${type}` });
    } finally {
      removePet(server, player);
    }
  }
});

// Regression guard for the empty-name wildcard: `behavior:` with no name used to
// match every anonymous BehaviorBuilder behavior (getSimpleName() == "") and strip
// the entire brain silently, because a large removal count suppressed the warning.
// The fix rejects it up front. Proof is twofold: the pet must stay peaceful (its
// real strips still applied) AND the malformed entry must be reported.
test('Brain.Disabled: an empty entry name is rejected, not treated as a wildcard', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);

  try {
    await setPetBrainDisabled(server, player, 'PiglinBrute',
      ['activity:idle', 'activity:fight', 'behavior:']);
    const pet = await createPet(server, player, 'PiglinBrute', { name: 'EmptyChk' });

    // The valid entries must still apply — the brute stays peaceful.
    for (let i = 0; i < 5; i++) {
      server.execute(`execute at ${player.username} run tp @e[tag=${pet.tag},limit=1] ~ ~ ~`);
      await expectScore(server, player,
        `data get entity @a[name=${player.username},limit=1] Health`, '20..20', { timeout: 3300 });
      await sleep(800);
    }
  } finally {
    removePet(server, player);
    await setPetBrainDisabled(server, player, 'PiglinBrute', ['activity:idle', 'activity:fight']);
  }
});
