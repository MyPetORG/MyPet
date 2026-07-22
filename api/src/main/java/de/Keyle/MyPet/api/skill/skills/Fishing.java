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
import de.Keyle.MyPet.api.skill.ToggleableSkill;
import de.Keyle.MyPet.api.skill.UpgradeComputer;
import de.Keyle.MyPet.api.skill.skilltree.Skill;
import de.Keyle.MyPet.api.util.Scheduler;

/**
 * Skill that lets the pet periodically fish at nearby open water, dropping
 * vanilla fishing loot beside itself. Radius and work interval scale with the
 * pet's skilltree level.
 *
 * @see Scheduler
 */
@SkillName(value = "Fishing", translationNode = "Name.Skill.Fishing")
public interface Fishing extends Skill, Scheduler, ToggleableSkill {

    /** Returns the upgrade computer controlling the water search radius in blocks. */
    UpgradeComputer<Number> getRange();

    /** Returns the upgrade computer controlling the seconds between fishing attempts. */
    UpgradeComputer<Integer> getInterval();

    /** Whether the pet fishes bare-pawed (true) or needs a real fishing rod in its Backpack (false). */
    UpgradeComputer<Boolean> getToolless();
}
