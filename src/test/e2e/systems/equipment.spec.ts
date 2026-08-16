import { test, expect } from '@drownek/plugwright';
import { expectCondition, expectConditionHolds } from '../lib/oracle.js';
import { createPet, removePet } from '../lib/pets.js';
import { ARENA, setupArena, equipItem, findEntity, sneakActivateEntity } from '../lib/world.js';

const sleep = (ms: number) => new Promise(r => setTimeout(r, ms));

/**
 * findEntity, but polls instead of taking one snapshot: a bot that just joined or
 * teleported can be server-confirmed "there" before its client has received the
 * nearby entity's spawn packet.
 */
async function findEntityPolled(player: any, entityTypeName: string, timeoutMs = 5000): Promise<any> {
  const deadline = Date.now() + timeoutMs;
  for (;;) {
    const found = findEntity(player, entityTypeName);
    if (found) return found;
    if (Date.now() > deadline) return null;
    await sleep(200);
  }
}

// (a) Two separate equip paths, split by whether the owner is crouching:
//     - NOT sneaking: PetImpl#onInteract only branches on empty-hand, GrowUpItem, and Food,
//       so a held armor piece falls through unconsumed and PetInteractionListener leaves the
//       event uncancelled — the horse tests below ride on pure vanilla mob-equip behavior.
//     - Sneaking: PetImpl#onInteract's PetEquipment branch handles it (shears strip all
//       slots, anything else equips into its Material slot). This is the only path for
//       species vanilla will not equip by right-click at all, e.g. the zombie tests below.
//
// (b) PetHorse is the only species giving both an armor and a saddle test (BODY + SADDLE
//     slots) while also being PetBaby + PetTameable, so it covers tests 1, 2, 3, and 5. On
//     this Paper build horse equipment lives under the unified component NBT shape, not the
//     legacy ArmorItems keys:
//       equipment:{body:{id:"minecraft:iron_horse_armor"}}   (armor)
//       equipment:{saddle:{id:"minecraft:saddle"}}            (saddle)
//     A horse must carry the "tamed" creation flag or vanilla rejects any right-click equip
//     (untamed horses buck). Shearing off body armor drops an item entity that keeps the
//     legacy capitalized "Item" NBT shape, unlike the mob's own equipment slot.
//
// (c) PetSaddleGateListener only gates NON-owner saddle application (`isOwner()` early
//     return) — deny default MyPet.Pets.Horse.AllowNonOwnerSaddle = false. Testing the deny
//     path needs a second, non-owning bot (`createPlayer`) since the owner path is excluded.
//
// (d) PetBucketGateListener cancels PlayerBucketEntityEvent for every marked pet with no
//     owner check, unlike the saddle gate — a single owner bot with a water bucket suffices.
//
// (e) PetAgeLockListener is a red herring here: it wires a Paper 26.1+-only event that
//     doesn't exist on this harness's API and no-ops via ClassNotFoundException. The real
//     freeze is VanillaMobSpawner#configureMob calling ageable.setAgeLock(true) for every
//     PetBaby+Ageable pet, gated by PetBaby#preventNaturalGrowup() (default true) — vanilla's
//     own "don't increment Age" flag, no MyPet ticking involved.
test('armor equip: right-click with horse armor sets the equipment.body NBT', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Horse', { flags: ['tamed'], name: 'EqArmor' });
  try {
    server.execute(`give ${player.username} minecraft:iron_horse_armor 1`);
    await equipItem(player, 'iron_horse_armor');
    const horse = await findEntityPolled(player, 'horse');
    if (!horse) throw new Error('bot cannot see the horse pet');
    await player.bot.activateEntity(horse);

    await expectCondition(server, player,
      `if entity @e[tag=${pet.tag},limit=1,nbt={equipment:{body:{id:"minecraft:iron_horse_armor"}}}]`);
  } finally {
    removePet(server, player);
    server.execute('kill @e[type=minecraft:horse]');
  }
});

