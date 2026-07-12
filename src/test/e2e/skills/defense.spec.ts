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
    // owner only takes ~0.8 (stays >=18). Pet max HP defaults to 20 (@DefaultInfo.hp()), not
    // the vanilla Cow attribute of 10, so the pet drops from 20 to ~12.8 (<=15).
    await expectScore(server, player, `data get entity @a[name=${player.username},limit=1] Health`, '18..', { timeout: 10000 });
    await expectScore(server, player, `data get entity ${pet.selector} Health`, '..15', { timeout: 10000 });
  } finally {
    removePet(server, player);
  }
});

test('test-heal: pet regenerates after damage', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Cow', { skilltree: 'test-heal' });

  try {
    // Pet max HP defaults to 20, not 10 -- an 8-damage hit alone would leave 12, already
    // satisfying "9.." with no healing. Deal 15 damage instead (-> 5 HP) so "back above 10"
    // can only be explained by HealImpl firing.
    server.execute(`damage ${pet.selector} 15 minecraft:generic`);
    // test-heal.st.json's "Timer": "-9" against a base of 0 keeps HealImpl's timeCounter gate
    // permanently satisfied, so it heals +10 HP on every 1s schedule() tick -- one tick
    // (5 + 10 = 15) already clears the threshold below.
    await expectScore(server, player, `data get entity ${pet.selector} Health`, '10..', { timeout: 15000 });
  } finally {
    removePet(server, player);
  }
});

test('test-life: max health is raised', async ({ player, server }) => {
  await player.makeOp();
  const pet = await createPet(server, player, 'Cow', { skilltree: 'test-life' });

  try {
    // Pet max HP defaults to 20 (@DefaultInfo.hp()), not the vanilla Cow attribute of 10;
    // Life's Health+20 upgrade raises it to 40. Attribute id is minecraft:max_health.
    await expectScore(server, player, `attribute ${pet.selector} minecraft:max_health get`, '40..', { timeout: 15000 });
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
