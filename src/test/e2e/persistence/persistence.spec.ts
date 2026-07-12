import { test, expect } from '@drownek/plugwright';
import { expectCondition } from '../lib/oracle.js';
import { msgPlain } from '../lib/locale.js';
import { createPet, removePet } from '../lib/pets.js';
import { setupArena } from '../lib/world.js';

// /petadmin replies go to the CommandSender; server.execute's sender is the console, so
// its replies never reach the player's message buffer. Use player.chat(...) instead.
test('pet state survives a store/switch round-trip', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  await createPet(server, player, 'Cow', { name: 'Keeper', skilltree: 'test-damage' });

  try {
    const sinceExp = player.getMessageBufferIndex();
    player.chat(`/petadmin exp ${player.username} 500 set`);
    await expect(player).toHaveReceivedMessage('exp to', { since: sinceExp });

    player.chat('/petstore');
    await expectCondition(server, player,
      `at ${player.username} unless entity @e[type=minecraft:cow,distance=..16]`);

    player.chat('/petswitch'); // single stored pet still routes through the selection GUI
    const gui = await player.gui({ title: msgPlain('Gui.PetSelection.Title') });
    await gui.locator((i: any) => String(i.getDisplayName() ?? '').includes('Keeper')).click();

    // Name survived (entity custom name):
    await expectCondition(server, player,
      `at ${player.username} if entity @e[type=minecraft:cow,name=Keeper,distance=..16]`);

    // Skilltree survived: /petinfo reports the tree's own Name field.
    const sinceInfo = player.getMessageBufferIndex();
    player.chat('/petinfo');
    await expect(player).toHaveReceivedMessage('test-damage', { since: sinceInfo });

    // Exp survived: assert the exp value itself, not just a bare "Pet is now level"
    // fragment, which would also match a level-1 pet whose exp was lost.
    const sinceLevel = player.getMessageBufferIndex();
    player.chat(`/petadmin exp ${player.username} 0 add`);
    await expect(player).toHaveReceivedMessage('set exp to 500.0. Pet is now level 16', { since: sinceLevel });
  } finally {
    removePet(server, player);
  }
});
