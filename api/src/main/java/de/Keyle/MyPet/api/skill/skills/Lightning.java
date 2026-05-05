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

import de.Keyle.MyPet.api.skill.OnHitSkill;
import de.Keyle.MyPet.api.skill.SkillName;
import de.Keyle.MyPet.api.skill.UpgradeComputer;
import de.Keyle.MyPet.api.skill.skilltree.Skill;

/**
 * Skill that gives the pet a chance to strike the target with a lightning bolt on hit.
 * The lightning deals configurable bonus damage in addition to the visual/sound effect.
 * Both the trigger chance and damage scale with the pet's skilltree level.
 *
 * @see OnHitSkill
 */
@SkillName(value = "Lightning", translationNode = "Name.Skill.Lightning")
public interface Lightning extends Skill, OnHitSkill {

    /** Returns the upgrade computer controlling the percent chance to strike lightning on hit. */
    UpgradeComputer<Integer> getChance();

    /** Returns the upgrade computer controlling the bonus damage dealt by the lightning strike. */
    UpgradeComputer<Number> getDamage();
}