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
 * Marker for pet types whose underlying vanilla mob combusts in direct
 * sunlight — the cohort whose Mojang counterpart returns {@code true} from
 * {@code Mob#isSunSensitive()} (zombies, skeletons, and their variants) plus
 * Phantom (which has its own sunlight check on {@code aiStep}).
 *
 * <p>The {@link #preventDaylightBurn()} default consults the per-pet
 * preference loaded from {@code MyPet.Pets.<Type>.PreventDaylightBurn} in
 * {@code pet-config.yml}. The YAML row is auto-registered for every type that
 * implements this marker — adding a new sun-sensitive pet only requires
 * implementing this interface. Wiring lives in {@code PetSurvivalListener}'s
 * {@code EntityCombustEvent} arm: when the flag is {@code true} the natural
 * combust is canceled before the visible flame appears. This flag does not
 * gate block-caused combust (lava, magma) and entity-caused combust (flame arrows).
 *
 * <p>Husk, ZombifiedPiglin, and WitherSkeleton are deliberately omitted —
 * vanilla overrides {@code isSunSensitive()} to {@code false} for those
 * types, so the event never fires and the knob would be misleading.
 */
public interface MyPetSunSensitive extends MyPet {

    default boolean preventDaylightBurn() {
        return Configuration.MyPet.preventDaylightBurn(getPetType());
    }
}
