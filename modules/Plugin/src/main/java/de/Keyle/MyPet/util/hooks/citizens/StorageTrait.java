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
import de.Keyle.MyPet.api.Util;
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
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static de.Keyle.MyPet.api.Configuration.Misc;
import static org.bukkit.ChatColor.*;

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
            player.sendMessage(Translation.getString("Message.No.Allowed", player));
            return;
        }


        if (MyPetApi.getPlayerManager().isMyPetPlayer(player)) {
            final MyPetPlayer myPetPlayer = MyPetApi.getPlayerManager().getMyPetPlayer(player);
            assert myPetPlayer != null;
            if (myPetPlayer.hasMyPet()) {

                final NPC npc = this.npc;

                MyPetApi.getRepository().getMyPets(myPetPlayer, new RepositoryCallback<List<StoredMyPet>>() {
                    @Override
                    public void callback(List<StoredMyPet> pets) {
                        WorldGroup wg = WorldGroup.getGroupByWorld(myPetPlayer.getPlayer().getWorld().getName());
                        int inactivePetCount = 0;
                        UUID activePetUUID = myPetPlayer.getMyPet().getUUID();

                        for (StoredMyPet mypet : pets) {
                            if (activePetUUID.equals(mypet.getUUID()) || (!mypet.getWorldGroup().equals("") && !mypet.getWorldGroup().equals(wg.getName()))) {
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
                            player.sendMessage(Translation.getString("Message.No.Allowed", player));
                            return;
                        }

                        if (inactivePetCount >= maxPetCount) {
                            String stats = "(" + inactivePetCount + "/" + maxPetCount + ")";

                            final MyPetSelectionGui gui = new MyPetSelectionGui(myPetPlayer, stats + " " + Translation.getString("Message.Npc.SwitchTitle", player));
                            gui.open(pets, new RepositoryCallback<StoredMyPet>() {
                                @Override
                                public void callback(StoredMyPet storedMyPet) {
                                    MyPetApi.getMyPetManager().deactivateMyPet(myPetPlayer, true);
                                    Optional<MyPet> activePet = MyPetApi.getMyPetManager().activateMyPet(storedMyPet);
                                    if (activePet.isPresent() && myPetPlayer.isOnline()) {
                                        Player p = myPetPlayer.getPlayer();
                                        myPetPlayer.sendMessage(Util.formatText(Translation.getString("Message.Npc.ChosenPet", player), activePet.get().getPetName()));
                                        WorldGroup wg = WorldGroup.getGroupByWorld(p.getWorld().getName());
                                        myPetPlayer.setMyPetForWorldGroup(wg, activePet.get().getUUID());

                                        switch (activePet.get().createEntity()) {
                                            case Canceled:
                                                myPetPlayer.sendMessage(Util.formatText(Translation.getString("Message.Spawn.Prevent", player), activePet.get().getPetName()));
                                                break;
                                            case NoSpace:
                                                myPetPlayer.sendMessage(Util.formatText(Translation.getString("Message.Spawn.NoSpace", player), activePet.get().getPetName()));
                                                break;
                                            case NotAllowed:
                                                myPetPlayer.sendMessage(Util.formatText(Translation.getString("Message.No.AllowedHere", player), activePet.get().getPetName()));
                                                break;
                                            case Dead:
                                                if (de.Keyle.MyPet.api.Configuration.Respawn.DISABLE_AUTO_RESPAWN) {
                                                    myPetPlayer.sendMessage(Util.formatText(Translation.getString("Message.Call.Dead", myPetPlayer), activePet.get().getPetName()));
                                                } else {
                                                    myPetPlayer.sendMessage(Util.formatText(Translation.getString("Message.Call.Dead.Respawn", myPetPlayer), activePet.get().getPetName(), activePet.get().getRespawnTime()));
                                                }
                                                break;
                                            case Spectator:
                                                myPetPlayer.sendMessage(Util.formatText(Translation.getString("Message.Spawn.Spectator", myPetPlayer), activePet.get().getPetName()));
                                                break;
                                        }
                                    }
                                }
                            });
                        } else {
                            IconMenu menu = new IconMenu(Translation.getString("Message.Npc.HandOverTitle", myPetPlayer), event -> {
                                if (!myPetPlayer.hasMyPet()) {
                                    return;
                                }
                                if (event.getPosition() == 3) {
                                    boolean store = true;
                                    double costs = calculateStorageCosts(myPetPlayer.getMyPet());
                                    if (MyPetApi.getHookHelper().isEconomyEnabled() && costs > 0 && npc.hasTrait(WalletTrait.class)) {
                                        WalletTrait walletTrait = npc.getTrait(WalletTrait.class);
                                        if (!MyPetApi.getHookHelper().getEconomy().canPay(myPetPlayer, costs)) {
                                            player.sendMessage(Util.formatText(Translation.getString("Message.No.Money", myPetPlayer), myPetPlayer.getMyPet().getPetName(), npcEvent.getNPC().getName()));
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

                                            player.sendMessage(Util.formatText(Translation.getString("Message.Npc.HandOver", myPetPlayer), storedMyPet.getPetName(), npcEvent.getNPC().getName()));
                                        }
                                    }
                                }
                                event.setWillClose(true);
                                event.setWillDestroy(true);
                            }, MyPetApi.getPlugin());
                            String[] lore;
                            double storageCosts = calculateStorageCosts(myPetPlayer.getMyPet());
                            if (MyPetApi.getPluginHookManager().isHookActive(VaultHook.class) && npc.hasTrait(WalletTrait.class) && storageCosts > 0) {
                                lore = new String[3];
                                lore[1] = "";
                                lore[2] = RESET + Translation.getString("Name.Costs", myPetPlayer) + ": " + (MyPetApi.getHookHelper().getEconomy().canPay(myPetPlayer, storageCosts) ? GREEN : RED) + storageCosts + DARK_GREEN + " " + MyPetApi.getHookHelper().getEconomy().currencyNameSingular();
                            } else {
                                lore = new String[1];
                            }
                            lore[0] = RESET + Util.formatText(Translation.getString("Message.Npc.YesHandOver", myPetPlayer), myPetPlayer.getMyPet().getPetName());
                            menu.setOption(3, new IconMenuItem().setMaterial(EnumSelector.find(Material.class, "WOOL", "GREEN_WOOL")).setData(5).setTitle(GREEN + Translation.getString("Name.Yes", myPetPlayer)).setLore(lore));
                            menu.setOption(5, new IconMenuItem().setMaterial(EnumSelector.find(Material.class, "WOOL", "RED_WOOL")).setData(14).setTitle(RED + Translation.getString("Name.No", myPetPlayer)).setLore(RESET + Util.formatText(Translation.getString("Message.Npc.NoHandOver", myPetPlayer), myPetPlayer.getMyPet().getPetName())));
                            menu.open(player);
                        }
                    }
                });
            } else {
                MyPetApi.getRepository().getMyPets(myPetPlayer, new RepositoryCallback<List<StoredMyPet>>() {
                    @Override
                    public void callback(List<StoredMyPet> pets) {
                        if (pets.size() > 0) {
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
                            MyPetSelectionGui gui = new MyPetSelectionGui(myPetPlayer, Translation.getString("Message.Npc.TakeTitle", myPetPlayer) + " " + stats);
                            gui.open(pets, new RepositoryCallback<StoredMyPet>() {
                                @Override
                                public void callback(StoredMyPet storedMyPet) {
                                    Optional<MyPet> myPet = MyPetApi.getMyPetManager().activateMyPet(storedMyPet);
                                    if (myPet.isPresent()) {
                                        Player player = myPetPlayer.getPlayer();
                                        myPetPlayer.sendMessage(Util.formatText(Translation.getString("Message.Npc.ChosenPet", myPetPlayer), myPet.get().getPetName()));
                                        WorldGroup wg = WorldGroup.getGroupByWorld(player.getWorld().getName());
                                        myPetPlayer.setMyPetForWorldGroup(wg, myPet.get().getUUID());
                                        MyPetApi.getRepository().updateMyPetPlayer(myPetPlayer, null);

                                        switch (myPet.get().createEntity()) {
                                            case Canceled:
                                                myPetPlayer.sendMessage(Util.formatText(Translation.getString("Message.Spawn.Prevent", myPetPlayer), myPet.get().getPetName()));
                                                break;
                                            case NoSpace:
                                                myPetPlayer.sendMessage(Util.formatText(Translation.getString("Message.Spawn.NoSpace", myPetPlayer), myPet.get().getPetName()));
                                                break;
                                            case NotAllowed:
                                                myPetPlayer.sendMessage(Translation.getString("Message.No.AllowedHere", myPetPlayer).replace("%petname%", myPet.get().getPetName()));
                                                break;
                                            case Dead:
                                                if (de.Keyle.MyPet.api.Configuration.Respawn.DISABLE_AUTO_RESPAWN) {
                                                    myPetPlayer.sendMessage(Util.formatText(Translation.getString("Message.Call.Dead", myPetPlayer), myPet.get().getPetName()));
                                                } else {
                                                    myPetPlayer.sendMessage(Util.formatText(Translation.getString("Message.Call.Dead.Respawn", myPetPlayer), myPet.get().getPetName(), myPet.get().getRespawnTime()));
                                                }
                                                break;
                                        }
                                    }
                                }
                            });
                        } else {
                            myPetPlayer.sendMessage(Translation.getString("Message.No.HasPet", myPetPlayer), 5000);
                        }
                    }
                });
            }
            return;
        }
        player.sendMessage(Translation.getString("Message.No.HasPet", player));
    }

    public double calculateStorageCosts(MyPet myPet) {
        return CitizensHook.NPC_STORAGE_COSTS_FIXED + (myPet.getExperience().getLevel() * CitizensHook.NPC_STORAGE_COSTS_FACTOR);
    }
}
