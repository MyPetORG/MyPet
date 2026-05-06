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

import de.Keyle.MyPet.api.skill.OnDamageByEntitySkill;
import de.Keyle.MyPet.api.skill.SkillName;
import de.Keyle.MyPet.api.skill.UpgradeComputer;
import de.Keyle.MyPet.api.skill.skilltree.Skill;

/**
 * Skill that gives the pet a chance to reflect a portion of incoming melee damage back
 * to the attacker. When triggered, the entity that hit the pet receives damage equal to
 * the configured percentage of the original hit. Both the reflection percentage and
 * trigger chance scale with the pet's skilltree level.
 *
 * @see OnDamageByEntitySkill
 */
@SkillName(value = "Thorns", translationNode = "Name.Skill.Thorns")
public interface Thorns extends Skill, OnDamageByEntitySkill {

    /** Returns the upgrade computer controlling the percentage of damage reflected back to the attacker. */
    UpgradeComputer<Integer> getReflectedDamage();

    /** Returns the upgrade computer controlling the percent chance to reflect damage when hit. */
    UpgradeComputer<Integer> getChance();
}