import { test, expect } from '@drownek/plugwright';
import { createHash } from 'node:crypto';
import { expectCondition, expectConditionHolds } from '../lib/oracle.js';
import { setupArena, spawnVictim, killTagged, equipItem, attackPinned, tameSwingExpecting } from '../lib/world.js';
import { removePet } from '../lib/pets.js';
import { msgFragment } from '../lib/locale.js';

const sleep = (ms: number) => new Promise(r => setTimeout(r, ms));

/**
 * Offline-mode UUID for a username (Bukkit's algorithm when online-mode=false):
 * `UUID.nameUUIDFromBytes("OfflinePlayer:"+name)`. Computed locally to avoid
 * racing `player.bot.player?.uuid` population.
 */
function offlineUuid(username: string): string {
  const md5 = createHash('md5').update(`OfflinePlayer:${username}`, 'utf8').digest();
  md5[6] = (md5[6] & 0x0f) | 0x30; // version 3
  md5[8] = (md5[8] & 0x3f) | 0x80; // variant RFC 4122
  const hex = md5.toString('hex');
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20, 32)}`;
}

/**
 * Vanilla SNBT for a UUID-typed NBT tag (`[I;a,b,c,d]`, Mojang's
 * `UUIDUtil.uuidToIntArray`). Required for `TamableAnimal`'s "Owner" tag — a
 * plain string there is silently ignored (wrong NBT tag type).
 */
function uuidToIntArraySnbt(uuid: string): string {
  const hex = uuid.replace(/-/g, '');
  const most = BigInt(`0x${hex.slice(0, 16)}`);
  const least = BigInt(`0x${hex.slice(16, 32)}`);
  const i32 = (v: bigint) => BigInt.asIntN(32, v).toString();
  return `[I;${i32(most >> 32n)},${i32(most)},${i32(least >> 32n)},${i32(least)}]`;
}

test('hitting a cow with a lead tames it into a pet', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const tag = 'wild_cow';
  try {
    await spawnVictim(server, player, 'cow', tag, { noAI: false, dx: 2 });

    // PetCow inherits the default LowHp leash flag: (health-incomingDamage)/
    // maxHealth <= 0.1 at the tame-hit. Drop Health via console first — safe,
    // since a successful leash cancels the damage event before it applies.
    server.execute(`data merge entity @e[tag=${tag},limit=1] {Health:0.5f}`);
    server.execute(`clear ${player.username} minecraft:lead`);
    server.execute(`give ${player.username} minecraft:lead 1`);
    // Fire-and-forget merge over stdin — confirm it landed before swinging
    // (a fixed sleep raced it under load, hitting a full-health cow instead).
    await expectCondition(server, player, `if entity @e[tag=${tag},limit=1,nbt={Health:0.5f}]`);
    await equipItem(player, 'lead');

    // Message.Leash.Add is the tame-success signal; /petcall confirms a live pet cow too.
    await tameSwingExpecting(server, player, tag, 'cow', 'lead', msgFragment('Message.Leash.Add'));
    player.chat('/petcall');
    await expectCondition(server, player,
      `at ${player.username} if entity @e[type=minecraft:cow,distance=..10]`);
  } finally {
    killTagged(server, tag);
    removePet(server, player);
  }
});

test('hitting a cow WITHOUT the lead item does not tame', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const tag = 'wild_cow2';
  try {
    await spawnVictim(server, player, 'cow', tag, { noAI: false, dx: 2 });

    // Ensure no leftover lead: inventory/held-slot persist across tests for
    // the same offline-UUID player.
    server.execute(`clear ${player.username} minecraft:lead`);
    await sleep(500);

    await attackPinned(server, player, tag, 'cow');
    await sleep(1500);
    // Kill the (still-wild) cow before asserting — otherwise "no cow nearby"
    // would hold trivially regardless of whether a tame occurred.
    killTagged(server, tag);

    // Primary proof: /petcall's "no pet" message (same as commands/call.spec.ts).
    player.chat('/petcall');
    await expect(player).toHaveReceivedMessage(msgFragment('Message.No.HasPet'));

    // Secondary guard: a real MyPet respawn would eventually reintroduce a
    // cow near the owner if a pet had incorrectly been created — confirm it doesn't.
    await expectConditionHolds(server, player,
      `at ${player.username} unless entity @e[type=minecraft:cow,distance=..16]`, { checks: 3 });
  } finally {
    killTagged(server, tag);
    removePet(server, player);
  }
});

test('a wolf requires vanilla taming first (Tamed leash flag)', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const tag = 'wolf1';
  try {
    await spawnVictim(server, player, 'wolf', tag, { noAI: false, dx: 2 });
    server.execute(`clear ${player.username} minecraft:lead`);
    server.execute(`give ${player.username} minecraft:lead 1`);
    server.execute(`give ${player.username} minecraft:bone 64`);
    await sleep(1000);

    // 1) Untamed wolf must not tame: PetWolf's only leash flag is Tamed
    // (TamedFlag.check requires isTamed() && getOwner() == player).
    await equipItem(player, 'lead');
    await attackPinned(server, player, tag, 'wolf');
    await sleep(1500);

    // { since } scopes the assertion to THIS /petcall reply, not stale buffer content.
    const sinceNoPet = player.getMessageBufferIndex();
    player.chat('/petcall');
    await expect(player).toHaveReceivedMessage(msgFragment('Message.No.HasPet'), { since: sinceNoPet });

    // 2) Vanilla-tame via console: merge the bot's offline UUID into Owner
    // (int-array SNBT). TamableAnimal#readAdditionalSaveData sets tame=true
    // automatically once a valid Owner UUID is present — no separate write needed.
    const ownerUuid = offlineUuid(player.username);
    const ownerSnbt = uuidToIntArraySnbt(ownerUuid);
    server.execute(`data merge entity @e[tag=${tag},limit=1] {Owner:${ownerSnbt}}`);
    // Same fire-and-forget hazard as the cow Health merge above.
    await expectCondition(server, player, `if entity @e[tag=${tag},limit=1,nbt={Owner:${ownerSnbt}}]`);

    // tameSwingExpecting's buffer index means only this second attempt can
    // satisfy the assertion, not a wrongly-emitted message from step 1.
    await equipItem(player, 'lead');
    await tameSwingExpecting(server, player, tag, 'wolf', 'lead', msgFragment('Message.Leash.Add'));
    player.chat('/petcall');
    await expectCondition(server, player,
      `at ${player.username} if entity @e[type=minecraft:wolf,distance=..10]`);
  } finally {
    killTagged(server, tag);
    removePet(server, player);
  }
});
