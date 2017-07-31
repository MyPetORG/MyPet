/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2017 Keyle
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

import de.Keyle.MyPet.api.commands.CommandOption;
import de.Keyle.MyPet.api.commands.CommandOptionTabCompleter;
import de.Keyle.MyPet.commands.options.CommandOptionHealthbar;
import de.Keyle.MyPet.commands.options.CommandOptionPetLivingSound;
import de.Keyle.MyPet.commands.options.CommandOptionResourcePack;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class CommandOptions implements CommandExecutor, TabCompleter {
    private static List<String> optionsList = new ArrayList<>();
    private static Map<String, CommandOption> commandOptions = new HashMap<>();

    public CommandOptions() {
        commandOptions.put("resource-pack", new CommandOptionResourcePack());
        commandOptions.put("healthbar", new CommandOptionHealthbar());
        commandOptions.put("idle-volume", new CommandOptionPetLivingSound());

        if (optionsList.size() != commandOptions.keySet().size()) {
            optionsList = new ArrayList<>(commandOptions.keySet());
            Collections.sort(optionsList);
        }
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            return false;
        }

        String[] parameter = Arrays.copyOfRange(args, 1, args.length);
        CommandOption option = commandOptions.get(args[0].toLowerCase());

        if (option != null) {
            return option.onCommandOption(sender, parameter);
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] strings) {
        if (sender instanceof Player) {
            if (strings.length == 1) {
                return optionsList;
            } else if (strings.length >= 1) {
                CommandOption co = commandOptions.get(strings[0]);
                if (co != null) {
                    if (co instanceof CommandOptionTabCompleter) {
                        return ((CommandOptionTabCompleter) co).onTabComplete(sender, strings);
                    }
                }
            }
        }
        return CommandAdmin.EMPTY_LIST;
    }
}