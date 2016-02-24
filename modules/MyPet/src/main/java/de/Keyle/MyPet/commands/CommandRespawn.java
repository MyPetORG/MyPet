/*
 * This file is part of mypet
 *
 * Copyright (C) 2011-2016 Keyle
 * mypet is licensed under the GNU Lesser General Public License.
 *
 * mypet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mypet is distributed in the hope that it will be useful,
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
import de.Keyle.MyPet.api.entity.ActiveMyPet;
import de.Keyle.MyPet.api.entity.ActiveMyPet.PetState;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.util.hooks.EconomyHook;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CommandRespawn implements CommandExecutor, TabCompleter {
    private static List<String> optionsList = new ArrayList<>();

    static {
        optionsList.add("show");
        optionsList.add("pay");
        optionsList.add("auto");
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player petOwner = (Player) sender;
            if (MyPetApi.getMyPetList().hasActiveMyPet(petOwner)) {
                ActiveMyPet myPet = MyPetApi.getMyPetList().getMyPet(petOwner);
                if (!EconomyHook.canUseEconomy() || !Permissions.has(petOwner, "MyPet.user.command.respawn")) {
                    myPet.getOwner().sendMessage(Translation.getString("Message.No.CanUse", petOwner));
                    return true;
                }

                double costs = myPet.getRespawnTime() * Configuration.Respawn.COSTS_FACTOR + Configuration.Respawn.COSTS_FIXED;
                if (args.length == 0) {
                    String costsString;
                    if (myPet.getStatus() != PetState.Dead) {
                        costsString = "-";
                    } else {
                        costsString = costs + " " + EconomyHook.getEconomy().currencyNameSingular();
                    }
                    myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Command.Respawn.Show", petOwner), myPet.getPetName(), costsString, (myPet.getOwner().hasAutoRespawnEnabled() ? ChatColor.GREEN : ChatColor.RED).toString()));
                    return true;
                }

                if (args.length >= 1) {
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
                            if (EconomyHook.canPay(myPet.getOwner(), costs)) {
                                EconomyHook.pay(myPet.getOwner(), costs);
                                myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Command.Respawn.Paid", petOwner), myPet.getPetName(), costs + " " + EconomyHook.getEconomy().currencyNameSingular()));
                                myPet.setRespawnTime(1);
                            } else {
                                myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Command.Respawn.NoMoney", petOwner), myPet.getPetName(), costs + " " + EconomyHook.getEconomy().currencyNameSingular()));
                            }
                        } else {
                            myPet.getOwner().sendMessage(Translation.getString("Message.No.CanUse", petOwner));
                        }
                    } else if (args[0].equalsIgnoreCase("show")) {
                        String costsString;
                        if (myPet.getStatus() != PetState.Dead) {
                            costsString = "-";
                        } else {
                            costsString = costs + " " + EconomyHook.getEconomy().currencyNameSingular();
                        }
                        myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Command.Respawn.Show", petOwner), myPet.getPetName(), costsString, (myPet.getOwner().hasAutoRespawnEnabled() ? ChatColor.GREEN : ChatColor.RED).toString()));
                    }
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
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        if (strings.length == 1) {
            return optionsList;
        }
        return CommandAdmin.EMPTY_LIST;
    }
}