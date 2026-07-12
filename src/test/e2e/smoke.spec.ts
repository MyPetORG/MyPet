import { test, expect } from '@drownek/plugwright';

const sleep = (ms: number) => new Promise(r => setTimeout(r, ms));

test('MyPet enables, a pet can be created, and its menu opens', async ({ player, server }) => {
  await player.makeOp();

  try {
    // Pet type is a namespaced key with a mandatory namespace (PetTypeArgument):
    // `minecraft:cow`, not `Cow`. A bare name fails with "Invalid ID".
    server.execute(`petadmin create -f ${player.username} minecraft:cow name:SmokePet`);

    // Poll a vanilla conditional through tellraw until the pet exists near the bot.
    let found = false;
    for (let i = 0; i < 20 && !found; i++) {
      server.execute(
        `execute at ${player.username} if entity @e[type=minecraft:cow,distance=..16] ` +
        `run tellraw ${player.username} {"text":"SMOKE_PET_OK"}`
      );
      try {
        await expect(player).toHaveReceivedMessage('SMOKE_PET_OK', { timeout: 750 });
        found = true;
      } catch { await sleep(250); }
    }
    if (!found) throw new Error('pet never spawned');

    player.chat('/pet');
    const gui = await player.gui({ title: /Your Pet/ });
    // ItemWrapper.name is the raw material id (e.g. "player_head"), not the display
    // name — use getDisplayName() to match on the item's visible text ("Stay").
    await gui.locator(item => item.getDisplayName().includes('Stay')).click();
  } finally {
    server.execute(`petadmin remove ${player.username}`);
  }
});
