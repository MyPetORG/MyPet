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
import de.Keyle.MyPet.api.entity.MyPet.PetState;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandRespawn implements CommandTabCompleter {
    private static List<String> optionsList = new ArrayList<>();

    static {
        optionsList.add("show");
        optionsList.add("pay");
        optionsList.add("auto");
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player petOwner = (Player) sender;
            if (WorldGroup.getGroupByWorld(petOwner.getWorld()).isDisabled()) {
                petOwner.sendMessage(Translation.getString("Message.No.AllowedHere", petOwner));
                return true;
            }
            if (MyPetApi.getMyPetManager().hasActiveMyPet(petOwner)) {
                MyPet myPet = MyPetApi.getMyPetManager().getMyPet(petOwner);
                if (!MyPetApi.getHookHelper().isEconomyEnabled() || !Permissions.has(petOwner, "MyPet.command.respawn")) {
                    myPet.getOwner().sendMessage(Translation.getString("Message.No.CanUse", petOwner));
                    return true;
                }

                double costs = myPet.getRespawnTime() * Configuration.Respawn.COSTS_FACTOR + Configuration.Respawn.COSTS_FIXED;
                if (args.length == 0) {
                    String costsString;
                    if (myPet.getStatus() != PetState.Dead) {
                        costsString = "-";
                    } else {
                        costsString = costs + " " + MyPetApi.getHookHelper().getEconomy().currencyNameSingular();
                    }
                    myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Command.Respawn.Show", petOwner), myPet.getPetName(), costsString, (myPet.getOwner().hasAutoRespawnEnabled() ? ChatColor.GREEN : ChatColor.RED).toString()));
                    myPet.getOwner().sendMessage(Translation.getString("Message.Command.Respawn.Show.Pay", petOwner));
                    return true;
                }

                if (args[0].equalsIgnoreCase("AUTO")) {
                    if (args.length >= 2) {
                        if (Util.isInt(args[1])) {
                            myPet.getOwner().setAutoRespawnMin(Integer.parseInt(args[1]));
                            myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Command.Respawn.AutoMin", petOwner), args[1]));
                        }
                    } else {
                        myPet.getOwner().setAutoRespawnEnabled(!myPet.getOwner().hasAutoRespawnEnabled());
                        myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Command.Respawn.Auto", petOwner), Translation.getString("Name." + (myPet.getOwner().hasAutoRespawnEnabled() ? "Enabled" : "Disabled"), petOwner)));
                    }
                } else if (args[0].equalsIgnoreCase("pay")) {
                    if (myPet.getStatus() == PetState.Dead) {
                        if (MyPetApi.getHookHelper().getEconomy().canPay(myPet.getOwner(), costs)) {
                            MyPetApi.getHookHelper().getEconomy().pay(myPet.getOwner(), costs);
                            myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Command.Respawn.Paid", petOwner), myPet.getPetName(), costs + " " + MyPetApi.getHookHelper().getEconomy().currencyNameSingular()));
                            myPet.setRespawnTime(1);
                        } else {
                            myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Command.Respawn.NoMoney", petOwner), myPet.getPetName(), costs + " " + MyPetApi.getHookHelper().getEconomy().currencyNameSingular()));
                        }
                    } else {
                        myPet.getOwner().sendMessage(Translation.getString("Message.No.CanUse", petOwner));
                    }
                } else if (args[0].equalsIgnoreCase("show")) {
                    String costsString;
                    if (myPet.getStatus() != PetState.Dead) {
                        costsString = "-";
                    } else {
                        costsString = costs + " " + MyPetApi.getHookHelper().getEconomy().currencyNameSingular();
                    }
                    myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Command.Respawn.Show", petOwner), myPet.getPetName(), costsString, (myPet.getOwner().hasAutoRespawnEnabled() ? ChatColor.GREEN : ChatColor.RED).toString()));
                    myPet.getOwner().sendMessage(Translation.getString("Message.Command.Respawn.Show.Pay", petOwner));
                }
            } else {
                sender.sendMessage(Translation.getString("Message.No.HasPet", petOwner));
            }
            return true;
        }
        sender.sendMessage("You can't use this command from server console!");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] strings) {
        if (sender instanceof Player) {
            if (strings.length == 1) {
                return filterTabCompletionResults(optionsList, strings[0]);
            }
        }
        return Collections.emptyList();
    }
}