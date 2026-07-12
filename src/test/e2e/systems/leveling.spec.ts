import { test, expect } from '@drownek/plugwright';
import { expectCondition, expectScore } from '../lib/oracle.js';
import { createPet, removePet } from '../lib/pets.js';
import { setupArena, spawnVictim, botAttack, killTagged, ARENA } from '../lib/world.js';

// Husk = fixed 5 XP/kill; damage-weighted split (default on) means the owner's trigger
// punches dilute the pet's share, so 3 kills land in [9,15) XP -> deterministically level 2
// (needs [7,17)). Assert the level, not the exp figure, since the exact total isn't pinned down.
// /petadmin exp echoes to the command sender, so drive it via player.chat to observe the reply.
test('pet kills grant exp and level the pet up', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  await createPet(server, player, 'Cow', { skilltree: 'test-damage' });
  try {
    // Husks (sunburn-immune) one-hit-killed by the pet's Damage:+50. A bot.attack swing can be
    // dropped server-side, so re-swing until the victim dies (safe no-op on a NoAI husk).
    for (const tag of ['lvl_a', 'lvl_b', 'lvl_c']) {
      const victim = await spawnVictim(server, player, 'husk', tag);
      const deadline = Date.now() + 25000;
      let dead = false;
      while (Date.now() < deadline) {
        await botAttack(player, 'husk');
        try {
          await expectCondition(server, player, `unless entity ${victim}`, { timeout: 3000 });
          dead = true;
          break;
        } catch { /* swing likely dropped or pet still closing distance -- retry */ }
      }
      if (!dead) throw new Error(`husk ${tag} never died`);
    }

    // amount=0, mode=add is state-mutation-free -- it just re-reports the pet's current level.
    const since = player.getMessageBufferIndex();
    player.chat(`/petadmin exp ${player.username} 0 add`);
    await expect(player).toHaveReceivedMessage('Pet is now level 2', { since });
  } finally {
    ['lvl_a', 'lvl_b', 'lvl_c'].forEach(t => killTagged(server, t));
    removePet(server, player);
  }
});

// PickupImpl's ExperienceOrb branch grants vacuumed orb XP to the OWNER's vanilla XP
// (Player#giveExp), never to the pet's own PetExperience -- so the oracle here is the owner's
// `xp query ... levels`, not a `petadmin exp` pet-level echo. PickupImpl.pickup starts false,
// so /petpickup must be toggled on first or the vacuum is skipped.
// `xp query points` resets every level-up (progress within the current level only); `levels`
// is the stable oracle. From a zeroed baseline, giveExp(20) clears vanilla levels 0->1 (7) and
// 1->2 (9), landing at level 2 with 4 points of progress toward level 3 (needs 11).
test('Pickup with Exp:true vacuums experience orbs into the owner\'s XP', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  await createPet(server, player, 'Cow', { skilltree: 'test-pickup-exp' });
  try {
    player.chat('/petpickup');

    // XP persists across tests for the same offline-mode UUID, so zero both levels and
    // in-level points first (each resets only its own component) to get a clean baseline.
    server.execute(`xp set ${player.username} 0 levels`);
    server.execute(`xp set ${player.username} 0 points`);

    server.execute(`summon minecraft:experience_orb ${ARENA.x + 2} ${ARENA.y + 1} ${ARENA.z} {Value:20,Tags:["xp_orb"]}`);
    await expectCondition(server, player, 'if entity @e[tag=xp_orb]');
    await expectCondition(server, player,
      `positioned ${ARENA.x} ${ARENA.y} ${ARENA.z} unless entity @e[type=minecraft:experience_orb,distance=..12]`,
      { timeout: 25000 });

    // Value:20 -> giveExp(20), deterministically landing the owner at level 2 (see above).
    await expectScore(server, player, `xp query ${player.username} levels`, '2');
  } finally {
    server.execute('kill @e[type=minecraft:experience_orb]');
    removePet(server, player);
  }
});
