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

package de.Keyle.MyPet.commands.admin;

import de.Keyle.MyPet.api.commands.CommandOption;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetList;
import de.Keyle.MyPet.util.BukkitUtil;
import de.Keyle.MyPet.util.MyPetPlayer;
import de.Keyle.MyPet.util.WorldGroup;
import de.Keyle.MyPet.util.locale.Locales;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandOptionRemove implements CommandOption
{
    @Override
    public boolean onCommandOption(CommandSender sender, String[] args)
    {
        String lang = BukkitUtil.getCommandSenderLanguage(sender);

        if (args.length >= 1)
        {
            Player player = Bukkit.getPlayer(args[0]);
            if (player == null || !player.isOnline())
            {
                sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] " + Locales.getString("Message.No.PlayerOnline", lang));
                return true;
            }
            if (MyPetPlayer.isMyPetPlayer(player))
            {
                MyPetPlayer petOwner = MyPetPlayer.getMyPetPlayer(player);
                if (petOwner.hasMyPet())
                {
                    MyPet myPet = petOwner.getMyPet();

                    sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] You removed the MyPet of: " + ChatColor.YELLOW + petOwner.getName());

                    myPet.getOwner().setMyPetForWorldGroup(WorldGroup.getGroup(player.getWorld().getName()).getName(), null);
                    MyPetList.removeInactiveMyPet(MyPetList.setMyPetInactive(myPet.getOwner()));
                }
            }
        }

        return true;
    }
}