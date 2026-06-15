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
 * Polynomial experience curve: each level {@code l} costs {@code Factor * l^Exponent}, summed
 * from level 1 up to the target. An exponent of 1 degrades to a linear ramp; higher exponents
 * accelerate the cost. There is no closed form for an arbitrary real exponent, so the sum is
 * computed iteratively (bounded by the level cap and cached by {@link ExperienceCache}).
 *
 * <p>Parameters are read live from {@link MyPetGlobal.LevelSystem.Curve} so an admin edit
 * followed by {@code /mypet reload} invalidates the {@link ExperienceCache}.
 */
public class PowerExperienceCalculator implements ExperienceCalculator {

    /**
     * Lower bound on {@code Factor}. {@code l^Exponent} is always positive for {@code l >= 1}
     * (any real exponent), so a positive factor guarantees a strictly increasing cumulative
     * curve and keeps {@code PetExperience.calculateLevel()} from looping forever.
     */
    private static final double MIN_FACTOR = 1.0E-6;

    public double getExpByLevel(Pet pet, int level) {
        if (level <= 1) {
            return 0;
        }
        double factor = Math.max(MyPetGlobal.LevelSystem.Curve.POWER_FACTOR.get(), MIN_FACTOR);
        double exponent = MyPetGlobal.LevelSystem.Curve.POWER_EXPONENT.get();
        double exp = 0;
        for (int l = 1; l < level; l++) {
            exp += factor * Math.pow(l, exponent);
        }
        return exp;
    }

    @Override
    public long getVersion() {
        return Objects.hash(
                MyPetGlobal.LevelSystem.Curve.POWER_FACTOR.get(),
                MyPetGlobal.LevelSystem.Curve.POWER_EXPONENT.get());
    }

    @Override
    public boolean isUsable() {
        return true;
    }

    @Override
    public String getIdentifier() {
        return "Power";
    }
}
