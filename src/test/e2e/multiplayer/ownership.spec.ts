import { test, expect } from '@drownek/plugwright';
import { expectCondition, expectConditionHolds } from '../lib/oracle.js';
import { msgFragment } from '../lib/locale.js';
import { createPet, removePet } from '../lib/pets.js';
import { ARENA, setupArena, equipItem, findEntity, pinNear } from '../lib/world.js';
import { secondBot } from '../lib/players.js';

const sleep = (ms: number) => new Promise(r => setTimeout(r, ms));

// findEntity, but polls: a just-joined/teleported bot can be server-confirmed
// "there" before its client has received the nearby entity's spawn packet.
// Duplicated from systems/equipment.spec.ts's local helper (that file is off-limits to edit).
async function findEntityPolled(player: any, entityTypeName: string, timeoutMs = 5000): Promise<any> {
  const deadline = Date.now() + timeoutMs;
  for (;;) {
    const found = findEntity(player, entityTypeName);
    if (found) return found;
    if (Date.now() > deadline) return null;
    await sleep(200);
  }
}

/**
 * Bounded retry with rejoin fallback for right-click interactions, to cover the
 * degraded-session dropped-use-entity-packet hazard. Only used for must-succeed
 * ("positive control") clicks -- a dropped packet there is a false negative worth
 * retrying; deny-side clicks stay single-attempt since a silent denial has no
 * landed-observable to retry against.
 *
 * Rejoining the pet's OWNER triggers MyPet's own despawn/respawn cycle, which
 * respawns the pet on a fresh untagged entity (see player-commands.spec.ts's
 * petrespawn test) -- `onRejoin` lets the caller re-apply `pet.tag` before retrying.
 */
// Re-applies `pet.tag` after a rejoin respawns the pet on a fresh, untagged entity.
async function reTagAfterRejoin(server: any, player: any, pet: any): Promise<void> {
  server.execute(
    `execute at ${player.username} run ` +
    `tag @e[type=minecraft:${pet.entityType},name=${pet.name},distance=..16,limit=1,sort=nearest] add ${pet.tag}`);
  await expectCondition(server, player, `if entity @e[tag=${pet.tag}]`);
}

async function retryInteraction<T>(
  attempts: number, player: any, action: () => Promise<T>,
  { onRejoin }: { onRejoin?: () => Promise<void> } = {},
): Promise<T> {
  let lastErr: unknown;
  for (let i = 0; i < attempts; i++) {
    if (i === Math.floor(attempts / 2)) {
      await player.rejoin();
      await player.deOp();
      await player.makeOp();
      if (onRejoin) await onRejoin();
    }
    try {
      return await action();
    } catch (err) {
      lastErr = err;
      if (i < attempts - 1) await sleep(500);
    }
  }
  throw lastErr;
}

// (a) Menu gate: PetInteractionListener's bare-hand-menu fallback requires isOwner(player,
//     pet) and excludes PetNaturallyRideable pets entirely, so this test uses a Cow.
//
// (b) Mount gate: RideGate.evaluate denies non-owner mounts unconditionally by default
//     (AllowNonOwnerPrimaryMount, admin-silent) before any ride-item/saddle/skill check.
//     A non-owner bare-hand click on a saddled horse reaches vanilla's mount path, which
//     PetMountGateListener's EntityMountEvent backstop then gates via the same RideGate
//     chain. The owner's positive control holds the RIDE_ITEM (default: lead) with the
//     Ride skill active (test-ride skilltree) to mount through the primary path instead.
//
// (c) /petcall: CommandCall's first check is hasActivePet(player) -- a non-owner with no
//     pet fails immediately, before any ownership logic touches the owner's pet.

test('non-owner right-click does not open the pet menu (owner can)', async ({ player, server, createPlayer }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Cow', { name: 'MenuGuard' });
  const intruder = await secondBot(createPlayer, server, player, { username: 'MenuIntruder' });

  try {
    // Cows wander on their own -- pin next to the clicker, since an out-of-reach click is
    // silently dropped, indistinguishable from a correctly-gated denial.
    await pinNear(server, pet.tag, intruder.bot.username);
    const cowForIntruder = await findEntityPolled(intruder.bot, 'cow');
    if (!cowForIntruder) throw new Error('intruder bot cannot see the cow pet');

    let opened = false;
    const guiPromise = intruder.bot.gui({ title: /./, timeout: 4000 }).then(() => { opened = true; }).catch(() => {});
    await intruder.bot.bot.activateEntity(cowForIntruder);
    await guiPromise;
    if (opened) throw new Error('non-owner was able to open the pet menu');

    // Positive control: owner's identical click DOES open the menu, proving the deny-side
    // assertion above isn't vacuous. Retry covers a dropped packet or the cow wandering off.
    const hub = await retryInteraction(4, player, async () => {
      await pinNear(server, pet.tag, player.username);
      const cowForOwner = await findEntityPolled(player, 'cow');
      if (!cowForOwner) throw new Error('owner bot cannot see the cow pet');
      await player.bot.activateEntity(cowForOwner);
      return await player.gui({ title: /Your Pet/, timeout: 3000 });
    }, { onRejoin: () => reTagAfterRejoin(server, player, pet) });
    if (!hub.title) throw new Error('owner right-click did not open the pet menu');
  } finally {
    removePet(server, player);
    await intruder.dispose();
    server.execute('kill @e[type=minecraft:cow]');
  }
});

// Vanilla excludes players from an entity's `Passengers` NBT, so an NBT-selector check is
// vacuously false for a player rider. `on passengers if entity @s[...]` walks the live
// passenger relationship instead. Fragment only (no leading "execute"), per suite convention.
function playerIsRidingCondition(tag: string, username: string): string {
  return `as @e[tag=${tag},limit=1] on passengers if entity @s[name=${username}]`;
}

