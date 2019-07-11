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
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.commands.CommandTabCompleter;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.repository.RepositoryCallback;
import de.Keyle.MyPet.api.util.chat.FancyMessage;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CommandList implements CommandTabCompleter {
    public boolean onCommand(final CommandSender sender, Command command, String label, String[] args) {
        final String lang;
        if (sender instanceof Player) {
            lang = MyPetApi.getPlatformHelper().getPlayerLanguage((Player) sender);
        } else {
            lang = "en";
        }

        final Player petOwner;
        if (args.length <= 0) {
            if (sender instanceof Player) {
                petOwner = (Player) sender;
            } else {
                sender.sendMessage("You can't use this command from server console!");
                return true;
            }
        } else {
            if (sender instanceof Player) {
                if (Permissions.has((Player) sender, "MyPet.admin", false)) {
                    petOwner = Bukkit.getPlayer(args[0]);
                } else {
                    petOwner = (Player) sender;
                }
            } else {
                sender.sendMessage("You can't use this command from server console!");
                return true;
            }
        }

        if (petOwner == null || !petOwner.isOnline()) {
            sender.sendMessage(Translation.getString("Message.No.PlayerOnline", lang));
            return true;
        }
        final MyPetPlayer owner;
        if (MyPetApi.getPlayerManager().isMyPetPlayer(petOwner)) {
            owner = MyPetApi.getPlayerManager().getMyPetPlayer(petOwner);
        } else {
            sender.sendMessage(Util.formatText(Translation.getString("Message.No.UserHavePet", lang), petOwner.getName()));
            return true;
        }


        if (owner != null) {
            MyPetApi.getRepository().getMyPets(owner, new RepositoryCallback<List<StoredMyPet>>() {
                @Override
                public void callback(List<StoredMyPet> value) {
                    if (petOwner == sender) {
                        sender.sendMessage(Util.formatText(Translation.getString("Message.Command.List.Yours", lang), owner.getName()));
                    } else {
                        sender.sendMessage(Util.formatText(Translation.getString("Message.Command.List.Player", lang), owner.getName()));
                    }
                    boolean doComma = false;
                    FancyMessage message = new FancyMessage("");
                    for (StoredMyPet mypet : value) {

                        if (doComma) {
                            message.then(", ");
                        }
                        message.then(mypet.getPetName())
                                .color(ChatColor.AQUA)
                                .itemTooltip(Util.myPetToItemTooltip(mypet, lang));
                        if (!doComma) {
                            doComma = true;
                        }
                    }
                    MyPetApi.getPlatformHelper().sendMessageRaw((Player) sender, message.toJSONString());
                }
            });
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] strings) {
        if (sender instanceof Player && strings.length == 1 && Permissions.has((Player) sender, "MyPet.admin", false)) {
            return null;
        }
        return Collections.emptyList();
    }
}