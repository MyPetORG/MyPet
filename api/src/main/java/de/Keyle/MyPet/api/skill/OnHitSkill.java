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

package de.Keyle.MyPet.api.skill;

import org.bukkit.entity.LivingEntity;

/**
 * Interface for skills that trigger when the pet <em>deals</em> melee damage to a
 * target. The attack goal calls {@link #trigger()} to determine whether the skill
 * fires (often a random chance), then invokes {@link #apply(LivingEntity)} to
 * execute the on-hit effect (e.g., Poison, Fire, Wither, Slow).
 *
 * <p>Called from {@code PetMeleeAttackGoal#applyPetDamage} in the plugin module.
 *
 * @see OnDamageByEntitySkill
 * @see ActiveSkill
 */
public interface OnHitSkill {

    /**
     * Determines whether this skill should fire for the current hit.
     * Typically based on a random chance scaled by the skill's upgrade level.
     *
     * @return {@code true} if the skill triggers; {@code false} to skip
     */
    boolean trigger();

    /**
     * Applies this skill's effect to the target that the pet just hit.
     *
     * @param target the living entity damaged by the pet
     */
    void apply(LivingEntity target);
}