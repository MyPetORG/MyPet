import { test, expect, sleep } from '@drownek/plugwright';
import { expectCondition, expectConditionHolds } from '../lib/oracle.js';
import { msgPlain, msgFragment } from '../lib/locale.js';
import { createPet, removePet } from '../lib/pets.js';
import { setupArena, ARENA, killTagged } from '../lib/world.js';
import { releasePet } from '../lib/gui.js';

// GUI item text lives in the resolved display name, NOT ItemWrapper.name
// (the material id, e.g. "red_bed"). Every predicate below matches getDisplayName().

test('pet menu: Stay toggle stops following', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  // Wolf is PetSittable (wolf/cat/camel/panda/fox), so PetSitGoal locks it in
  // place and the vanilla `Sitting` NBT flag becomes an observable proxy for
  // the toggle — a Cow has no Sittable pose to check.
  const pet = await createPet(server, player, 'Wolf');

  try {
    player.chat('/pet');
    const gui = await player.gui({ title: /Your Pet/ });
    await gui
      .locator((i: any) => String(i.getDisplayName() ?? '').includes(msgPlain('Gui.PetMenu.Stay.Title')))
      .click();

    // Confirm the click registered server-side before asserting behavior:
    // the vanilla Sitting NBT flag flips in the same setSitting() call that
    // backs Pet#canMove().
    await expectCondition(server, player, `if entity @e[tag=${pet.tag},nbt={Sitting:1b}]`, { timeout: 3000 });

    // Move away in ≤10-block hops: a single 25-block `/tp` would bypass the
    // sit gate entirely — PlayerListener.onPet(PlayerTeleportEvent) respawns
    // the pet at the owner on any >10-block teleport, regardless of sitting.
    for (const [dx, dz] of [[9, 0], [18, 0], [18, 9], [18, 18]]) {
      server.execute(`tp ${player.username} ${ARENA.x + dx} ${ARENA.y} ${ARENA.z + dz}`);
      await sleep(300);
    }

    // Owner is now ~25 blocks from the sitting pet; a following pet would
    // close that in ~2-3s, so a staying pet must stay outside 12 blocks.
    await expectConditionHolds(server, player,
      `at ${player.username} unless entity @e[tag=${pet.tag},distance=..12]`, { checks: 5 });
  } finally {
    removePet(server, player);
  }
});

test('pet-selection: switch back to a stored pet', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  await createPet(server, player, 'Cow', { name: 'StoredCow' });
  try {
    player.chat('/petstore');
    await expectCondition(server, player,
      `at ${player.username} unless entity @e[type=minecraft:cow,distance=..16]`);
    await createPet(server, player, 'Pig', { name: 'ActivePig' });

    player.chat('/petswitch');
    // Gui.PetSelection.Title has no dynamic content ("Select a Pet") — plain
    // string is fine (player.gui matches by substring for string titles).
    const gui = await player.gui({ title: msgPlain('Gui.PetSelection.Title') });
    await gui.locator((i: any) => String(i.getDisplayName() ?? '').includes('StoredCow')).click();

    await expectCondition(server, player,
      `at ${player.username} if entity @e[type=minecraft:cow,name=StoredCow,distance=..16]`);
  } finally {
    // Switching back reactivates the cow (removePet targets it); the stored
    // pig record stays behind in the DB with no live entity — harmless for
    // later specs (only inactive/stored, not a stray world entity).
    removePet(server, player);
  }
});

test('choose-skilltree assigns the clicked tree', async ({ player, server }) => {
  await player.makeOp();
  await createPet(server, player, 'Cow');
  try {
    player.chat('/petchooseskilltree');
    const gui = await player.gui({ title: msgPlain('Gui.ChooseSkilltree.Title') });
    await gui.locator((i: any) => String(i.getDisplayName() ?? '').includes('test-heal')).click();
    await expect(player).toHaveReceivedMessage(msgFragment('Message.Skilltree.SwitchedTo'));
  } finally {
    removePet(server, player);
  }
});

test('release-confirm frees the pet: entity remains, ownership gone', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Cow', { name: 'FreeMe' });
  try {
    await releasePet(player, server);

    // stripPetCustomizations clears CustomName/PDC keys on release, so
    // name=FreeMe would no longer match — the untouched scoreboard tag proves
    // the mob entity itself survived.
    await expectCondition(server, player, `if entity @e[tag=${pet.tag}]`);

    player.chat('/petinfo');
    await expect(player).toHaveReceivedMessage(msgFragment('Message.No.HasPet'));
  } finally {
    killTagged(server, pet.tag);
    removePet(server, player);
  }
});

test('backpack GUI opens with the pet-named title', async ({ player, server }) => {
  await player.makeOp();
  await createPet(server, player, 'Cow', { name: 'PackCow', skilltree: 'test-backpack' });
  try {
    player.chat('/petinventory');
    await player.gui({ title: /PackCow's Backpack/ });
  } finally {
    removePet(server, player);
  }
});
