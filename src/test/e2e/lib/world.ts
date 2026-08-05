import { expect } from '@drownek/plugwright';
import { expectCondition } from './oracle.js';

const sleep = (ms: number) => new Promise(r => setTimeout(r, ms));

export const ARENA = { x: 0, y: 200, z: 0 };

/**
 * Idempotent: gamerules are silent no-ops on this Paper build, so mob-spawn suppression
 * actually comes from a persistent sea-lantern platform (light 15) + air ceiling +
 * `time set day`. Safe to call repeatedly — the platform is shared infra, never cleaned up.
 */
export async function setupArena(server: any, player: any): Promise<void> {
  for (const cmd of [
    'gamerule doMobSpawning false',
    'gamerule doDaylightCycle false',
    'gamerule doWeatherCycle false',
    'gamerule mobGriefing false',
    'gamerule doImmediateRespawn true',
    'time set day',
    // Without this the fills silently fail with "position not loaded" if the bot joins far away.
    `forceload add ${ARENA.x - 20} ${ARENA.z - 20} ${ARENA.x + 20} ${ARENA.z + 20}`,
    `fill ${ARENA.x - 20} ${ARENA.y - 1} ${ARENA.z - 20} ${ARENA.x + 20} ${ARENA.y - 1} ${ARENA.z + 20} minecraft:sea_lantern`,
    `fill ${ARENA.x - 20} ${ARENA.y} ${ARENA.z - 20} ${ARENA.x + 20} ${ARENA.y + 4} ${ARENA.z + 20} minecraft:air`,
    `tp ${player.username} ${ARENA.x} ${ARENA.y} ${ARENA.z}`,
  ]) server.execute(cmd);

  await expectCondition(server, player,
    `positioned ${ARENA.x} ${ARENA.y} ${ARENA.z} if entity @a[name=${player.username},distance=..4]`);
}

/** Summons a tagged, persistent mob near ARENA; returns its `@e[tag=…,limit=1]` selector. */
export async function spawnVictim(
  server: any, player: any, mob: string, tag: string,
  { noAI = true, dx = 2, dz = 0 } = {},
): Promise<string> {
  const nbt = `{Tags:["${tag}"],PersistenceRequired:1b${noAI ? ',NoAI:1b' : ''}}`;
  server.execute(`summon minecraft:${mob} ${ARENA.x + dx} ${ARENA.y} ${ARENA.z + dz} ${nbt}`);
  await expectCondition(server, player, `if entity @e[tag=${tag}]`);
  return `@e[tag=${tag},limit=1]`;
}

/** Kills every entity carrying `tag`. Fire-and-forget, like `server.execute`. */
export function killTagged(server: any, tag: string): void {
  server.execute(`kill @e[tag=${tag}]`);
}

/**
 * Finds the live Mineflayer entity of type `entityTypeName` NEAREST to the bot.
 *
 * Nearest, not first. `attackPinned` teleports the *tagged* mob to ~1.5 blocks in front of
 * the bot and then swings, but the swing resolves its target by entity TYPE — so the pin
 * only means anything if the swing picks the closest match. Returning an arbitrary entry
 * from the (unordered) entity map means that with two mobs of one type in play, the bot
 * can pin one and hit the other: the tagged mob stays at HurtTime 0 and the test fails as
 * an unexplained "no message after N swings" timeout rather than pointing at the cause.
 */
export function findEntity(player: any, entityTypeName: string): any {
  const origin = player.bot.entity?.position;
  const matches = Object.values(player.bot.entities as Record<string, any>)
    .filter((e: any) => e?.name === entityTypeName && e.isValid !== false && e.position);
  if (!origin) return matches[0];
  let nearest: any = null;
  let nearestDistance = Infinity;
  for (const entity of matches) {
    const distance = entity.position.distanceTo(origin);
    if (distance < nearestDistance) {
      nearestDistance = distance;
      nearest = entity;
    }
  }
  return nearest;
}

/** Melee-swings the bot at the nearest entity of the given type name. */
export async function botAttack(player: any, entityTypeName: string): Promise<void> {
  const target = findEntity(player, entityTypeName);
  if (!target) throw new Error(`Bot cannot see any ${entityTypeName}`);
  await player.bot.attack(target);
}

/** Polls the bot's inventory until `give` (console, fire-and-forget) actually arrives, then equips it. */
export async function equipItem(player: any, itemName: string, timeoutMs = 5000): Promise<void> {
  const deadline = Date.now() + timeoutMs;
  for (;;) {
    const item = player.bot.inventory.items().find((i: any) => i.name === itemName);
    if (item) { await player.bot.equip(item, 'hand'); return; }
    if (Date.now() > deadline) throw new Error(`bot has no ${itemName}`);
    await sleep(150);
  }
}

/**
 * Teleports the tagged mob back into melee reach, then swings at it. AI-enabled victims
 * wander, and a swing outside server-side melee reach is silently rejected (no damage, no
 * tame). Also safe (a no-op re-pin) against a stationary NoAI victim.
 */
export async function attackPinned(server: any, player: any, tag: string, mobName: string): Promise<void> {
  // Pin relative to the bot's CURRENT position: retaliation knockback can displace the
  // bot, and a fixed arena point would then leave later swings permanently out of reach.
  server.execute(`execute at ${player.username} run tp @e[tag=${tag},limit=1] ~1.5 ~ ~`);
  await sleep(400);
  await botAttack(player, mobName);
}

/**
 * Teleports the tagged entity back next to `username` and waits for it to settle. Same
 * out-of-reach hazard as `attackPinned`, generalized for any right-click interaction —
 * safe (a no-op re-pin) against a stationary NoAI entity.
 */
export async function pinNear(server: any, tag: string, username: string): Promise<void> {
  server.execute(`execute at ${username} run tp @e[tag=${tag},limit=1] ~1.5 ~ ~`);
  await sleep(400);
}

/**
 * attackPinned with a verified outcome, for tame swings: a pinned swing can still be
 * silently rejected if the mob drifts out of reach between tp and attack, so re-pin and
 * retry until the tame reply arrives. Safe only for tame swings — a successful leash
 * cancels the damage, so retries can't kill the low-health victim.
 */
export async function tameSwingExpecting(
  server: any, player: any, tag: string, mobName: string, itemName: string,
  fragment: string, attempts = 8,
): Promise<void> {
  let since = player.getMessageBufferIndex();
  for (let i = 0; i < attempts; i++) {
    if (i === 2 || i === 5) {
      // Rejoin fallback at attempts 2 and 5: a long-range-teleported bot can end up in a
      // session state where use-entity packets are silently dropped while chat still
      // works. A fresh connection at the arena (no long tp) clears it; a second rejoin
      // covers sessions that stay degraded through the first.
      await player.rejoin();
      await equipItem(player, itemName);
      since = player.getMessageBufferIndex();
    }
    await attackPinned(server, player, tag, mobName);
    try {
      await expect(player).toHaveReceivedMessage(fragment, { since, timeout: 2500 });
      return;
    } catch { /* swing likely dropped server-side — re-pin and retry */ }
  }
  throw new Error(`No "${fragment}" after ${attempts} pinned swings at ${mobName}`);
}
