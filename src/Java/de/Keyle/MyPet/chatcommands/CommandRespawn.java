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
import de.Keyle.MyPet.util.*;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandRespawn implements CommandExecutor
{
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (sender instanceof Player)
        {
            Player petOwner = (Player) sender;
            if (MyPetList.hasActiveMyPets(petOwner))
            {
                for (MyPet myPet : MyPetList.getActiveMyPets(petOwner))
                {

                    double costs = myPet.respawnTime * MyPetConfiguration.RESPAWN_COSTS_FACTOR + MyPetConfiguration.RESPAWN_COSTS_FIXED;

                    if (!MyPetEconomy.canUseEconomy())
                    {
                        return true;
                    }
                    if (!MyPetPermissions.has(petOwner, "MyPet.user.respawn"))
                    {
                        myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_CantUse")));
                        return true;
                    }

                    if (args.length == 0)
                    {
                        if (myPet.getStatus() != PetState.Dead)
                        {
                            myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_RespawnShow").replace("%costs%", "-").replace("%petname%", myPet.petName).replace("%color%", (myPet.getOwner().hasAutoRespawnEnabled() ? ChatColor.GREEN : ChatColor.RED).toString())));
                        }
                        else
                        {
                            myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_RespawnShow").replace("%costs%", costs + " " + MyPetEconomy.getEconomy().currencyNameSingular()).replace("%petname%", myPet.petName)));
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
                                    myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_RespawnAutoMin").replace("%time%", args[1])));
                                }
                            }
                            else
                            {
                                myPet.getOwner().setAutoRespawnEnabled(!myPet.getOwner().hasAutoRespawnEnabled());
                                myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_RespawnAuto").replace("%status%", myPet.getOwner().hasAutoRespawnEnabled() ? MyPetLanguage.getString("Name_Enabled") : MyPetLanguage.getString("Name_Disabled"))));
                            }
                        }
                        else if (args[0].equalsIgnoreCase("pay"))
                        {
                            if (MyPetEconomy.canPay(myPet.getOwner(), costs))
                            {
                                MyPetEconomy.pay(myPet.getOwner(), costs);
                                myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_RespawnPaid").replace("%costs%", costs + " " + MyPetEconomy.getEconomy().currencyNameSingular()).replace("%petname%", myPet.petName)));
                                myPet.respawnTime = 1;
                            }
                            else
                            {
                                myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_RespawnNoMoney").replace("%costs%", costs + " " + MyPetEconomy.getEconomy().currencyNameSingular()).replace("%petname%", myPet.petName)));
                            }
                        }
                    }
                }
            }
            return true;
        }
        sender.sendMessage("You can't use this command from server console!");
        return true;
    }
}