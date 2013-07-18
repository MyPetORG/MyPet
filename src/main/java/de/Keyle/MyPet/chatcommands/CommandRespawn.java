/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2013 Keyle
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

package de.Keyle.MyPet.chatcommands;

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.entity.types.MyPetList;
import de.Keyle.MyPet.util.MyPetConfiguration;
import de.Keyle.MyPet.util.MyPetEconomy;
import de.Keyle.MyPet.util.MyPetPermissions;
import de.Keyle.MyPet.util.MyPetUtil;
import de.Keyle.MyPet.util.locale.MyPetLocales;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CommandRespawn implements CommandExecutor, TabCompleter
{
    private static List<String> optionsList = new ArrayList<String>();
    private static List<String> emptyList = new ArrayList<String>();

    static
    {
        optionsList.add("show");
        optionsList.add("pay");
        optionsList.add("auto");
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (!MyPetEconomy.canUseEconomy())
        {
            return true;
        }
        if (sender instanceof Player)
        {
            Player petOwner = (Player) sender;
            if (MyPetList.hasMyPet(petOwner))
            {
                MyPet myPet = MyPetList.getMyPet(petOwner);
                if (!MyPetPermissions.has(petOwner, "MyPet.user.respawn"))
                {
                    myPet.sendMessageToOwner(MyPetLocales.getString("Message.CantUse", petOwner));
                    return true;
                }

                double costs = myPet.getRespawnTime() * MyPetConfiguration.RESPAWN_COSTS_FACTOR + MyPetConfiguration.RESPAWN_COSTS_FIXED;
                if (args.length == 0)
                {
                    String costsString;
                    if (myPet.getStatus() != PetState.Dead)
                    {
                        costsString = "-";
                    }
                    else
                    {
                        costsString = costs + " " + MyPetEconomy.getEconomy().currencyNameSingular();
                    }
                    myPet.sendMessageToOwner(MyPetUtil.formatText(MyPetLocales.getString("Message.RespawnShow", petOwner), myPet.getPetName(), costsString, (myPet.getOwner().hasAutoRespawnEnabled() ? ChatColor.GREEN : ChatColor.RED).toString()));
                    return true;
                }

                if (args.length >= 1)
                {
                    if (args[0].equalsIgnoreCase("AUTO"))
                    {
                        if (args.length >= 2)
                        {
                            if (MyPetUtil.isInt(args[1]))
                            {
                                myPet.getOwner().setAutoRespawnMin(Integer.parseInt(args[1]));
                                myPet.sendMessageToOwner(MyPetUtil.formatText(MyPetLocales.getString("Message.RespawnAutoMin", petOwner), args[1]));
                            }
                        }
                        else
                        {
                            myPet.getOwner().setAutoRespawnEnabled(!myPet.getOwner().hasAutoRespawnEnabled());
                            myPet.sendMessageToOwner(MyPetUtil.formatText(MyPetLocales.getString("Message.RespawnAuto", petOwner), MyPetLocales.getString("Name." + (myPet.getOwner().hasAutoRespawnEnabled() ? "Enabled" : "Disabled"), petOwner)));
                        }
                    }
                    else if (args[0].equalsIgnoreCase("pay"))
                    {
                        if (MyPetEconomy.canPay(myPet.getOwner(), costs))
                        {
                            MyPetEconomy.pay(myPet.getOwner(), costs);
                            myPet.sendMessageToOwner(MyPetUtil.formatText(MyPetLocales.getString("Message.RespawnPaid", petOwner), myPet.getPetName(), costs + " " + MyPetEconomy.getEconomy().currencyNameSingular()));
                            myPet.setRespawnTime(1);
                        }
                        else
                        {
                            myPet.sendMessageToOwner(MyPetUtil.formatText(MyPetLocales.getString("Message.RespawnNoMoney", petOwner), myPet.getPetName(), costs + " " + MyPetEconomy.getEconomy().currencyNameSingular()));
                        }
                    }
                    else if (args[0].equalsIgnoreCase("show"))
                    {
                        String costsString;
                        if (myPet.getStatus() != PetState.Dead)
                        {
                            costsString = "-";
                        }
                        else
                        {
                            costsString = costs + " " + MyPetEconomy.getEconomy().currencyNameSingular();
                        }
                        myPet.sendMessageToOwner(MyPetUtil.formatText(MyPetLocales.getString("Message.RespawnShow", petOwner), myPet.getPetName(), costsString, (myPet.getOwner().hasAutoRespawnEnabled() ? ChatColor.GREEN : ChatColor.RED).toString()));
                    }
                }
            }
            return true;
        }
        sender.sendMessage("You can't use this command from server console!");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings)
    {
        if (strings.length == 1)
        {
            return optionsList;
        }
        return emptyList;
    }
}