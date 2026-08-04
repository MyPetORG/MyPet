import { test, expect } from '@drownek/plugwright';
import { expectCondition } from '../lib/oracle.js';
import { msgFragment } from '../lib/locale.js';
import { switchToStoredPet } from '../lib/pets.js';
import { ensureOp } from '../lib/players.js';

// Fixed bot identity, NOT the per-test random `Test_<hex>`: the fixture row
// (testdata/legacy/pets.db) is keyed to THIS username. The plugwright server
// runs offline mode, so the bot's real join UUID *is*
// nameUUIDFromBytes("OfflinePlayer:OfflineOwner") — which is precisely what
// BackfillOfflineUuidsFromName derives. That equality is the whole feature.
const OFFLINE_OWNER = 'OfflineOwner';

test('offline-mode MyPet-3 player keeps their pet through migration', async ({ createPlayer, server }) => {
  const player = await createPlayer({ username: OFFLINE_OWNER });
  await ensureOp(player);
  try {
    // Reaching the pet at all proves the players row survived: without the
    // backfill, RebuildPlayersSchemaOnMojangUuid deletes it and this bot
    // joins as a brand-new player with an empty /petswitch menu.
    await switchToStoredPet(player, 'OfflineCow');
    await expectCondition(server, player,
      `at ${player.username} if entity @e[type=minecraft:cow,name=OfflineCow,distance=..16]`);

    // 500 exp on the default curve = level 16. A 0-exp "add" reports the
    // resulting exp verbatim, so this distinguishes recovered data from a
    // fresh level-1 pet that merely happens to share the name.
    const since = player.getMessageBufferIndex();
    player.chat(`/petadmin exp ${player.username} 0 add`);
    await expect(player).toHaveReceivedMessage('set exp to 500.0. Pet is now level 16', { since });
  } finally {
    server.execute(`petadmin remove ${player.username}`);
  }
});

test('an unrecoverable nameless legacy row does not abort the migration', async ({ createPlayer }) => {
  // The fixture holds a players row with mojang_uuid NULL *and* name NULL
  // (66666666-...). Nothing identifies it, so the backfill must skip it rather
  // than fail. If its post-condition query dropped the name guard, the
  // migration would throw, MigrationService would disable MyPet, and the
  // command below would go unanswered.
  const player = await createPlayer({ username: 'NamelessProbe' });
  const since = player.getMessageBufferIndex();
  player.chat('/petcall');
  await expect(player).toHaveReceivedMessage(msgFragment('Message.No.HasPet'), { since });
});
