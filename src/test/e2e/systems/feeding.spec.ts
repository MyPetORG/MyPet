import { test } from '@drownek/plugwright';
// Deep import of the runner's live message buffer (no "exports" field, so dist paths are
// importable) — needed to parse the /petinfo hunger number, since nextMessage() can miss
// lines arriving in the same burst and toHaveReceivedMessage can't return a match.
import { messageBuffer } from '@drownek/plugwright/dist/lib/bot-utils.js';
import { expectScore } from '../lib/oracle.js';
import { createPet, removePet } from '../lib/pets.js';
import { setupArena, equipItem, findEntity } from '../lib/world.js';

const sleep = (ms: number) => new Promise(r => setTimeout(r, ms));

/**
 * Right-clicks a nearby entity of `entityType` with `food` equipped. No pinning/retry
 * needed here — unlike taming, the pet is owned and follows the owner.
 */
async function feedPet(player: any, server: any, entityType: string, food: string): Promise<void> {
  server.execute(`give ${player.username} minecraft:${food} 4`);
  await equipItem(player, food);
  await activatePet(player, entityType);
}

/** One right-click on the pet with whatever is already equipped. */
async function activatePet(player: any, entityType: string): Promise<void> {
  const pet = findEntity(player, entityType);
  if (!pet) throw new Error(`bot cannot see ${entityType}`);
  await player.bot.activateEntity(pet);
}

/**
 * Runs /petinfo and parses the printed hunger number ("   Hunger: <n>",
 * PetInfoBuilder.hungerLine). Scoped with the message-buffer index captured
 * before the chat, so it can only match THIS /petinfo's output.
 */
async function readHunger(player: any): Promise<number> {
  const since = player.getMessageBufferIndex();
  player.chat('/petinfo');
  const deadline = Date.now() + 5000;
  while (Date.now() < deadline) {
    for (const msg of messageBuffer.slice(since)) {
      const m = /Hunger: (\d+)/.exec(msg);
      if (m) return parseInt(m[1], 10);
    }
    await sleep(150);
  }
  throw new Error('/petinfo printed no Hunger line within 5s');
}

/** Total wheat count across the bot's inventory (client-side view). */
function heldWheat(player: any): number {
  return player.bot.inventory.items()
    .filter((i: any) => i.name === 'wheat')
    .reduce((n: number, i: any) => n + i.count, 0);
}

/** Polls until the client-side wheat count drops below `from` (consume observed). */
async function waitWheatBelow(player: any, from: number, timeoutMs = 3000): Promise<boolean> {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    if (heldWheat(player) < from) return true;
    await sleep(150);
  }
  return false;
}

// (a) PetImpl's feed branch refuses (no consume, no heal) only when saturation>=100 AND
//     health>=maxHealth (commit 815f0d51f, "Fix pets eating food when already full"); a
//     damaged-but-saturated pet still eats and heals +1 HP. Otherwise it consumes one food
//     item and heals +1 HP.
//
// (b) Cow's food is Wheat, grow-up item is experience_bottle (PetCow @DefaultInfo /
//     GrowUpItem). Grow-up is a separate branch above the feed loop — consumes one item and
//     calls setBaby(false), no saturation/health interaction.
//
// (c) HungerSystem.Time defaults 60s/decrement — too slow for the suite's 30s cap, so
//     build.gradle.kts's plugwright config stages it down to 4s. Hunger decay ticks on the
//     pet's own mob-scheduler (from spawn), not the player ticker, so no rejoin is needed.
//
// Flake fix: a fresh pet starts decaying immediately, so the staged 4s period can race both
// saturation-sensitive tests (setup round-trips outlasting the first decrement). The refusal
// test now proves activate-packets land first (consume on a damaged pet), then retries the
// refusal attempt until one lands while /petinfo reads exactly 100, topping back up via a
// legitimate feed if a decrement slipped in; a real regression still eats on every
// verified-full attempt and exhausts the retry loop. The hunger test asserts strictly-lower
// than a captured baseline instead of an assumed literal 100.
test('feeding a damaged pet heals it', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Cow');
  try {
    // Cow base is 10. Damage to 6 so feeding's heal is observable without killing it.
    server.execute(`damage ${pet.selector} 4 minecraft:generic`);
    await expectScore(server, player, `data get entity ${pet.selector} Health`, '..8');
    await feedPet(player, server, 'cow', 'wheat');
    await expectScore(server, player, `data get entity ${pet.selector} Health`, '7..', { timeout: 15000 });
  } finally {
    removePet(server, player);
  }
});

test('a full, healthy pet refuses food (a19 regression)', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Cow');
  try {
    server.execute(`clear ${player.username} minecraft:wheat`);
    server.execute(`give ${player.username} minecraft:wheat 8`);
    await equipItem(player, 'wheat');

    // Positive control first: prove activateEntity packets land and consume, so a dropped
    // packet can't make "held count unchanged" pass vacuously. Damage the pet (Cow base 10)
    // well below max so it eats regardless of saturation and vanilla regen can't close the
    // gap before the feed lands.
    server.execute(`damage ${pet.selector} 4 minecraft:generic`);
    await expectScore(server, player, `data get entity ${pet.selector} Health`, '..8');
    let consumed = false;
    for (let i = 0; i < 4 && !consumed; i++) {
      const before = heldWheat(player);
      await activatePet(player, 'cow');
      consumed = await waitWheatBelow(player, before);
    }
    if (!consumed) throw new Error('positive control failed: a feed on a damaged pet never consumed wheat');
    // Heal to full so the refusal check below tests a genuinely full-health pet.
    server.execute(`effect give ${pet.selector} minecraft:instant_health 1 9`);
    await expectScore(server, player, `data get entity ${pet.selector} Health`, '10..');

    // Only an activate performed while /petinfo reads exactly 100 counts. If a decay
    // decrement slipped in, top saturation back up with a legitimate feed and retry.
    let refused = false;
    for (let i = 0; i < 5 && !refused; i++) {
      const hunger = await readHunger(player);
      if (hunger < 100) {
        const before = heldWheat(player);
        await activatePet(player, 'cow'); // top-up feed (sat = min(100, sat+6))
        await waitWheatBelow(player, before);
        continue;
      }
      const before = heldWheat(player);
      await activatePet(player, 'cow');
      await sleep(1500); // inventory-sync window: long enough to observe a consume
      refused = heldWheat(player) === before;
    }
    if (!refused) {
      throw new Error('food was consumed by a full pet on every verified-full attempt (a19 regression)');
    }
  } finally {
    removePet(server, player);
  }
});

test('hunger decays over time when the hunger system ticks', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  await createPet(server, player, 'Cow');
  try {
    // Baseline-relative, not an assumed "100": the first decrement can land before this
    // read, so the baseline may legitimately be 99. The point is strict decrease from here.
    const before = await readHunger(player);
    if (before < 90) throw new Error(`implausible fresh-pet hunger baseline: ${before}`);

    // Time staged to 4s; a 10s wait guarantees at least two decrements land.
    await sleep(10000);

    const after = await readHunger(player);
    if (!(after < before)) {
      throw new Error(`hunger never decayed (before=${before}, after=${after})`);
    }
  } finally {
    removePet(server, player);
  }
});

test('grow-up item turns a baby pet adult', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Cow', { baby: true });
  try {
    await expectScore(server, player, `data get entity ${pet.selector} Age`, '..-1');
    await feedPet(player, server, 'cow', 'experience_bottle');
    await expectScore(server, player, `data get entity ${pet.selector} Age`, '0..', { timeout: 15000 });
  } finally {
    removePet(server, player);
  }
});
