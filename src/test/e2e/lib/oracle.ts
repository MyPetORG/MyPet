import { expect } from '@drownek/plugwright';

const sleep = (ms: number) => new Promise(r => setTimeout(r, ms));
let seq = 0;

/** Polls a vanilla `/execute <condition>` until true, observing via tellraw to the bot. */
export async function expectCondition(
  server: any, player: any, condition: string,
  { pre = [] as string[], timeout = 15000, interval = 750 } = {},
): Promise<void> {
  const deadline = Date.now() + timeout;
  while (Date.now() < deadline) {
    for (const cmd of pre) server.execute(cmd);
    const marker = `E2E_OK_${++seq}`;
    server.execute(`execute ${condition} run tellraw ${player.username} {"text":"${marker}"}`);
    try {
      await expect(player).toHaveReceivedMessage(marker, { timeout: interval });
      return;
    } catch { await sleep(100); }
  }
  throw new Error(`Condition never became true: ${condition}`);
}

/** Asserts the condition is true on `checks` consecutive polls (e.g. "pet stays put"). */
export async function expectConditionHolds(
  server: any, player: any, condition: string,
  { checks = 5, interval = 800 } = {},
): Promise<void> {
  for (let i = 0; i < checks; i++) {
    await expectCondition(server, player, condition, { timeout: interval + 2000, interval });
    await sleep(interval);
  }
}

/** Stores a command result into a scoreboard and asserts it matches a vanilla range (e.g. "200..", "..9"). */
export async function expectScore(
  server: any, player: any, storeCommand: string, range: string,
  opts: { timeout?: number } = {},
): Promise<void> {
  server.execute('scoreboard objectives add e2e dummy');
  await expectCondition(server, player, `if score chk e2e matches ${range}`, {
    pre: [`execute store result score chk e2e run ${storeCommand}`],
    ...opts,
  });
}
