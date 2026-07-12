import { test } from '@drownek/plugwright';
import { expectCondition, expectConditionHolds } from '../lib/oracle.js';
import { msgPlain } from '../lib/locale.js';
import { createPet, removePet } from '../lib/pets.js';
import { setupArena, ARENA } from '../lib/world.js';
import { secondBot } from '../lib/players.js';

// BuffReceiver `Everyone` is the only mode an unrelated second bot can qualify for (Owner
// is default, Party needs a live party-plugin hook this harness lacks), so both tests
// cycle the receiver slot twice (Owner -> Party -> Everyone) after arming the buff.
//
// test-beacon.st.json: Range+10, Duration+8 (locked fixture). BeaconImpl.schedule() squares
// range and scales it by log10(saturation)/2 -- a fresh pet's saturation 100 means no shrink,
// so range² stays 100 for these short tests. Near bot sits at distance² 16 (well inside);
// far bot moves to distance² 324 (well outside, but still on setupArena's ±20 platform --
// going further drops it off the edge and masks the range check under a fall). Both pets
// are NoAI-frozen so the range math has a fixed reference point.

// beacon.json "receiver" section renders at raw slot row*9+col = 40. Located by fixed slot,
// not display text, since the button's title text changes on every click.
const RECEIVER_SLOT = 4 * 9 + 4;

const STRENGTH_ACTIVE = `nbt={active_effects:[{id:"minecraft:strength"}]}`;
const FAR_DX = 18; // outside range² 100, still on the ARENA.x±20 platform

/** Arms test-beacon's Strength buff for the Everyone receiver mode. */
async function armBeaconForEveryone(player: any): Promise<void> {
  player.chat('/petbeacon');
  const gui = await player.gui({ title: msgPlain('Gui.Beacon.Title'), timeout: 8000 });

  // Buff title is the raw untranslated potion-effect key -- lowercase-includes match.
  await gui.locator((i: any) => String(i.getDisplayName() ?? '').toLowerCase().includes('strength')).click();

  // owner -> party -> everyone.
  await gui.locator((i: any) => i.slot === RECEIVER_SLOT).click();
  await gui.locator((i: any) => i.slot === RECEIVER_SLOT).click();

  // Separate "active" toggle -- selecting a buff alone never arms the beacon.
  await gui.locator((i: any) => String(i.getDisplayName() ?? '').includes(msgPlain('Gui.Beacon.Toggle.Off'))).click();
}

test('second bot inside beacon range gains the buff', async ({ player, server, createPlayer }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Cow', { skilltree: 'test-beacon' });
  server.execute(`data merge entity ${pet.selector} {NoAI:1b}`);
  const nearby = await secondBot(createPlayer, server, player, { username: 'BeaconNear' });

  try {
    await armBeaconForEveryone(player);

    await expectCondition(server, player,
      `if entity @a[name=${nearby.bot.username},${STRENGTH_ACTIVE}]`, { timeout: 10000 });
  } finally {
    removePet(server, player);
    await nearby.dispose();
  }
});

test('second bot moved out of beacon range loses the buff and it does not reapply', async ({ player, server, createPlayer }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Cow', { skilltree: 'test-beacon' });
  server.execute(`data merge entity ${pet.selector} {NoAI:1b}`);
  const roamer = await secondBot(createPlayer, server, player, { username: 'BeaconFar' });

  try {
    await armBeaconForEveryone(player);

    // Confirm the buff lands first, so the absence proof below is a genuine
    // "went away and stayed away", not a vacuous "never had it".
    await expectCondition(server, player,
      `if entity @a[name=${roamer.bot.username},${STRENGTH_ACTIVE}]`, { timeout: 10000 });

    await roamer.bot.teleport(ARENA.x + FAR_DX, ARENA.y, ARENA.z);
    await expectCondition(server, player,
      `positioned ${ARENA.x + FAR_DX} ${ARENA.y} ${ARENA.z} if entity @a[name=${roamer.bot.username},distance=..4]`);

    // Wait out the last-applied instance's remaining duration (Duration+8 -> 8s) for a
    // genuine expiry, not a race with an already-scheduled reapplication.
    await expectCondition(server, player,
      `unless entity @a[name=${roamer.bot.username},${STRENGTH_ACTIVE}]`, { timeout: 12000 });

    // Held longer to prove the range check excludes the far bot on every cycle, not once.
    await expectConditionHolds(server, player,
      `unless entity @a[name=${roamer.bot.username},${STRENGTH_ACTIVE}]`, { checks: 5, interval: 1500 });
  } finally {
    removePet(server, player);
    await roamer.dispose();
  }
});
