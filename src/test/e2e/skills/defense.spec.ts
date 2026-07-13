import { test, expect } from '@drownek/plugwright';
import { expectScore } from '../lib/oracle.js';
import { createPet, removePet } from '../lib/pets.js';
import { setupArena } from '../lib/world.js';

// ShieldImpl.apply() is only wired to the OWNER's EntityDamageEvent (PlayerListener), never
// the pet's own -- damaging the pet directly would never reach it. So this test damages the
// owner and asserts the redirect split on both sides.

test('test-shield: incoming damage on the owner is redirected onto the pet', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Cow', { skilltree: 'test-shield' });

  try {
    // Full-heal the owner first: health persists across bot reconnects for the same
    // offline-UUID player, so leftover scar damage could confound "owner barely hurt".
    server.execute(`effect give ${player.username} minecraft:instant_health 1 9`);
    await expectScore(server, player, `data get entity @a[name=${player.username},limit=1] Health`, '19..', { timeout: 10000 });

    server.execute(`damage ${player.username} 8 minecraft:generic`);

    // Chance+100, Redirect+90: 90% of the 8 damage (7.2) redirects onto the pet, so the
    // owner only takes ~0.8 (stays >=18) while the Cow (base 10) drops from 10 to ~2.8 (<=5).
    await expectScore(server, player, `data get entity @a[name=${player.username},limit=1] Health`, '18..', { timeout: 10000 });
    await expectScore(server, player, `data get entity ${pet.selector} Health`, '..5', { timeout: 10000 });
  } finally {
    removePet(server, player);
  }
});

test('test-heal: pet regenerates after damage', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Cow', { skilltree: 'test-heal' });

  try {
    // Cow base is 10. Damage to 4, then only HealImpl firing can bring it back to full.
    server.execute(`damage ${pet.selector} 6 minecraft:generic`);
    // test-heal.st.json's "Timer": "-9" against a base of 0 keeps HealImpl's timeCounter gate
    // permanently satisfied, so it heals +10 HP on every 1s schedule() tick -- one tick clears
    // the gap (4 + 10, capped at the 10 max), far faster than vanilla regen over this window.
    await expectScore(server, player, `data get entity ${pet.selector} Health`, '10..', { timeout: 15000 });
  } finally {
    removePet(server, player);
  }
});

test('pets inherit their species vanilla max health by default', async ({ player, server }) => {
  await player.makeOp();
  const cow = await createPet(server, player, 'Cow');
  try {
    // Default HP now derives from the vanilla entity (Cow = 10), not a flat 20.
    await expectScore(server, player, `attribute ${cow.selector} minecraft:max_health get`, '10..10', { timeout: 15000 });
  } finally {
    removePet(server, player);
  }
  const chicken = await createPet(server, player, 'Chicken');
  try {
    await expectScore(server, player, `attribute ${chicken.selector} minecraft:max_health get`, '4..4', { timeout: 15000 });
  } finally {
    removePet(server, player);
  }
});

test('test-life: max health is raised above the species base', async ({ player, server }) => {
  await player.makeOp();
  const pet = await createPet(server, player, 'Cow', { skilltree: 'test-life' });

  try {
    // Cow base is the vanilla 10; Life's Health+20 upgrade raises it to exactly 30.
    await expectScore(server, player, `attribute ${pet.selector} minecraft:max_health get`, '30..30', { timeout: 15000 });
  } finally {
    removePet(server, player);
  }
});

test('test-sprint: skill is active on the pet', async ({ player, server }) => {
  await player.makeOp();
  const pet = await createPet(server, player, 'Cow', { skilltree: 'test-sprint' });

  try {
    // /petskill lists every active skill's localized name ("Sprint"), which appears once
    // Active:true (test-sprint.st.json) makes SprintImpl#isActive() true.
    player.chat('/petskill');
    await expect(player).toHaveReceivedMessage('Sprint');
  } finally {
    removePet(server, player);
  }
});
