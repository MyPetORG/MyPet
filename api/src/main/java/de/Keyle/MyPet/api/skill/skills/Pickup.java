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
 * Skill that allows the pet to automatically pick up nearby dropped items and experience
 * orbs on behalf of its owner. The pickup radius scales with the pet's skilltree level.
 * The owner can toggle automatic pickup on or off via {@link ActiveSkill#activate()}.
 *
 * <p>The skill ticks via {@link Scheduler} to scan for nearby collectibles and persists
 * its active/inactive toggle via a registered {@link SkillStateCodec}.
 *
 * @see ActiveSkill#activate()
 * @see Scheduler
 */
@SkillName(value = "Pickup", translationNode = "Name.Skill.Pickup")
public interface Pickup extends Skill, Scheduler, ActiveSkill {

    /** Returns the upgrade computer controlling the pickup radius in blocks. */
    UpgradeComputer<Number> getRange();

    /** Returns the upgrade computer controlling whether experience orb pickup is enabled. */
    UpgradeComputer<Boolean> getExpPickup();

    /** True if the pickup auto-toggle is currently on. */
    boolean isPickupEnabled();

    /** Sets the pickup auto-toggle directly (skips the {@link #activate()} owner messaging). */
    void setPickupEnabled(boolean enabled);

    /** Snapshot of the Pickup auto-toggle (persisted or live). */
    record State(boolean active) implements SkillState {}
}