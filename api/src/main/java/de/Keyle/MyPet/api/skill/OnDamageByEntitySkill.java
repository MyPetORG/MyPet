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
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Interface for skills that react when the pet <em>receives</em> damage from another
 * entity. The combat listener calls {@link #trigger()} first to check if the skill
 * should fire (e.g. random chance based on level), and if it returns {@code true},
 * invokes {@link #apply(LivingEntity, EntityDamageByEntityEvent)} to execute the
 * reactive effect (e.g. Thorns reflecting damage back to the attacker).
 *
 * <p>Implementations are called from {@code PetPvPListener} / pet damage handling.
 *
 * @see OnHitSkill
 * @see ActiveSkill
 */
public interface OnDamageByEntitySkill {

    /**
     * Determines whether this skill should fire for the current damage event.
     * Typically based on a random chance scaled by the skill's upgrade level.
     *
     * @return {@code true} if the skill triggers; {@code false} to skip
     */
    boolean trigger();

    /**
     * Applies this skill's reactive effect.
     *
     * @param damager the entity that dealt damage to the pet
     * @param event   the original damage event (may be modified, e.g. to reduce damage)
     */
    void apply(LivingEntity damager, EntityDamageByEntityEvent event);
}