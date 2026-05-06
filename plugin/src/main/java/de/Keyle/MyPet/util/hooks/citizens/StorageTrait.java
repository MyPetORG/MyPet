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

package de.Keyle.MyPet.util.hooks.citizens;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.StoredPet;
import de.Keyle.MyPet.api.gui.IconMenu;
import de.Keyle.MyPet.api.gui.IconMenuItem;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.util.locale.Locale;
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
            player.sendMessage(Locale.getComponent("Message.No.Allowed", player));
            return;
        }


        if (MyPetApi.getPlayerManager().isMyPetPlayer(player)) {
            final MyPetPlayer myPetPlayer = MyPetApi.getPlayerManager().getMyPetPlayer(player);
            assert myPetPlayer != null;
            if (myPetPlayer.hasMyPet()) {

                final NPC npc = this.npc;

                MyPetPlugin.getInstance().getRepository().getPets(myPetPlayer).thenAccept(pets -> player.getScheduler().run(MyPetApi.getPlugin(), folaTask -> {
                        WorldGroup wg = WorldGroup.getGroupByWorld(myPetPlayer.getPlayer().getWorld().getName());
                        int inactivePetCount = 0;
                        UUID activePetUUID = myPetPlayer.getMyPet().getUUID();

                        for (StoredPet mypet : pets) {
                            if (activePetUUID.equals(mypet.getUUID()) || (!mypet.getWorldGroup().isEmpty() && !mypet.getWorldGroup().equals(wg.getName()))) {
                                continue;
                            }
                            inactivePetCount++;
                        }

                        int maxPetCount = 0;
                        if (!Permissions.has(player, "MyPet.admin")) {
                            for (int i = Misc.MAX_STORED_PET_COUNT; i > 0; i--) {
                                if (Permissions.has(player, "MyPet.petstorage.limit." + i)) {
                                    maxPetCount = i;
                                    break;
                                }
                            }
                        } else {
                            maxPetCount = Misc.MAX_STORED_PET_COUNT;
                        }

                        if (inactivePetCount == 0 && maxPetCount == 0) {
                            player.sendMessage(Locale.getComponent("Message.No.Allowed", player));
                            return;
                        }

                        if (inactivePetCount >= maxPetCount) {
                            String stats = "(" + inactivePetCount + "/" + maxPetCount + ")";

                            final MyPetSelectionGui gui = new MyPetSelectionGui(myPetPlayer, Component.text(stats + " ").append(Locale.getComponent("Message.Npc.SwitchTitle", player)));
                            gui.open(pets, storedPet -> {
                                    MyPetApi.getPetManager().deactivateMyPet(myPetPlayer, true);
                                    Optional<MyPet> activePet = MyPetApi.getPetManager().activateMyPet(storedPet);
                                    if (activePet.isPresent() && myPetPlayer.isOnline()) {
                                        Player p = myPetPlayer.getPlayer();
                                        myPetPlayer.sendMessage(Locale.getFormattedComponent("Message.Npc.ChosenPet", player, activePet.get().getDisplayName()));
                                        WorldGroup activeWg = WorldGroup.getGroupByWorld(p.getWorld().getName());
                                        myPetPlayer.setMyPetForWorldGroup(activeWg, activePet.get().getUUID());

                                        switch (activePet.get().createEntity()) {
                                            case Canceled:
                                                myPetPlayer.sendMessage(Locale.getFormattedComponent("Message.Spawn.Prevent", player, activePet.get().getDisplayName()));
                                                break;
                                            case NoSpace:
                                                myPetPlayer.sendMessage(Locale.getFormattedComponent("Message.Spawn.NoSpace", player, activePet.get().getDisplayName()));
                                                break;
                                            case NotAllowed:
                                                myPetPlayer.sendMessage(Locale.getFormattedComponent("Message.No.AllowedHere", player, activePet.get().getDisplayName()));
                                                break;
                                            case Dead:
                                                if (Configuration.Respawn.DISABLE_AUTO_RESPAWN) {
                                                    myPetPlayer.sendMessage(Locale.getFormattedComponent("Message.Call.Dead", myPetPlayer, activePet.get().getDisplayName()));
                                                } else {
                                                    myPetPlayer.sendMessage(Locale.getFormattedComponent("Message.Call.Dead.Respawn", myPetPlayer, activePet.get().getDisplayName(), activePet.get().getRespawnTime()));
                                                }
                                                break;
                                            case Spectator:
                                                myPetPlayer.sendMessage(Locale.getFormattedComponent("Message.Spawn.Spectator", myPetPlayer, activePet.get().getDisplayName()));
                                                break;
                                        }
                                    }
                            });
                        } else {
                            IconMenu menu = new IconMenu(Locale.getComponent("Message.Npc.HandOverTitle", myPetPlayer), event -> {
                                if (!myPetPlayer.hasMyPet()) {
                                    return;
                                }
                                if (event.getPosition() == 3) {
                                    boolean store = true;
                                    double costs = calculateStorageCosts(myPetPlayer.getMyPet());
                                    if (MyPetApi.getHookHelper().isEconomyEnabled() && costs > 0 && npc.hasTrait(WalletTrait.class)) {
                                        WalletTrait walletTrait = npc.getTrait(WalletTrait.class);
                                        if (!MyPetApi.getHookHelper().getEconomy().canPay(myPetPlayer, costs)) {
                                            player.sendMessage(Locale.getFormattedComponent("Message.No.Money", myPetPlayer, myPetPlayer.getMyPet().getDisplayName(), npcEvent.getNPC().getName()));
                                            store = false;
                                        }
                                        if (MyPetApi.getHookHelper().getEconomy().pay(myPetPlayer, costs)) {
                                            walletTrait.deposit(costs);
                                        } else {
                                            store = false;
                                        }
                                    }

                                    if (store) {
                                        StoredPet storedPet = myPetPlayer.getMyPet();
                                        if (MyPetApi.getPetManager().deactivateMyPet(myPetPlayer, true)) {
                                            // remove pet from world groups
                                            String wg1 = myPetPlayer.getWorldGroupForMyPet(storedPet.getUUID());
                                            myPetPlayer.setMyPetForWorldGroup(wg1, null);
                                            MyPetPlugin.getInstance().getRepository().updateMyPetPlayer(myPetPlayer);

                                            player.sendMessage(Locale.getFormattedComponent("Message.Npc.HandOver", myPetPlayer, storedPet.getDisplayName(), npcEvent.getNPC().getName()));
                                        }
                                    }
                                }
                                event.setWillClose(true);
                                event.setWillDestroy(true);
                            }, MyPetApi.getPlugin());
                            double storageCosts = calculateStorageCosts(myPetPlayer.getMyPet());
                            IconMenuItem yesIcon = new IconMenuItem()
                                    .setMaterial(Material.GREEN_WOOL)
                                    .setData(5)
                                    .setTitle(Locale.getComponent("Name.Yes", myPetPlayer).color(NamedTextColor.GREEN));
                            yesIcon.addLoreLine(Locale.getFormattedComponent("Message.Npc.YesHandOver", myPetPlayer, myPetPlayer.getMyPet().getDisplayName()));
                            if (MyPetApi.getPluginHookManager().isHookActive(VaultHook.class) && npc.hasTrait(WalletTrait.class) && storageCosts > 0) {
                                NamedTextColor canPay = MyPetApi.getHookHelper().getEconomy().canPay(myPetPlayer, storageCosts) ? NamedTextColor.GREEN : NamedTextColor.RED;
                                yesIcon.addLoreLine(Component.empty());
                                yesIcon.addLoreLine(Component.text()
                                        .append(Locale.getComponent("Name.Costs", myPetPlayer))
                                        .append(Component.text(": "))
                                        .append(Component.text(storageCosts).color(canPay))
                                        .append(Component.text(" ").color(NamedTextColor.DARK_GREEN))
                                        .append(Component.text(MyPetApi.getHookHelper().getEconomy().currencyNameSingular()).color(NamedTextColor.DARK_GREEN))
                                        .build());
                            }
                            menu.setOption(3, yesIcon);
                            IconMenuItem noIcon = new IconMenuItem()
                                    .setMaterial(Material.RED_WOOL)
                                    .setData(14)
                                    .setTitle(Locale.getComponent("Name.No", myPetPlayer).color(NamedTextColor.RED));
                            noIcon.addLoreLine(Locale.getFormattedComponent("Message.Npc.NoHandOver", myPetPlayer, myPetPlayer.getMyPet().getDisplayName()));
                            menu.setOption(5, noIcon);
                            menu.open(player);
                        }
                }, null));
            } else {
                MyPetPlugin.getInstance().getRepository().getPets(myPetPlayer).thenAccept(pets -> player.getScheduler().run(MyPetApi.getPlugin(), folaTask -> {
                        if (!pets.isEmpty()) {
                            int maxPetCount = 0;
                            if (!Permissions.has(player, "MyPet.admin")) {
                                for (int i = Misc.MAX_STORED_PET_COUNT; i > 0; i--) {
                                    if (Permissions.has(player, "MyPet.petstorage.limit." + i)) {
                                        maxPetCount = i;
                                        break;
                                    }
                                }
                            } else {
                                maxPetCount = Misc.MAX_STORED_PET_COUNT;
                            }
                            String stats = "(" + pets.size() + "/" + maxPetCount + ")";
                            MyPetSelectionGui gui = new MyPetSelectionGui(myPetPlayer, Locale.getComponent("Message.Npc.TakeTitle", myPetPlayer).append(Component.text(" " + stats)));
                            gui.open(pets, storedPet -> {
                                    Optional<MyPet> myPet = MyPetApi.getPetManager().activateMyPet(storedPet);
                                    if (myPet.isPresent()) {
                                        Player ownerPlayer = myPetPlayer.getPlayer();
                                        myPetPlayer.sendMessage(Locale.getFormattedComponent("Message.Npc.ChosenPet", myPetPlayer, myPet.get().getDisplayName()));
                                        WorldGroup takeWg = WorldGroup.getGroupByWorld(ownerPlayer.getWorld().getName());
                                        myPetPlayer.setMyPetForWorldGroup(takeWg, myPet.get().getUUID());
                                        MyPetPlugin.getInstance().getRepository().updateMyPetPlayer(myPetPlayer);

                                        switch (myPet.get().createEntity()) {
                                            case Canceled:
                                                myPetPlayer.sendMessage(Locale.getFormattedComponent("Message.Spawn.Prevent", myPetPlayer, myPet.get().getDisplayName()));
                                                break;
                                            case NoSpace:
                                                myPetPlayer.sendMessage(Locale.getFormattedComponent("Message.Spawn.NoSpace", myPetPlayer, myPet.get().getDisplayName()));
                                                break;
                                            case NotAllowed:
                                                myPetPlayer.sendMessage(Locale.getFormattedComponent("Message.No.AllowedHere", myPetPlayer, myPet.get().getDisplayName()));
                                                break;
                                            case Dead:
                                                if (Configuration.Respawn.DISABLE_AUTO_RESPAWN) {
                                                    myPetPlayer.sendMessage(Locale.getFormattedComponent("Message.Call.Dead", myPetPlayer, myPet.get().getDisplayName()));
                                                } else {
                                                    myPetPlayer.sendMessage(Locale.getFormattedComponent("Message.Call.Dead.Respawn", myPetPlayer, myPet.get().getDisplayName(), myPet.get().getRespawnTime()));
                                                }
                                                break;
                                        }
                                    }
                            });
                        } else {
                            myPetPlayer.sendMessage(Locale.getComponent("Message.No.HasPet", myPetPlayer), 5000);
                        }
                }, null));
            }
            return;
        }
        player.sendMessage(Locale.getComponent("Message.No.HasPet", player));
    }

    public double calculateStorageCosts(MyPet myPet) {
        return CitizensHook.NPC_STORAGE_COSTS_FIXED + (myPet.getExperience().getLevel() * CitizensHook.NPC_STORAGE_COSTS_FACTOR);
    }
}
