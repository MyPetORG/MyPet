// Rewrites plugwright's hardcoded bot-connect port (25565) to 25599 so the E2E
// server can coexist with a local Velocity proxy on the default port.
// The server side is set via `server-port=25599` staged by the Gradle
// writeFiles block (build.gradle.kts) — keep BOTH in sync.
// Runs from npm postinstall; idempotent; fails loudly if plugwright's runner
// no longer matches (e.g. after an upstream upgrade) so the redirect can't
// silently revert to 25565.
import { readFileSync, writeFileSync } from 'fs';
import { fileURLToPath } from 'url';
import { dirname, resolve } from 'path';

const PORT = 25599;
const runner = resolve(dirname(fileURLToPath(import.meta.url)), '../node_modules/@drownek/plugwright/dist/runner.js');

const src = readFileSync(runner, 'utf8');
if (src.includes(`port: ${PORT}`) && !src.includes('port: 25565')) {
  console.log(`[patch-port] already patched to ${PORT}`);
  process.exit(0);
}
const patched = src.replaceAll('port: 25565', `port: ${PORT}`);
const count = (src.match(/port: 25565/g) || []).length;
if (count !== 2) {
  console.error(`[patch-port] FAILED: expected exactly 2 \`port: 25565\` literals in runner.js, found ${count} — plugwright layout changed; re-verify before running tests.`);
  process.exit(1);
}
writeFileSync(runner, patched);
console.log(`[patch-port] rewrote ${count} literal(s) to ${PORT} in runner.js`);
