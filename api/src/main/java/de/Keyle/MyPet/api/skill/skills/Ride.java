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
 * Skill that allows the pet's owner to mount and ride the pet as a controllable mount.
 * Movement speed, jump height, and optional flight capabilities all scale with the pet's
 * skilltree level.
 *
 * <p>When flight is enabled, the pet can ascend while ridden, consuming a flight energy
 * resource that regenerates over time. The flight limit and regeneration rate are
 * configurable per upgrade level.
 *
 * @see de.Keyle.MyPet.api.skill.UpgradeComputer
 */
@SkillName(value = "Ride", translationNode = "Name.Skill.Ride")
public interface Ride extends Skill {

    /** Returns the upgrade computer controlling whether the Ride skill is unlocked. */
    UpgradeComputer<Boolean> getActive();

    /** Returns the upgrade computer controlling the bonus movement speed percentage while ridden. */
    UpgradeComputer<Integer> getSpeedIncrease();

    /** Returns the upgrade computer controlling the maximum jump height when ridden. */
    UpgradeComputer<Number> getJumpHeight();

    /** Returns the upgrade computer controlling the maximum flight energy (distance/duration limit). */
    UpgradeComputer<Number> getFlyLimit();

    /** Returns the upgrade computer controlling the rate at which flight energy regenerates. */
    UpgradeComputer<Number> getFlyRegenRate();

    /** Returns the upgrade computer controlling whether the pet can fly while ridden. */
    UpgradeComputer<Boolean> getCanFly();

    /** Whether a ridden pet can climb walls spider-style (was the standalone Climb skill). */
    UpgradeComputer<Boolean> getClimb();
}