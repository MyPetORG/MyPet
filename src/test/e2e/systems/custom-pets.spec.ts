import { test, expect } from '@drownek/plugwright';
import * as fs from 'fs';
import * as path from 'path';
import { fileURLToPath } from 'url';
import { expectCondition } from '../lib/oracle.js';
import { expectPlaceholder } from '../lib/placeholder.js';
import { removePet } from '../lib/pets.js';
import { setupArena } from '../lib/world.js';

// ESM has no __dirname (see lib/locale.ts's identical note) — derive it from import.meta.url.
const __dirname = path.dirname(fileURLToPath(import.meta.url));
const PET_CONFIG_PATH = path.resolve(__dirname, '../../../../../build/plugwright-run/plugins/MyPet/pet-config.yml');

// Two Host-only custom creatures (no Model block: no renderer plugin is installed in the
// harness, and the type-identity mechanism under test is independent of rendering). Both
// types share the ModelPet class — which is exactly what this spec exercises.
const CUSTOM_TYPES_BLOCK =
  '    E2ECapy:\n' +
  '      Host: PIG\n' +
  '    E2ECham:\n' +
  '      Host: CAT\n';

/**
 * Inserts the two custom-type sections directly under the `  Pets:` line (appending at
 * EOF would create a duplicate top-level `MyPet:` key). Idempotent across retries.
 */
function installCustomTypes(): void {
  const text = fs.readFileSync(PET_CONFIG_PATH, 'utf8');
  if (text.includes('E2ECapy:')) return;
  const re = /^(  Pets:[ \t]*\r?\n)/m;
  if (!re.test(text)) {
    throw new Error(`"  Pets:" section not found in ${PET_CONFIG_PATH} — has the server booted at least once?`);
  }
  fs.writeFileSync(PET_CONFIG_PATH, text.replace(re, `$1${CUSTOM_TYPES_BLOCK}`), 'utf8');
}

/** Hot-reloads config — MyPetReloader re-runs CustomPetLoader.registerCustomTypes(). */
async function reloadConfig(player: any): Promise<void> {
  const since = player.getMessageBufferIndex();
  player.chat('/mypet reload config');
  await expect(player).toHaveReceivedMessage('config reloaded!', { since, timeout: 6000 });
}

/**
 * lib/pets.ts createPet derives its spawn-wait selector from the type name, which is wrong
 * for custom types (the spawned entity is the Host mob, not `minecraft:<typename>`), so the
 * create + wait + tag sequence is done here with an explicit host entity id.
 */
async function createCustomPet(
  server: any, player: any, typeName: string, hostEntity: string, name: string,
): Promise<string> {
  server.execute(`petadmin create -f ${player.username} mypet:${typeName.toLowerCase()} name:${name}`);
  await expectCondition(server, player,
    `at ${player.username} if entity @e[type=minecraft:${hostEntity},name=${name},distance=..16]`);
  const tag = `pet_${name}`;
  server.execute(
    `execute at ${player.username} run ` +
    `tag @e[type=minecraft:${hostEntity},name=${name},distance=..16,limit=1,sort=nearest] add ${tag}`);
  await expectCondition(server, player, `if entity @e[tag=${tag}]`);
  return tag;
}

// Regression: every custom creature shares the ModelPet class, and PetImpl used to resolve
// its PetType by scanning PetType.values() for the first type with a matching class — so
// every custom pet after the first came out as the first-registered custom type (wrong
// model, wrong skilltree, and the wrong type persisted back to the database on save).
// The type is now injected through the (MyPetPlayer, PetType) constructor.
test('two custom-creature pets keep their own type identity', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const originalPetConfig = fs.readFileSync(PET_CONFIG_PATH, 'utf8');
  installCustomTypes();
  const tags: string[] = [];
  try {
    await reloadConfig(player);

    tags.push(await createCustomPet(server, player, 'E2ECapy', 'pig', 'CapyPet'));
    await expectPlaceholder(player, 'mypet_type', 'E2ECapy');
    removePet(server, player);

    tags.push(await createCustomPet(server, player, 'E2ECham', 'cat', 'ChamPet'));
    // %mypet_type% reads the ACTIVE pet's getPetType() — pre-fix this reported E2ECapy.
    await expectPlaceholder(player, 'mypet_type', 'E2ECham');
  } finally {
    removePet(server, player);
    for (const tag of tags) server.execute(`kill @e[tag=${tag}]`);
    // Restore the file so no other spec sees the two test-only types. The in-memory
    // PetType registrations survive (types can't be unregistered), which is harmless.
    fs.writeFileSync(PET_CONFIG_PATH, originalPetConfig, 'utf8');
    player.chat('/mypet reload config');
  }
});
