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
import de.Keyle.MyPet.util.MyPetBukkitUtil;
import de.Keyle.MyPet.util.MyPetLanguage;
import de.Keyle.MyPet.util.MyPetList;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandStop implements CommandExecutor
{
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (sender instanceof Player)
        {
            Player petOwner = (Player) sender;
            if (MyPetList.hasMyPet(petOwner))
            {
                MyPet myPet = MyPetList.getMyPet(petOwner);

                if (myPet.getStatus() == PetState.Despawned)
                {
                    sender.sendMessage(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_CallFirst")).replace("%petname%", myPet.petName));
                    return true;
                }
                else if (myPet.getStatus() == PetState.Dead)
                {
                    sender.sendMessage(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_CallDead")).replace("%petname%", myPet.petName).replace("%time%", "" + myPet.respawnTime));
                    return true;
                }
                sender.sendMessage(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_StopAttack")).replace("%petname%", myPet.petName));
                myPet.getCraftPet().getHandle().setTarget(null);
                myPet.getCraftPet().getHandle().setGoalTarget(null);
                myPet.getCraftPet().getHandle().goalTarget = null;
                if (myPet.getCraftPet().getHandle().canMove())
                {
                    myPet.getCraftPet().getHandle().getNavigation().a(myPet.getCraftPet().getHandle().getOwner(), myPet.getCraftPet().getHandle().getWalkSpeed());
                }
            }
            else
            {
                sender.sendMessage(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_DontHavePet")));
            }
            return true;
        }
        sender.sendMessage("You can't use this command from server console!");
        return true;
    }
}