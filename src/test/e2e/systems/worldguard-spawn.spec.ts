import { test, expect } from '@drownek/plugwright';
import { expectCondition } from '../lib/oracle.js';
import { createPet, removePet } from '../lib/pets.js';
import { ARENA, setupArena } from '../lib/world.js';
import { msgFragment } from '../lib/locale.js';
import { releasePet } from '../lib/gui.js';

// Centre of the `no-mobs` cuboid staged in build.gradle.kts (992..1056 on x/z).
// Kept far from ARENA on general principle, even though pets are exempt from
// region spawn rules (only releases are gated) - no other spec releases a
// pet at ARENA's coordinates, but there's no upside to tempting fate.
export const DENY = { x: 1024, y: 200, z: 1024 };
// Just outside the cuboid on x, same platform altitude.
const ALLOW = { x: 1100, y: 200, z: 1024 };

/** setupArena's platform trick, applied to an arbitrary centre. */
async function buildPlatform(server: any, player: any, at: { x: number; y: number; z: number }) {
  for (const cmd of [
    `forceload add ${at.x - 20} ${at.z - 20} ${at.x + 20} ${at.z + 20}`,
    `fill ${at.x - 20} ${at.y - 1} ${at.z - 20} ${at.x + 20} ${at.y - 1} ${at.z + 20} minecraft:sea_lantern`,
    `fill ${at.x - 20} ${at.y} ${at.z - 20} ${at.x + 20} ${at.y + 4} ${at.z + 20} minecraft:air`,
  ]) server.execute(cmd);
}

async function teleportTo(server: any, player: any, at: { x: number; y: number; z: number }) {
  server.execute(`tp ${player.username} ${at.x} ${at.y} ${at.z}`);
  await expectCondition(server, player,
    `positioned ${at.x} ${at.y} ${at.z} if entity @a[name=${player.username},distance=..4]`);
}

test('release inside a mob-spawning:deny region is refused and the pet survives', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  await buildPlatform(server, player, DENY);

  // Create the pet at ARENA (spawning is allowed there), then walk *only the pet
  // entity* into the region - never the player. This isolates the release check
  // (VanillaMobSpawner#releaseToWild reads oldMob.getLocation(), not the player's)
  // from an unrelated side effect: teleporting the player this far would also
  // trip PlayerListener#onPet's recall listener, which is exempt from region
  // rules the same as any other call/recall path (see the "calling a pet"
  // test below) and would just add noise here.
  const pet = await createPet(server, player, 'Cow', { name: 'DenyRel' });
  try {
    server.execute(`tp ${pet.selector} ${DENY.x} ${DENY.y} ${DENY.z}`);
    await expectCondition(server, player,
      `positioned ${DENY.x} ${DENY.y} ${DENY.z} if entity @e[tag=${pet.tag},distance=..8]`);

    player.chat(`/petrelease ${pet.name}`);
    await expect(player).toHaveReceivedMessage(msgFragment('Message.Command.Release.NotAllowed'));

    // The pet is still a pet: still tagged, still owned, still spawned.
    await expectCondition(server, player, `if entity @e[tag=${pet.tag}]`);
  } finally {
    removePet(server, player);
  }
});

test('release outside the region still works', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  await buildPlatform(server, player, ALLOW);

  const pet = await createPet(server, player, 'Cow', { name: 'AllowRel' });
  try {
    await teleportTo(server, player, ALLOW);
    server.execute(`tp ${pet.selector} ${ALLOW.x} ${ALLOW.y} ${ALLOW.z}`);
    await expectCondition(server, player,
      `positioned ${ALLOW.x} ${ALLOW.y} ${ALLOW.z} if entity @e[tag=${pet.tag},distance=..8]`);

    player.chat(`/petrelease ${pet.name}`);
    await expect(player).toHaveReceivedMessage(msgFragment('Message.Command.Release.Success'));

    // A wild cow is left behind at the release point.
    await expectCondition(server, player,
      `positioned ${ALLOW.x} ${ALLOW.y} ${ALLOW.z} if entity @e[type=minecraft:cow,distance=..8]`);
  } finally {
    removePet(server, player);
  }
});

