import { expect } from '@drownek/plugwright';

/**
 * Reads a MyPet PlaceholderAPI placeholder back through `/papi parse me ...` and
 * asserts it resolves to `expected`.
 *
 * The placeholder is wrapped in an `e2e_ph=` sentinel so the assertion anchors on
 * our own token and can't accidentally match PlaceholderAPI's command chrome or a
 * stale line already in the buffer. Each attempt scopes its match with `since`, and
 * we retry because some placeholders are backed by asynchronously-refreshed state
 * (e.g. `mypet_owns_pet`'s ownership cache) that can lag a tick or two behind the
 * command that changed it.
 *
 * @param placeholder bare placeholder name without the surrounding `%`, e.g. `mypet_owns_pet`
 */
export async function expectPlaceholder(
  player: any, placeholder: string, expected: string,
  { attempts = 6, timeout = 2000 }: { attempts?: number; timeout?: number } = {},
): Promise<void> {
  // Right-guard so "no" can't match inside a longer token and "yes" is exact.
  const pattern = new RegExp(`e2e_ph=${expected}(?![\\w])`);
  let lastErr: unknown;
  for (let i = 0; i < attempts; i++) {
    const since = player.getMessageBufferIndex();
    player.chat(`/papi parse me e2e_ph=%${placeholder}%`);
    try {
      await expect(player).toHaveReceivedMessage(pattern, { since, timeout });
      return;
    } catch (e) { lastErr = e; }
  }
  throw lastErr;
}
