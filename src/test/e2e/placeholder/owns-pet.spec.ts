import { test } from '@drownek/plugwright';
import { createPet, removePet } from '../lib/pets.js';
import { expectPlaceholder } from '../lib/placeholder.js';

const sleep = (ms: number) => new Promise(r => setTimeout(r, ms));

/**
 * E2E coverage for `%mypet_owns_pet%` — the placeholder that is
 * "yes" when a player owns any pet at all, active or stored, as opposed to the
 * pre-existing `%mypet_has_pet%` which only reflects an active (summoned) pet.
 *
 * The value is served synchronously from an in-memory ownership cache
 * (PetManager#ownsAnyPet) that is refreshed off-thread when a pet is created
 * (AbstractSqlRepository#addPet), removed (the /petrelease, menu, admin-remove,
 * death and trade sites), and on login (PlayerManager#setOnline). These tests
 * drive those transitions and read the placeholder back via PlaceholderAPI's
 * `/papi parse`.
 *
 * Note on the has_pet≠owns_pet divergence: the state where a player has *no
 * active* pet but *does* own a stored one only arises across a world-group
 * boundary (a sent-away/despawned pet stays in the active map, so has_pet stays
 * "yes"). Provisioning a second world group is out of scope for this suite, so
 * these tests assert the two placeholders *agree* in the single-group flows they
 * can reach; the divergence itself is guarded by owns_pet reading the DB-backed
 * cache rather than the active map.
 */

/**
 * Drains every pet the player owns so a test starts from a known "owns nothing"
 * baseline. State persists across specs on the shared server/DB, and `petadmin
 * remove` only deletes the *active* pet, so we loop /petcall (activate a stored
 * pet) + remove until the ownership placeholder reads "no". Bounded so a stuck
 * state fails loudly instead of hanging.
 */
async function drainPets(server: any, player: any): Promise<void> {
  for (let i = 0; i < 6; i++) {
    try {
      await expectPlaceholder(player, 'mypet_owns_pet', 'no', { attempts: 1, timeout: 1500 });
      return;
    } catch { /* still owns at least one — activate and remove it */ }
    player.chat('/petcall');
    await sleep(1000);
    removePet(server, player);
    await sleep(1000);
  }
  // Final assertion: if this fails the baseline could not be established.
  await expectPlaceholder(player, 'mypet_owns_pet', 'no', { attempts: 2 });
}

test('%mypet_owns_pet% is "no" for a player who owns no pet', async ({ player, server }) => {
  await player.deOp();
  await player.makeOp();
  await drainPets(server, player);
  await expectPlaceholder(player, 'mypet_owns_pet', 'no');
});

test('%mypet_owns_pet% flips to "yes" once a pet is created', async ({ player, server }) => {
  await player.deOp();
  await player.makeOp();
  await drainPets(server, player);
  try {
    await createPet(server, player, 'Cow');
    await expectPlaceholder(player, 'mypet_owns_pet', 'yes');
    // With an active pet present, the active-only has_pet agrees.
    await expectPlaceholder(player, 'mypet_has_pet', 'yes');
  } finally {
    removePet(server, player);
  }
});

test('%mypet_owns_pet% returns to "no" after the pet is removed', async ({ player, server }) => {
  await player.deOp();
  await player.makeOp();
  await drainPets(server, player);
  await createPet(server, player, 'Cow');
  await expectPlaceholder(player, 'mypet_owns_pet', 'yes');

  // Deleting the DB row triggers the async ownership refresh at the remove site.
  removePet(server, player);
  await expectPlaceholder(player, 'mypet_owns_pet', 'no');
  await expectPlaceholder(player, 'mypet_has_pet', 'no');
});
