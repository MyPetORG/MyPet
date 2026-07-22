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
 * Skill that lets the pet periodically fell a nearby tree (a log column with a
 * leaf canopy), dropping the logs naturally. Radius and work interval scale
 * with the pet's skilltree level.
 *
 * @see Scheduler
 */
@SkillName(value = "Lumberjack", translationNode = "Name.Skill.Lumberjack")
public interface Lumberjack extends Skill, Scheduler, ToggleableSkill {

    /** Returns the upgrade computer controlling the tree search radius in blocks. */
    UpgradeComputer<Number> getRange();

    /** Returns the upgrade computer controlling the seconds between felling attempts. */
    UpgradeComputer<Integer> getInterval();

    /** Whether the pet fells bare-pawed (true) or needs a real axe in its Backpack (false). */
    UpgradeComputer<Boolean> getToolless();

    /** How many trunk logs the pet fells per work cycle (base 1; the tree-feller upgrade raises it). */
    UpgradeComputer<Integer> getLogs();
}
