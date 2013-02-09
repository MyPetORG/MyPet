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

import de.Keyle.MyPet.util.MyPetLanguage;
import de.Keyle.MyPet.util.MyPetList;
import de.Keyle.MyPet.util.MyPetPermissions;
import de.Keyle.MyPet.util.MyPetUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandHelp implements CommandExecutor
{

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (sender instanceof Player)
        {
            Player player = (Player) sender;
            player.sendMessage("--------------- MyPet - " + MyPetLanguage.getString("Name_Help") + " -------------------------");
            player.sendMessage(MyPetUtil.setColors("/petinfo" + MyPetLanguage.getString("Msg_Cmd_petinfo")));
            if (MyPetPermissions.has(player, "MyPet.admin", false))
            {
                player.sendMessage(MyPetUtil.setColors("/petadmin" + MyPetLanguage.getString("Msg_Cmd_petadmin")));
            }
            if (MyPetList.hasMyPet(player))
            {
                player.sendMessage(MyPetUtil.setColors("/petname" + MyPetLanguage.getString("Msg_Cmd_petname")));
                player.sendMessage(MyPetUtil.setColors("/petrelease" + MyPetLanguage.getString("Msg_Cmd_petrelease")));
                player.sendMessage(MyPetUtil.setColors("/petstop" + MyPetLanguage.getString("Msg_Cmd_petstop")));
                player.sendMessage(MyPetUtil.setColors("/petcall" + MyPetLanguage.getString("Msg_Cmd_petcall")));
                player.sendMessage(MyPetUtil.setColors("/petsendaway" + MyPetLanguage.getString("Msg_Cmd_petsendaway")));
                player.sendMessage(MyPetUtil.setColors("/petskill" + MyPetLanguage.getString("Msg_Cmd_petskill")));
                player.sendMessage(MyPetUtil.setColors("/petchooseskilltree" + MyPetLanguage.getString("Msg_Cmd_petchooseskilltree")));

                if (MyPetList.getMyPet(player).getSkills().isSkillActive("Inventory"))
                {
                    player.sendMessage(MyPetUtil.setColors("/petinventory" + MyPetLanguage.getString("Msg_Cmd_petinventory")));
                }
                if (MyPetList.getMyPet(player).getSkills().isSkillActive("Pickup"))
                {
                    player.sendMessage(MyPetUtil.setColors("/petpickup" + MyPetLanguage.getString("Msg_Cmd_petpickup")));
                }
                if (MyPetList.getMyPet(player).getSkills().isSkillActive("Behavior"))
                {
                    player.sendMessage(MyPetUtil.setColors("/petbehavior" + MyPetLanguage.getString("Msg_Cmd_petbehavior")));
                }
            }
            player.sendMessage("");
            player.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_Cmd_moreinfo") + "mypet.keyle.de"));
            player.sendMessage("-----------------------------------------------------");
        }
        return true;
    }
}