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
 * Skill that enables the pet to perform ranged attacks by launching projectiles at
 * targets. The projectile type, rate of fire, and damage all scale with the pet's
 * skilltree level.
 *
 * <p>The pet will use ranged attacks instead of (or in addition to) melee when this
 * skill is active and the target is within range.
 *
 * @see Projectile
 */
@SkillName(value = "Ranged", translationNode = "Name.Skill.Ranged")
public interface Ranged extends Skill {

    /** Returns the upgrade computer controlling the delay (in ticks) between consecutive shots. */
    UpgradeComputer<Integer> getRateOfFire();

    /** Returns the upgrade computer controlling the damage dealt per projectile. */
    UpgradeComputer<Number> getDamage();

    /** Returns the upgrade computer controlling the type of projectile launched. */
    UpgradeComputer<Projectile> getProjectile();

    /**
     * Enumerates the projectile types the pet can fire with the Ranged skill.
     */
    enum Projectile {
        Arrow, Snowball, LargeFireball, SmallFireball, WitherSkull, Egg, DragonFireball, Trident, EnderPearl, LlamaSpit
    }
}