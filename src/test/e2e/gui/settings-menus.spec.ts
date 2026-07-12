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

// MenuIds.PET_ADMIN_SELECTION is registered but unreachable -- no command or
// listener in the plugin module ever calls GuiService#openMenu(...,
// PET_ADMIN_SELECTION, ...) (confirmed by grepping every /petadmin subcommand
// and openMenu call site); likely unwired GUI-system scaffolding. Driving it
// would need a Java call site (out of scope), so this test exercises the real
// reachable equivalent instead: CommandOptionSwitch's chat-based flow
// (`/petadmin switch <player>` lists stored pets; `... <petname>` switches).
test("petadmin switch lists and selects a player's pets for admins (pet-admin-selection GUI is unwired dead code)", async ({ player, server }) => {
  await player.makeOp();
  await createPet(server, player, 'Cow', { name: 'AdminSeen' });
  try {
    const sinceList = player.getMessageBufferIndex();
    player.chat(`/petadmin switch ${player.username}`);
    // CommandOptionSwitch#showPetList sends this exact literal header (not a
    // locale key) before the clickable pet-name list.
    await expect(player).toHaveReceivedMessage(
      'Select the Pet you want the player to switch to:', { since: sinceList });
    await expect(player).toHaveReceivedMessage('AdminSeen', { since: sinceList });

    // Run the exact switch command the chat list's click-event would fire, to
    // prove the flow works end-to-end, not just the listing.
    const sinceSwitch = player.getMessageBufferIndex();
    player.chat(`/petadmin switch ${player.username} AdminSeen`);
    await expect(player).toHaveReceivedMessage(msgFragment('Message.Command.Success'), { since: sinceSwitch });
  } finally {
    removePet(server, player);
  }
});
