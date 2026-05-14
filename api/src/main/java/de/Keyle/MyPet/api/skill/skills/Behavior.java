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

import de.Keyle.MyPet.api.skill.ActiveSkill;
import de.Keyle.MyPet.api.skill.SkillName;
import de.Keyle.MyPet.api.skill.SkillState;
import de.Keyle.MyPet.api.skill.SkillStateCodec;
import de.Keyle.MyPet.api.skill.UpgradeComputer;
import de.Keyle.MyPet.api.skill.skilltree.Skill;
import de.Keyle.MyPet.api.util.Scheduler;

/**
 * Skill that controls the pet's combat behavior mode. The owner can cycle through
 * available modes to change how the pet reacts to nearby entities:
 * <ul>
 *   <li><b>Normal</b> -- follows the owner, only attacks targets the owner hits</li>
 *   <li><b>Friendly</b> -- never attacks, ignores all hostiles</li>
 *   <li><b>Aggressive</b> -- attacks all nearby hostile mobs autonomously</li>
 *   <li><b>Raid</b> -- attacks only mobs, never players (useful in group PvE)</li>
 *   <li><b>Farm</b> -- attacks hostile mobs but stays within a set range</li>
 *   <li><b>Duel</b> -- attacks other players' pets only</li>
 * </ul>
 *
 * <p>Which modes are available depends on the pet's skilltree level; each mode is
 * individually unlocked via its own {@link UpgradeComputer}{@code <Boolean>}. The
 * selected mode is persisted via a registered {@link SkillStateCodec} and ticks
 * via {@link Scheduler} to reset mode if needed.
 *
 * @see BehaviorMode
 * @see ActiveSkill#activate()
 */
@SkillName(value = "Behavior", translationNode = "Name.Skill.Behavior")
public interface Behavior extends Skill, Scheduler, ActiveSkill {

    /** Returns the pet's currently active behavior mode. */
    BehaviorMode getBehavior();

    /**
     * Sets the pet's behavior mode.
     *
     * @param mode the new behavior mode to apply
     */
    void setBehavior(BehaviorMode mode);

    /** Returns the upgrade computer controlling whether Farm mode is unlocked. */
    UpgradeComputer<Boolean> getFarmBehavior();

    /** Returns the upgrade computer controlling whether Raid mode is unlocked. */
    UpgradeComputer<Boolean> getRaidBehavior();

    /** Returns the upgrade computer controlling whether Duel mode is unlocked. */
    UpgradeComputer<Boolean> getDuelBehavior();

    /** Returns the upgrade computer controlling whether Aggressive mode is unlocked. */
    UpgradeComputer<Boolean> getAggressiveBehavior();

    /** Returns the upgrade computer controlling whether Friendly mode is unlocked. */
    UpgradeComputer<Boolean> getFriendlyBehavior();

    /**
     * Enumerates the possible pet behavior modes. Each mode changes the pet's
     * AI target selection logic.
     */
    enum BehaviorMode {
        Normal, Friendly, Aggressive, Raid, Farm, Duel
    }

    /** Snapshot of a Behavior skill's selected mode (persisted or live). */
    record State(BehaviorMode mode) implements SkillState {}
}