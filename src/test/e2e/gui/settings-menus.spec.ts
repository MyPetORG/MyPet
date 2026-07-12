import { test, expect } from '@drownek/plugwright';
import { msgPlain, msgFragment } from '../lib/locale.js';
import { createPet, removePet } from '../lib/pets.js';

// GUI item text lives in the resolved display name, NOT ItemWrapper.name
// (the material id). Every predicate below matches getDisplayName()/getLore().

// Bug: a `<lang:key>` tag followed by trailing text on the same lore line
// truncates to just the resolved lang string, dropping everything after it
// (verified via a raw-NBT lore dump) -- so the "Current: X%" lore readback is
// unusable here (not fixed, no Java changes). Bar fill state is a bug-immune
// substitute: pet-volume.json's value-bar renders cells with distinct
// MATERIALs (LIME_DYE high / GRAY_DYE low, per ValueBarSection's fill rule),
// and `ItemWrapper.name` (lowercased material id) reads that directly.
function rawCell(gui: any, percentTitle: string): { name: string } | undefined {
  const snapshot = gui._getCurrentGuiSnapshot();
  return snapshot?._findItemInternal((i: any) => String(i.getDisplayName() ?? '').includes(percentTitle));
}

test('pet-volume menu opens and a bar click persists the volume', async ({ player, server }) => {
  await player.makeOp();
  await createPet(server, player, 'Cow');
  try {
    player.chat('/petsettings volume');
    const gui = await player.gui({ title: msgPlain('Gui.PetVolume.Title') });

    // Width 5 (PetVolumeMenuHandler.BAR_WIDTH): each cell's title is fixed at
    // percentForCell(index) = index * (100/(width-1)), so "25%" uniquely
    // identifies cell index 1. Click sets MyPetPlayer#petVolume = 0.25f
    // (default 1f) and refreshes both sections.
    await gui.locator((i: any) => String(i.getDisplayName() ?? '').includes('25%')).click();

    // Readback: cell 1 ("25%") must be the high-item (LIME_DYE, index <=
    // position) and cell 2 ("50%") the low-item (GRAY_DYE) -- together they
    // pin the fill position at index 1, i.e. petVolume == 0.25f.
    await expect.poll(() => rawCell(gui, '25%')?.name, { timeout: 3000 }).toBe('lime_dye');
    await expect.poll(() => rawCell(gui, '50%')?.name, { timeout: 3000 }).toBe('gray_dye');

    // Persistence: reopening rebuilds a fresh context from
    // MyPetPlayer#getPetVolume(), so re-checking the same two cells proves
    // the value survived on the player object, not just in-memory state.
    player.chat('/petsettings volume');
    const gui2 = await player.gui({ title: msgPlain('Gui.PetVolume.Title') });
    await expect.poll(() => rawCell(gui2, '25%')?.name, { timeout: 3000 }).toBe('lime_dye');
    await expect.poll(() => rawCell(gui2, '50%')?.name, { timeout: 3000 }).toBe('gray_dye');
  } finally {
    removePet(server, player);
  }
});

// `/petadmin switch <player>` opens the pet-admin-selection GUI for an in-game admin
// (console gets a chat list instead). Clicking a pet switches the target player to it.
test("petadmin switch opens the admin GUI and switching a player's pet works", async ({ player, server }) => {
  await player.makeOp();
  await createPet(server, player, 'Cow', { name: 'AdminSeen' });
  try {
    player.chat(`/petadmin switch ${player.username}`);
    const gui = await player.gui({ title: msgPlain('Gui.PetAdminSelection.Title') }); // "Admin: Select a Pet"
    const sinceSwitch = player.getMessageBufferIndex();
    await gui.locator((i: any) => String(i.getDisplayName() ?? '').includes('AdminSeen')).click();
    await expect(player).toHaveReceivedMessage(msgFragment('Message.Command.Success'), { since: sinceSwitch });
  } finally {
    removePet(server, player);
  }
});
