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

import de.Keyle.MyPet.api.config.PetConfigLookup;

/**
 * Marker for pet types that vanilla matures into a <i>different species</i> once
 * an internal age timer expires. Tadpole → Frog is the only vanilla instance.
 *
 * <p>Deliberately not {@link PetBaby}: that marker models the baby↔adult
 * lifecycle of a single {@code Ageable} species (and gates the shop's "babies"
 * section plus the {@link DefaultInfo#growUpItem()} interaction). A
 * metamorphosis discards the host entity and adds a new one of another type, so
 * the {@link Pet} domain object has to be re-typed as well — the same flow as
 * {@link PetZombifiable} and {@link PetLightningConvertible}, via
 * {@code PetManager#convertPetType}, preserving UUID, name, XP, skill state,
 * and owner.
 *
 * <p>Reads {@code MyPet.Pets.<Type>.AllowMetamorphosis}, default {@code false}:
 * a pet keeps the type its owner tamed rather than silently becoming a
 * different mob, matching the {@code AllowZombification} /
 * {@code AllowLightningConversion} stance. The {@code false} case is enforced up
 * front by locking the host entity's age timer when the pet spawns
 * ({@code VanillaMobSpawner}); {@code PetMetamorphosisListener} is the backstop
 * for a pet whose timer was unlocked by something else.
 */
public interface PetMetamorphic extends Pet {

    default boolean allowMetamorphosis() {
        return PetConfigLookup.boolValue(getClass(), "AllowMetamorphosis", false);
    }
}
