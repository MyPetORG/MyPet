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

package de.Keyle.MyPet.api.skill.experience.modifier;

import lombok.Getter;
import lombok.Setter;

/**
 * An experience modifier that multiplies the incoming experience by a configurable factor.
 *
 * <p>The modifier value defaults to {@code 1.0} (no change). Values above {@code 1.0}
 * increase experience; values between {@code 0.0} and {@code 1.0} decrease it.
 *
 * <p>Unlike {@link GlobalModifier}, this modifier carries its own per-instance multiplier
 * rather than reading from global configuration.
 */
public class MultiplicativeModifier extends ExperienceModifier {

    @Setter
    @Getter
    private double modifier = 1;

    /**
     * Creates a multiplicative modifier with the specified factor.
     *
     * @param modifier the multiplication factor (e.g. {@code 1.5} for +50%)
     */
    public MultiplicativeModifier(double modifier) {
        this.modifier = modifier;
    }

    /** Creates a multiplicative modifier with a factor of {@code 1.0} (no-op). */
    public MultiplicativeModifier() {
    }

    /** {@inheritDoc} Multiplies the experience by the configured factor. */
    public double modify(double experience, double baseExperience) {
        return experience * modifier;
    }
}
