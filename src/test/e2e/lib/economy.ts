import { expect } from '@drownek/plugwright';

/**
 * Economy provider: VaultUnlocked 2.20.2 (Vault Economy/Permission API MyPet's VaultHook
 * targets) + PlayerPoints 3.3.5, which registers a single `/points` command and doesn't
 * shadow vanilla commands. EssentialsX was rejected — its command interception strips the
 * `execute` source context.
 * PlayerPoints surface: `points give|set <player> <amount>` (console, integer only);
 * `/points me` replies "You have %amount% Points." with "," as thousands separator.
 */

/** Sets a player's Vault balance to an exact amount — console command, fire-and-forget. */
export function setBalance(server: any, player: any, amount: number): void {
  server.execute(`points set ${player.username} ${amount}`);
}

/** Adds `amount` to a player's Vault balance via the provider's admin give command — console, fire-and-forget. */
export function fundBot(server: any, player: any, amount: number): void {
  server.execute(`points give ${player.username} ${amount}`);
}

/**
 * Polls `/points me` until it reports `expectedAmount`. PlayerPoints applies console
 * give/set asynchronously, so a single immediate query can read the pre-mutation balance —
 * each attempt scopes its match to its own `since` so a stale reply can't satisfy it.
 * Doubles as the funding barrier before purchases.
 */
export async function expectBalanceReply(
  player: any, expectedAmount: number, opts: { attempts?: number; timeout?: number } = {},
): Promise<void> {
  const amountStr = expectedAmount.toLocaleString('en-US');
  // Anchored on "You have " and right-guarded so 900 can't match inside "9,000".
  const pattern = new RegExp(`You have ${amountStr.replace(/,/g, ',?')}(?![\\d,])`);
  const attempts = opts.attempts ?? 4;
  let lastErr: unknown;
  for (let i = 0; i < attempts; i++) {
    const since = player.getMessageBufferIndex();
    player.chat('/points me');
    try {
      await expect(player).toHaveReceivedMessage(pattern, { since, timeout: opts.timeout ?? 2000 });
      return;
    } catch (e) { lastErr = e; }
  }
  throw lastErr;
}
