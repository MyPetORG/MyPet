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

import de.Keyle.MyPet.api.MyPetGlobal;
import de.Keyle.MyPet.api.entity.Pet;

import java.util.Objects;

/**
 * Flat experience curve: every level costs the same {@code Base} amount, so the cumulative
 * experience to reach level {@code L} is {@code Base * (L - 1)}.
 *
 * <p>Parameters are read live from {@link MyPetGlobal.LevelSystem.Curve} so an admin edit
 * followed by {@code /mypet reload} invalidates the {@link ExperienceCache}.
 */
public class LinearExperienceCalculator implements ExperienceCalculator {

    /**
     * Lower bound on the per-level cost. A non-positive cost would make the cumulative curve
     * flat or decreasing, which hangs {@code PetExperience.calculateLevel()} (it counts levels
     * upward until the threshold exceeds the pet's XP — a threshold that never rises loops forever).
     */
    private static final double MIN_COST = 1.0E-6;

    public double getExpByLevel(Pet pet, int level) {
        if (level <= 1) {
            return 0;
        }
        double base = Math.max(MyPetGlobal.LevelSystem.Curve.LINEAR_BASE.get(), MIN_COST);
        return base * (level - 1);
    }

    @Override
    public long getVersion() {
        return Objects.hash(MyPetGlobal.LevelSystem.Curve.LINEAR_BASE.get());
    }

    @Override
    public boolean isUsable() {
        return true;
    }

    @Override
    public String getIdentifier() {
        return "Linear";
    }
}
