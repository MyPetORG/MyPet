/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.commands.CommandOption;
import de.Keyle.MyPet.api.repository.RepositoryCallback;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Calendar;
import java.util.Date;

public class CommandOptionCleanup implements CommandOption {
    @Override
    public boolean onCommandOption(final CommandSender sender, String[] args) {
        sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] cleaning up MyPet database...");

        long timestamp;
        if (args.length == 0) {
            timestamp = -1;
            sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] delete unused MyPets...");
        } else {
            Calendar cal = Calendar.getInstance();

            for (String arg : args) {
                if (arg.endsWith("y") || arg.endsWith("Y")) {
                    if (Util.isInt(arg.replaceAll("[yY]", ""))) {
                        int years = Integer.parseInt(arg.replaceAll("[yY]", ""));
                        cal.add(Calendar.YEAR, -years);
                    }
                }
                if (arg.endsWith("d") || arg.endsWith("D")) {
                    if (Util.isInt(arg.replaceAll("[dD]", ""))) {
                        int days = Integer.parseInt(arg.replaceAll("[dD]", ""));
                        cal.add(Calendar.DAY_OF_YEAR, -days);
                    }
                }
                if (arg.endsWith("h") || arg.endsWith("H")) {
                    if (Util.isInt(arg.replaceAll("[hH]", ""))) {
                        int hours = Integer.parseInt(arg.replaceAll("[hH]", ""));
                        cal.add(Calendar.HOUR, -hours);
                    }
                }
                if (arg.endsWith("m") || arg.endsWith("M")) {
                    if (Util.isInt(arg.replaceAll("[mM]", ""))) {
                        int minutes = Integer.parseInt(arg.replaceAll("[mM]", ""));
                        cal.add(Calendar.MINUTE, -minutes);
                    }
                }
            }
            timestamp = cal.getTimeInMillis();
            sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] delete MyPets older than " + new Date(cal.getTimeInMillis()) + "...");
        }

        MyPetApi.getRepository().cleanup(timestamp, new RepositoryCallback<Integer>() {
            @Override
            public void callback(Integer value) {
                sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] removed " + value + " MyPets.");
            }
        });
        return true;
    }
}