/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
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

/*
 * This file is part of MyPet-NPC
 *
 * Copyright (C) 2011-2013 Keyle
 * MyPet-NPC is licensed under the GNU Lesser General Public License.
 *
 * MyPet-NPC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyPet-NPC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.util.hooks.citizens;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.gui.IconMenu;
import de.Keyle.MyPet.api.gui.IconMenuItem;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.repository.RepositoryCallback;
import de.Keyle.MyPet.api.util.EnumSelector;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.gui.selectionmenu.MyPetSelectionGui;
import de.Keyle.MyPet.util.hooks.CitizensHook;
import de.Keyle.MyPet.util.hooks.VaultHook;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static de.Keyle.MyPet.api.Configuration.Misc;

public class StorageTrait extends Trait {

    public StorageTrait() {
        super("mypet-storage");
    }

    @EventHandler
    public void onRightClick(final NPCRightClickEvent npcEvent) {
        if (this.npc != npcEvent.getNPC()) {
            return;
        }

        final Player player = npcEvent.getClicker();

        if (!Permissions.has(player, "MyPet.npc.storage")) {
            player.sendMessage(Translation.getComponent("Message.No.Allowed", player));
            return;
        }


        if (MyPetApi.getPlayerManager().isMyPetPlayer(player)) {
            final MyPetPlayer myPetPlayer = MyPetApi.getPlayerManager().getMyPetPlayer(player);
            assert myPetPlayer != null;
            if (myPetPlayer.hasMyPet()) {

                final NPC npc = this.npc;

                MyPetApi.getRepository().getMyPets(myPetPlayer, new RepositoryCallback<>() {
                    @Override
                    public void callback(List<StoredMyPet> pets) {
                        WorldGroup wg = WorldGroup.getGroupByWorld(myPetPlayer.getPlayer().getWorld().getName());
                        int inactivePetCount = 0;
                        UUID activePetUUID = myPetPlayer.getMyPet().getUUID();

                        for (StoredMyPet mypet : pets) {
                            if (activePetUUID.equals(mypet.getUUID()) || (!mypet.getWorldGroup().isEmpty() && !mypet.getWorldGroup().equals(wg.getName()))) {
                                continue;
                            }
                            inactivePetCount++;
                        }

                        int maxPetCount = 0;
                        if (!Permissions.has(player, "MyPet.admin")) {
                            for (int i = Misc.MAX_STORED_PET_COUNT; i > 0; i--) {
                                if (Permissions.hasLegacy(player, "MyPet.petstorage.limit.", i)) {
                                    maxPetCount = i;
                                    break;
                                }
                            }
                        } else {
                            maxPetCount = Misc.MAX_STORED_PET_COUNT;
                        }

                        if (inactivePetCount == 0 && maxPetCount == 0) {
                            player.sendMessage(Translation.getComponent("Message.No.Allowed", player));
                            return;
                        }

                        if (inactivePetCount >= maxPetCount) {
                            String stats = "(" + inactivePetCount + "/" + maxPetCount + ")";

                            final MyPetSelectionGui gui = new MyPetSelectionGui(myPetPlayer, Component.text(stats + " ").append(Translation.getComponent("Message.Npc.SwitchTitle", player)));
                            gui.open(pets, new RepositoryCallback<>() {
                                @Override
                                public void callback(StoredMyPet storedMyPet) {
                                    MyPetApi.getMyPetManager().deactivateMyPet(myPetPlayer, true);
                                    Optional<MyPet> activePet = MyPetApi.getMyPetManager().activateMyPet(storedMyPet);
                                    if (activePet.isPresent() && myPetPlayer.isOnline()) {
                                        Player p = myPetPlayer.getPlayer();
                                        myPetPlayer.sendMessage(Translation.getFormattedComponent("Message.Npc.ChosenPet", player, activePet.get().getDisplayName()));
                                        WorldGroup wg = WorldGroup.getGroupByWorld(p.getWorld().getName());
                                        myPetPlayer.setMyPetForWorldGroup(wg, activePet.get().getUUID());

                                        switch (activePet.get().createEntity()) {
                                            case Canceled:
                                                myPetPlayer.sendMessage(Translation.getFormattedComponent("Message.Spawn.Prevent", player, activePet.get().getDisplayName()));
                                                break;
                                            case NoSpace:
                                                myPetPlayer.sendMessage(Translation.getFormattedComponent("Message.Spawn.NoSpace", player, activePet.get().getDisplayName()));
                                                break;
                                            case NotAllowed:
                                                myPetPlayer.sendMessage(Translation.getFormattedComponent("Message.No.AllowedHere", player, activePet.get().getDisplayName()));
                                                break;
                                            case Dead:
                                                if (de.Keyle.MyPet.api.Configuration.Respawn.DISABLE_AUTO_RESPAWN) {
                                                    myPetPlayer.sendMessage(Translation.getFormattedComponent("Message.Call.Dead", myPetPlayer, activePet.get().getDisplayName()));
                                                } else {
                                                    myPetPlayer.sendMessage(Translation.getFormattedComponent("Message.Call.Dead.Respawn", myPetPlayer, activePet.get().getDisplayName(), activePet.get().getRespawnTime()));
                                                }
                                                break;
                                            case Spectator:
                                                myPetPlayer.sendMessage(Translation.getFormattedComponent("Message.Spawn.Spectator", myPetPlayer, activePet.get().getDisplayName()));
                                                break;
                                        }
                                    }
                                }
                            });
                        } else {
                            IconMenu menu = new IconMenu(Translation.getComponent("Message.Npc.HandOverTitle", myPetPlayer), event -> {
                                if (!myPetPlayer.hasMyPet()) {
                                    return;
                                }
                                if (event.getPosition() == 3) {
                                    boolean store = true;
                                    double costs = calculateStorageCosts(myPetPlayer.getMyPet());
                                    if (MyPetApi.getHookHelper().isEconomyEnabled() && costs > 0 && npc.hasTrait(WalletTrait.class)) {
                                        WalletTrait walletTrait = npc.getTrait(WalletTrait.class);
                                        if (!MyPetApi.getHookHelper().getEconomy().canPay(myPetPlayer, costs)) {
                                            player.sendMessage(Translation.getFormattedComponent("Message.No.Money", myPetPlayer, myPetPlayer.getMyPet().getDisplayName(), npcEvent.getNPC().getName()));
                                            store = false;
                                        }
                                        if (MyPetApi.getHookHelper().getEconomy().pay(myPetPlayer, costs)) {
                                            walletTrait.deposit(costs);
                                        } else {
                                            store = false;
                                        }
                                    }

                                    if (store) {
                                        StoredMyPet storedMyPet = myPetPlayer.getMyPet();
                                        if (MyPetApi.getMyPetManager().deactivateMyPet(myPetPlayer, true)) {
                                            // remove pet from world groups
                                            String wg1 = myPetPlayer.getWorldGroupForMyPet(storedMyPet.getUUID());
                                            myPetPlayer.setMyPetForWorldGroup(wg1, null);
                                            MyPetApi.getRepository().updateMyPetPlayer(myPetPlayer, null);

                                            player.sendMessage(Translation.getFormattedComponent("Message.Npc.HandOver", myPetPlayer, storedMyPet.getDisplayName(), npcEvent.getNPC().getName()));
                                        }
                                    }
                                }
                                event.setWillClose(true);
                                event.setWillDestroy(true);
                            }, MyPetApi.getPlugin());
                            double storageCosts = calculateStorageCosts(myPetPlayer.getMyPet());
                            IconMenuItem yesIcon = new IconMenuItem()
                                    .setMaterial(EnumSelector.find(Material.class, "WOOL", "GREEN_WOOL"))
                                    .setData(5)
                                    .setTitle(Translation.getComponent("Name.Yes", myPetPlayer).color(NamedTextColor.GREEN));
                            yesIcon.addLoreLine(Translation.getFormattedComponent("Message.Npc.YesHandOver", myPetPlayer, myPetPlayer.getMyPet().getDisplayName()));
                            if (MyPetApi.getPluginHookManager().isHookActive(VaultHook.class) && npc.hasTrait(WalletTrait.class) && storageCosts > 0) {
                                NamedTextColor canPay = MyPetApi.getHookHelper().getEconomy().canPay(myPetPlayer, storageCosts) ? NamedTextColor.GREEN : NamedTextColor.RED;
                                yesIcon.addLoreLine(Component.empty());
                                yesIcon.addLoreLine(Component.text()
                                        .append(Translation.getComponent("Name.Costs", myPetPlayer))
                                        .append(Component.text(": "))
                                        .append(Component.text(storageCosts).color(canPay))
                                        .append(Component.text(" ").color(NamedTextColor.DARK_GREEN))
                                        .append(Component.text(MyPetApi.getHookHelper().getEconomy().currencyNameSingular()).color(NamedTextColor.DARK_GREEN))
                                        .build());
                            }
                            menu.setOption(3, yesIcon);
                            IconMenuItem noIcon = new IconMenuItem()
                                    .setMaterial(EnumSelector.find(Material.class, "WOOL", "RED_WOOL"))
                                    .setData(14)
                                    .setTitle(Translation.getComponent("Name.No", myPetPlayer).color(NamedTextColor.RED));
                            noIcon.addLoreLine(Translation.getFormattedComponent("Message.Npc.NoHandOver", myPetPlayer, myPetPlayer.getMyPet().getDisplayName()));
                            menu.setOption(5, noIcon);
                            menu.open(player);
                        }
                    }
                });
            } else {
                MyPetApi.getRepository().getMyPets(myPetPlayer, new RepositoryCallback<>() {
                    @Override
                    public void callback(List<StoredMyPet> pets) {
                        if (!pets.isEmpty()) {
                            int maxPetCount = 0;
                            if (!Permissions.has(player, "MyPet.admin")) {
                                for (int i = Misc.MAX_STORED_PET_COUNT; i > 0; i--) {
                                    if (Permissions.hasLegacy(player, "MyPet.petstorage.limit.", i)) {
                                        maxPetCount = i;
                                        break;
                                    }
                                }
                            } else {
                                maxPetCount = Misc.MAX_STORED_PET_COUNT;
                            }
                            String stats = "(" + pets.size() + "/" + maxPetCount + ")";
                            MyPetSelectionGui gui = new MyPetSelectionGui(myPetPlayer, Translation.getComponent("Message.Npc.TakeTitle", myPetPlayer).append(Component.text(" " + stats)));
                            gui.open(pets, new RepositoryCallback<>() {
                                @Override
                                public void callback(StoredMyPet storedMyPet) {
                                    Optional<MyPet> myPet = MyPetApi.getMyPetManager().activateMyPet(storedMyPet);
                                    if (myPet.isPresent()) {
                                        Player player = myPetPlayer.getPlayer();
                                        myPetPlayer.sendMessage(Translation.getFormattedComponent("Message.Npc.ChosenPet", myPetPlayer, myPet.get().getDisplayName()));
                                        WorldGroup wg = WorldGroup.getGroupByWorld(player.getWorld().getName());
                                        myPetPlayer.setMyPetForWorldGroup(wg, myPet.get().getUUID());
                                        MyPetApi.getRepository().updateMyPetPlayer(myPetPlayer, null);

                                        switch (myPet.get().createEntity()) {
                                            case Canceled:
                                                myPetPlayer.sendMessage(Translation.getFormattedComponent("Message.Spawn.Prevent", myPetPlayer, myPet.get().getDisplayName()));
                                                break;
                                            case NoSpace:
                                                myPetPlayer.sendMessage(Translation.getFormattedComponent("Message.Spawn.NoSpace", myPetPlayer, myPet.get().getDisplayName()));
                                                break;
                                            case NotAllowed:
                                                myPetPlayer.sendMessage(Translation.getFormattedComponent("Message.No.AllowedHere", myPetPlayer, myPet.get().getDisplayName()));
                                                break;
                                            case Dead:
                                                if (de.Keyle.MyPet.api.Configuration.Respawn.DISABLE_AUTO_RESPAWN) {
                                                    myPetPlayer.sendMessage(Translation.getFormattedComponent("Message.Call.Dead", myPetPlayer, myPet.get().getDisplayName()));
                                                } else {
                                                    myPetPlayer.sendMessage(Translation.getFormattedComponent("Message.Call.Dead.Respawn", myPetPlayer, myPet.get().getDisplayName(), myPet.get().getRespawnTime()));
                                                }
                                                break;
                                        }
                                    }
                                }
                            });
                        } else {
                            myPetPlayer.sendMessage(Translation.getComponent("Message.No.HasPet", myPetPlayer), 5000);
                        }
                    }
                });
            }
            return;
        }
        player.sendMessage(Translation.getComponent("Message.No.HasPet", player));
    }

    public double calculateStorageCosts(MyPet myPet) {
        return CitizensHook.NPC_STORAGE_COSTS_FIXED + (myPet.getExperience().getLevel() * CitizensHook.NPC_STORAGE_COSTS_FACTOR);
    }
}
