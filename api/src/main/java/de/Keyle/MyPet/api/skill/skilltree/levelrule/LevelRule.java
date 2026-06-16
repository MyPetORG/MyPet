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

package de.Keyle.MyPet.api.skill.skilltree.levelrule;

import java.util.Set;

/**
 * A rule that determines whether a particular upgrade or notification should activate at a
 * given pet level.
 *
 * <p>Level rules are the gating mechanism in a {@link de.Keyle.MyPet.api.skill.skilltree.Skilltree}:
 * each upgrade is paired with a rule, and the upgrade is only applied when
 * {@link #check(int)} returns {@code true} for the pet's current level.
 *
 * <p>The closed set of rule kinds is the two nested records: {@link Dynamic} (an "every N levels,
 * optionally bounded" pattern) and {@link Static} (an explicit set of levels).
 *
 * <p>When multiple rules match the same level, they are applied in ascending
 * {@link #getPriority()} order to ensure deterministic upgrade sequencing.
 */
public sealed interface LevelRule permits LevelRule.Dynamic, LevelRule.Static {

    /**
     * Tests whether this rule matches the given level.
     *
     * @param level the pet's current level (1-based)
     * @return {@code true} if the associated upgrade/notification should activate
     */
    boolean check(int level);

    /**
     * Returns the priority used to order this rule relative to other rules that match the
     * same level. Lower values execute first.
     */
    int getPriority();

    /**
     * Matches levels by an "every {@code modulo} levels" pattern, optionally restricted to the
     * inclusive range {@code [start, end]} ({@code end == 0} means unbounded).
     */
    record Dynamic(int modulo, int start, int end) implements LevelRule {

        public Dynamic {
            modulo = Math.max(1, modulo);
            start = Math.max(1, start);
            end = Math.max(0, end);
        }

        @Override
        public boolean check(int level) {
            if (start > 1 && level < start) {
                return false;
            }
            if (end > 0 && level > end) {
                return false;
            }
            return (level - start) % modulo == 0;
        }

        @Override
        public int getPriority() {
            return 0;
        }
    }

    /** Matches an explicit, fixed set of levels. */
    record Static(Set<Integer> levels) implements LevelRule {

        public Static {
            levels = Set.copyOf(levels);
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
}
