import { test, expect } from '@drownek/plugwright';
import { expectCondition } from '../lib/oracle.js';
import { msgFragment } from '../lib/locale.js';
import { createPet, removePet } from '../lib/pets.js';
import { setupArena } from '../lib/world.js';
import { secondBot } from '../lib/players.js';
import { tradePet } from '../lib/gui.js';

// GUI trade flow is owner-only and always price 0 (no price input in the GUI; priced
// trades are `/pettrade <player> <price>`, covered under economy). beginTrade() doesn't
// open a GUI on the target's side -- the accept affordance is a clickable chat component
// a Mineflayer bot can't click, so the target drives `/pettrade accept`/`reject` via chat.

test('full trade: owner offers via GUI, target accepts, ownership transfers', async ({ player, server, createPlayer }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Cow', { name: 'TradeMe' });
  const buyer = await secondBot(createPlayer, server, player, { username: 'Buyer' });

  try {
    const ownerOfferSince = player.getMessageBufferIndex();
    const buyerOfferSince = buyer.bot.getMessageBufferIndex();
    await tradePet(player, buyer.bot.username);

    await expect(player).toHaveReceivedMessage(
      msgFragment('Message.Command.Trade.Owner.Offer'), { since: ownerOfferSince }); // "You offered ... to ..."
    await expect(buyer.bot).toHaveReceivedMessage(
      msgFragment('Message.Command.Trade.Receiver.Offer'), { since: buyerOfferSince }); // "... offered you a pet ..."

    const ownerAcceptSince = player.getMessageBufferIndex();
    const buyerAcceptSince = buyer.bot.getMessageBufferIndex();
    buyer.bot.chat('/pettrade accept');

    await expect(buyer.bot).toHaveReceivedMessage(
      msgFragment('Message.Command.Trade.Receiver.Success'), { since: buyerAcceptSince, timeout: 6000 }); // "was successful ... owner of ... now"
    await expect(player).toHaveReceivedMessage(
      msgFragment('Message.Command.Trade.Owner.Success'), { since: ownerAcceptSince, timeout: 6000 }); // "accepted your offer for ..."

    // Prove ownership genuinely transferred: new owner's /petinfo shows the pet by name,
    // and /petcall actually teleports a live entity to them.
    const buyerInfoSince = buyer.bot.getMessageBufferIndex();
    buyer.bot.chat('/petinfo');
    await expect(buyer.bot).toHaveReceivedMessage('TradeMe', { since: buyerInfoSince });

    const buyerCallSince = buyer.bot.getMessageBufferIndex();
    buyer.bot.chat('/petcall');
    await expect(buyer.bot).toHaveReceivedMessage(
      msgFragment('Message.Command.Call.Success'), { since: buyerCallSince });
    await expectCondition(server, buyer.bot,
      `at ${buyer.bot.username} if entity @e[type=minecraft:cow,name=TradeMe,distance=..12]`);

    // Old owner: no pet at all anymore.
    const ownerInfoSince = player.getMessageBufferIndex();
    player.chat('/petinfo');
    await expect(player).toHaveReceivedMessage(
      msgFragment('Message.No.HasPet'), { since: ownerInfoSince });
  } finally {
    // Ownership transferred to buyer; owner-side call is a harmless no-op safety net
    // if the transfer assertions above failed before completing.
    removePet(server, buyer.bot);
    removePet(server, player);
    await buyer.dispose();
    server.execute('kill @e[type=minecraft:cow]');
  }
});

test('trade decline: target rejects, ownership stays with the original owner', async ({ player, server, createPlayer }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Cow', { name: 'KeepMe' });
  const rejecter = await secondBot(createPlayer, server, player, { username: 'Rejecter' });

  try {
    const buyerOfferSince = rejecter.bot.getMessageBufferIndex();
    await tradePet(player, rejecter.bot.username);
    await expect(rejecter.bot).toHaveReceivedMessage(
      msgFragment('Message.Command.Trade.Receiver.Offer'), { since: buyerOfferSince });

    const ownerRejectSince = player.getMessageBufferIndex();
    const rejecterRejectSince = rejecter.bot.getMessageBufferIndex();
    rejecter.bot.chat('/pettrade reject');

    await expect(rejecter.bot).toHaveReceivedMessage(
      msgFragment('Message.Command.Trade.Receiver.Reject'), { since: rejecterRejectSince }); // "You rejected ... offer."
    await expect(player).toHaveReceivedMessage(
      msgFragment('Message.Command.Trade.Owner.Reject'), { since: ownerRejectSince }); // "... rejected your offer for ..."

    // Never-vacuous: ownership genuinely unchanged, not just message text.
    const ownerInfoSince = player.getMessageBufferIndex();
    player.chat('/petinfo');
    await expect(player).toHaveReceivedMessage('KeepMe', { since: ownerInfoSince });

    const rejecterInfoSince = rejecter.bot.getMessageBufferIndex();
    rejecter.bot.chat('/petinfo');
    await expect(rejecter.bot).toHaveReceivedMessage(
      msgFragment('Message.No.HasPet'), { since: rejecterInfoSince });
  } finally {
    removePet(server, player);
    await rejecter.dispose();
    server.execute('kill @e[type=minecraft:cow]');
  }
});