test('calling a pet inside a mob-spawning:deny region still works', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  await buildPlatform(server, player, DENY);

  // Unlike the release tests above, the player (not just the pet) needs to be
  // inside the region here - /petcall spawns the pet at the player's own
  // location, so the region only bites if the player is standing in it.
  const pet = await createPet(server, player, 'Cow', { name: 'DenyCall' });
  try {
    player.chat('/petsendaway');
    await teleportTo(server, player, DENY);

    // A MyPet pet is not a regular mob: region mob-spawning rules never apply
    // to calling, recalling, or login/respawn-spawning a pet - only to a
    // released pet, which becomes a genuine wild mob subject to those rules.
    // So the call must succeed even though this region denies mob-spawning.
    player.chat('/petcall');
    await expect(player).toHaveReceivedMessage(msgFragment('Message.Command.Call.Success'));
    await expectCondition(server, player,
      `at ${player.username} if entity @e[tag=${pet.tag},distance=..12]`);

    // Teleport-recall goes through a different trigger (PlayerListener#onPet's
    // despawn-and-recreate on a >10-block owner teleport, not CommandCall) but
    // the same VanillaMobSpawner path, and must be exempt the same way. Move
    // the player further inside the region (still >10 blocks, still within
    // the no-mobs cuboid) to fire it.
    const RECALL = { x: DENY.x + 20, y: DENY.y, z: DENY.z };
    await teleportTo(server, player, RECALL);
    await expectCondition(server, player,
      `at ${player.username} if entity @e[tag=${pet.tag},distance=..12]`);
  } finally {
    removePet(server, player);
  }
});

test('a denied release does not drop the backpack', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  await buildPlatform(server, player, DENY);

  // test-pickup grants the same Backpack config as test-backpack (rows +3, drop:false)
  // plus the Pickup skill. There is no supported plugwright GUI operation to transfer an
  // arbitrary item from the player's inventory into a container slot (LiveGuiHandle only
  // locates-and-clicks existing items, e.g. menu buttons) - opening /petinventory and
  // closing it again puts nothing in the backpack, which would make the "nothing dropped"
  // assertion below vacuously true even with the bug present. Toggling Pickup on and
  // letting the pet scoop up a dropped item is the same proven mechanism
  // ai/regressions.spec.ts's backpack-dupe test uses, so the backpack genuinely holds an
  // item before the release attempt.
  const pet = await createPet(server, player, 'Cow', { name: 'DenyPack', skilltree: 'test-pickup' });
  try {
    player.chat('/petpickup');
    server.execute(
      `summon minecraft:item ${ARENA.x + 2} ${ARENA.y + 1} ${ARENA.z} {Item:{id:"minecraft:diamond",count:1}}`);
    await expectCondition(server, player,
      `positioned ${ARENA.x} ${ARENA.y} ${ARENA.z} unless entity @e[type=minecraft:item,distance=..12]`,
      { timeout: 30000 }); // diamond is now in the backpack

    // Only the pet entity moves into the region - see the comment on the first
    // test in this file for why the release check is isolated from the player's
    // own location.
    server.execute(`tp ${pet.selector} ${DENY.x} ${DENY.y} ${DENY.z}`);
    await expectCondition(server, player,
      `positioned ${DENY.x} ${DENY.y} ${DENY.z} if entity @e[tag=${pet.tag},distance=..8]`);

    player.chat(`/petrelease ${pet.name}`);
    await expect(player).toHaveReceivedMessage(msgFragment('Message.Command.Release.NotAllowed'));

    // Nothing spilled onto the ground - the diamond genuinely parked in the backpack above.
    await expectCondition(server, player,
      `positioned ${DENY.x} ${DENY.y} ${DENY.z} unless entity @e[type=minecraft:item,distance=..10]`);
  } finally {
    // Pickup stays armed across the whole test; a leftover ground diamond (e.g. if the
    // release actually did drop it) would otherwise pollute a later spec.
    server.execute('kill @e[type=minecraft:item,nbt={Item:{id:"minecraft:diamond"}}]');
    removePet(server, player);
  }
});

test('GUI release inside a deny region is refused the same way', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  await buildPlatform(server, player, DENY);

  const pet = await createPet(server, player, 'Cow', { name: 'DenyGui' });
  try {
    // Only the pet entity moves into the region - see the comment on the first
    // test in this file for why the release check is isolated from the player's
    // own location.
    server.execute(`tp ${pet.selector} ${DENY.x} ${DENY.y} ${DENY.z}`);
    await expectCondition(server, player,
      `positioned ${DENY.x} ${DENY.y} ${DENY.z} if entity @e[tag=${pet.tag},distance=..8]`);

    await releasePet(player, server);
    await expect(player).toHaveReceivedMessage(msgFragment('Message.Command.Release.NotAllowed'));
    await expectCondition(server, player, `if entity @e[tag=${pet.tag}]`);
  } finally {
    removePet(server, player);
  }
});
