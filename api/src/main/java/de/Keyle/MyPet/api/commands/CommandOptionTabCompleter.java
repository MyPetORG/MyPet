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

package de.Keyle.MyPet.api.commands;

import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public interface CommandOptionTabCompleter extends CommandOption {

    List<String> onTabComplete(CommandSender commandSender, String[] strings);

    default List<String> filterTabCompletionResults(Collection<String> collection, String startsWith) {
        return collection
                .stream()
                .filter(s -> s.toLowerCase().startsWith(startsWith.toLowerCase()))
                .sorted()
                .collect(Collectors.toList());
    }
}