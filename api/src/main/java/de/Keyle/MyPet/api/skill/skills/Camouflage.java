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
import de.Keyle.MyPet.api.util.Scheduler;

/**
 * Skill that turns the pet invisible after it stands still for a configured number of
 * seconds. Moving or gaining a combat target breaks the camouflage. Ticks via
 * {@link Scheduler} to track stillness.
 */
@SkillName(value = "Camouflage", translationNode = "Name.Skill.Camouflage")
public interface Camouflage extends Skill, Scheduler {

    /** Returns the upgrade computer controlling the seconds of stillness required before turning invisible; 0 disables the skill. */
    UpgradeComputer<Integer> getDelay();
}
