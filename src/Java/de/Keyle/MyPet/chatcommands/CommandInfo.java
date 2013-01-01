/*
 * Copyright (C) 2011-2013 Keyle
 *
 * This file is part of MyPet
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
 * along with MyPet. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.chatcommands;

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.util.*;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandInfo implements CommandExecutor
{
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (sender instanceof Player)
        {
            Player player = (Player) sender;
            String playerName = sender.getName();
            if (args != null && args.length > 0 && MyPetPermissions.has(player, "MyPet.admin"))
            {
                playerName = args[0];
            }

            if (MyPetList.hasMyPet(playerName))
            {
                MyPet myPet = MyPetList.getMyPet(playerName);
                String msg;
                if (myPet.getHealth() > myPet.getMaxHealth() / 3 * 2)
                {
                    msg = "" + ChatColor.GREEN + myPet.getHealth() + ChatColor.WHITE + "/" + myPet.getMaxHealth();
                }
                else if (myPet.getHealth() > myPet.getMaxHealth() / 3)
                {
                    msg = "" + ChatColor.YELLOW + myPet.getHealth() + ChatColor.WHITE + "/" + myPet.getMaxHealth();
                }
                else
                {
                    msg = "" + ChatColor.RED + myPet.getHealth() + ChatColor.WHITE + "/" + myPet.getMaxHealth();
                }


                player.sendMessage(MyPetUtil.setColors("%aqua%%petname%%white%:").replace("%petname%", myPet.petName));
                player.sendMessage(MyPetUtil.setColors("   %N_HP%: %hp%").replace("%petname%", myPet.petName).replace("%hp%", msg).replace("%N_HP%", MyPetLanguage.getString("Name_HP")));

                if (!myPet.isPassiv())
                {
                    int damage = MyPet.getStartDamage(myPet.getClass()) + (myPet.getSkills().hasSkill("Damage") ? myPet.getSkills().getSkillLevel("Damage") : 0);
                    player.sendMessage(MyPetUtil.setColors("   %N_Damage%: %dmg%").replace("%petname%", myPet.petName).replace("%dmg%", "" + damage).replace("%N_Damage%", MyPetLanguage.getString("Name_Damage")));
                }
                if (MyPetConfig.hungerSystem)
                {
                    player.sendMessage(MyPetUtil.setColors("   %N_Hunger%: %hunger%").replace("%hunger%", "" + myPet.getHungerValue()).replace("%N_Hunger%", MyPetLanguage.getString("Name_Hunger")));
                }
                if (MyPetConfig.levelSystem)
                {
                    int lvl = myPet.getExperience().getLevel();
                    double exp = myPet.getExperience().getCurrentExp();
                    double reqEXP = myPet.getExperience().getRequiredExp();
                    player.sendMessage(MyPetUtil.setColors("   %N_Level%: %lvl%").replace("%lvl%", "" + lvl).replace("%N_Level%", MyPetLanguage.getString("Name_Level")));
                    player.sendMessage(MyPetUtil.setColors("   %N_Exp%: %exp%/%reqexp%").replace("%exp%", String.format("%1.2f", exp)).replace("%reqexp%", String.format("%1.2f", reqEXP)).replace("%N_Exp%", MyPetLanguage.getString("Name_Exp")));
                }
                if (!playerName.equalsIgnoreCase(sender.getName()))
                {
                    player.sendMessage(MyPetUtil.setColors("   %N_Owner%: %owner%").replace("%owner%", playerName).replace("%N_Owner%", MyPetLanguage.getString("Name_Owner")));
                }
                return true;
            }
            else
            {
                if (args != null && args.length > 0)
                {
                    sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_UserDontHavePet").replace("%playername%", playerName)));
                }
                else
                {
                    sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_DontHavePet")));
                }
            }
        }
        return true;
    }
}