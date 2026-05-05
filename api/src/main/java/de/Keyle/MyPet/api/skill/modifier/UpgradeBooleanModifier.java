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

import lombok.Getter;

/**
 * An {@link UpgradeModifier} for boolean skill parameters.
 *
 * <p>Unlike numeric modifiers that add/subtract, boolean modifiers unconditionally replace
 * the current value with their own. This is typically used to enable or disable a skill
 * feature at a specific level (e.g., enabling fire aspect on the Damage skill).
 *
 * <p>Implemented as an enum with exactly two constants: {@link #True} and {@link #False}.
 */
public enum UpgradeBooleanModifier implements UpgradeModifier<Boolean> {
    /** Modifier that sets the parameter to {@code true}. */
    True(true),
    /** Modifier that sets the parameter to {@code false}. */
    False(false);

    private final Boolean value;

    UpgradeBooleanModifier(Boolean b) {
        value = b;
    }

    @Override
    public Boolean value() {
        return value;
    }

    /** Returns the primitive boolean value of this modifier. */
    public boolean getBoolean() {
        return value;
    }

    /** {@inheritDoc} Unconditionally returns this modifier's boolean value, ignoring the input. */
    @Override
    public Boolean modify(Boolean value) {
        return this.value;
    }
}
