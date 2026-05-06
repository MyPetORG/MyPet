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

package de.Keyle.MyPet.api.skill.experience;

import de.Keyle.MyPet.api.entity.MyPet;

/**
 * The built-in experience calculator that ships with MyPet.
 *
 * <p>Uses a progressive formula where each level requires {@code 7 + floor((level - 1) * 3.5)}
 * more experience than the previous one, producing a smoothly accelerating curve similar to
 * vanilla Minecraft's XP requirements.
 *
 * <p>This calculator is always usable and serves as the fallback when no custom calculator
 * is configured or when a custom calculator fails to load.
 */
public class DefaultExperienceCalculator implements ExperienceCalculator {

    /**
     * Calculates the cumulative experience required to reach the given level.
     *
     * <p>For level 1 or below, returns {@code 0}. For higher levels, sums the per-level
     * increments from level 1 up to {@code level - 1}, where each increment is
     * {@code 7 + floor((currentLevel - 1) * 3.5)}.
     */
    public double getExpByLevel(MyPet myPet, int level) {
        if (level <= 1) {
            return 0;
        }
        double tmpExp = 0;
        int tmpLvl = 1;

        while (tmpLvl < level) {
            tmpExp += 7 + Math.floor((tmpLvl - 1) * 3.5);
            tmpLvl++;
        }
        return tmpExp;
    }

    /** {@inheritDoc} Returns {@code 1} -- the formula has never changed. */
    @Override
    public long getVersion() {
        return 1;
    }

    /** {@inheritDoc} Always returns {@code true}; the default calculator has no external dependencies. */
    @Override
    public boolean isUsable() {
        return true;
    }

    /** {@inheritDoc} Returns {@code "MyPet"}, identifying this as the built-in calculator. */
    @Override
    public String getIdentifier() {
        return "MyPet";
    }
}