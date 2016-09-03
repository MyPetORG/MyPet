package de.Keyle.MyPet.util.shop;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.exceptions.MyPetTypeNotFoundException;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.repository.RepositoryCallback;
import de.Keyle.MyPet.api.util.Colorizer;
import de.Keyle.MyPet.api.util.WalletType;
import de.Keyle.MyPet.api.util.inventory.IconMenu;
import de.Keyle.MyPet.api.util.inventory.IconMenuItem;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.util.hooks.VaultHook;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

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
        IconMenu shop = new IconMenu(Colorizer.setColors(displayName), new IconMenu.OptionClickEventHandler() {
            @Override
            public void onOptionClick(IconMenu.OptionClickEvent event) {
                if (pets.containsKey(event.getPosition())) {
                    final ShopMyPet pet = pets.get(event.getPosition());
                    if (pet != null) {
                        final Player p = event.getPlayer();
                        final MyPetPlayer owner;
                        if (MyPetApi.getPlayerManager().isMyPetPlayer(p)) {
                            owner = MyPetApi.getPlayerManager().getMyPetPlayer(player);

                            if (owner.hasMyPet() && !Permissions.has(owner, "MyPet.shop.storage")) {
                                p.sendMessage(Translation.getString("Message.Command.Trade.Receiver.HasPet", player));
                                return;
                            }
                        } else {
                            owner = null;
                        }
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                IconMenu menu = new IconMenu(Util.formatText(Translation.getString("Message.Shop.Confirm.Title", player), pet.getPetName(), economyHook.getEconomy().format(pet.getPrice())), new IconMenu.OptionClickEventHandler() {
                                    @Override
                                    public void onOptionClick(IconMenu.OptionClickEvent event) {
                                        if (event.getPosition() == 3) {
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

                                            final MyPetPlayer petOwner;
                                            if (owner == null) {
                                                petOwner = MyPetApi.getPlayerManager().registerMyPetPlayer(player);
                                            } else {
                                                petOwner = owner;
                                            }

                                            pet.setOwner(petOwner);
                                            final StoredMyPet clonedPet = MyPetApi.getMyPetManager().getInactiveMyPetFromMyPet(pet);

                                            clonedPet.setOwner(petOwner);
                                            clonedPet.setWorldGroup(WorldGroup.getGroupByWorld(player.getWorld().getName()).getName());
                                            clonedPet.setUUID(null);

                                            MyPetApi.getRepository().addMyPet(clonedPet, new RepositoryCallback<Boolean>() {
                                                @Override
                                                public void callback(Boolean value) {
                                                    p.sendMessage(Util.formatText(Translation.getString("Message.Shop.Success", player), clonedPet.getPetName(), economyHook.getEconomy().format(pet.getPrice())));
                                                    if (petOwner.hasMyPet()) {
                                                        p.sendMessage(Util.formatText(Translation.getString("Message.Shop.SuccessStorage", player), clonedPet.getPetName()));
                                                    } else {
                                                        petOwner.setMyPetForWorldGroup(WorldGroup.getGroupByWorld(player.getWorld().getName()), clonedPet.getUUID());
                                                        MyPetApi.getRepository().updateMyPetPlayer(petOwner, null);
                                                        MyPet activePet = MyPetApi.getMyPetManager().activateMyPet(clonedPet).get();
                                                        activePet.createEntity();
                                                    }
                                                }
                                            });
                                        }
                                        event.setWillClose(true);
                                        event.setWillDestroy(true);
                                    }
                                }, MyPetApi.getPlugin());
                                IconMenuItem icon = new IconMenuItem()
                                        .setMaterial(Material.WOOL)
                                        .setData(5)
                                        .setTitle(ChatColor.GREEN + Translation.getString("Name.Yes", player))
                                        .setLore(Util.formatText(Translation.getString("Message.Shop.Confirm.Yes", player), pet.getPetName(), economyHook.getEconomy().format(pet.getPrice())));
                                if (owner != null && owner.hasMyPet()) {
                                    icon.addLoreLine("").addLoreLine(Util.formatText(Translation.getString("Message.Shop.Confirm.SendStorage", player)));
                                }
                                menu.setOption(3, icon);
                                menu.setOption(5, new IconMenuItem()
                                        .setMaterial(Material.WOOL)
                                        .setData(14)
                                        .setTitle(ChatColor.RED + Translation.getString("Name.No", player))
                                        .setLore(Util.formatText(Translation.getString("Message.Shop.Confirm.No", player), pet.getPetName(), economyHook.getEconomy().format(pet.getPrice()))));
                                menu.open(player);
                            }
                        }.runTaskLater(MyPetApi.getPlugin(), 5L);
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