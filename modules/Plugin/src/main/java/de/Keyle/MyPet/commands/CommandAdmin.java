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

import de.Keyle.MyPet.api.MyPetVersion;
import de.Keyle.MyPet.api.commands.CommandOption;
import de.Keyle.MyPet.api.commands.CommandOptionTabCompleter;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.commands.admin.*;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class CommandAdmin implements CommandExecutor, TabCompleter {
    private static List<String> optionsList = new ArrayList<>();
    public final static List<String> EMPTY_LIST = Collections.unmodifiableList(new ArrayList<String>());
    public static final Map<String, CommandOption> COMMAND_OPTIONS = new HashMap<>();

    public CommandAdmin() {
        COMMAND_OPTIONS.clear();

        COMMAND_OPTIONS.put("name", new CommandOptionName());
        COMMAND_OPTIONS.put("exp", new CommandOptionExp());
        COMMAND_OPTIONS.put("respawn", new CommandOptionRespawn());
        COMMAND_OPTIONS.put("reload", new CommandOptionReload());
        COMMAND_OPTIONS.put("reloadskills", new CommandOptionReloadSkilltrees());
        COMMAND_OPTIONS.put("skilltree", new CommandOptionSkilltree());
        COMMAND_OPTIONS.put("create", new CommandOptionCreate());
        COMMAND_OPTIONS.put("clone", new CommandOptionClone());
        COMMAND_OPTIONS.put("remove", new CommandOptionRemove());
        COMMAND_OPTIONS.put("cleanup", new CommandOptionCleanup());
        COMMAND_OPTIONS.put("ticket", new CommandOptionTicket());
        COMMAND_OPTIONS.put("switch", new CommandOptionSwitch());
        COMMAND_OPTIONS.put("update", new CommandOptionUpdate());
        //COMMAND_OPTIONS.put("test", new CommandOptionTest());

        COMMAND_OPTIONS.put("build", (sender, parameter) -> {
            sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] MyPet-" + MyPetVersion.getVersion() + "-b#" + MyPetVersion.getBuild());
            return true;
        });
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            if (!Permissions.has((Player) sender, "MyPet.admin", false)) {
                return true;
            }
        }

        if (args.length < 1) {
            return false;
        }

        String[] parameter = Arrays.copyOfRange(args, 1, args.length);
        CommandOption option = COMMAND_OPTIONS.get(args[0].toLowerCase());

        if (option != null) {
            return option.onCommandOption(sender, parameter);
        }
        sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] \"" + ChatColor.ITALIC + args[0].toLowerCase() + ChatColor.RESET + "\" is not a valid option!");
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        if (commandSender instanceof Player && !Permissions.has((Player) commandSender, "MyPet.admin", false)) {
            return EMPTY_LIST;
        }
        if (strings.length == 1) {
            if (optionsList.size() != COMMAND_OPTIONS.keySet().size()) {
                optionsList = new ArrayList<>(COMMAND_OPTIONS.keySet());
                Collections.sort(optionsList);
            }
            return optionsList;
        } else if (strings.length >= 1) {
            CommandOption co = COMMAND_OPTIONS.get(strings[0]);
            if (co != null) {
                if (co instanceof CommandOptionTabCompleter) {
                    return ((CommandOptionTabCompleter) co).onTabComplete(commandSender, strings);
                }
            }
        }
        return EMPTY_LIST;
    }
}