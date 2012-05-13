/*
 * Copyright (C) 2011-2012 Keyle
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

import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.entity.types.wolf.MyWolf;
import de.Keyle.MyPet.skill.skills.Inventory;
import de.Keyle.MyPet.util.MyPetLanguage;
import de.Keyle.MyPet.util.MyPetList;
import de.Keyle.MyPet.util.MyPetPermissions;
import de.Keyle.MyPet.util.MyPetUtil;
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
                    MyWolf MPet = MyPetList.getMyPet(player);
                    if (MPet.Status == PetState.Despawned)
                    {
                        sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_CallFirst")));
                        return true;
                    }
                    if (MPet.Status == PetState.Dead)
                    {
                        sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_CallDead")).replace("%petname%", MPet.Name).replace("%time%", "" + MPet.RespawnTime));
                        return true;
                    }
                    if (MPet.getSkillSystem().hasSkill("Inventory"))
                    {
                        MPet.getSkillSystem().getSkill("Inventory").activate();
                    }
                }
                else
                {
                    sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_DontHavePet")));
                }
            }
            else if (args.length == 1 && MyPetPermissions.has(player, "MyPet.admin"))
            {
                if (MyPetList.hasMyPet(MyPetUtil.getOfflinePlayer(args[0])))
                {
                    MyWolf MPet = MyPetList.getMyPet(MyPetUtil.getOfflinePlayer(args[0]));
                    if (MPet.getSkillSystem().getSkill("Inventory") != null && MPet.getSkillSystem().getSkill("Inventory").getLevel() > 0)
                    {
                        ((Inventory) MPet.getSkillSystem().getSkill("Inventory")).OpenInventory(player);
                    }
                }
            }
        }
        return true;
    }
}