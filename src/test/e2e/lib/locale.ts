import * as fs from 'fs';
import * as path from 'path';
import { fileURLToPath } from 'url';

// This package compiles to ESM ("type": "module" in package.json) — __dirname
// is not defined at runtime, so derive it from import.meta.url instead.
const __dirname = path.dirname(fileURLToPath(import.meta.url));

// dist/lib (compiled output, one level deeper than src/test/e2e/lib) → project root is five levels up.
const BUNDLE = path.resolve(__dirname, '../../../../../plugin/build/resources/main/locale/MyPet_en.properties');

let table: Map<string, string> | null = null;

function load(): Map<string, string> {
  if (table) return table;
  if (!fs.existsSync(BUNDLE)) {
    throw new Error(`English locale bundle missing at ${BUNDLE} — run ./gradlew downloadTranslations build first`);
  }
  table = new Map(
    fs.readFileSync(BUNDLE, 'utf8').split('\n')
      .filter(l => l.includes('=') && !l.startsWith('#'))
      .map(l => {
        const i = l.indexOf('=');
        return [l.slice(0, i).trim(), l.slice(i + 1).trim()] as [string, string];
      }),
  );
  return table;
}

const stripTags = (s: string) => s.replace(/<[^<>]+>/g, '');

/** Full resolved English value; MiniMessage tags stripped. Throws on {n} placeholders. */
export function msgPlain(key: string): string {
  const raw = load().get(key);
  if (raw === undefined) throw new Error(`No locale key ${key}`);
  const plain = stripTags(raw);
  if (/\{\d+\}/.test(plain)) throw new Error(`${key} has placeholders — use msgFragment`);
  return plain;
}

/** Longest static fragment (tags stripped, split at {n}) — for partial-match toHaveReceivedMessage. */
export function msgFragment(key: string): string {
  const raw = load().get(key);
  if (raw === undefined) throw new Error(`No locale key ${key}`);
  const fragments = stripTags(raw).split(/\{\d+\}/).map(f => f.trim()).filter(Boolean);
  if (!fragments.length) throw new Error(`${key} resolves to nothing static`);
  return fragments.reduce((a, b) => (b.length > a.length ? b : a));
}
