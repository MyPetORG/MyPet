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
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CommandCall implements CommandTabCompleter {
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player petOwner) {
            if (WorldGroup.getGroupByWorld(petOwner.getWorld()).isDisabled()) {
                sender.sendMessage(Translation.getComponent("Message.No.AllowedHere", petOwner));
                return true;
            }
            if (MyPetApi.getMyPetManager().hasActiveMyPet(petOwner)) {
                MyPet myPet = MyPetApi.getMyPetManager().getMyPet(petOwner);

                if (myPet.getEntity().isPresent()) {
                    //Only let it respawn if it actually was there before
                    myPet.removePet(true);
                }

                switch (myPet.createEntity()) {
                    case Success:
                        sender.sendMessage(MessageUtil.success(
                                Util.formatTranslation(
                                        "Message.Command.Call.Success",
                                        petOwner,
                                        MessageUtil.petName(myPet.getPetName())
                                ), false
                        ));
                        break;
                    case Canceled:
                        sender.sendMessage(MessageUtil.error(
                                Util.formatTranslation(
                                        "Message.Spawn.Prevent",
                                        petOwner,
                                        MessageUtil.petName(myPet.getPetName())
                                ), false
                        ));
                        break;
                    case NoSpace:
                        sender.sendMessage(MessageUtil.error(
                                Util.formatTranslation(
                                        "Message.Spawn.NoSpace",
                                        petOwner,
                                        MessageUtil.petName(myPet.getPetName())
                                ), false
                        ));
                        break;
                    case NotAllowed:
                        sender.sendMessage(MessageUtil.error(
                                Util.formatTranslation(
                                        "Message.No.AllowedHere",
                                        petOwner,
                                        MessageUtil.petName(myPet.getPetName())
                                ), false
                        ));
                        break;
                    case Dead:
                        if (Configuration.Respawn.DISABLE_AUTO_RESPAWN) {
                            sender.sendMessage(MessageUtil.info(
                                    Util.formatTranslation(
                                            "Message.Call.Dead",
                                            petOwner,
                                            MessageUtil.petName(myPet.getPetName())
                                    ), false
                            ));
                        } else {
                            sender.sendMessage(MessageUtil.info(
                                    Util.formatTranslation(
                                            "Message.Call.Dead.Respawn",
                                            petOwner,
                                            MessageUtil.petName(myPet.getPetName()),
                                            myPet.getRespawnTime()
                                    ), false
                            ));
                        }
                        break;
                    case Flying:
                        sender.sendMessage(MessageUtil.error(
                                Util.formatTranslation(
                                        "Message.Spawn.Flying",
                                        petOwner,
                                        MessageUtil.petName(myPet.getPetName())
                                ), false
                        ));
                        break;
                    case Spectator:
                        sender.sendMessage(MessageUtil.error(
                                Util.formatTranslation(
                                        "Message.Spawn.Spectator",
                                        petOwner,
                                        MessageUtil.petName(myPet.getPetName())
                                ), false
                        ));
                        break;
                }
                return true;
            } else {
                sender.sendMessage(Translation.getComponent("Message.No.HasPet", petOwner));
            }
            return true;
        }
        sender.sendMessage("You can't use this command from server console!");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] strings) {
        return Collections.emptyList();
    }

    @Override
    public String getHelpTranslationKey() {
        return "Message.Command.Help.Call";
    }

    @Override
    public String getHelpCommand() {
        return "/petcall";
    }

    @Override
    public boolean isVisibleTo(Player player) {
        return MyPetApi.getMyPetManager().hasActiveMyPet(player);
    }

    @Override
    public int getHelpOrder() {
        return 60;
    }
}
