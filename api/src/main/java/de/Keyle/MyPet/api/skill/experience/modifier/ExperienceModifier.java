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

/**
 * Abstract base class for experience modifiers that transform the amount of experience a pet
 * receives from a kill.
 *
 * <p>Modifiers are applied in a chain. Each modifier receives the experience value as
 * modified by the previous modifier, plus the original unmodified base value for reference.
 * Subclasses implement specific strategies such as additive bonuses, multiplicative scaling,
 * or permission-based multipliers.
 *
 * @see AdditiveModifier
 * @see MultiplicativeModifier
 * @see GlobalModifier
 * @see PermissionModifier
 */
public abstract class ExperienceModifier {

    /**
     * Applies this modifier to the given experience value.
     *
     * @param experience     the experience value as modified by prior modifiers in the chain
     * @param baseExperience the original unmodified experience from the mob kill
     * @return the modified experience value to pass to the next modifier (or to award)
     */
    public abstract double modify(double experience, double baseExperience);

}
