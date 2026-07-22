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
import java.util.List;
import org.bukkit.inventory.ItemStack;

/**
 * Skill that lets the pet periodically "sniff out" a drop and leave it beside itself.
 * The drops are configured per skilltree as a weighted pool (see {@link DropEntry});
 * one entry is chosen at random by weight on each dig.
 */
@SkillName(value = "Sniff", translationNode = "Name.Skill.Sniff")
public interface Sniff extends Skill, Scheduler, ToggleableSkill {

    /** One weighted drop: a full item and the random stack-size range [amountMin, amountMax]. */
    record DropEntry(ItemStack item, int weight, int amountMin, int amountMax) {
    }

    /** Returns the upgrade computer controlling the seconds between drops (lower is better). */
    UpgradeComputer<Integer> getInterval();

    /** Adds a drop entry to this pet's pool (called per granted skilltree level). */
    void addDrop(DropEntry entry);

    /** Removes a previously-added drop entry (skilltree level-down / invert). */
    void removeDrop(DropEntry entry);

    /** The live weighted drop pool for this pet. */
    List<DropEntry> getDropPool();
}
