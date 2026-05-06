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

package de.Keyle.MyPet.api.skill.modifier;

/**
 * An {@link UpgradeModifier} for enum-typed skill parameters.
 *
 * <p>Like {@link UpgradeBooleanModifier}, this is a replacement modifier: it unconditionally
 * sets the parameter to the enum constant carried by this record, ignoring the previous value.
 * Useful for skills that switch behavior modes at specific upgrade levels (e.g., selecting an
 * attack type or targeting strategy).
 *
 * @param value the enum constant to set when this modifier is applied
 * @param <T>   the enum type of the skill parameter
 */
public record UpgradeEnumModifier<T extends Enum<T>>(T value) implements UpgradeModifier<T> {

    /** {@inheritDoc} Unconditionally returns this modifier's enum constant, ignoring the input. */
    @Override
    public T modify(T value) {
        return this.value;
    }

}
