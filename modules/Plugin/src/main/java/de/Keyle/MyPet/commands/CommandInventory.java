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

package de.Keyle.MyPet.commands;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.commands.CommandTabCompleter;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPet.PetState;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.repository.RepositoryCallback;
import de.Keyle.MyPet.api.skill.skilltree.Skill;
import de.Keyle.MyPet.api.util.NBTStorage;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.entity.MyPetClass;
import de.Keyle.MyPet.gui.selectionmenu.MyPetAdminSelectionGui;
import de.Keyle.MyPet.skill.skills.BackpackImpl;
import de.keyle.knbt.TagCompound;

public class CommandInventory implements CommandTabCompleter {
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (WorldGroup.getGroupByWorld(player.getWorld()).isDisabled()) {
                player.sendMessage(Translation.getString("Message.No.AllowedHere", player));
                return true;
            }
            if (args.length == 0) {
                if (MyPetApi.getMyPetManager().hasActiveMyPet(player)) {
                    MyPet myPet = MyPetApi.getMyPetManager().getMyPet(player);
                    if (myPet.getStatus() == PetState.Despawned) {
                        sender.sendMessage(Util.formatText(Translation.getString("Message.Call.First", player), myPet.getPetName()));
                        return true;
                    }
                    if (myPet.getStatus() == PetState.Dead) {
                        sender.sendMessage(Util.formatText(Translation.getString("Message.Action.Dead", player), myPet.getPetName()));
                        return true;
                    }
                    if (!Permissions.hasExtended(player, "MyPet.extended.inventory") && !Permissions.has(player, "MyPet.admin", false)) {
                        myPet.getOwner().sendMessage(Translation.getString("Message.No.CanUse", player));
                        return true;
                    }
                    if (myPet.getSkills().has(BackpackImpl.class)) {
                        myPet.getSkills().get(BackpackImpl.class).activate();
                    }
                } else {
                    sender.sendMessage(Translation.getString("Message.No.HasPet", player));
                }
            } else if (args.length == 1 && Permissions.has(player, "MyPet.admin", false)) {		//Active Pet
            	Player petOwner = Bukkit.getServer().getOfflinePlayer(args[0]).getPlayer();
            	MyPet myPet = MyPetApi.getMyPetManager().getMyPet(petOwner);
            	
            	if (petOwner == null) {
                    sender.sendMessage(Translation.getString("Message.No.PlayerOnline", player));
                } else if (MyPetApi.getMyPetManager().hasActiveMyPet(petOwner)) { 
	                if (myPet.getSkills().isActive(BackpackImpl.class)) {
	                    myPet.getSkills().get(BackpackImpl.class).openInventory(player);
	                }
                }
            }/* else if (args.length == 2 && Permissions.has(player, "MyPet.admin", false) && args[1].equalsIgnoreCase("inactive")) {		//Inactive Pets
                final Player petOwner = Bukkit.getServer().getOfflinePlayer(args[0]).getPlayer();
                final MyPetPlayer myPetOwner = MyPetApi.getPlayerManager().getMyPetPlayer(petOwner);

                if (petOwner == null) {
                    sender.sendMessage(Translation.getString("Message.No.PlayerOnline", player));
                } else {
                	MyPetApi.getRepository().getMyPets(myPetOwner, new RepositoryCallback<List<StoredMyPet>>() {
                        @Override
                        public void callback(List<StoredMyPet> pets) {
                            if (pets.size() - (myPetOwner.hasMyPet() ? 1 : 0) == 0) {
                            	myPetOwner.sendMessage(Translation.getString("Message.Command.Switch.NoStoredPets", myPetOwner));
                                return;
                            }
                            if (myPetOwner.isOnline()) {
                                String worldGroup = WorldGroup.getGroupByWorld(myPetOwner.getPlayer().getWorld().getName()).getName();
                                int inactivePetCount = getInactivePetCount(pets, worldGroup);
                                int maxPetCount = getMaxPetCount(myPetOwner.getPlayer());

                                String title;
                                if (myPetOwner.hasMyPet()) {
                                    inactivePetCount--;
                                    title = Translation.getString("Message.SelectMyPet", myPetOwner);
                                } else {
                                    title = Translation.getString("Message.SelectMyPet", myPetOwner);
                                }

                                String stats = "(" + inactivePetCount + "/" + maxPetCount + ")";

                                final MyPetAdminSelectionGui gui = new MyPetAdminSelectionGui(myPetOwner,player, title + " " + stats);
                                gui.open(pets, new RepositoryCallback<StoredMyPet>() {
                                    @Override
                                    public void callback(StoredMyPet storedMyPet) {
                                    	MyPet myPet = MyPetClass.getByMyPetType(storedMyPet.getPetType()).getNewMyPetInstance(storedMyPet.getOwner());
                                    	Collection<Skill> skills = myPet.getSkills().all();
                                        if (skills.size() > 0) {
                                            for (Skill skill : skills) {
                                                if (skill instanceof NBTStorage) {
                                                    NBTStorage storageSkill = (NBTStorage) skill;
                                                    if (storedMyPet.getSkillInfo().getCompoundData().containsKey(skill.getName())) {
                                                        storageSkill.load(storedMyPet.getSkillInfo().getAs(skill.getName(), TagCompound.class));
                                                    }
                                                }
                                            }
                                        }
                                    	myPet.getSkills().get(BackpackImpl.class).openInventory(player);
                                    }
                                });
                            }
                        }
                    });
                }
            } */
            return true;
        }
        sender.sendMessage("You can't use this command from server console!");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] strings) {
        if (sender instanceof Player && strings.length == 1 && Permissions.has((Player) sender, "MyPet.admin", false)) {
            return null;
        }
        return Collections.emptyList();
    }
    
    private int getMaxPetCount(Player p) {
        int maxPetCount = 0;
        if (Permissions.has(p, "MyPet.admin")) {
            maxPetCount = Configuration.Misc.MAX_STORED_PET_COUNT;
        } else {
            for (int i = Configuration.Misc.MAX_STORED_PET_COUNT; i > 0; i--) {
                if (Permissions.has(p, "MyPet.petstorage.limit." + i)) {
                    maxPetCount = i;
                    break;
                }
            }
        }
        return maxPetCount;
    }

    private int getInactivePetCount(List<StoredMyPet> pets, String worldGroup) {
        int inactivePetCount = 0;

        for (StoredMyPet pet : pets) {
            if (!pet.getWorldGroup().equals(worldGroup)) {
                continue;
            }
            inactivePetCount++;
        }

        return inactivePetCount;
    }
}