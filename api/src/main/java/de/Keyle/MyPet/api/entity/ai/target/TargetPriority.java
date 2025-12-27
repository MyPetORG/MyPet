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

package de.Keyle.MyPet.api.entity.ai.target;

public enum TargetPriority {
    None(0x80000000),

    Bukkit(0),

    Farm(3),
    Control(4),
    GetHurt(5),
    OwnerHurts(6),
    OwnerGetsHurt(7),
    Aggressive(9),
    Duel(10),

    Overwrite(0x7fffffff);

    private int priority;

    TargetPriority(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }
}