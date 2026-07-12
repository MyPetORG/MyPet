import { test, expect } from '@drownek/plugwright';
import { expectCondition } from '../lib/oracle.js';
import { msgPlain } from '../lib/locale.js';

// Fixed bot identity, NOT the per-test random `Test_<hex>` the default
// `player` fixture gets: the legacy fixture (testdata/legacy/pets.db, staged
// by build.gradle.kts) is keyed to THIS username's offline-mode UUID
// (same algorithm as taming.spec.ts's offlineUuid), so every test here must
// drive its own `createPlayer({ username: LEGACY_OWNER })` bot.
const LEGACY_OWNER = 'LegacyOwner';

/** Opens /petswitch and clicks the stored pet whose display name contains `nameFragment`. */
async function switchToStoredPet(player: any, nameFragment: string): Promise<void> {
  player.chat('/petswitch');
  const gui = await player.gui({ title: msgPlain('Gui.PetSelection.Title') });
  await gui.locator((i: any) => String(i.getDisplayName() ?? '').includes(nameFragment)).click();
}

/**
 * The fixed username persists its op grant across this file's 3 tests
 * (ops.json survives reconnects), but `player.makeOp()`'s confirmation poll
 * only matches the FIRST-ever grant message, so it times out on an
 * already-opped player. deop-then-op is a robust idempotent "ensure op".
 */
async function ensureOp(player: any): Promise<void> {
  await player.deOp();
  await player.makeOp();
}

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
