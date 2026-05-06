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

import de.Keyle.MyPet.api.skill.OnHitSkill;
import de.Keyle.MyPet.api.skill.SkillName;
import de.Keyle.MyPet.api.skill.UpgradeComputer;
import de.Keyle.MyPet.api.skill.skilltree.Skill;
import de.Keyle.MyPet.api.util.Scheduler;

/**
 * Skill that gives the pet a chance to inflict a bleeding damage-over-time effect on
 * hit targets. When triggered, the target takes periodic damage for the configured
 * duration. The bleed ticks are managed via {@link Scheduler}.
 *
 * <p>All parameters (damage per tick, tick interval, total duration, and trigger chance)
 * scale with the pet's skilltree level.
 *
 * @see OnHitSkill
 */
@SkillName(value = "Bleed", translationNode = "Name.Skill.Bleed")
public interface Bleed extends Skill, OnHitSkill, Scheduler {

    /** Returns the upgrade computer controlling the damage dealt per bleed tick. */
    UpgradeComputer<Number> getDamage();

    /** Returns the upgrade computer controlling the interval (in ticks) between bleed damage applications. */
    UpgradeComputer<Integer> getInterval();

    /** Returns the upgrade computer controlling the total bleed duration in ticks. */
    UpgradeComputer<Integer> getDuration();

    /** Returns the upgrade computer controlling the percent chance to inflict bleed on hit. */
    UpgradeComputer<Integer> getChance();
}
