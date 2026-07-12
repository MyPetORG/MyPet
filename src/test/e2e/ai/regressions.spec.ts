import { test, sleep } from '@drownek/plugwright';
import { expectCondition, expectConditionHolds, expectScore } from '../lib/oracle.js';
import { msgPlain } from '../lib/locale.js';
import { createPet, removePet } from '../lib/pets.js';
import { setupArena, ARENA } from '../lib/world.js';
import { releasePet } from '../lib/gui.js';

// 4f4087f46 -- bats would not stay still when told to sit.
//
// Only /pet hub's Stay toggle calls PetImpl#setSitting (/petstop just calls
// forgetTarget()). Bat isn't PetSittable (no Sitting pose), so the fix
// (PetBat.SitFlightFreezer) instead polls isSitting() and toggles the
// vanilla Bat's own AI via the NoAI flag. Uses ≤10-block teleport hops, not
// a single 25-block jump: over 10 blocks, PlayerListener.onPet reunites
// owner and pet regardless of sitting, masking the bug.
test('regression: a bat told to stay stays put', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Bat');

  try {
    player.chat('/pet');
    const hub = await player.gui({ title: /Your Pet/ });
    await hub
      .locator((i: any) => String(i.getDisplayName() ?? '').includes(msgPlain('Gui.PetMenu.Stay.Title')))
      .click();

    // Confirm the SitFlightFreezer lifecycle hook actually engaged (grounds
    // the bat by disabling its vanilla AI) before relying on the position
    // hold below -- a bat that merely got lucky and wandered back near
    // start would otherwise be indistinguishable from a real fix.
    await expectCondition(server, player, `if entity @e[tag=${pet.tag},nbt={NoAI:1b}]`, { timeout: 3000 });

    for (const [dx, dz] of [[9, 0], [18, 0], [18, 9], [18, 18]]) {
      server.execute(`tp ${player.username} ${ARENA.x + dx} ${ARENA.y} ${ARENA.z + dz}`);
      await sleep(300);
    }

    await expectConditionHolds(server, player,
      `positioned ${ARENA.x} ${ARENA.y} ${ARENA.z} if entity @e[tag=${pet.tag},distance=..8]`,
      { checks: 6 });
  } finally {
    removePet(server, player);
  }
});

// 440a69a23 -- released pets kept the pet's boosted speed instead of the
// vanilla default.
//
// releaseToWild strips CustomName/PDC keys and respawns under a NEW UUID
// (NBT-snapshot restore), so name= or UUID-based selectors never match --
// both checks below select by the untouched scoreboard tag instead.
test('regression: a released cow reverts to vanilla movement speed', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Cow', { name: 'SpeedCheck' });

  try {
    await releasePet(player, server);
    await expectCondition(server, player, `if entity @e[tag=${pet.tag}]`);

    // vanilla cow base speed 0.2 -> scaled x1000 = 200 exactly
    await expectScore(server, player,
      `attribute @e[tag=${pet.tag},limit=1] minecraft:movement_speed base get 1000`,
      '200..200');
  } finally {
    server.execute(`kill @e[tag=${pet.tag}]`);
    removePet(server, player);
  }
});

// b1833e1c9 -- backpack contents duplicated on owner death with
// DropWhenOwnerDies: the old code dropped from a transient CustomInventory
// snapshot that never wrote back to the persisted `contents` array, so the
// item landed on the ground AND stayed in the backpack. Fix
// (BackpackImpl#dropContents) mutates `contents` directly.
//
// Notes: Pickup starts OFF, must be toggled before the diamond spawns.
// PlayerListener's death handler calls pet.removePet() (no auto-respawn), so
// the pet is Despawned post-death and /petinventory refuses to open
// (Message.Call.First) until an explicit /petcall. doImmediateRespawn is a
// no-op on this Paper build, so the bot is respawned explicitly.
test('regression: owner death drops backpack contents exactly once', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Cow', { name: 'DupeCheck', skilltree: 'test-pickup' });

  try {
    player.chat('/petpickup');
    server.execute(
      `summon minecraft:item ${ARENA.x + 2} ${ARENA.y + 1} ${ARENA.z} {Item:{id:"minecraft:diamond",count:1}}`);
    await expectCondition(server, player,
      `positioned ${ARENA.x} ${ARENA.y} ${ARENA.z} unless entity @e[type=minecraft:item,distance=..12]`,
      { timeout: 30000 }); // diamond is now in the backpack

    server.execute(`kill ${player.username}`);
    (player.bot as any).respawn?.();
    await expectCondition(server, player,
      `if entity @a[name=${player.username},nbt={Health:20.0f}]`, { timeout: 15000 });

    // Sanity check the drop happened at all; the real dupe proof is the backpack recheck below.
    await expectCondition(server, player, `if entity @e[type=minecraft:item,nbt={Item:{id:"minecraft:diamond"}}]`);

    // Kill the dropped diamond before recalling the pet: Pickup stays armed
    // across death/respawn, so a ground diamond would get vacuumed back into
    // the backpack and look exactly like a dupe without being one.
    server.execute('kill @e[type=minecraft:item,nbt={Item:{id:"minecraft:diamond"}}]');

    server.execute(`tp ${player.username} ${ARENA.x} ${ARENA.y} ${ARENA.z}`);
    player.chat('/petcall'); // pet is Despawned post-death; /petinventory refuses to open otherwise
    await expectCondition(server, player,
      `at ${player.username} if entity @e[tag=${pet.tag},distance=..8]`);

    player.chat('/petinventory');
    const gui = await player.gui({ title: /DupeCheck's Backpack/ });
    let sawDiamond = false;
    try {
      await gui.locator((i: any) => String(i.getDisplayName() ?? '').toLowerCase().includes('diamond')).click({ timeout: 3000 });
      sawDiamond = true;
    } catch { /* expected: no diamond in backpack */ }
    if (sawDiamond) throw new Error('DUPE: diamond dropped on death AND still in backpack');
  } finally {
    server.execute('kill @e[type=minecraft:item,nbt={Item:{id:"minecraft:diamond"}}]');
    server.execute(`kill @e[tag=${pet.tag}]`);
    removePet(server, player);
  }
});
