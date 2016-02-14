/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2016 Keyle
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

import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.repository.RepositoryCallback;
import de.Keyle.MyPet.entity.types.InactiveMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.repository.MyPetList;
import de.Keyle.MyPet.repository.PlayerList;
import de.Keyle.MyPet.util.Util;
import de.Keyle.MyPet.util.WorldGroup;
import de.Keyle.MyPet.util.hooks.Permissions;
import de.Keyle.MyPet.util.locale.Translation;
import de.Keyle.MyPet.util.selectionmenu.MyPetSelectionGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CommandSwitch implements CommandExecutor, TabCompleter {
    private List<String> storeList = new ArrayList<>();

    public CommandSwitch() {
        storeList.add("store");
    }

    public boolean onCommand(final CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("You can't use this command from server console!");
            return true;
        }
        Player player = (Player) sender;
        if (!Permissions.has(player, "MyPet.user.command.switch")) {
            player.sendMessage(Translation.getString("Message.No.Allowed", player));
            return true;
        }

        if (PlayerList.isMyPetPlayer(player)) {
            final MyPetPlayer owner = PlayerList.getMyPetPlayer(player);

            if (args.length > 0 && args[0].equalsIgnoreCase("store")) {
                if (owner.hasMyPet()) {
                    MyPetList.getInactiveMyPets(owner, new RepositoryCallback<List<InactiveMyPet>>() {
                        @Override
                        public void callback(List<InactiveMyPet> pets) {
                            WorldGroup wg = WorldGroup.getGroupByWorld(owner.getPlayer().getWorld().getName());
                            int inactivePetCount = 0;
                            UUID activePetUUID = owner.getMyPet().getUUID();

                            for (InactiveMyPet mypet : pets) {
                                if (activePetUUID.equals(mypet.getUUID()) || !mypet.getWorldGroup().equals("") || !mypet.getWorldGroup().equals(wg.getName())) {
                                    continue;
                                }
                                inactivePetCount++;
                            }
                            int maxPetCount = 0;
                            Player p = owner.getPlayer();
                            if (!p.isOp()) {
                                for (int i = 54; i > 0; i--) {
                                    if (Permissions.has(p, "MyPet.user.command.switch.limit." + i)) {
                                        maxPetCount = i;
                                        break;
                                    }
                                }
                            } else {
                                maxPetCount = 54;
                            }
                            if (inactivePetCount >= maxPetCount) {
                                p.sendMessage(Util.formatText(Translation.getString("Message.Command.Switch.Limit", owner), maxPetCount));
                                return;
                            }
                            MyPet myPet = owner.getMyPet();
                            if (MyPetList.deactivateMyPet(owner, true)) {
                                owner.setMyPetForWorldGroup(wg.getName(), null);
                                myPet.sendMessageToOwner(Util.formatText(Translation.getString("Message.Command.Switch.Success", owner), myPet.getPetName()));
                            }
                        }
                    });
                } else {
                    sender.sendMessage(Translation.getString("Message.Command.Switch.NoPet", player));
                }
                return true;
            }

            MyPetSelectionGui gui = new MyPetSelectionGui(owner);
            gui.open(new RepositoryCallback<InactiveMyPet>() {
                @Override
                public void callback(InactiveMyPet myPet) {
                    MyPet activePet = MyPetList.activateMyPet(myPet);
                    if (activePet != null && owner.isOnline()) {
                        Player player = owner.getPlayer();
                        activePet.sendMessageToOwner(Util.formatText(Translation.getString("Message.Npc.ChosenPet", owner), activePet.getPetName()));
                        WorldGroup wg = WorldGroup.getGroupByWorld(player.getWorld().getName());
                        owner.setMyPetForWorldGroup(wg.getName(), activePet.getUUID());

                        switch (activePet.createPet()) {
                            case Canceled:
                                activePet.sendMessageToOwner(Util.formatText(Translation.getString("Message.Spawn.Prevent", owner), activePet.getPetName()));
                                break;
                            case NoSpace:
                                activePet.sendMessageToOwner(Util.formatText(Translation.getString("Message.Spawn.NoSpace", owner), activePet.getPetName()));
                                break;
                            case NotAllowed:
                                activePet.sendMessageToOwner(Translation.getString("Message.No.AllowedHere", owner).replace("%petname%", activePet.getPetName()));
                                break;
                            case Dead:
                                activePet.sendMessageToOwner(Translation.getString("Message.Spawn.Respawn.In", owner).replace("%petname%", activePet.getPetName()).replace("%time%", "" + activePet.getRespawnTime()));
                                break;
                        }
                    }
                }
            });
        } else {
            sender.sendMessage(Translation.getString("Message.No.HasPet", player));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        if (MyPetList.hasActiveMyPet((Player) commandSender)) {
            return storeList;
        }
        return CommandAdmin.EMPTY_LIST;
    }
}