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

package de.Keyle.MyPet.skill.skilltree.levelrule;

import de.Keyle.MyPet.api.skill.skilltree.levelrule.LevelRule;

import java.util.Collection;
import java.util.HashSet;

public class StaticLevelRule implements LevelRule {
    HashSet<Integer> levels = new HashSet<>();

    public StaticLevelRule(int... levels) {
        for (int level : levels) {
            this.levels.add(level);
        }
    }

    public StaticLevelRule(Collection<Integer> levels) {
        this.levels.addAll(levels);
    }

    public StaticLevelRule(int level) {
        this.levels.add(level);
    }

    @Override
    public boolean check(int level) {
        return levels.contains(level);
    }

    @Override
    public int getPriority() {
        return 1;
    }

}
