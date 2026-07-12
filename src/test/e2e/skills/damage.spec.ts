import { test } from '@drownek/plugwright';
import { expectCondition } from '../lib/oracle.js';
import { createPet, removePet } from '../lib/pets.js';
import { setupArena, spawnVictim, botAttack, killTagged } from '../lib/world.js';

// A pet with no Damage upgrade can't attack at all (PetMeleeAttackGoal.shouldActivate()
// refuses when getDamage() <= 0), and the bot's own bare-handed trigger punch can't kill a
// husk's 20 HP alone — so a fast kill can only mean the skilltree's +50 Damage applied.
// Husk, not zombie: same stats but doesn't sun-burn, avoiding burn-tick i-frames that can
// swallow the bot's trigger punch.
test('Damage skill: one pet hit kills a husk', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  await createPet(server, player, 'Cow', { skilltree: 'test-damage' });

  try {
    const victim = await spawnVictim(server, player, 'husk', 'v_dmg');

    // Owner-hurt-target AI: the bot attacking makes the pet engage.
    await botAttack(player, 'husk');

    // Well under the 30s framework ceiling to fail fast if the pet never engages.
    await expectCondition(server, player, `unless entity ${victim}`, { timeout: 10000 });
  } finally {
    killTagged(server, 'v_dmg');
    removePet(server, player);
  }
});