test('shears removal: right-click with shears clears the armor and drops it', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Horse', { flags: ['tamed'], name: 'EqShear' });
  try {
    server.execute(`give ${player.username} minecraft:iron_horse_armor 1`);
    await equipItem(player, 'iron_horse_armor');
    let horse = await findEntityPolled(player, 'horse');
    if (!horse) throw new Error('bot cannot see the horse pet');
    await player.bot.activateEntity(horse);
    // Baseline: prove the armor is actually equipped before shearing it off.
    await expectCondition(server, player,
      `if entity @e[tag=${pet.tag},limit=1,nbt={equipment:{body:{id:"minecraft:iron_horse_armor"}}}]`);

    server.execute(`give ${player.username} minecraft:shears 1`);
    await equipItem(player, 'shears');
    horse = await findEntityPolled(player, 'horse');
    if (!horse) throw new Error('bot cannot see the horse pet');
    await player.bot.activateEntity(horse);

    await expectCondition(server, player,
      `unless entity @e[tag=${pet.tag},limit=1,nbt={equipment:{body:{id:"minecraft:iron_horse_armor"}}}]`);
    // The owner stands in melee reach, so the dropped stack can be auto-picked-up (10-tick
    // vanilla pickup delay) before the ground poll lands. Either resting place (ground item
    // or the bot's own inventory) proves "cleared AND dropped", so accept both.
    try {
      await expectCondition(server, player,
        `at @e[tag=${pet.tag},limit=1] if entity @e[type=minecraft:item,distance=..10,nbt={Item:{id:"minecraft:iron_horse_armor"}}]`,
        { timeout: 4000 });
    } catch {
      await expect(player).toContainItem('iron_horse_armor', { timeout: 4000 });
    }
  } finally {
    removePet(server, player);
    server.execute('kill @e[type=minecraft:horse]');
    server.execute('kill @e[type=minecraft:item]');
  }
});

// Zombie, not horse, for the sneak path: vanilla never equips a zombie from a player
// right-click, so a helmet landing in the head slot can only have come from MyPet's own
// handler. PreventDaylightBurn defaults to true (PetZombie), so the lit open-air arena
// won't cook the pet mid-test.
test('sneak equip: crouch + right-click with an iron helmet arms a zombie pet\'s head slot', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Zombie', { name: 'EqHelm' });
  try {
    server.execute(`give ${player.username} minecraft:iron_helmet 1`);
    await equipItem(player, 'iron_helmet');
    const zombie = await findEntityPolled(player, 'zombie');
    if (!zombie) throw new Error('bot cannot see the zombie pet');
    await sneakActivateEntity(player, zombie);

    await expectCondition(server, player,
      `if entity @e[tag=${pet.tag},limit=1,nbt={equipment:{head:{id:"minecraft:iron_helmet"}}}]`);
  } finally {
    removePet(server, player);
    server.execute('kill @e[type=minecraft:zombie]');
  }
});

test('sneak equip: the helmet leaves the owner\'s hand', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Zombie', { name: 'EqCost' });
  try {
    server.execute(`give ${player.username} minecraft:iron_helmet 1`);
    await equipItem(player, 'iron_helmet');
    const zombie = await findEntityPolled(player, 'zombie');
    if (!zombie) throw new Error('bot cannot see the zombie pet');
    await sneakActivateEntity(player, zombie);

    await expectCondition(server, player,
      `if entity @e[tag=${pet.tag},limit=1,nbt={equipment:{head:{id:"minecraft:iron_helmet"}}}]`);
    // Survival owner pays for the equip. Asserted after the equip landed so a helmet that
    // simply never left the hand can't read as a pass.
    await expect(player).not.toContainItem('iron_helmet', { timeout: 4000 });
  } finally {
    removePet(server, player);
    server.execute('kill @e[type=minecraft:zombie]');
  }
});

