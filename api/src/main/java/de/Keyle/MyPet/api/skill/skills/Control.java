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
 * Skill that enables the owner to direct their pet to move to a specific location by
 * left-clicking on a block with a configured control item. When active, the pet pathfinds
 * to the clicked position rather than following the owner.
 *
 * <p>The skill is either unlocked or not, controlled by a single boolean
 * {@link UpgradeComputer}.
 */
@SkillName(value = "Control", translationNode = "Name.Skill.Control")
public interface Control extends Skill {

    /** Returns the upgrade computer controlling whether the Control skill is unlocked. */
    UpgradeComputer<Boolean> getActive();
}