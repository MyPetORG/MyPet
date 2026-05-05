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

import de.Keyle.MyPet.api.Configuration.LevelSystem.Experience.Modifier;

/**
 * An experience modifier that multiplies incoming experience by the server-wide global factor.
 *
 * <p>The multiplier is read from {@link Modifier#GLOBAL} at each invocation, so changes to
 * the configuration value take effect immediately without re-registering this modifier.
 *
 * <p>A global factor of {@code 1.0} means no change; {@code 2.0} doubles all experience;
 * {@code 0.5} halves it.
 */
public class GlobalModifier extends ExperienceModifier {

    /** {@inheritDoc} Scales the experience by the global multiplier from configuration. */
    public double modify(double experience, double baseExperience) {
        return experience * Modifier.GLOBAL;
    }
}
