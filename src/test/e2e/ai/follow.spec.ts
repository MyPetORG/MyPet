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

test('a fish pet stranded on land closes the distance to its owner', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Cod');

  try {
    // Park the cod 9 blocks out: past the follow-start distance, but inside the 12-block
    // teleport threshold — so the only way it can get back is by actually moving. Vanilla
    // AbstractFish has a water-bound navigation and a MoveControl that zeroes its forward
    // input on land, which leaves it flopping in place forever.
    server.execute(`execute at ${player.username} run tp ${pet.selector} ~9 ~ ~`);
    await expectCondition(server, player,
      `at ${player.username} if entity ${pet.selector.replace(']', ',distance=8..]')}`);

    await expectCondition(server, player,
      `at ${player.username} if entity ${pet.selector.replace(']', ',distance=..5]')}`,
      { timeout: 30000 });
  } finally {
    removePet(server, player);
  }
});
