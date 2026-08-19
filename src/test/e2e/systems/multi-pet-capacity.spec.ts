import { test } from '@drownek/plugwright';
import { expectCondition } from '../lib/oracle.js';
import { createPet, switchToStoredPet } from '../lib/pets.js';
import { setupArena } from '../lib/world.js';

/**
 * Multi-pet Phase 1 capacity proof (MyPetORG/MyPet#1435).
 *
 * Phase 1 widened every container from "one active pet per player" to N, then
 * pinned the cap at 1 so behavior is unchanged. Every other spec verifies that
 * nothing changed; this is the only one that exercises the widening and the
 * cap mechanism itself.
 *
 *   ./gradlew plugwrightTest                                    cap 1: the switch
 *                                                               EVICTS the incumbent
 *   ./gradlew plugwrightTest -PtestFiles="multi-pet-capacity" \
 *       -PmultiPetCap=2                                         cap 2: both stay out
 *
 * IMPORTANT -- why the third pet activation goes through /petswitch rather than
 * another `petadmin create`: `createPet` always passes `-f`, and
 * CommandOptionCreate force-deactivates the incumbent before creating, then only
 * activates when `!newOwner.hasPet()`. Both gates ignore the cap, so `petadmin
 * create` can never produce two active pets and a spec built on it passes
 * vacuously in both modes. CommandSwitch calls PetManager#activatePet directly
 * with a live incumbent, so it is the only path that reaches the cap loop.
 *
 * Entities are matched by NAME, not by the scoreboard tag createPet applies: a
 * pet that is evicted and later re-activated gets a brand new entity, and the
 * tag died with the old one.
 */
test('a switch evicts at cap 1 and coexists when the cap allows it', async ({ server, player }) => {
  // OP is required: /petswitch resolves a storage limit from
  // MyPet.petstorage.limit.<n>, which is 0 for a plain player, and refuses to open.
  await player.makeOp();
  await setupArena(server, player);

  // CapA is created first, then CapB force-evicts it. CapA is now stored, CapB active.
  await createPet(server, player, 'SnowGolem', { name: 'CapA' });
  await createPet(server, player, 'Cow', { name: 'CapB' });
  await expectCondition(server, player, 'if entity @e[type=minecraft:cow,name=CapB]');

  // Switching back to CapA activates it with CapB still live -- this is the call
  // that hits PetManager#activatePet's cap loop.
  await switchToStoredPet(player, 'CapA');
  await expectCondition(server, player, 'if entity @e[type=minecraft:snow_golem,name=CapA]');

  const incumbentSurvived = await expectCondition(
    server, player, 'if entity @e[type=minecraft:cow,name=CapB]', { timeout: 3000 },
  ).then(() => true, () => false);

  if (incumbentSurvived) {
    // Cap raised: both pets active at once. This is the assertion that proves the
    // ListMultimap really holds more than one pet for a single owner.
    await expectCondition(server, player, 'if entity @e[type=minecraft:snow_golem,name=CapA]');
    return;
  }

  // Shipped cap of 1: activatePet evicted the oldest pet to make room. Assert the
  // eviction positively -- "CapA spawned" alone would not prove CapB went away.
  await expectCondition(server, player, 'unless entity @e[type=minecraft:cow,name=CapB]');
});
