import { test, expect } from '@drownek/plugwright';
import { expectCondition } from '../lib/oracle.js';
import { msgFragment } from '../lib/locale.js';
import { createPet, removePet } from '../lib/pets.js';

test('petcall teleports the pet to the owner; petsendaway despawns it', async ({ player, server }) => {
  await player.makeOp();
  const pet = await createPet(server, player, 'Cow');

  try {
    server.execute(`tp ${player.username} 100 200 100`);
    server.execute(`execute positioned 100 199 100 run fill ~-3 ~ ~-3 ~3 ~ ~3 minecraft:smooth_stone`);

    player.chat('/petcall');
    await expect(player).toHaveReceivedMessage(msgFragment('Message.Command.Call.Success')); // "comes to you."
    await expectCondition(server, player, `at ${player.username} if entity ${pet.selector.replace(']', ',distance=..12]')}`);

    player.chat('/petsendaway');
    await expectCondition(server, player, `unless entity ${pet.selector}`);
  } finally {
    removePet(server, player);
    // World state persists across tests in a run — revert the platform we placed above.
    server.execute(`execute positioned 100 199 100 run fill ~-3 ~ ~-3 ~3 ~ ~3 minecraft:air`);
  }
});

test('petcall without a pet reports the no-pet error', async ({ player }) => {
  await player.makeOp();
  player.chat('/petcall');
  await expect(player).toHaveReceivedMessage(msgFragment('Message.No.HasPet')); // "have a pet!"
});
