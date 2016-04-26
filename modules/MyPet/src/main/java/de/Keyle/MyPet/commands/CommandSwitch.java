/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2016 Keyle
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

import com.google.common.base.Optional;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.repository.RepositoryCallback;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.util.selectionmenu.MyPetSelectionGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

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
        if (!Permissions.hasLegacy(player, "MyPet.command.switch")) {
            player.sendMessage(Translation.getString("Message.No.Allowed", player));
            return true;
        }

        if (MyPetApi.getPlayerManager().isMyPetPlayer(player)) {
            final MyPetPlayer owner = MyPetApi.getPlayerManager().getMyPetPlayer(player);

            if (args.length > 0 && args[0].equalsIgnoreCase("store")) {
                if (owner.isOnline() && owner.hasMyPet()) {
                    MyPetApi.getRepository().getMyPets(owner, new RepositoryCallback<List<StoredMyPet>>() {
                        @Override
                        public void callback(List<StoredMyPet> pets) {
                            MyPet myPet = owner.getMyPet();
                            String worldGroup = myPet.getWorldGroup();

                            int inactivePetCount = getInactivePetCount(pets, worldGroup) - 1; // -1 for active pet
                            int maxPetCount = getMaxPetCount(owner.getPlayer());

                            if (inactivePetCount >= maxPetCount) {
                                sender.sendMessage(Util.formatText(Translation.getString("Message.Command.Switch.Limit", owner), maxPetCount));
                                return;
                            }
                            if (MyPetApi.getMyPetManager().deactivateMyPet(owner, true)) {
                                owner.setMyPetForWorldGroup(worldGroup, null);
                                sender.sendMessage(Util.formatText(Translation.getString("Message.Command.Switch.Success", owner), myPet.getPetName()));
                            }
                        }
                    });
                } else {
                    player.sendMessage(Translation.getString("Message.Command.Switch.NoPet", player));
                }
                return true;
            }


            MyPetApi.getRepository().getMyPets(owner, new RepositoryCallback<List<StoredMyPet>>() {
                @Override
                public void callback(List<StoredMyPet> pets) {
                    if (pets.size() - (owner.hasMyPet() ? 1 : 0) == 0) {
                        owner.sendMessage(Translation.getString("Message.Command.Switch.NoStoredPets", owner));
                        return;
                    }
                    if (owner.isOnline()) {
                        String worldGroup = WorldGroup.getGroupByWorld(owner.getPlayer().getWorld().getName()).getName();
                        int inactivePetCount = getInactivePetCount(pets, worldGroup);
                        int maxPetCount = getMaxPetCount(owner.getPlayer());

                        if (owner.hasMyPet()) {
                            inactivePetCount--;
                            if (!Permissions.hasLegacy(owner, "MyPet.command.switch.bypass")) {
                                if (inactivePetCount > maxPetCount) {
                                    sender.sendMessage(Util.formatText(Translation.getString("Message.Command.Switch.Limit", owner), maxPetCount));
                                    return;
                                }
                            }
                        }

                        String stats = "(" + inactivePetCount + "/" + maxPetCount + ")";

                        final MyPetSelectionGui gui = new MyPetSelectionGui(owner, stats + " " + Translation.getString("Message.SelectMyPet", owner));
                        gui.open(pets, new RepositoryCallback<StoredMyPet>() {
                            @Override
                            public void callback(StoredMyPet storedMyPet) {
                                Optional<MyPet> activePet = MyPetApi.getMyPetManager().activateMyPet(storedMyPet);
                                if (activePet.isPresent() && owner.isOnline()) {
                                    Player player = owner.getPlayer();
                                    activePet.get().getOwner().sendMessage(Util.formatText(Translation.getString("Message.Npc.ChosenPet", owner), activePet.get().getPetName()));
                                    WorldGroup wg = WorldGroup.getGroupByWorld(player.getWorld().getName());
                                    owner.setMyPetForWorldGroup(wg.getName(), activePet.get().getUUID());

                                    switch (activePet.get().createEntity()) {
                                        case Canceled:
                                            owner.sendMessage(Util.formatText(Translation.getString("Message.Spawn.Prevent", owner), activePet.get().getPetName()));
                                            break;
                                        case NoSpace:
                                            owner.sendMessage(Util.formatText(Translation.getString("Message.Spawn.NoSpace", owner), activePet.get().getPetName()));
                                            break;
                                        case NotAllowed:
                                            owner.sendMessage(Translation.getString("Message.No.AllowedHere", owner).replace("%petname%", activePet.get().getPetName()));
                                            break;
                                        case Dead:
                                            owner.sendMessage(Translation.getString("Message.Spawn.Respawn.In", owner).replace("%petname%", activePet.get().getPetName()).replace("%time%", "" + activePet.get().getRespawnTime()));
                                            break;
                                        case Spectator:
                                            sender.sendMessage(Util.formatText(Translation.getString("Message.Spawn.Spectator", owner), activePet.get().getPetName()));
                                            break;
                                    }
                                }
                            }
                        });
                    }
                }
            });
        } else {
            sender.sendMessage(Translation.getString("Message.No.HasPet", player));
        }
        return true;
    }

    private int getMaxPetCount(Player p) {
        int maxPetCount = 0;
        if (p.isOp()) {
            maxPetCount = Configuration.Misc.MAX_STORED_PET_COUNT;
        } else {
            for (int i = Configuration.Misc.MAX_STORED_PET_COUNT; i > 0; i--) {
                if (Permissions.hasLegacy(p, "MyPet.command.switch.limit.", i)) {
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
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        if (MyPetApi.getMyPetManager().hasActiveMyPet((Player) commandSender)) {
            return storeList;
        }
        return CommandAdmin.EMPTY_LIST;
    }
}