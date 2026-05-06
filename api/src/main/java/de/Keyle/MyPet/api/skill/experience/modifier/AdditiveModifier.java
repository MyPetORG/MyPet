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

package de.Keyle.MyPet.api.skill.experience.modifier;

import lombok.Getter;
import lombok.Setter;

/**
 * An experience modifier that adds a fixed amount to the incoming experience value.
 *
 * <p>The modifier value defaults to {@code 0} (no change). Positive values increase experience;
 * negative values decrease it.
 *
 * <p>Example: if a mob kill yields 10 XP and this modifier is set to {@code 5}, the result
 * passed to the next modifier in the chain will be {@code 15}.
 */
public class AdditiveModifier extends ExperienceModifier {

    @Setter
    @Getter
    private double modifier = 0;

    /**
     * Creates an additive modifier with the specified bonus.
     *
     * @param modifier the flat amount to add (can be negative to subtract)
     */
    public AdditiveModifier(double modifier) {
        this.modifier = modifier;
    }

    /** Creates an additive modifier with a bonus of {@code 0} (no-op). */
    public AdditiveModifier() {
    }

    /** {@inheritDoc} Adds the configured flat modifier value to the experience. */
    public double modify(double experience, double baseExperience) {
        return experience + modifier;
    }
}
