/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2026 Keyle
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

import de.Keyle.MyPet.util.CompatUtil;

import java.util.ArrayList;
import java.util.List;

public class CommandOptionCreator {

    protected List<String> options = new ArrayList<>();

    public CommandOptionCreator() {
    }

    public CommandOptionCreator add(String option) {
        options.add(option);
        return this;
    }

    public CommandOptionCreator add(String from, String option) {
        if (CompatUtil.minecraftVersionEqualsOrAbove(from)) {
            options.add(option);
        }
        return this;
    }

    public List<String> get() {
        return options;
    }
}
