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

package de.Keyle.MyPet.api.skill.skills;

import de.Keyle.MyPet.api.skill.SkillName;
import de.Keyle.MyPet.api.skill.UpgradeComputer;
import de.Keyle.MyPet.api.skill.skilltree.Skill;

/**
 * Skill that lets a pet climb walls <b>on its own</b> during pathfinding, spider-style: when the
 * pet is heading somewhere higher and beyond a wall it's pressed against, it scales the wall as a
 * shortcut instead of pathing all the way around, then resumes normal navigation once it crests.
 * Does nothing for pets that can already fly. (Climbing <i>while ridden</i> is a separate upgrade
 * on the Ride skill.)
 */
@SkillName(value = "Climb", translationNode = "Name.Skill.Climb")
public interface Climb extends Skill {

    /** Returns the upgrade computer controlling whether autonomous wall-climbing is unlocked. */
    UpgradeComputer<Boolean> getActive();
}
