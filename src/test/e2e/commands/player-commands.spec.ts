import { test, expect } from '@drownek/plugwright';
import { expectCondition } from '../lib/oracle.js';
import { msgFragment, msgPlain } from '../lib/locale.js';
import { createPet, removePet } from '../lib/pets.js';
import { setupArena } from '../lib/world.js';

test('petname renames the pet (message + entity name)', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Cow');
  try {
    player.chat('/petname Bessie');
    await expect(player).toHaveReceivedMessage(msgFragment('Message.Command.Name.New')); // "Your pet's name is now:"
    await expectCondition(server, player, `if entity @e[tag=${pet.tag},name=Bessie]`);
  } finally {
    removePet(server, player);
  }
});

test('petinfo prints pet information', async ({ player, server }) => {
  await player.makeOp();
  const pet = await createPet(server, player, 'Cow', { name: 'InfoPet' });
  try {
    player.chat('/petinfo');
    await expect(player).toHaveReceivedMessage('InfoPet');
  } finally {
    removePet(server, player);
  }
});

test('petbehavior sets a specific mode', async ({ player, server }) => {
  await player.makeOp();
  // test-behavior.st.json enables Aggro + Duel only (Friend: false), so this asserts
  // /petbehavior duel -- a mode the fixture actually enables.
  const pet = await createPet(server, player, 'Cow', { skilltree: 'test-behavior' });
  try {
    const since = player.getMessageBufferIndex();
    player.chat('/petbehavior duel');
    await expect(player).toHaveReceivedMessage(msgFragment('Message.Skill.Behavior.NewMode'), { since });
    // Name.Duel confirms it's specifically Duel mode, not a generic/fallback confirmation.
    await expect(player).toHaveReceivedMessage(msgPlain('Name.Duel'), { since });
  } finally {
    removePet(server, player);
  }
});

test('petrespawn: a killed pet can be respawned immediately', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Cow');
  try {
    // Timer.startPlayerTicking (fixed by commit 23684000e) now registers a first-session
    // player's schedule() timer too, but rejoin() is kept anyway: this test's selectors
    // rely on the fresh-entity respawn rejoin() produces (quit despawns the old bukkit
    // entity, join recreates it).
    await player.rejoin();

    // Rejoin respawns the pet on a fresh (untagged) entity -- track by type/distance
    // instead of `pet.tag` from here on.
    await expectCondition(server, player,
      `at ${player.username} if entity @e[type=minecraft:cow,distance=..16]`);

    // Kill retried as the poll's `pre`, not one-shot: a single kill can land in the gap
    // between the quit-despawn and the join-respawn's async activation and find nothing,
    // leaving the freshly-respawned cow alive and the unless-poll failing forever.
    await expectCondition(server, player,
      `at ${player.username} unless entity @e[type=minecraft:cow,distance=..16]`,
      { pre: [`execute at ${player.username} run kill @e[type=minecraft:cow,distance=..16,limit=1]`] });

    server.execute(`petadmin respawn ${player.username} 0`);
    await expectCondition(server, player,
      `at ${player.username} if entity @e[type=minecraft:cow,distance=..16]`);
  } finally {
    removePet(server, player);
  }
});
