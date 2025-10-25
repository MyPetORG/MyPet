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

package de.Keyle.MyPet.commands;

import de.Keyle.MyPet.api.commands.CommandOption;
import de.Keyle.MyPet.api.commands.CommandOptionTabCompleter;
import de.Keyle.MyPet.api.commands.CommandTabCompleter;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.commands.settings.CommandSettingHealthbar;
import de.Keyle.MyPet.commands.settings.CommandSettingsPetLivingSound;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class CommandSettings implements CommandTabCompleter {

    private static List<String> optionsList = new ArrayList<>();
    private static Map<String, CommandOption> commandOptions = new HashMap<>();

    public CommandSettings() {
        commandOptions.put("healthbar", new CommandSettingHealthbar());
        commandOptions.put("idle-volume", new CommandSettingsPetLivingSound());

        if (optionsList.size() != commandOptions.keySet().size()) {
            optionsList = new ArrayList<>(commandOptions.keySet());
            Collections.sort(optionsList);
        }
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(Translation.getString("Message.Command.Help.MissingParameter", sender));
            sender.sendMessage(" -> " + ChatColor.DARK_AQUA + String.join(ChatColor.RESET + ", " + ChatColor.DARK_AQUA, commandOptions.keySet()));
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
                return filterTabCompletionResults(optionsList, strings[0]);
            } else if (strings.length >= 1) {
                CommandOption co = commandOptions.get(strings[0]);
                if (co != null) {
                    if (co instanceof CommandOptionTabCompleter) {
                        return ((CommandOptionTabCompleter) co).onTabComplete(sender, strings);
                    }
                }
            }
        }
        return Collections.emptyList();
    }
}