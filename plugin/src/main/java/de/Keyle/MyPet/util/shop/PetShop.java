/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2026 Keyle
 * MyPet is licensed under the GNU Lesser General Public License.
 *
 * MyPet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyPet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.util.shop;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.PersistedPet;
import de.Keyle.MyPet.api.event.PetCreateEvent;
import de.Keyle.MyPet.api.exceptions.PetTypeNotFoundException;
import de.Keyle.MyPet.api.gui.MenuId;
import de.Keyle.MyPet.api.gui.MenuIds;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.skill.skilltree.SkilltreeIcon;
import de.Keyle.MyPet.util.WalletType;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.gui.context.PetShopContext;
import de.Keyle.MyPet.util.hooks.VaultHook;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;

import static de.Keyle.MyPet.api.util.configuration.Try.tryToLoad;

public class PetShop {

    protected String name;
    protected String displayName = "Pet - Shop";
    protected Map<Integer, ShopPet> pets = new HashMap<>();
    protected WalletType wallet = WalletType.None;
    @Getter
    @Setter
    protected int position = -1;
    @Getter
    @Setter
    protected SkilltreeIcon icon = new SkilltreeIcon().setMaterial("chest");
    protected String walletOwner = null;
    protected boolean defaultShop = false;
    protected double privateWallet = 0;

    public PetShop(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void open(final Player player) {
        if (!MyPetApi.getHookHelper().isEconomyEnabled()) {
            player.sendMessage(Locale.getComponent("Message.No.Economy", player));
            return;
        }
        MyPetApi.getGuiService().openMenu(
                player,
                (MenuId<PetShopContext>) (MenuId<?>) MenuIds.PET_SHOP,
                new PetShopContext(player, getPets(), this::buy));
    }

    public void buy(final Player player, final ShopPet shopPet) {
        if (!MyPetApi.getHookHelper().isEconomyEnabled()) {
            player.sendMessage(Locale.getComponent("Message.No.Economy", player));
            return;
        }
        VaultHook economyHook = (VaultHook) MyPetApi.getHookHelper().getEconomy();

        final MyPetPlayer owner;
        if (MyPetApi.getPlayerManager().isMyPetPlayer(player)) {
            owner = MyPetApi.getPlayerManager().getMyPetPlayer(player);
            if (owner == null) {
                return;
            }
            if (owner.hasPet() && !Permissions.has(owner, "MyPet.shop.storage")) {
                player.sendMessage(Locale.getComponent("Message.Command.Trade.Receiver.HasPet", player));
                return;
            }
        } else {
            owner = null;
        }

        if (shopPet.getPrice() > 0) {
            if (economyHook.canPay(player.getUniqueId(), shopPet.getPrice())) {
                if (economyHook.getEconomy().withdrawPlayer(player, shopPet.getPrice()).transactionSuccess()) {
                    switch (wallet) {
                        case Bank:
                            economyHook.getEconomy().bankDeposit(walletOwner, shopPet.getPrice());
                            break;
                        case Player:
                            economyHook.getEconomy().depositPlayer(Bukkit.getOfflinePlayer(UUID.fromString(walletOwner)), shopPet.getPrice());
                            break;
                        case Private:
                            depositPrivate(shopPet.getPrice());
                            break;
                    }
                } else {
                    player.sendMessage(Locale.getComponent("Message.No.Money", player));
                    return;
                }
            } else {
                player.sendMessage(Locale.getComponent("Message.Shop.NoMoney", player));
                return;
            }
        }

        final MyPetPlayer petOwner = owner == null ? MyPetApi.getPlayerManager().registerMyPetPlayer(player) : owner;
        final PersistedPet clonedPet = shopPet.toPersisted(petOwner)
                .withWorldGroup(WorldGroup.getGroupByWorld(player.getWorld().getName()).getName())
                .withUuid(UUID.randomUUID());

        MyPetPlugin.getInstance().getRepository().addPet(clonedPet).thenAccept(value -> player.getScheduler().run(MyPetApi.getPlugin(), addTask -> {
                player.sendMessage(Locale.getFormattedComponent("Message.Shop.Success", player, clonedPet.getDisplayName(), economyHook.getEconomy().format(shopPet.getPrice())));
                PetCreateEvent createEvent = new PetCreateEvent(clonedPet, PetCreateEvent.Source.PET_SHOP);
                Bukkit.getServer().getPluginManager().callEvent(createEvent);
                if (petOwner.hasPet()) {
                    player.sendMessage(Locale.getFormattedComponent("Message.Shop.SuccessStorage", player, clonedPet.getDisplayName()));
                } else {
                    petOwner.setPetForWorldGroup(WorldGroup.getGroupByWorld(player.getWorld().getName()), clonedPet.getUUID());
                    MyPetPlugin.getInstance().getRepository().updateMyPetPlayer(petOwner);
                    MyPetApi.getPetManager().activatePet(clonedPet).ifPresent(Pet::createEntity);
                }
        }, null));
    }

    public List<ShopPet> getPets() {
        return pets.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .collect(java.util.stream.Collectors.toList());
    }

    public void depositPrivate(double amount) {
        privateWallet += amount;
    }

    public boolean isDefault() {
        return defaultShop;
    }

    public void load(ConfigurationSection section) {
        tryToLoad("Name", () -> displayName = section.getString("Name", name));
        tryToLoad("Default", () -> defaultShop = section.getBoolean("Default", false));
        tryToLoad("Position", () -> position = section.getInt("Position", -1));

        tryToLoad("Icon", () -> {
            if (section.contains("Icon")) {
                ConfigurationSection iconSection = section.getConfigurationSection("Icon");
                SkilltreeIcon icon = new SkilltreeIcon();
                tryToLoad("Icon.Material", () -> {
                    if (iconSection.contains("Material")) {
                        icon.setMaterial(iconSection.getString("Material", "chest"));
                    }
                });
                tryToLoad("Icon.Glowing", () -> {
                    if (iconSection.contains("Glowing")) {
                        icon.setGlowing(iconSection.getBoolean("Glowing", false));
                    }
                });
                this.icon = icon;
            }
        });


        tryToLoad("Balance.Type", () -> {
            wallet = WalletType.getByName(section.getString("Balance.Type", "")).orElse(WalletType.None);
            switch (wallet) {
                case Bank:
                case Player:
                    tryToLoad("Display Name", () -> walletOwner = section.getString("Balance.Owner", null));
            }
        });

        tryToLoad("Pets", () -> {
            ConfigurationSection pets = section.getConfigurationSection("Pets");
            if (pets == null) {
                MyPetApi.getLogger().warning(displayName + " shop failed to load! Please check your shop config.");
                return;
            }

            Queue<ShopPet> filler = new ArrayDeque<>();
            for (String name : pets.getKeys(false)) {
                tryToLoad("Pets." + name, () -> {
                    ShopPet pet = new ShopPet(name);
                    try {
                        pet.load(pets.getConfigurationSection(name));

                        if (pet.getPosition() < 0) {
                            filler.add(pet);
                            return;
                        }

                        this.pets.put(pet.getPosition(), pet);
                    } catch (PetTypeNotFoundException ignored) {
                    }
                });
            }

            int slot = 0;
            while (!filler.isEmpty()) {
                if (this.pets.containsKey(slot)) {
                    slot++;
                    continue;
                }
                ShopPet pet = filler.poll();
                this.pets.put(slot, pet);
            }
        });
    }
}