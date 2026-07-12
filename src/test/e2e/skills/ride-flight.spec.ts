import { test } from '@drownek/plugwright';
import { expectScore } from '../lib/oracle.js';
import { createPet, removePet } from '../lib/pets.js';
import { setupArena } from '../lib/world.js';

const sleep = (ms: number) => new Promise(r => setTimeout(r, ms));

// mineflayer disables its own physics loop while mounted, so setControlState('jump') only
// queues a local flag that's never sent as a packet. Write the raw player_input packet
// directly instead (same wire shape mineflayer's own dismount() uses) so the server's
// getCurrentInput().isJump() -- what RideSkillFlightController reads -- actually sees it.
// The arena has no ceiling, so a sustained hold climbs tens of blocks (~7.5 blocks/s).
function rideJump(player: any, jump: boolean): void {
  (player.bot as any)._client.write('player_input', {
    inputs: { forward: false, backward: false, left: false, right: false, jump, shift: false, sprint: false },
  });
}

// A full 6s FlyLimit hold climbs ~45 blocks; releasing free-falls that far, which would kill
// a 20 HP pet in fall damage. Invulnerable only gates damage, not physics, so it doesn't
// touch anything the Ride skill controls -- just keeps the pet alive to observe fuel regen.
function makeFallproof(server: any, pet: any): void {
  server.execute(`data merge entity ${pet.selector} {Invulnerable:1b}`);
}

test('test-ride-fly: holding jump lifts a flying-capable pet off the ground', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Horse', { skilltree: 'test-ride-fly' });

  try {
    makeFallproof(server, pet);
    player.chat('/petride');
    await sleep(1000);

    rideJump(player, true);
    await expectScore(server, player, `data get entity ${pet.selector} Pos[1]`, '203..', { timeout: 8000 });
  } finally {
    rideJump(player, false);
    removePet(server, player);
  }
});

// FlyLimit "+6" is seconds of continuous held-jump flight (fuelTicks = 120 at 20 ticks/s).
// Holding jump drains 1 fuel-tick/server-tick; releasing regenerates FlyRegenRate/20 per
// idle tick (0.1/tick here). Fuel lives on the flight controller for the pet's whole spawn
// lifetime, so a fresh pet starts at max (120 ticks) and a full 6s hold exhausts it exactly.
test('test-ride-fly: fuel exhausts after the FlyLimit window and regenerates enough to re-engage', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Horse', { skilltree: 'test-ride-fly' });

  try {
    makeFallproof(server, pet);
    player.chat('/petride');
    await sleep(1000);

    rideJump(player, true);
    // Hold through the full 6s / 120-tick fuel budget, with margin for jitter.
    await sleep(6800);
    rideJump(player, false);

    // Exhausted: gravity pulls it back down past the platform's floor line.
    await expectScore(server, player, `data get entity ${pet.selector} Pos[1]`, '..201', { timeout: 20000 });

    // Idle lets fuel regenerate enough for a small climb back past 203.
    await sleep(8000);

    rideJump(player, true);
    await expectScore(server, player, `data get entity ${pet.selector} Pos[1]`, '203..', { timeout: 8000 });
  } finally {
    rideJump(player, false);
    removePet(server, player);
  }
});
