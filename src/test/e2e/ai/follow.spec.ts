import { test } from '@drownek/plugwright';
import { expectCondition } from '../lib/oracle.js';
import { createPet, removePet } from '../lib/pets.js';
import { setupArena, ARENA } from '../lib/world.js';

test('pet follows / teleports to the owner across the arena', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Cow');

  try {
    server.execute(`tp ${player.username} ${ARENA.x + 18} ${ARENA.y} ${ARENA.z + 18}`);

    await expectCondition(server, player,
      `at ${player.username} if entity ${pet.selector.replace(']', ',distance=..10]')}`,
      { timeout: 30000 });
  } finally {
    removePet(server, player);
  }
});
