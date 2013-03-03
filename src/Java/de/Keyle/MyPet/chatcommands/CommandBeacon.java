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
import de.Keyle.MyPet.skill.skills.Beacon;
import de.Keyle.MyPet.util.MyPetLanguage;
import de.Keyle.MyPet.util.MyPetList;
import de.Keyle.MyPet.util.MyPetPermissions;
import de.Keyle.MyPet.util.MyPetUtil;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandBeacon implements CommandExecutor
{
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (sender instanceof Player)
        {
            Player player = (Player) sender;
            if (args.length == 1 && MyPetPermissions.has(player, "MyPet.admin", false))
            {
                Player petOwner = MyPetUtil.getServer().getPlayer(args[0]);

                if (petOwner == null || !petOwner.isOnline())
                {
                    sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_PlayerNotOnline")));
                    return true;
                }
                else if (!MyPetList.hasMyPet(petOwner))
                {
                    sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_UserDontHavePet").replace("%playername%", petOwner.getName())));
                    return true;
                }

                MyPet myPet = MyPetList.getMyPet(petOwner);
                if (myPet.getSkills().isSkillActive("Beacon"))
                {
                    ((Beacon) myPet.getSkills().getSkill("Beacon")).activate(player);
                }
            }
            else if (MyPetList.hasMyPet(player))
            {
                MyPet myPet = MyPetList.getMyPet(player);
                if (!MyPetPermissions.hasExtended(player, "MyPet.user.extended.Beacon"))
                {
                    myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_CantUse")));
                    return true;
                }
                if (myPet.getStatus() == PetState.Despawned)
                {
                    sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_CallFirst")).replace("%petname%", myPet.petName));
                    return true;
                }
                if (myPet.getStatus() == PetState.Dead)
                {
                    sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_CallDead")).replace("%petname%", myPet.petName).replace("%time%", "" + myPet.respawnTime));
                    return true;
                }
                if (args.length >= 1 && args[0].equalsIgnoreCase("stop"))
                {
                    ((Beacon) myPet.getSkills().getSkill("Beacon")).stop(true);
                    return true;
                }
                if (player.getGameMode() == GameMode.CREATIVE && !MyPetPermissions.has(player, "MyPet.admin", false))
                {
                    sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_BeaconCreative")).replace("%petname%", myPet.petName));
                    return true;
                }
                if (myPet.getSkills().hasSkill("Beacon"))
                {
                    myPet.getSkills().getSkill("Beacon").activate();
                }
            }
            else
            {
                sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_DontHavePet")));
            }
            return true;
        }
        sender.sendMessage("You can't use this command from server console!");
        return true;
    }
}