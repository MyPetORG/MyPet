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

package de.Keyle.MyPet.api.entity;

import de.Keyle.MyPet.api.Configuration;

/**
 * Marker for pet types whose underlying vanilla mob is a strict water-breather
 * — swims naturally and takes vanilla {@code drown}-type damage when exposed
 * to air. The cohort that extends Mojang's {@code WaterMob} (fish, squids,
 * guardians, dolphin) plus Tadpole (which uses the same out-of-water
 * suffocation tick).
 *
 * <p>Sibling to {@link MyPetAmphibiousEntity} under the shared
 * {@link MyPetSwimmingEntity} base. The swim physics gate (`canSwim()`) is
 * inherited from the base; this marker adds the suffocation gate.
 *
 * <p>The {@link #preventSuffocation()} default consults the per-pet
 * preference loaded from {@code MyPet.Pets.<Type>.PreventSuffocation} in
 * {@code pet-config.yml}. The YAML row is auto-registered for every type
 * that implements this marker. Wiring lives in {@code PetSurvivalListener}'s
 * {@code EntityDamageEvent} arm: when the flag is {@code true}
 * {@code DamageCause.DROWNING} is canceled for marked pets (Bukkit reuses
 * {@code DROWNING} for both "land-breather underwater" and "water-breather
 * in air"; only the latter case applies to these mobs).
 */
public interface MyPetAquaticEntity extends MyPetSwimmingEntity {

    default boolean preventSuffocation() {
        return Configuration.MyPet.preventSuffocation(getPetType());
    }
}
