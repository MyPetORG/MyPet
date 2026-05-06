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

package de.Keyle.MyPet.api.skill;

import de.Keyle.MyPet.api.skill.skilltree.Skill;

/**
 * Represents a single level-up upgrade for a skill. Upgrades are produced by an
 * {@link UpgradeParser} from {@code .st.json} skilltree data and are applied to the
 * owning {@link Skill} when the pet reaches the corresponding level.
 *
 * <p>Upgrades must be <em>invertible</em>: when a skilltree is reset or changed,
 * previously applied upgrades are rolled back via {@link #invert(Skill)} so the
 * skill's state returns to its pre-upgrade baseline.
 *
 * @param <T> the specific skill type this upgrade targets
 * @see UpgradeParser
 * @see UpgradeComputer
 */
public interface Upgrade<T extends Skill> {

    /**
     * Applies this upgrade's modifiers to the given skill (e.g. adds damage,
     * increases chance, enables a boolean flag).
     *
     * @param skill the skill instance to upgrade
     */
    void apply(T skill);

    /**
     * Reverts this upgrade's modifiers from the given skill, restoring the skill
     * to its state before this upgrade was applied. Must be the exact inverse of
     * {@link #apply(Skill)}.
     *
     * @param skill the skill instance to roll back
     */
    void invert(T skill);
}
