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

import de.Keyle.MyPet.api.skill.SkillName;
import de.Keyle.MyPet.api.skill.UpgradeComputer;
import de.Keyle.MyPet.api.skill.skilltree.Skill;

/**
 * Skill that gives the pet a chance to intercept damage aimed at its owner, redirecting
 * a percentage of the incoming damage to itself instead. Both the trigger chance and the
 * fraction of damage redirected scale with the pet's skilltree level.
 *
 * <p>When triggered, the owner takes reduced damage and the pet absorbs the redirected
 * portion.
 */
@SkillName(value = "Shield", translationNode = "Name.Skill.Shield")
public interface Shield extends Skill {

    /** Returns the upgrade computer controlling the percent chance to intercept damage for the owner. */
    UpgradeComputer<Integer> getChance();

    /** Returns the upgrade computer controlling the percentage of damage redirected from owner to pet. */
    UpgradeComputer<Integer> getRedirectedDamage();
}