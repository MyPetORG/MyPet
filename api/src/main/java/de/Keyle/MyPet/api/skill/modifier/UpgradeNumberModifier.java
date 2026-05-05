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

package de.Keyle.MyPet.api.skill.modifier;

import java.math.BigDecimal;

/**
 * An {@link UpgradeModifier} for generic numeric skill parameters (doubles, floats, etc.).
 *
 * <p>Performs addition or subtraction on {@link Number} values using {@link BigDecimal}
 * arithmetic for precision. This is the general-purpose numeric modifier; for
 * integer-only parameters prefer {@link UpgradeIntegerModifier} which returns an
 * {@code int} directly.
 *
 * @param value the numeric amount to add or subtract
 * @param type  the operation to perform ({@link Type#Add} or {@link Type#Subtract})
 */
public record UpgradeNumberModifier(Number value, Type type) implements UpgradeModifier<Number> {

    /**
     * {@inheritDoc}
     *
     * <p>Adds or subtracts this modifier's value from the given number, depending on the
     * configured {@link Type}. Returns a {@link BigDecimal}.
     */
    public Number modify(Number n) {
        return switch (type) {
            case Add -> new BigDecimal(n.toString()).add(new BigDecimal(value.toString()));
            case Subtract -> new BigDecimal(n.toString()).subtract(new BigDecimal(value.toString()));
        };
    }

    /**
     * Applies the inverse of this modifier's operation.
     *
     * <p>If the type is {@link Type#Add}, this subtracts; if {@link Type#Subtract}, this adds.
     * Useful for rolling back an upgrade or computing the previous level's value.
     *
     * @param n the number to un-modify
     * @return the result of the inverse operation
     */
    public Number invert(Number n) {
        return switch (type) {
            case Add -> new BigDecimal(n.toString()).subtract(new BigDecimal(value.toString()));
            case Subtract -> new BigDecimal(n.toString()).add(new BigDecimal(value.toString()));
        };
    }

    /**
     * The arithmetic operation a numeric upgrade modifier can perform.
     */
    public enum Type {
        /** Adds the modifier value to the current parameter value. */
        Add,
        /** Subtracts the modifier value from the current parameter value. */
        Subtract
    }
}
