import { expect } from '@drownek/plugwright';
import * as fs from 'fs';
import * as path from 'path';
import { fileURLToPath } from 'url';

// ESM has no __dirname (see lib/locale.ts's identical note) — derive it from import.meta.url.
const __dirname = path.dirname(fileURLToPath(import.meta.url));

/**
 * The run directory's live pet-config.yml. Sibling of lib/config.ts's CONFIG_PATH
 * (config.yml); kept in its own module because the two files have different shapes:
 * config.yml is a flat global tree where a bare key name is unique, pet-config.yml
 * repeats every key once per pet type, so edits here must be block-scoped.
 */
export const PET_CONFIG_PATH = path.resolve(
  __dirname, '../../../../../build/plugwright-run/plugins/MyPet/pet-config.yml');

export function readPetConfig(): string {
  if (!fs.existsSync(PET_CONFIG_PATH)) {
    throw new Error(`pet-config.yml missing at ${PET_CONFIG_PATH} — has the server booted at least once?`);
  }
  return fs.readFileSync(PET_CONFIG_PATH, 'utf8');
}

/**
 * Parses the `MyPet.Pets.<Type>.<Key>: <value>` rows into `type -> (key -> raw value)`.
 *
 * Bukkit's YamlConfiguration writer indents two spaces per level, so under
 * `MyPet:` / `  Pets:` a type header sits at 4 spaces and its scalar keys at 6.
 * Rows deeper than 6 (a nested `Model:` block) and block-sequence entries
 * (`      - LowHp`, how a string list such as LeashRequirements is written) are
 * skipped on purpose — every flag this suite cares about is a 6-space scalar.
 */
export function parsePetConfig(text = readPetConfig()): Map<string, Map<string, string>> {
  const types = new Map<string, Map<string, string>>();
  let current: Map<string, string> | null = null;
  for (const line of text.split('\n')) {
    if (!line.trim() || line.trimStart().startsWith('#')) continue;
    const indent = line.length - line.trimStart().length;
    if (indent <= 3) { current = null; continue; }           // MyPet: / Pets:
    if (indent === 4) {                                       // a pet-type header
      const header = /^ {4}([A-Za-z0-9_]+):\s*$/.exec(line);
      current = header ? new Map<string, string>() : null;
      if (header) types.set(header[1], current!);
      continue;
    }
    if (!current || indent !== 6) continue;
    const kv = /^ {6}([A-Za-z0-9_]+):\s*(.*)$/.exec(line);
    if (kv) current.set(kv[1], kv[2].trim());
  }
  return types;
}

/**
 * Rewrites one `MyPet.Pets.<petType>.<key>` scalar in place and hot-reloads it with
 * `/mypet reload config` (MyPetReloader.reloadConfig re-runs setDefault +
 * loadConfiguration, which republishes both the per-pet ConfigKeys and the
 * PetInfo-backed flags such as ReleaseOnDeath / RemoveAfterRelease).
 *
 * Block-scoped, not a bare `s/key: .../`: every pet type carries its own copy of
 * most of these keys, so a whole-file regex would silently edit whichever type
 * happens to sort first. Throws when the row is absent rather than appending one —
 * a missing row means the plugin never materialized the default, which is a real
 * failure and should surface as one instead of being papered over here.
 *
 * The confirmation string is hardcoded English in CommandOptionReload (not a locale
 * key), same as lib/config.ts asserts it.
 */
export async function setPetFlag(
  player: any, petType: string, key: string, value: string | boolean,
): Promise<void> {
  const lines = readPetConfig().split('\n');
  let inBlock = false;
  let edited = false;
  for (let i = 0; i < lines.length && !edited; i++) {
    const line = lines[i];
    if (!line.trim()) continue;
    const indent = line.length - line.trimStart().length;
    if (indent <= 3) { inBlock = false; continue; }
    if (indent === 4) { inBlock = new RegExp(`^ {4}${petType}:\\s*$`).test(line); continue; }
    if (!inBlock || indent !== 6) continue;
    const kv = new RegExp(`^( {6}${key}:\\s*)\\S.*$`).exec(line);
    if (kv) { lines[i] = `${kv[1]}${value}`; edited = true; }
  }
  if (!edited) {
    throw new Error(`MyPet.Pets.${petType}.${key} not found in ${PET_CONFIG_PATH}`);
  }
  fs.writeFileSync(PET_CONFIG_PATH, lines.join('\n'), 'utf8');

  const since = player.getMessageBufferIndex();
  player.chat('/mypet reload config');
  await expect(player).toHaveReceivedMessage('config reloaded!', { since, timeout: 6000 });
}

/**
 * The pet types that implement the `PetEquipment` marker, per the locked design doc
 * (docs/superpowers/specs/2026-08-09-retain-equipment-on-tame-design.md) — the set
 * `RetainEquipmentOnTame` is registered for, and only that set.
 *
 * Membership is asserted against whatever pet-config.yml actually contains rather
 * than by count: PetType is auto-discovered from Bukkit's EntityType enum, so the
 * types added in newer Minecraft releases (CamelHusk, Nautilus, Parched,
 * ZombieNautilus at the time of writing) are simply absent on an older server and
 * must not be read as a missing flag.
 */
export const EQUIPMENT_CAPABLE_TYPES: readonly string[] = [
  'Allay', 'Bogged', 'Camel', 'CamelHusk', 'Donkey', 'Drowned', 'Evoker', 'Fox',
  'Giant', 'Horse', 'Husk', 'Illusioner', 'Mule', 'Nautilus', 'Parched', 'Piglin',
  'PiglinBrute', 'Pillager', 'Skeleton', 'SkeletonHorse', 'Stray', 'Villager', 'Vex',
  'Vindicator', 'WitherSkeleton', 'Zombie', 'ZombieHorse', 'ZombieNautilus',
  'ZombieVillager', 'ZombifiedPiglin',
];