test('sneak unequip: crouch + right-click with shears strips the zombie pet and drops the gear', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Zombie', { name: 'EqStrip' });
  try {
    server.execute(`give ${player.username} minecraft:iron_helmet 1`);
    await equipItem(player, 'iron_helmet');
    let zombie = await findEntityPolled(player, 'zombie');
    if (!zombie) throw new Error('bot cannot see the zombie pet');
    await sneakActivateEntity(player, zombie);
    // Baseline: prove the helmet is on before shearing it off.
    await expectCondition(server, player,
      `if entity @e[tag=${pet.tag},limit=1,nbt={equipment:{head:{id:"minecraft:iron_helmet"}}}]`);

    server.execute(`give ${player.username} minecraft:shears 1`);
    await equipItem(player, 'shears');
    zombie = await findEntityPolled(player, 'zombie');
    if (!zombie) throw new Error('bot cannot see the zombie pet');
    await sneakActivateEntity(player, zombie);

    await expectCondition(server, player,
      `unless entity @e[tag=${pet.tag},limit=1,nbt={equipment:{head:{id:"minecraft:iron_helmet"}}}]`);
    // Same auto-pickup race as the horse shear test above: the owner is in melee reach, so
    // the dropped stack may reach the bot's inventory before the ground poll lands.
    try {
      await expectCondition(server, player,
        `at @e[tag=${pet.tag},limit=1] if entity @e[type=minecraft:item,distance=..10,nbt={Item:{id:"minecraft:iron_helmet"}}]`,
        { timeout: 4000 });
    } catch {
      await expect(player).toContainItem('iron_helmet', { timeout: 4000 });
    }
  } finally {
    removePet(server, player);
    server.execute('kill @e[type=minecraft:zombie]');
    server.execute('kill @e[type=minecraft:item]');
  }
});

// The gear a pet is wearing lives in two places: the mob's own inventory (persisted in the
// NBT snapshot) and PetImpl's in-memory `equipment` map. A despawn/respawn restores the mob
// from the snapshot but leaves the map empty -- VanillaMobSpawner only pushes the map ONTO a
// fresh mob, never reads a restored one back IN. Since the strip loop reads the map, shears
// used to clear only what had been equipped since the pet last spawned.
//
// It takes a RELOG, not just a despawn: /petsendaway + /petcall destroys and re-creates the
// entity while the PetImpl object (and its map) stays in memory, so that cycle does not
// reproduce this -- an earlier version of this test used it and passed against the bug.
// Rejoining rebuilds the Pet from the repository, which is when the map starts empty.
//
// The entity is re-created too, so the scoreboard tag has to be reapplied afterwards.
test('sneak unequip: shears strip gear the pet was wearing before the owner relogged', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Zombie', { name: 'EqPersist' });
  try {
    server.execute(`give ${player.username} minecraft:iron_helmet 1`);
    await equipItem(player, 'iron_helmet');
    let zombie = await findEntityPolled(player, 'zombie');
    if (!zombie) throw new Error('bot cannot see the zombie pet');
    await sneakActivateEntity(player, zombie);
    await expectCondition(server, player,
      `if entity @e[tag=${pet.tag},limit=1,nbt={equipment:{head:{id:"minecraft:iron_helmet"}}}]`);

    // Relog: the helmet comes back on the mob from the saved NBT, but the rebuilt Pet
    // object starts with an empty equipment map.
    await player.rejoin();
    await player.makeOp();
    server.execute(`tp ${player.username} ${ARENA.x} ${ARENA.y} ${ARENA.z}`);
    player.chat('/petcall');
    await expectCondition(server, player,
      `at ${player.username} if entity @e[type=minecraft:zombie,name=${pet.name},distance=..16]`,
      { timeout: 6000 });
    server.execute(
      `execute at ${player.username} run ` +
      `tag @e[type=minecraft:zombie,name=${pet.name},distance=..16,limit=1,sort=nearest] add ${pet.tag}`);
    await expectCondition(server, player, `if entity @e[tag=${pet.tag}]`);
    // The helmet survived the cycle -- otherwise this test would pass for the wrong reason.
    await expectCondition(server, player,
      `if entity @e[tag=${pet.tag},limit=1,nbt={equipment:{head:{id:"minecraft:iron_helmet"}}}]`);

    server.execute(`give ${player.username} minecraft:shears 1`);
    await equipItem(player, 'shears');
    zombie = await findEntityPolled(player, 'zombie');
    if (!zombie) throw new Error('bot cannot see the recalled zombie pet');
    await sneakActivateEntity(player, zombie);

    await expectCondition(server, player,
      `unless entity @e[tag=${pet.tag},limit=1,nbt={equipment:{head:{id:"minecraft:iron_helmet"}}}]`);
  } finally {
    removePet(server, player);
    server.execute('kill @e[type=minecraft:zombie]');
    server.execute('kill @e[type=minecraft:item]');
  }
});