test('non-owner cannot ride (owner can)', async ({ player, server, createPlayer }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Horse', { flags: ['tamed'], skilltree: 'test-ride', name: 'RideGuard' });
  const intruder = await secondBot(createPlayer, server, player, { username: 'RideIntruder' });

  try {
    // Owner saddles first -- unsaddled tamed horses don't mount on bare-hand click.
    server.execute(`give ${player.username} minecraft:saddle 1`);
    await equipItem(player, 'saddle');
    await pinNear(server, pet.tag, player.username);
    let horse = await findEntityPolled(player, 'horse');
    if (!horse) throw new Error('owner bot cannot see the horse pet');
    await player.bot.activateEntity(horse);
    await expectCondition(server, player,
      `if entity @e[tag=${pet.tag},limit=1,nbt={equipment:{saddle:{id:"minecraft:saddle"}}}]`);

    // Non-owner bare-hand click: RideInteractListener steps aside, vanilla's saddled-mount
    // path engages, gated by PetMountGateListener's EntityMountEvent backstop.
    await pinNear(server, pet.tag, intruder.bot.username);
    const horseForIntruder = await findEntityPolled(intruder.bot, 'horse');
    if (!horseForIntruder) throw new Error('intruder bot cannot see the horse pet');
    await intruder.bot.bot.activateEntity(horseForIntruder);
    await sleep(1000); // no message oracle for this silent admin-denial rejection

    if ((intruder.bot.bot as any).vehicle) throw new Error('non-owner mounted the pet (client-side)');
    // No expectCondition helper for "assert stays false" -- check live state via the same
    // tellraw-oracle mechanism inverted.
    let riding = false;
    const sinceRidingCheck = player.getMessageBufferIndex();
    server.execute(`execute ${playerIsRidingCondition(pet.tag, intruder.bot.username)} run tellraw ${player.username} {"text":"INTRUDER_RIDING_CHECK"}`);
    try {
      await expect(player).toHaveReceivedMessage('INTRUDER_RIDING_CHECK', { since: sinceRidingCheck, timeout: 1500 });
      riding = true;
    } catch { /* expected: condition is false, no tellraw fires */ }
    if (riding) throw new Error('non-owner mounted the pet (server-side, live passenger check)');

    // Positive control: owner, holding the RIDE_ITEM (lead), mounts the same pet.
    server.execute(`give ${player.username} minecraft:lead 1`);
    await equipItem(player, 'lead');
    await retryInteraction(4, player, async () => {
      await pinNear(server, pet.tag, player.username);
      const ownerHorse = await findEntityPolled(player, 'horse');
      if (!ownerHorse) throw new Error('owner bot cannot see the horse pet');
      await player.bot.activateEntity(ownerHorse);
      await expectCondition(server, player, playerIsRidingCondition(pet.tag, player.username), { timeout: 3000 });
    }, {
      // Saddle NBT survives rejoin-respawn, but tag and held item are safer to reassert.
      onRejoin: async () => {
        await reTagAfterRejoin(server, player, pet);
        await equipItem(player, 'lead');
      },
    });
  } finally {
    removePet(server, player);
    await intruder.dispose();
    server.execute('kill @e[type=minecraft:horse]');
  }
});

test("non-owner /petcall does not touch the owner's pet", async ({ player, server, createPlayer }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Cow', { name: 'CallGuard' });
  const intruder = await secondBot(createPlayer, server, player, { username: 'CallIntruder' });

  const farX = ARENA.x - 10;
  try {
    // Pin the pet at a fixed point away from the intruder (~4 blocks from ARENA center) --
    // both the ">6 blocks" and "didn't move" proofs fall out of the same pinned position.
    // NoAI freezes it there so a live Cow wandering doesn't flake the hold checks below.
    server.execute(`tp @e[tag=${pet.tag},limit=1] ${farX} ${ARENA.y} ${ARENA.z}`);
    server.execute(`data merge entity @e[tag=${pet.tag},limit=1] {NoAI:1b}`);
    await expectCondition(server, player, `if entity @e[tag=${pet.tag},limit=1,x=${farX},y=${ARENA.y},z=${ARENA.z},distance=..1]`);

    const since = intruder.bot.getMessageBufferIndex();
    intruder.bot.chat('/petcall');
    await expect(intruder.bot).toHaveReceivedMessage(msgFragment('Message.No.HasPet'), { since });

    // Held across several polls: the pet never approaches the intruder or leaves its spot.
    await expectConditionHolds(server, player,
      `if entity @e[tag=${pet.tag},limit=1,x=${farX},y=${ARENA.y},z=${ARENA.z},distance=..1]`,
      { checks: 4, interval: 700 });
    // `positioned` at the intruder's known fixed spot (secondBot tp'd them to ARENA.x + 4)
    // gives an authoritative distance check; a bare `distance=` filter measures from console.
    await expectConditionHolds(server, player,
      `positioned ${ARENA.x + 4} ${ARENA.y} ${ARENA.z} unless entity @e[tag=${pet.tag},limit=1,distance=..6]`,
      { checks: 4, interval: 700 });

    // Positive control: owner's own /petcall on the same pet genuinely works.
    const ownerSince = player.getMessageBufferIndex();
    player.chat('/petcall');
    await expect(player).toHaveReceivedMessage(msgFragment('Message.Command.Call.Success'), { since: ownerSince });
    await expectCondition(server, player, `at ${player.username} if entity @e[tag=${pet.tag},limit=1,distance=..8]`);
  } finally {
    removePet(server, player);
    await intruder.dispose();
  }
});
