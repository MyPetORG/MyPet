import { msgPlain } from './locale.js';

/**
 * Drives the pet-release confirm-dialog flow. Bare `/petrelease` only sends a clickable
 * chat component (`CommandRelease.executeNoArgs`) that a Mineflayer bot can't click, so
 * this opens `/pet`, clicks "Release Pet", then confirms in the resulting dialog.
 */
export async function releasePet(player: any, server: any): Promise<void> {
  player.chat('/pet');
  const hub = await player.gui({ title: /Your Pet/, timeout: 8000 });
  await hub
    .locator((i: any) => String(i.getDisplayName() ?? '').includes(msgPlain('Gui.PetMenu.Release.Title')))
    .click();

  const confirm = await player.gui({ timeout: 8000, title: /./ }); // dynamic dialog title (pet name)
  await confirm
    .locator((i: any) => String(i.getDisplayName() ?? '').includes(msgPlain('Gui.PetReleaseConfirm.Yes')))
    .click();
}

/**
 * Drives `/pet` hub -> trade-target-picker -> trade-confirm to submit a free trade offer
 * (the GUI flow has no price input) to `targetUsername`. Accept/reject is left to the
 * target via `/pettrade accept|reject` chat — there's no GUI on their side.
 * The picker's per-item title is the target's raw username (no wrapper text), so the
 * locator matches `targetUsername` directly rather than through `msgPlain`.
 */
export async function tradePet(player: any, targetUsername: string): Promise<void> {
  player.chat('/pet');
  const hub = await player.gui({ title: /Your Pet/, timeout: 8000 });
  await hub
    .locator((i: any) => String(i.getDisplayName() ?? '').includes(msgPlain('Gui.PetMenu.Trade.Title')))
    .click();

  const picker = await player.gui({ title: new RegExp(msgPlain('Gui.PetTradeTarget.Title')), timeout: 8000 });
  await picker
    .locator((i: any) => String(i.getDisplayName() ?? '').includes(targetUsername))
    .click();

  const confirm = await player.gui({ timeout: 8000, title: /./ }); // dynamic dialog title (pet name + target)
  await confirm
    .locator((i: any) => String(i.getDisplayName() ?? '').includes(msgPlain('Gui.PetTradeConfirm.Yes')))
    .click();
}
