import { test, expect } from '@drownek/plugwright';
import { expectCondition } from '../lib/oracle.js';
import { createPet, removePet } from '../lib/pets.js';

// /petadmin subcommands reply to whichever sender issued them. lib/pets.ts's helpers issue
// via server.execute (console), so we issue directly via player.chat here to get replies
// routed back to the bot instead.

test('petadmin exp set reports the new level', async ({ player, server }) => {
  await player.makeOp();
  const pet = await createPet(server, player, 'Cow', { skilltree: 'test-damage' });
  try {
    player.chat(`/petadmin exp ${player.username} 500 set`);
    await expect(player).toHaveReceivedMessage('exp to');

    // levels-mode reply uses the same literal format; scope with { since } or this would
    // trivially match the first command's reply too.
    const since = player.getMessageBufferIndex();
    player.chat(`/petadmin exp ${player.username} 5 levels set`);
    await expect(player).toHaveReceivedMessage('Pet is now level', { since });
  } finally {
    removePet(server, player);
  }
});

test('petadmin skilltree switches the tree', async ({ player, server }) => {
  await player.makeOp();
  const pet = await createPet(server, player, 'Cow', { skilltree: 'test-damage' });
  try {
    const since = player.getMessageBufferIndex();
    player.chat(`/petadmin skilltree ${player.username} test-heal`);
    // {1} is the target skilltree's display name -- test-heal.st.json's Name is "test-heal".
    await expect(player).toHaveReceivedMessage('test-heal', { since });
  } finally {
    removePet(server, player);
  }
});

test('petadmin name + remove', async ({ player, server }) => {
  await player.makeOp();
  const pet = await createPet(server, player, 'Cow');
  try {
    const sinceName = player.getMessageBufferIndex();
    player.chat(`/petadmin name ${player.username} AdminNamed`);
    await expect(player).toHaveReceivedMessage('new name is now', { since: sinceName });
    await expectCondition(server, player, `if entity @e[tag=${pet.tag},name=AdminNamed]`);

    const sinceRemove = player.getMessageBufferIndex();
    player.chat(`/petadmin remove ${player.username}`);
    await expect(player).toHaveReceivedMessage('You removed the Pet of', { since: sinceRemove });
    await expectCondition(server, player, `unless entity ${pet.selector}`);
  } finally {
    removePet(server, player);
  }
});

test('petadmin create options: baby + skilltree + name', async ({ player, server }) => {
  await player.makeOp();
  const pet = await createPet(server, player, 'Cow', { baby: true, skilltree: 'test-life', name: 'BabyCow' });
  try {
    // VanillaMobSpawner applies "baby" via Ageable::setBaby(true), setting Age NBT to -24000.
    await expectCondition(server, player, `if entity @e[tag=${pet.tag},nbt={Age:-24000}]`);
  } finally {
    removePet(server, player);
  }
});
