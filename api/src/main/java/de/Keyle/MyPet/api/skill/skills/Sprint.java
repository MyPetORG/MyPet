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

package de.Keyle.MyPet.api.skill.skills;

import de.Keyle.MyPet.api.skill.SkillName;
import de.Keyle.MyPet.api.skill.UpgradeComputer;
import de.Keyle.MyPet.api.skill.skilltree.Skill;

/**
 * Skill that enables the pet to sprint (move at increased speed) when its owner is
 * sprinting. Once unlocked, the pet automatically matches the owner's sprint state.
 *
 * <p>The skill is either unlocked or not, controlled by a single boolean
 * {@link UpgradeComputer}.
 */
@SkillName(value = "Sprint", translationNode = "Name.Skill.Sprint")
public interface Sprint extends Skill {

    /** Returns the upgrade computer controlling whether the Sprint skill is unlocked. */
    UpgradeComputer<Boolean> getActive();
}