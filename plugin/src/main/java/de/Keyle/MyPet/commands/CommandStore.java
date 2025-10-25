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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.commands.CommandTabCompleter;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.repository.RepositoryCallback;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CommandStore implements CommandTabCompleter {

    public boolean onCommand(final CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("You can't use this command from server console!");
            return true;
        }
        final Player player = (Player) sender;
        if (WorldGroup.getGroupByWorld(player.getWorld()).isDisabled()) {
            player.sendMessage(Translation.getString("Message.No.AllowedHere", player));
            return true;
        }

        if (!Permissions.has(player, "MyPet.command.store")) {
            player.sendMessage(Translation.getString("Message.No.Allowed", player));
            return true;
        }

        if (MyPetApi.getPlayerManager().isMyPetPlayer(player)) {
            final MyPetPlayer owner = MyPetApi.getPlayerManager().getMyPetPlayer(player);
            final int maxPetCount = getMaxPetCount(owner.getPlayer());

            if (maxPetCount == 0) {
                player.sendMessage(Translation.getString("Message.No.Allowed", player));
                return true;
            }

            if (owner.hasMyPet()) {
                MyPetApi.getRepository().getMyPets(owner, new RepositoryCallback<List<StoredMyPet>>() {
                    @Override
                    public void callback(List<StoredMyPet> pets) {
                        if (owner.hasMyPet()) {
                            MyPet myPet = owner.getMyPet();
                            String worldGroup = myPet.getWorldGroup();

                            int inactivePetCount = getInactivePetCount(pets, worldGroup) - 1; // -1 for active pet
                            if (inactivePetCount >= maxPetCount) {
                                sender.sendMessage(Util.formatText(Translation.getString("Message.Command.Switch.Limit", player), maxPetCount));
                                return;
                            }
                            if (MyPetApi.getMyPetManager().deactivateMyPet(owner, true)) {
                                owner.setMyPetForWorldGroup(worldGroup, null);
                                sender.sendMessage(Util.formatText(Translation.getString("Message.Command.Switch.Success", player), myPet.getPetName()));
                            }
                        } else {
                            player.sendMessage(Translation.getString("Message.Command.Switch.NoPet", player));
                        }
                    }
                });
                return true;
            }
        }
        player.sendMessage(Translation.getString("Message.Command.Switch.NoPet", player));
        return true;
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] strings) {
        return Collections.emptyList();
    }
}