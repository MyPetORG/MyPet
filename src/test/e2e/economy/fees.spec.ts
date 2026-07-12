import { test, expect } from '@drownek/plugwright';
import { createPet, removePet } from '../lib/pets.js';

const sleep = (ms: number) => new Promise(r => setTimeout(r, ms));

// "SwitchFee" is an experience penalty removed from the pet, never a Vault currency charge
// -- no economy hook is involved. build.gradle.kts stages Fixed=25, Percent=0, so the
// deduction is exactly 25 regardless of seeded exp. Read-back uses the `petadmin exp
// <player> 0 add` no-op probe, which echoes the pet's exact raw exp, sidestepping the
// exp-curve/level-boundary math /petinfo's display would introduce.
//
// Gate: `!Permissions.has(owner, BYPASS_FEE) || SWITCH_FEE_ADMIN.get()`. isOp() always
// satisfies BYPASS_FEE, and the suite's default SWITCH_FEE_ADMIN=false simplifies this to
// `!isOp()` -- ops exempt, non-ops pay. Both sides are asserted below.

test('switching skilltree deducts the fixed exp fee for a non-admin player', async ({ player, server }) => {
  await player.deOp();
  await player.makeOp();
  const pet = await createPet(server, player, 'Cow', { skilltree: 'test-damage' });
  try {
    let since = player.getMessageBufferIndex();
    player.chat(`/petadmin exp ${player.username} 500 set`);
    await expect(player).toHaveReceivedMessage('set exp to 500.0', { since, timeout: 5000 });

    // Drop admin status: BYPASS_FEE is unreachable for a plain player, so the switch
    // below pays the real fee regardless of SWITCH_FEE_ADMIN.
    await player.deOp();

    since = player.getMessageBufferIndex();
    player.chat('/petchooseskilltree test-heal');
    await expect(player).toHaveReceivedMessage('test-heal', { since, timeout: 5000 });

    // Re-op to read exp back via the admin-gated probe; wait for a fresh op confirmation
    // and retry the no-op probe until it answers, since it can fire before the
    // op/command-tree resync and bounce with "Unknown command".
    const sinceOp = player.getMessageBufferIndex();
    await player.makeOp();
    await expect(player).toHaveReceivedMessage('server operator', { since: sinceOp, timeout: 5000 });
    let ok = false;
    for (let i = 0; i < 3 && !ok; i++) {
      await sleep(500);
      since = player.getMessageBufferIndex();
      player.chat(`/petadmin exp ${player.username} 0 add`);
      try {
        await expect(player).toHaveReceivedMessage('set exp to 475.0', { since, timeout: 3000 });
        ok = true;
      } catch { /* command tree not resynced yet — retry */ }
    }
    if (!ok) throw new Error('exp read-back after the fee never reported 475.0');
  } finally {
    removePet(server, player);
  }
});

test('an op bypasses the skilltree switch fee by default (SwitchFee.Admin=false)', async ({ player, server }) => {
  await player.deOp();
  await player.makeOp();
  const pet = await createPet(server, player, 'Cow', { skilltree: 'test-damage' });
  try {
    let since = player.getMessageBufferIndex();
    player.chat(`/petadmin exp ${player.username} 500 set`);
    await expect(player).toHaveReceivedMessage('set exp to 500.0', { since, timeout: 5000 });

    // Stays op'd through the switch this time — BYPASS_FEE is satisfied.
    since = player.getMessageBufferIndex();
    player.chat('/petchooseskilltree test-heal');
    await expect(player).toHaveReceivedMessage('test-heal', { since, timeout: 5000 });

    since = player.getMessageBufferIndex();
    player.chat(`/petadmin exp ${player.username} 0 add`);
    // Unchanged from the seeded 500 — no fee was deducted.
    await expect(player).toHaveReceivedMessage('set exp to 500.0', { since, timeout: 5000 });
  } finally {
    removePet(server, player);
  }
});
