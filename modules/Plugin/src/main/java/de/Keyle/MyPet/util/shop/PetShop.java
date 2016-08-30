package de.Keyle.MyPet.util.shop;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.exceptions.MyPetTypeNotFoundException;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.repository.RepositoryCallback;
import de.Keyle.MyPet.api.util.WalletType;
import de.Keyle.MyPet.api.util.inventory.IconMenu;
import de.Keyle.MyPet.api.util.inventory.IconMenuItem;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.util.hooks.VaultHook;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;

public class PetShop {
    protected String name;
    protected String displayName = "Pet - Shop";
    protected Map<Integer, ShopMyPet> pets = new HashMap<>();
    protected WalletType wallet = WalletType.None;
    protected String walletOwner = null;
    protected boolean defaultShop = false;
    VaultHook economyHook;

    protected double privateWallet = 0;

    public PetShop(String name) {
        this.name = name;
        economyHook = (VaultHook) MyPetApi.getHookHelper().getEconomy();
    }

    public void open(final Player player) {
        IconMenu shop = new IconMenu(displayName, new IconMenu.OptionClickEventHandler() {
            @Override
            public void onOptionClick(IconMenu.OptionClickEvent event) {
                if (pets.containsKey(event.getPosition())) {
                    final ShopMyPet pet = pets.get(event.getPosition());
                    if (pet != null) {
                        final Player p = event.getPlayer();
                        MyPetPlayer owner = null;
                        if (MyPetApi.getPlayerManager().isMyPetPlayer(p)) {
                            owner = MyPetApi.getPlayerManager().getMyPetPlayer(player);

                            if (owner.hasMyPet()) {
                                p.sendMessage(Translation.getString("Message.Command.Trade.Receiver.HasPet", player));
                                return;
                            }
                        }

                        if (pet.getPrice() > 0) {
                            if (economyHook.canPay(p.getUniqueId(), pet.getPrice())) {
                                if (economyHook.getEconomy().withdrawPlayer(p, pet.getPrice()).transactionSuccess()) {
                                    switch (wallet) {
                                        case Bank:
                                            economyHook.getEconomy().bankDeposit(walletOwner, pet.getPrice());
                                            break;
                                        case Player:
                                            economyHook.getEconomy().depositPlayer(Bukkit.getOfflinePlayer(UUID.fromString(walletOwner)), pet.getPrice());
                                        case Private:
                                            depositPrivate(pet.getPrice());
                                            break;
                                    }
                                } else {
                                    p.sendMessage(Translation.getString("Message.No.Money", player));
                                    return;
                                }
                            } else {
                                p.sendMessage(Translation.getString("Message.Shop.NoMoney", player));
                                return;
                            }
                        }

                        if (owner == null) {
                            owner = MyPetApi.getPlayerManager().registerMyPetPlayer(player);
                        }

                        pet.setOwner(owner);

                        MyPetApi.getRepository().addMyPet(pet, new RepositoryCallback<Boolean>() {
                            @Override
                            public void callback(Boolean value) {
                                pet.getOwner().setMyPetForWorldGroup(WorldGroup.getGroupByWorld(player.getWorld().getName()), pet.getUUID());
                                MyPetApi.getRepository().updateMyPetPlayer(pet.getOwner(), null);
                                p.sendMessage(Util.formatText(Translation.getString("Message.Shop.Success", player), pet.getPetName()));
                                MyPet activePet = MyPetApi.getMyPetManager().activateMyPet(pet).get();
                                activePet.createEntity();
                            }
                        });

                    }
                }
            }
        }, MyPetApi.getPlugin());

        for (int pos : pets.keySet()) {
            ShopMyPet pet = pets.get(pos);
            IconMenuItem icon = pet.getIcon();
            ChatColor canPay = economyHook.canPay(player, pet.getPrice()) ? ChatColor.GREEN : ChatColor.RED;
            icon.addLoreLine(ChatColor.BLUE + Translation.getString("Name.price", player) + ": " + canPay + economyHook.getEconomy().format(pet.getPrice()), 0);
            shop.setOption(pos, icon);
        }

        shop.open(player);
    }

    public void depositPrivate(double amount) {
        privateWallet += amount;
    }

    public boolean isDefault() {
        return defaultShop;
    }

    public void load(ConfigurationSection section) {
        displayName = section.getString("Name", name);
        defaultShop = section.getBoolean("Default", false);
        wallet = WalletType.getByName(section.getString("Balance.Type", ""));
        if (wallet == null) {
            wallet = WalletType.None;
        }
        switch (wallet) {
            case Bank:
            case Player:
                walletOwner = section.getString("Balance.Owner", null);
        }

        ConfigurationSection pets = section.getConfigurationSection("Pets");

        Queue<ShopMyPet> filler = new ArrayDeque<>();
        for (String name : pets.getKeys(false)) {
            ShopMyPet pet = new ShopMyPet(name);
            try {
                pet.load(pets.getConfigurationSection(name));
                if (Util.isBetween(0, 53, pet.getPosition())) {
                    this.pets.put(pet.getPosition(), pet);
                } else {
                    filler.add(pet);
                }
            } catch (MyPetTypeNotFoundException ignored) {
            }
        }
        int slot = 0;
        while (!filler.isEmpty() && slot < 54) {
            if (this.pets.containsKey(slot)) {
                slot++;
                continue;
            }
            ShopMyPet pet = filler.poll();
            this.pets.put(slot, pet);
        }
    }
}