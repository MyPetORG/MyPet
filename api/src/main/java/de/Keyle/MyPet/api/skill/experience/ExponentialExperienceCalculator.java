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
 * Geometric experience curve: each level {@code l} costs {@code Base * Growth^(l-1)}, summed
 * from level 1 up to the target. A growth of 1 degrades to a flat ramp; values above 1 make
 * each level progressively more expensive than the last. The cumulative sum has a closed form
 * (geometric series), so no per-level loop is needed.
 *
 * <p>Parameters are read live from {@link MyPetGlobal.LevelSystem.Curve} so an admin edit
 * followed by {@code /mypet reload} invalidates the {@link ExperienceCache}.
 */
public class ExponentialExperienceCalculator implements ExperienceCalculator {

    /**
     * Lower bound on {@code Base} and {@code Growth}. Both must stay positive so every per-level
     * cost is positive and the cumulative curve strictly increases; otherwise
     * {@code PetExperience.calculateLevel()} would loop forever on a flat/decreasing curve.
     */
    private static final double MIN_VALUE = 1.0E-6;

    public double getExpByLevel(Pet pet, int level) {
        if (level <= 1) {
            return 0;
        }
        double base = Math.max(MyPetGlobal.LevelSystem.Curve.EXPONENTIAL_BASE.get(), MIN_VALUE);
        double growth = Math.max(MyPetGlobal.LevelSystem.Curve.EXPONENTIAL_GROWTH.get(), MIN_VALUE);
        // Sum of base * growth^0 .. growth^(level-2). Growth == 1 has no division form, so handle it directly.
        if (growth == 1.0) {
            return base * (level - 1);
        }
        return base * (Math.pow(growth, level - 1) - 1) / (growth - 1);
    }

    @Override
    public long getVersion() {
        return Objects.hash(
                MyPetGlobal.LevelSystem.Curve.EXPONENTIAL_BASE.get(),
                MyPetGlobal.LevelSystem.Curve.EXPONENTIAL_GROWTH.get());
    }

    @Override
    public boolean isUsable() {
        return true;
    }

    @Override
    public String getIdentifier() {
        return "Exponential";
    }
}
