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

package de.Keyle.MyPet.commands;

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.entity.types.MyPetList;
import de.Keyle.MyPet.skill.skills.ISkillActive;
import de.Keyle.MyPet.util.Permissions;
import de.Keyle.MyPet.util.Util;
import de.Keyle.MyPet.util.locale.Locales;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandPickup implements CommandExecutor
{
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (sender instanceof Player)
        {
            Player owner = (Player) sender;
            if (MyPetList.hasMyPet(owner))
            {
                MyPet myPet = MyPetList.getMyPet(owner);

                if (!Permissions.hasExtended(myPet.getOwner().getPlayer(), "MyPet.user.extended.Pickup"))
                {
                    sender.sendMessage(Locales.getString("Message.NotAllowed", owner));
                    return true;
                }
                else if (myPet.getStatus() == PetState.Despawned)
                {
                    sender.sendMessage(Util.formatText(Locales.getString("Message.CallFirst", owner), myPet.getPetName()));
                    return true;
                }
                else if (myPet.getStatus() == PetState.Dead)
                {
                    sender.sendMessage(Util.formatText(Locales.getString("Message.CallWhenDead", owner), myPet.getPetName(), myPet.getRespawnTime()));
                    return true;
                }
                if (myPet.getSkills().hasSkill("Pickup"))
                {
                    ((ISkillActive) myPet.getSkills().getSkill("Pickup")).activate();
                }
            }
            else
            {
                sender.sendMessage(Locales.getString("Message.DontHavePet", owner));
            }
            return true;
        }
        sender.sendMessage("You can't use this command from server console!");
        return true;
    }
}