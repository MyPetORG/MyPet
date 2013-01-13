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
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.skill.skills.Inventory;
import de.Keyle.MyPet.util.MyPetLanguage;
import de.Keyle.MyPet.util.MyPetList;
import de.Keyle.MyPet.util.MyPetPermissions;
import de.Keyle.MyPet.util.MyPetUtil;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandInventory implements CommandExecutor
{
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (sender instanceof Player)
        {
            Player player = (Player) sender;
            if (args.length == 0)
            {
                if (MyPetList.hasMyPet(player))
                {
                    MyPet myPet = MyPetList.getMyPet(player);
                    if (myPet.status == PetState.Despawned)
                    {
                        sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_CallFirst")).replace("%petname%", myPet.petName));
                        return true;
                    }
                    if (myPet.status == PetState.Dead)
                    {
                        sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_CallDead")).replace("%petname%", myPet.petName).replace("%time%", "" + myPet.respawnTime));
                        return true;
                    }
                    if (player.getGameMode() == GameMode.CREATIVE && !MyPetPermissions.has(player, "MyPet.admin"))
                    {
                        sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_InventoryCreative")).replace("%petname%", myPet.petName));
                        return true;
                    }
                    if (!MyPetPermissions.hasExtended(player, "MyPet.user.extended.Inventory"))
                    {
                        myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_CantUse")));
                        return true;
                    }
                    if (myPet.getSkills().hasSkill("Inventory"))
                    {
                        myPet.getSkills().getSkill("Inventory").activate();
                    }
                }
                else
                {
                    sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_DontHavePet")));
                }
            }
            else if (args.length == 1 && MyPetPermissions.has(player, "MyPet.admin"))
            {
                if (MyPetList.hasMyPet(args[0]))
                {
                    MyPet myPet = MyPetList.getMyPet(args[0]);
                    if (myPet.getSkills().isSkillActive("Inventory"))
                    {
                        ((Inventory) myPet.getSkills().getSkill("Inventory")).OpenInventory(player);
                    }
                }
            }
        }
        return true;
    }
}