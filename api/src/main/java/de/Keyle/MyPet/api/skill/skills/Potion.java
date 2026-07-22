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
import org.bukkit.potion.PotionType;

import java.util.List;

/**
 * Skill that turns the pet into a field alchemist: the skilltree grants, per level, which splash
 * potions the pet may throw (its <em>arsenal</em>), each with its own cooldown. The pet throws from
 * real bottles in its Backpack, aiming each by the potion's effect category — harmful potions at the
 * mob its {@link Behavior} has targeted, helpful ones at its owner. The {@code materialize} upgrade
 * lets it conjure permitted potions without backpack stock.
 *
 * @see Scheduler
 */
@SkillName(value = "Potion", translationNode = "Name.Skill.Potion")
public interface Potion extends Skill, Scheduler {

    /** One permitted potion and how many seconds must pass between throws of it. */
    record Entry(PotionType type, int cooldown) {}

    /** Adds a permitted potion to the arsenal (called as the skilltree upgrades apply). */
    void addEntry(Entry entry);

    /** Removes a permitted potion from the arsenal (upgrade inversion). */
    void removeEntry(Entry entry);

    /** The current arsenal — every potion the pet may throw, with its cooldown. */
    List<Entry> getArsenal();

    /** When granted, the pet conjures permitted potions with no backpack stock (the {@code toolless} analog). */
    UpgradeComputer<Boolean> getMaterialize();
}
