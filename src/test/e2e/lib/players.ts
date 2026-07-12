import { ARENA } from './world.js';
import { expectCondition } from './oracle.js';

/**
 * Creates and configures a second bot for multiplayer specs: connects via the test
 * context's `createPlayer`, ops it, and teleports it to ARENA.
 *
 * Ops via deop-then-op: `makeOp()` can't be called twice on the same username in one
 * server session, and op status persists across tests reusing the default username.
 *
 * Teleport retries with a rejoin fallback: a fresh bot's join position is randomized near
 * world spawn, and a long tp at login is the profile observed to trigger dropped-packet
 * session issues. Each attempt is verified both client-side (`teleport()`'s poll) and
 * server-side (tellraw oracle) so a client-only illusion of arrival can't slip through.
 */
export async function secondBot(
  createPlayer: any, server: any, primary: any,
  { username = 'TestBot2', attempts = 3 }: { username?: string; attempts?: number } = {},
): Promise<{ bot: any; dispose(): Promise<void> }> {
  const bot = await createPlayer({ username });
  await bot.deOp();
  await bot.makeOp();

  let lastErr: unknown;
  for (let i = 0; i < attempts; i++) {
    try {
      await bot.teleport(ARENA.x + 4, ARENA.y, ARENA.z);
      await expectCondition(server, primary,
        `positioned ${ARENA.x} ${ARENA.y} ${ARENA.z} if entity @a[name=${username},distance=..8]`,
        { timeout: 6000 });
      lastErr = undefined;
      break;
    } catch (err) {
      lastErr = err;
      if (i === attempts - 1) break;
      await bot.rejoin();
      await bot.deOp();
      await bot.makeOp();
    }
  }
  if (lastErr) throw lastErr;

  return {
    bot,
    /**
     * Quits the bot mid-test. Mirrors plugwright's internal `disconnectBot` (not part of
     * its public export surface): `bot.quit()` + wait for mineflayer's `'end'` event, 3s
     * fallback. Idempotent — safe even if the runner's own cleanup already disconnected it.
     */
    async dispose(): Promise<void> {
      const raw = bot.bot;
      if ((raw as any)._client?.ended) return;
      await new Promise<void>((resolve) => {
        const timeout = setTimeout(resolve, 3000);
        try {
          raw.once('end', () => { clearTimeout(timeout); resolve(); });
          raw.quit();
        } catch {
          clearTimeout(timeout);
          resolve();
        }
      });
    },
  };
}
