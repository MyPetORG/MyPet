import { test, expect } from '@drownek/plugwright';
import { expectCondition } from '../lib/oracle.js';
import { removePet } from '../lib/pets.js';
import { msgPlain } from '../lib/locale.js';
import { fundBot, setBalance, expectBalanceReply } from '../lib/economy.js';

// With exactly one staged shop (pet-shops.yml "e2e", Default: true), executeDefault's
// single-shop branch opens it directly -- no PET_SHOP_SELECTION menu, just PET_SHOP
// (browse) then PET_SHOP_CONFIRM (buy). Shop access needs the per-shop
// "MyPet.shop.access.e2e" permission, granted only to an op by default.

test('funding a bot and buying the cheap shop pet spends the price and creates the pet', async ({ player, server }) => {
  await player.deOp();
  await player.makeOp();
  fundBot(server, player, 1000);
  try {
    // PlayerPoints applies `points give` asynchronously; the balance reply doubles as the
    // "give landed" sync so a buy click can't race ahead and bounce off Message.Shop.NoMoney.
    await expectBalanceReply(player, 1000);

    player.chat('/petshop');
    const shop = await player.gui({ title: new RegExp(msgPlain('Gui.PetShop.Title')), timeout: 8000 });
    await shop.locator((i: any) => String(i.getDisplayName() ?? '').includes('ShopCheap')).click();

    const confirm = await player.gui({ title: new RegExp(msgPlain('Gui.PetShopConfirm.Title')), timeout: 8000 });
    const since = player.getMessageBufferIndex();
    await confirm.locator((i: any) => String(i.getDisplayName() ?? '').includes(msgPlain('Gui.PetShopConfirm.Yes'))).click();

    await expect(player).toHaveReceivedMessage('ShopCheap', { since, timeout: 5000 });
    await expectCondition(server, player,
      `at ${player.username} if entity @e[type=minecraft:wolf,name=ShopCheap,distance=..64]`);
    await expectBalanceReply(player, 900);
  } finally {
    removePet(server, player);
  }
});

test('an unfunded bot cannot buy the expensive shop pet — denied, no pet created', async ({ player, server }) => {
  await player.deOp();
  await player.makeOp();
  setBalance(server, player, 0);
  try {
    player.chat('/petshop');
    const shop = await player.gui({ title: new RegExp(msgPlain('Gui.PetShop.Title')), timeout: 8000 });
    await shop.locator((i: any) => String(i.getDisplayName() ?? '').includes('ShopExpensive')).click();

    const confirm = await player.gui({ title: new RegExp(msgPlain('Gui.PetShopConfirm.Title')), timeout: 8000 });
    const since = player.getMessageBufferIndex();
    await confirm.locator((i: any) => String(i.getDisplayName() ?? '').includes(msgPlain('Gui.PetShopConfirm.Yes'))).click();

    // Sent by PetShop.buy() when economyHook.canPay(...) fails, before any pet is minted.
    await expect(player).toHaveReceivedMessage(msgPlain('Message.Shop.NoMoney'), { since, timeout: 5000 });
    await expectCondition(server, player,
      `at ${player.username} unless entity @e[type=minecraft:ocelot,name=ShopExpensive,distance=..64]`);
  } finally {
    removePet(server, player);
  }
});
