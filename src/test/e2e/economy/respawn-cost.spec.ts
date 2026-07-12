import { test, expect } from '@drownek/plugwright';
import { expectCondition } from '../lib/oracle.js';
import { createPet, removePet } from '../lib/pets.js';
import { msgFragment } from '../lib/locale.js';
import { fundBot, setBalance, expectBalanceReply } from '../lib/economy.js';

const sleep = (ms: number) => new Promise(r => setTimeout(r, ms));

// hasAutoRespawnEnabled() defaults false, so these tests exercise the "pay" (manual)
// branch only, not PetImpl.tickRespawnTimer's auto-respawn-via-economy branch.
//
// build.gradle.kts stages respawn timer Factor=0/Fixed=10 (flat 10s regardless of pet
// level or death attribution) and EconomyCost.Factor=0/Fixed=50, so the Vault charge is a
// flat, countdown-immune 50 -- a nonzero Factor would decay as respawnTime ticks down.

test('a killed pet shows its respawn cost, and a funded player can pay to respawn it', async ({ player, server }) => {
  await player.deOp();
  await player.makeOp();
  const pet = await createPet(server, player, 'Cow', { name: 'RespawnPayCow', skilltree: 'test-damage' });
  const aliveSelector = `@e[type=minecraft:${pet.entityType},name=${pet.name},distance=..64]`;
  try {
    server.execute(`kill ${pet.selector}`);
    await expectCondition(server, player, `at ${player.username} unless entity ${aliveSelector}`);

    let since = player.getMessageBufferIndex();
    player.chat('/petrespawn show');
    await expect(player).toHaveReceivedMessage(msgFragment('Message.Command.Respawn.Show'), { since, timeout: 5000 });
    await expect(player).toHaveReceivedMessage('50', { since, timeout: 5000 });

    fundBot(server, player, 1000);
    // PlayerPoints applies `points give` async (see shop.spec.ts); the balance reply
    // proves the funds landed before the pay attempt.
    await expectBalanceReply(player, 1000);
    since = player.getMessageBufferIndex();
    player.chat('/petrespawn pay');
    await expect(player).toHaveReceivedMessage(msgFragment('Message.Command.Respawn.Paid'), { since, timeout: 5000 });
    await expectBalanceReply(player, 1000 - 50);

    // Respawns on a fresh entity that doesn't inherit our tag -- matched by type+name.
    await expectCondition(server, player, `at ${player.username} if entity ${aliveSelector}`, { timeout: 8000 });
  } finally {
    removePet(server, player);
  }
});

test('an unfunded player cannot pay to respawn a dead pet', async ({ player, server }) => {
  await player.deOp();
  await player.makeOp();
  const pet = await createPet(server, player, 'Cow', { name: 'RespawnNoPayCow', skilltree: 'test-damage' });
  const aliveSelector = `@e[type=minecraft:${pet.entityType},name=${pet.name},distance=..64]`;
  try {
    setBalance(server, player, 0);
    server.execute(`kill ${pet.selector}`);
    await expectCondition(server, player, `at ${player.username} unless entity ${aliveSelector}`);

    const since = player.getMessageBufferIndex();
    player.chat('/petrespawn pay');
    await expect(player).toHaveReceivedMessage(msgFragment('Message.Command.Respawn.NoMoney'), { since, timeout: 5000 });
    await expectBalanceReply(player, 0);

    // Confirm still dead a moment later, under the staged 10s countdown so a free
    // auto-respawn can't race this and mask a denied payment as "not respawned yet".
    await sleep(1500);
    await expectCondition(server, player, `at ${player.username} unless entity ${aliveSelector}`);
  } finally {
    removePet(server, player);
  }
});
