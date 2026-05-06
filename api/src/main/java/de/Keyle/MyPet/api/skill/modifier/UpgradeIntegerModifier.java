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

import de.Keyle.MyPet.api.skill.modifier.UpgradeNumberModifier.Type;

/**
 * An {@link UpgradeModifier} for integer-typed skill parameters.
 *
 * <p>Supports addition and subtraction. This is the integer-specific counterpart of
 * {@link UpgradeNumberModifier} and is preferred when the skill parameter is known to be
 * a whole number (e.g., beacon range in blocks, backpack row count).
 *
 * @param value the integer amount to add or subtract
 * @param type  the operation to perform ({@link Type#Add} or {@link Type#Subtract})
 */
public record UpgradeIntegerModifier(Integer value, Type type) implements UpgradeModifier<Integer> {

    /**
     * {@inheritDoc}
     *
     * <p>Adds or subtracts this modifier's value from the given current integer, depending on
     * the configured {@link Type}.
     */
    public Integer modify(Integer n) {
        return switch (type) {
            case Add -> n + value;
            case Subtract -> n - value;
        };
    }
}
