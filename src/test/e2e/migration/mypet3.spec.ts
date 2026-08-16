import { test, expect } from '@drownek/plugwright';
import { expectCondition } from '../lib/oracle.js';
import { switchToStoredPet } from '../lib/pets.js';
import { ensureOp } from '../lib/players.js';

// Fixed bot identity, NOT the per-test random `Test_<hex>` the default
// `player` fixture gets: the legacy fixture (testdata/legacy/pets.db, staged
// by build.gradle.kts) is keyed to THIS username's offline-mode UUID
// (same algorithm as taming.spec.ts's offlineUuid), so every test here must
// drive its own `createPlayer({ username: LEGACY_OWNER })` bot.
const LEGACY_OWNER = 'LegacyOwner';

test('legacy MyPet-3 pet converts with name and exp intact', async ({ createPlayer, server }) => {
  const player = await createPlayer({ username: LEGACY_OWNER });
  await ensureOp(player);
  try {
    await switchToStoredPet(player, 'LegacyCow');
    await expectCondition(server, player,
      `at ${player.username} if entity @e[type=minecraft:cow,name=LegacyCow,distance=..16]`);

    // 500 exp on the default curve = level 16. A 0-exp "add" reports the
    // resulting exp verbatim, proving the stored 500 survived the 3.x->4.0
    // conversion — a bare "Pet is now level" fragment would also match a lost-data "level 1."
    const since = player.getMessageBufferIndex();
    player.chat(`/petadmin exp ${player.username} 0 add`);
    await expect(player).toHaveReceivedMessage('set exp to 500.0. Pet is now level 16', { since });
  } finally {
    server.execute(`petadmin remove ${player.username}`);
  }
});

test('legacy rabbit keeps its fur type', async ({ createPlayer, server }) => {
  const player = await createPlayer({ username: LEGACY_OWNER });
  await ensureOp(player);
  try {
    await switchToStoredPet(player, 'LegacyRabbit');

    // Fixture wrote legacy MyPet-3 info {Variant: 4} (generate_mypet3_db.py).
    // LegacyPetReader maps 4 -> Rabbit.Type.GOLD; vanilla's "RabbitType" tag
    // uses the same ids, so a successful conversion leaves RabbitType:4 (a
    // regression, b3c669cbd, would leave the vanilla default RabbitType:0).
    // RabbitType is TAG_Int on this Paper build (not TAG_Byte), so the
    // selector match must be `RabbitType:4`, not `RabbitType:4b`.
    await expectCondition(server, player,
      `at ${player.username} if entity @e[type=minecraft:rabbit,name=LegacyRabbit,` +
      `nbt={RabbitType:4},distance=..16]`);
  } finally {
    server.execute(`petadmin remove ${player.username}`);
  }
});

test('legacy tropical fish keeps body colour, pattern and pattern colour', async ({ createPlayer, server }) => {
  const player = await createPlayer({ username: LEGACY_OWNER });
  await ensureOp(player);
  try {
    await switchToStoredPet(player, 'LegacyFish');

    // Fixture wrote legacy MyPet-3 info {Variant: 184615681} (generate_mypet3_db.py):
    // shape=1 (large) | patternIndex=3<<8 (BLOCKFISH) | bodyColor=1<<16
    // (ORANGE) | patternColor=11<<24 (BLUE), MyPet-3's "vanilla layout"
    // packing. A successful conversion leaves Variant:184615681 on the live
    // entity (a regression, 23c9efcdc, dropped bodyColor/patternColor,
    // leaving a plain white fish).
    await expectCondition(server, player,
      `at ${player.username} if entity @e[type=minecraft:tropical_fish,name=LegacyFish,` +
      `nbt={Variant:184615681},distance=..16]`);
  } finally {
    server.execute(`petadmin remove ${player.username}`);
  }
});

// Reported symptom: pets' armor vanished when servers converted from MyPet 3 to 4.
//
// The fixture row carries armor in MyPet 3's exact storage shape: an "Equipment" list of
// `itemStack.save(...)` compounds with an added "Slot" string, the item itself in the
// PRE-1.20.5 layout (capital "Count", a "tag" sub-compound) that any server running
// MyPet 3 on <=1.20.4 has on disk. LegacyNbtItemDecoder feeds that to
// ItemStack.deserializeBytes without the DataVersion seed that MigratePetBackpackItems
// uses for backpack items -- so the decode was the prime suspect, and this test was
// written expecting it to fail. It does not: Paper decodes that shape unseeded. The
// conversion is NOT where the armor is lost, and this test now pins that down.
//
// The loss is in the respawn afterwards, which the second half covers.
test('legacy pet keeps the armor it was wearing in MyPet 3', async ({ createPlayer, server }) => {
  const player = await createPlayer({ username: LEGACY_OWNER });
  await ensureOp(player);
  try {
    await switchToStoredPet(player, 'LegacyZombie');
    await expectCondition(server, player,
      `at ${player.username} if entity @e[type=minecraft:zombie,name=LegacyZombie,distance=..16]`);

    await expectCondition(server, player,
      `at ${player.username} if entity @e[type=minecraft:zombie,name=LegacyZombie,` +
      `nbt={equipment:{head:{id:"minecraft:iron_helmet"}}},distance=..16]`);

    // Surviving the conversion is not enough -- the armor also has to survive a respawn.
    // A recall runs VanillaMobSpawner#configureMob, which reconciles the domain equipment
    // cache with the mob's slots; that cache is empty for a pet just rebuilt from the
    // repository, so a sync in the wrong direction wipes the migrated gear, and the next
    // snapshot capture makes the loss permanent.
    //
    // Both stages share one test because `petadmin remove` deletes the pet from storage,
    // so a second test cannot reuse this fixture row.
    player.chat('/petsendaway');
    await expectCondition(server, player,
      `at ${player.username} unless entity @e[type=minecraft:zombie,name=LegacyZombie,distance=..16]`,
      { timeout: 6000 });
    player.chat('/petcall');
    await expectCondition(server, player,
      `at ${player.username} if entity @e[type=minecraft:zombie,name=LegacyZombie,distance=..16]`,
      { timeout: 6000 });

    await expectCondition(server, player,
      `at ${player.username} if entity @e[type=minecraft:zombie,name=LegacyZombie,` +
      `nbt={equipment:{head:{id:"minecraft:iron_helmet"}}},distance=..16]`);
  } finally {
    server.execute(`petadmin remove ${player.username}`);
  }
});
