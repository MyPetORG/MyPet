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

/*
 * This file is part of MyPet-NPC
 *
 * Copyright (C) 2011-2013 Keyle
 * MyPet-NPC is licensed under the GNU Lesser General Public License.
 *
 * MyPet-NPC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyPet-NPC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.commands.admin;

import de.Keyle.MyPet.api.commands.CommandOption;
import de.Keyle.MyPet.api.commands.CommandOptionTabCompleter;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.commands.admin.npc.CommandOptionShop;
import de.Keyle.MyPet.commands.admin.npc.CommandOptionWallet;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class CommandOptionNpc implements CommandOptionTabCompleter {

    private static List<String> optionsList = new ArrayList<>();
    public static final Map<String, CommandOption> COMMAND_OPTIONS = new HashMap<>();

    public CommandOptionNpc() {
        COMMAND_OPTIONS.put("wallet", new CommandOptionWallet());
        COMMAND_OPTIONS.put("shop", new CommandOptionShop());
    }

    @Override
    public boolean onCommandOption(CommandSender sender, String[] args) {
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

        return option != null && option.onCommandOption(sender, parameter);
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, String[] strings) {
        if (commandSender instanceof Player) {
            if (!Permissions.has((Player) commandSender, "MyPet.admin", false)) {
                return Collections.emptyList();
            }
        }
        if (strings.length == 2) {
            if (optionsList.size() != COMMAND_OPTIONS.keySet().size()) {
                optionsList = new ArrayList<>(COMMAND_OPTIONS.keySet());
                Collections.sort(optionsList);
            }
            return optionsList;
        } else if (strings.length >= 2) {
            CommandOption co = COMMAND_OPTIONS.get(strings[1].toLowerCase());
            if (co != null) {
                if (co instanceof CommandOptionTabCompleter) {
                    return ((CommandOptionTabCompleter) co).onTabComplete(commandSender, strings);
                }
            }
        }
        return Collections.emptyList();
    }
}
