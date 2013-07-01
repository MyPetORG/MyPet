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
import de.Keyle.MyPet.skill.ISkillActive;
import de.Keyle.MyPet.skill.skills.implementation.Beacon;
import de.Keyle.MyPet.util.MyPetBukkitUtil;
import de.Keyle.MyPet.util.MyPetPermissions;
import de.Keyle.MyPet.util.locale.MyPetLocales;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CommandBeacon implements CommandExecutor, TabCompleter
{
    private static List<String> emptyList = new ArrayList<String>();

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (sender instanceof Player)
        {
            Player player = (Player) sender;
            if (args.length == 1 && !args[0].equalsIgnoreCase("stop") && MyPetPermissions.has(player, "MyPet.admin", false))
            {
                Player petOwner = Bukkit.getServer().getPlayer(args[0]);

                if (petOwner == null || !petOwner.isOnline())
                {
                    sender.sendMessage(MyPetBukkitUtil.setColors(MyPetLocales.getString("Message.PlayerNotOnline", player)));
                    return true;
                }
                else if (!MyPetList.hasMyPet(petOwner))
                {
                    sender.sendMessage(MyPetBukkitUtil.setColors(MyPetLocales.getString("Message.UserDontHavePet", player).replace("%playername%", petOwner.getName())));
                    return true;
                }

                MyPet myPet = MyPetList.getMyPet(petOwner);
                if (myPet.getSkills().isSkillActive("Beacon"))
                {
                    ((Beacon) myPet.getSkills().getSkill("Beacon")).activate(player);
                }
                return true;
            }
            if (MyPetList.hasMyPet(player))
            {
                MyPet myPet = MyPetList.getMyPet(player);
                if (!MyPetPermissions.hasExtended(player, "MyPet.user.extended.Beacon", true))
                {
                    myPet.sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLocales.getString("Message.CantUse", player)));
                    return true;
                }
                if (myPet.getStatus() == PetState.Despawned)
                {
                    sender.sendMessage(MyPetBukkitUtil.setColors(MyPetLocales.getString("Message.CallFirst", player)).replace("%petname%", myPet.getPetName()));
                    return true;
                }
                if (myPet.getStatus() == PetState.Dead)
                {
                    sender.sendMessage(MyPetBukkitUtil.setColors(MyPetLocales.getString("Message.CallWhenDead", player)).replace("%petname%", myPet.getPetName()).replace("%time%", "" + myPet.getRespawnTime()));
                    return true;
                }
                if (args.length >= 1 && args[0].equalsIgnoreCase("stop"))
                {
                    ((Beacon) myPet.getSkills().getSkill("Beacon")).stop(true);
                    sender.sendMessage(MyPetBukkitUtil.setColors(MyPetLocales.getString("Message.Skill.Beacon.Stop", player)).replace("%petname%", myPet.getPetName()));
                    return true;
                }
                if (player.getGameMode() == GameMode.CREATIVE && !MyPetPermissions.has(player, "MyPet.admin", false))
                {
                    sender.sendMessage(MyPetBukkitUtil.setColors(MyPetLocales.getString("Message.Skill.Beacon.Creative", player)).replace("%petname%", myPet.getPetName()));
                    return true;
                }
                if (myPet.getSkills().hasSkill("Beacon"))
                {
                    ((ISkillActive) myPet.getSkills().getSkill("Beacon")).activate();
                }
            }
            else
            {
                sender.sendMessage(MyPetBukkitUtil.setColors(MyPetLocales.getString("Message.DontHavePet", player)));
            }
            return true;
        }
        sender.sendMessage("You can't use this command from server console!");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings)
    {
        if (strings.length == 1 && MyPetPermissions.has((Player) commandSender, "MyPet.admin", false))
        {
            return null;
        }
        return emptyList;
    }
}