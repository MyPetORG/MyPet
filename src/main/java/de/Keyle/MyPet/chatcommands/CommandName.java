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
import de.Keyle.MyPet.entity.types.MyPetList;
import de.Keyle.MyPet.util.Colorizer;
import de.Keyle.MyPet.util.locale.MyPetLocales;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandName implements CommandExecutor
{
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (sender instanceof Player)
        {
            Player petOwner = (Player) sender;
            if (MyPetList.hasMyPet(petOwner))
            {
                if (args.length < 1)
                {
                    return false;
                }
                String name = "";
                for (String arg : args)
                {
                    if (!name.equals(""))
                    {
                        name += " ";
                    }
                    name += arg;
                }
                name = Colorizer.setColors(name);

                Pattern regex = Pattern.compile("§[abcdefklmnor0-9]");
                Matcher regexMatcher = regex.matcher(name);
                if (regexMatcher.find())
                {
                    name += Colorizer.setColors("%reset%");
                }

                MyPet myPet = MyPetList.getMyPet(petOwner);
                myPet.setPetName(name);
                sender.sendMessage(Colorizer.setColors(MyPetLocales.getString("Message.NewName", petOwner)).replace("%petname%", myPet.getPetName()));
            }
            else
            {
                sender.sendMessage(Colorizer.setColors(MyPetLocales.getString("Message.DontHavePet", petOwner)));
            }
            return true;
        }
        sender.sendMessage("You can't use this command from server console!");
        return true;
    }
}