import { expect } from '@drownek/plugwright';
import * as fs from 'fs';
import * as path from 'path';
import { fileURLToPath } from 'url';

// ESM has no __dirname (see lib/locale.ts's identical note) — derive it from import.meta.url.
const __dirname = path.dirname(fileURLToPath(import.meta.url));
const CONFIG_PATH = path.resolve(__dirname, '../../../../../build/plugwright-run/plugins/MyPet/config.yml');

/**
 * Flips `MyPet.DisablePetVersusPlayer` on disk and hot-reloads it via `/mypet reload config`
 * (genuinely hot, no restart — `loadGlobalsFromYaml` republishes the `ConfigKey`).
 * Uses a plain regex substitution rather than a YAML round-trip: `setDefault()` already
 * wrote this exact key at boot, so a full parse+rewrite would only risk reformatting the file.
 */
export async function setDisablePetVersusPlayer(server: any, player: any, value: boolean): Promise<void> {
  const text = fs.readFileSync(CONFIG_PATH, 'utf8');
  const re = /^(\s*DisablePetVersusPlayer:\s*)\S+/m;
  if (!re.test(text)) {
    throw new Error(`DisablePetVersusPlayer key not found in ${CONFIG_PATH} — has the server booted at least once?`);
  }
  fs.writeFileSync(CONFIG_PATH, text.replace(re, `$1${value}`), 'utf8');

  const since = player.getMessageBufferIndex();
  player.chat('/mypet reload config');
  await expect(player).toHaveReceivedMessage('config reloaded!', { since, timeout: 6000 });
}
