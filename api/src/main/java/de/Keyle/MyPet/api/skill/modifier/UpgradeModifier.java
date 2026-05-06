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
 * A typed modifier that transforms a skill parameter value when a skilltree upgrade is applied.
 *
 * <p>Skill parameters (damage, chance, range, etc.) are stored as simple values and modified
 * at each level-up according to the skilltree's configured upgrade rules. Each
 * {@code UpgradeModifier} encapsulates both the modifier's own value and the logic to
 * combine it with the current parameter value.
 *
 * <p>Implementations include:
 * <ul>
 *   <li>{@link UpgradeNumberModifier} -- add/subtract a {@link Number}</li>
 *   <li>{@link UpgradeIntegerModifier} -- add/subtract an {@link Integer} (avoids floating-point)</li>
 *   <li>{@link UpgradeBooleanModifier} -- replaces the value with {@code true} or {@code false}</li>
 *   <li>{@link UpgradeEnumModifier} -- replaces the value with a specific enum constant</li>
 * </ul>
 *
 * @param <T> the type of the skill parameter being modified
 */
public interface UpgradeModifier<T> {

    /** Returns the raw modifier value carried by this instance. */
    T value();

    /**
     * Applies this modifier to the given current value, producing an updated result.
     *
     * @param value the current parameter value before modification
     * @return the modified parameter value
     */
    T modify(T value);
}
