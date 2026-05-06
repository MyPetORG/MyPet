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
 * Skill that determines the pet's base melee attack damage. The damage value scales
 * with the pet's skilltree level, replacing the mob's default attack damage attribute.
 *
 * <p>This is a fundamental combat skill -- without it (or at level zero) the pet deals
 * no melee damage.
 */
@SkillName(value = "Damage", translationNode = "Name.Skill.Damage")
public interface Damage extends Skill {

    /** Returns the upgrade computer controlling the pet's melee attack damage value. */
    UpgradeComputer<Number> getDamage();
}