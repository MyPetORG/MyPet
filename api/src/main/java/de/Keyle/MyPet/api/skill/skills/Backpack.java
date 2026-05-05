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

import de.Keyle.MyPet.api.skill.ActiveSkill;
import de.Keyle.MyPet.api.skill.SkillName;
import de.Keyle.MyPet.api.skill.SkillState;
import de.Keyle.MyPet.api.skill.UpgradeComputer;
import de.Keyle.MyPet.api.skill.skilltree.Skill;
import de.Keyle.MyPet.api.util.NBTStorage;
import de.Keyle.MyPet.api.util.inventory.CustomInventory;

/**
 * Skill that grants the pet a portable inventory (backpack) which the owner can open
 * to store and retrieve items. The number of available inventory rows scales with
 * the pet's skilltree level. Activating the skill opens the backpack GUI for the owner.
 *
 * <p>This skill persists its inventory contents via {@link NBTStorage} and supports
 * an optional "drop on death" mechanic where all stored items are dropped when the
 * pet dies.
 *
 * @see ActiveSkill#activate()
 * @see CustomInventory
 */
@SkillName(value = "Backpack", translationNode = "Name.Skill.Inventory")
public interface Backpack extends Skill, NBTStorage, ActiveSkill {

    /** Returns the pet's backpack inventory instance. */
    CustomInventory getInventory();

    /** Returns the upgrade computer controlling whether items drop when the pet dies. */
    UpgradeComputer<Boolean> getDropOnDeath();

    /** Returns the upgrade computer controlling the number of inventory rows available. */
    UpgradeComputer<Number> getRows();

    /**
     * Snapshot of a Backpack skill's persisted or live contents. The
     * {@link CustomInventory} is owned by the snapshot — for persisted
     * pets it's a freshly materialized read-only view; for live pets it's
     * the same instance the live skill exposes via {@link #getInventory()},
     * so mutations write through.
     */
    record State(CustomInventory inventory) implements SkillState {}
}