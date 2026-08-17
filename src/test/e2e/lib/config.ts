import { expect } from '@drownek/plugwright';
import * as fs from 'fs';
import * as path from 'path';
import { fileURLToPath } from 'url';
import * as yaml from 'js-yaml';

// ESM has no __dirname (see lib/locale.ts's identical note) — derive it from import.meta.url.
const __dirname = path.dirname(fileURLToPath(import.meta.url));
const CONFIG_PATH = path.resolve(__dirname, '../../../../../build/plugwright-run/plugins/MyPet/config.yml');
const PET_CONFIG_PATH = path.resolve(__dirname, '../../../../../build/plugwright-run/plugins/MyPet/pet-config.yml');

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

/**
 * Rewrites `MyPet.Pets.<petType>.Brain.Disabled` and hot-reloads it.
 *
 * Unlike `setDisablePetVersusPlayer` above, this goes through a full
 * `yaml.load`/`yaml.dump` round-trip (and does reformat pet-config.yml and
 * drop its header). A regex substitution isn't a good fit here: the target
 * is a nested list under a per-pet-type section, not a single flat scalar,
 * and the entry count varies per call. Harmless in practice — the file is
 * config-managed and the header is restored on the next boot — so the
 * round-trip is the pragmatic choice for this shape rather than a
 * regression from the sibling helper's approach.
 *
 * The strip is applied by PetGoalInstaller at SPAWN, so callers must create or
 * respawn the pet after this returns for the change to take effect.
 */
export async function setPetBrainDisabled(
  server: any, player: any, petType: string, entries: string[],
): Promise<void> {
  const doc = yaml.load(fs.readFileSync(PET_CONFIG_PATH, 'utf8')) as any;
  const section = doc?.MyPet?.Pets?.[petType];
  if (!section) throw new Error(`No pet-config section for ${petType} — has the server booted?`);
  section.Brain = { ...(section.Brain ?? {}), Disabled: entries };
  fs.writeFileSync(PET_CONFIG_PATH, yaml.dump(doc, { lineWidth: -1 }), 'utf8');

  const since = player.getMessageBufferIndex();
  player.chat('/mypet reload config');
  await expect(player).toHaveReceivedMessage('config reloaded!', { since, timeout: 6000 });
}
