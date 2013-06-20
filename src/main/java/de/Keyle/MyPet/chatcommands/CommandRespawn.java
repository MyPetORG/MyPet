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
import de.Keyle.MyPet.util.*;
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
                    myPet.sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLocales.getString("Message.CantUse", petOwner)));
                    return true;
                }

                double costs = myPet.getRespawnTime() * MyPetConfiguration.RESPAWN_COSTS_FACTOR + MyPetConfiguration.RESPAWN_COSTS_FIXED;
                if (args.length == 0)
                {
                    if (myPet.getStatus() != PetState.Dead)
                    {
                        myPet.sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLocales.getString("Message.RespawnShow", petOwner).replace("%costs%", "-").replace("%petname%", myPet.getPetName()).replace("%color%", (myPet.getOwner().hasAutoRespawnEnabled() ? ChatColor.GREEN : ChatColor.RED).toString())));
                    }
                    else
                    {
                        myPet.sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLocales.getString("Message.RespawnShow", petOwner).replace("%costs%", costs + " " + MyPetEconomy.getEconomy().currencyNameSingular()).replace("%petname%", myPet.getPetName())));
                    }
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
                                myPet.sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLocales.getString("Message.RespawnAutoMin", petOwner).replace("%time%", args[1])));
                            }
                        }
                        else
                        {
                            myPet.getOwner().setAutoRespawnEnabled(!myPet.getOwner().hasAutoRespawnEnabled());
                            myPet.sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLocales.getString("Message.RespawnAuto", petOwner).replace("%status%", myPet.getOwner().hasAutoRespawnEnabled() ? MyPetLocales.getString("Name.Enabled", petOwner) : MyPetLocales.getString("Name.Disabled", petOwner))));
                        }
                    }
                    else if (args[0].equalsIgnoreCase("pay"))
                    {
                        if (MyPetEconomy.canPay(myPet.getOwner(), costs))
                        {
                            MyPetEconomy.pay(myPet.getOwner(), costs);
                            myPet.sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLocales.getString("Message.RespawnPaid", petOwner).replace("%costs%", costs + " " + MyPetEconomy.getEconomy().currencyNameSingular()).replace("%petname%", myPet.getPetName())));
                            myPet.setRespawnTime(1);
                        }
                        else
                        {
                            myPet.sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLocales.getString("Message.RespawnNoMoney", petOwner).replace("%costs%", costs + " " + MyPetEconomy.getEconomy().currencyNameSingular()).replace("%petname%", myPet.getPetName())));
                        }
                    }
                    else if (args[0].equalsIgnoreCase("show"))
                    {
                        if (myPet.getStatus() != PetState.Dead)
                        {
                            myPet.sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLocales.getString("Message.RespawnShow", petOwner).replace("%costs%", "-").replace("%petname%", myPet.getPetName()).replace("%color%", (myPet.getOwner().hasAutoRespawnEnabled() ? ChatColor.GREEN : ChatColor.RED).toString())));
                        }
                        else
                        {
                            myPet.sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLocales.getString("Message.RespawnShow", petOwner).replace("%costs%", costs + " " + MyPetEconomy.getEconomy().currencyNameSingular()).replace("%petname%", myPet.getPetName())));
                        }
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