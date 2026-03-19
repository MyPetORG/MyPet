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

import de.Keyle.MyPet.api.commands.CommandCategory;
import de.Keyle.MyPet.api.commands.CommandOption;
import de.Keyle.MyPet.api.commands.CommandOptionTabCompleter;
import de.Keyle.MyPet.api.commands.CommandTabCompleter;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.commands.admin.*;
import de.Keyle.MyPet.util.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class CommandAdmin implements CommandTabCompleter {

    private final Map<String, CommandOption> commandOptions = new HashMap<>();
    private List<String> optionsList = new ArrayList<>();

    {
        commandOptions.put("name", new CommandOptionName());
        commandOptions.put("exp", new CommandOptionExp());
        commandOptions.put("exp-rate", new CommandOptionExpRate());
        commandOptions.put("respawn", new CommandOptionRespawn());
        commandOptions.put("skilltree", new CommandOptionSkilltree());
        commandOptions.put("create", new CommandOptionCreate());
        commandOptions.put("clone", new CommandOptionClone());
        commandOptions.put("remove", new CommandOptionRemove());
        commandOptions.put("cleanup", new CommandOptionCleanup());
        commandOptions.put("switch", new CommandOptionSwitch());
        commandOptions.put("info", new CommandOptionInfo());
    }

    public Map<String, CommandOption> getCommandOptions() {
        return commandOptions;
    }

    public void registerOption(String name, CommandOption option) {
        commandOptions.put(name, option);
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            if (!Permissions.has(player, "MyPet.admin", false)) {
                sender.sendMessage(Translation.getComponent("Message.No.Allowed", sender));
                return true;
            }
        }

        if (args.length < 1) {
            sender.sendMessage(Translation.getComponent("Message.Command.Help.MissingParameter", sender));
            sender.sendMessage(Component.text(" -> ").append(Component.text(String.join(", ", commandOptions.keySet())).color(NamedTextColor.DARK_AQUA)));
            return false;
        }

        String[] parameter = Arrays.copyOfRange(args, 1, args.length);
        CommandOption option = commandOptions.get(args[0].toLowerCase());

        if (option != null) {
            return option.onCommandOption(sender, parameter);
        }
        sender.sendMessage(MessageUtil.prefixed(Component.text()
                .append(Component.text("\""))
                .append(Component.text(args[0].toLowerCase()).decorate(TextDecoration.ITALIC))
                .append(Component.text("\" is not a valid option!"))
                .build()));
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        if (commandSender instanceof Player player && !Permissions.has(player, "MyPet.admin", false)) {
            return Collections.emptyList();
        }
        if (strings.length == 1) {
            if (optionsList.size() != commandOptions.size()) {
                optionsList = new ArrayList<>(commandOptions.keySet());
                Collections.sort(optionsList);
            }
            return filterTabCompletionResults(optionsList, strings[0]);
        } else if (strings.length >= 1) {
            CommandOption co = commandOptions.get(strings[0]);
            if (co instanceof CommandOptionTabCompleter tabCompleter) {
                return tabCompleter.onTabComplete(commandSender, strings);
            }
        }
        return Collections.emptyList();
    }

    @Override
    public boolean isVisibleTo(Player player) {
        return Permissions.has(player, "MyPet.admin", false);
    }

    @Override
    public CommandCategory getHelpCategory() {
        return CommandCategory.ADMIN;
    }
}