test('saddle-gate: a non-owner cannot saddle the pet, the owner can', async ({ player, server, createPlayer }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Horse', { flags: ['tamed'], name: 'EqSaddle' });
  const intruder = await createPlayer({ username: 'Trespasser' });
  try {
    // The intruder's join position is randomized around world spawn; move it to the arena.
    server.execute(`tp ${intruder.username} ${ARENA.x} ${ARENA.y} ${ARENA.z}`);
    await expectCondition(server, player,
      `positioned ${ARENA.x} ${ARENA.y} ${ARENA.z} if entity @a[name=${intruder.username},distance=..4]`);

    server.execute(`give ${intruder.username} minecraft:saddle 1`);
    await equipItem(intruder, 'saddle');
    const horseForIntruder = await findEntityPolled(intruder, 'horse');
    if (!horseForIntruder) throw new Error('intruder bot cannot see the horse pet');
    await intruder.bot.activateEntity(horseForIntruder);
    await sleep(1000); // no message oracle for "nothing happened" — settle then read state directly

    const held = intruder.bot.inventory.items()
      .filter((i: any) => i.name === 'saddle')
      .reduce((n: number, i: any) => n + i.count, 0);
    if (held !== 1) throw new Error(`non-owner's saddle was consumed (held ${held}/1)`);
    await expectCondition(server, player,
      `unless entity @e[tag=${pet.tag},limit=1,nbt={equipment:{saddle:{id:"minecraft:saddle"}}}]`);

    // Positive control: owner's own saddle application is excluded from the gate, proving
    // saddling this pet works at all (deny-side assertion above isn't vacuously true).
    server.execute(`give ${player.username} minecraft:saddle 1`);
    await equipItem(player, 'saddle');
    const horseForOwner = await findEntityPolled(player, 'horse');
    if (!horseForOwner) throw new Error('owner bot cannot see the horse pet');
    await player.bot.activateEntity(horseForOwner);
    await expectCondition(server, player,
      `if entity @e[tag=${pet.tag},limit=1,nbt={equipment:{saddle:{id:"minecraft:saddle"}}}]`);
  } finally {
    removePet(server, player);
    server.execute('kill @e[type=minecraft:horse]');
  }
});

test('bucket-gate: an axolotl pet cannot be scooped into a bucket', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Axolotl', { name: 'EqBucket' });
  try {
    server.execute(`give ${player.username} minecraft:water_bucket 1`);
    await equipItem(player, 'water_bucket');
    const axolotl = await findEntityPolled(player, 'axolotl');
    if (!axolotl) throw new Error('bot cannot see the axolotl pet');
    await player.bot.activateEntity(axolotl);
    await sleep(1000); // no message oracle for "nothing happened" — settle then read state directly

    // The pet entity survived the bucket-capture attempt.
    await expectCondition(server, player, `if entity @e[tag=${pet.tag},limit=1]`);

    // A real capture would consume the water bucket and replace it with an axolotl_bucket.
    const items = player.bot.inventory.items();
    const heldWater = items.filter((i: any) => i.name === 'water_bucket')
      .reduce((n: number, i: any) => n + i.count, 0);
    const heldAxolotlBucket = items.filter((i: any) => i.name === 'axolotl_bucket').length;
    if (heldWater !== 1 || heldAxolotlBucket !== 0) {
      throw new Error(`bucket gate failed: water_bucket=${heldWater}, axolotl_bucket=${heldAxolotlBucket}`);
    }
  } finally {
    removePet(server, player);
    server.execute('kill @e[type=minecraft:axolotl]');
  }
});

test('age-lock: a baby pet\'s Age NBT is frozen (PreventNaturalGrowup default)', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Horse', { baby: true, name: 'EqAge' });
  try {
    // Baseline: vanilla's Ageable#setBaby(true) sets Age to -24000.
    await expectCondition(server, player, `if entity @e[tag=${pet.tag},limit=1,nbt={Age:-24000}]`);

    // Without the freeze, vanilla ticks Age up 1/tick (20/s); over this ~4s poll an unlocked
    // baby would drift off -24000, so staying pinned proves setAgeLock(true) is in effect.
    await expectConditionHolds(server, player, `if entity @e[tag=${pet.tag},limit=1,nbt={Age:-24000}]`,
      { checks: 5, interval: 800 });
  } finally {
    removePet(server, player);
    server.execute('kill @e[type=minecraft:horse]');
  }
});
