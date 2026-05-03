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

package de.Keyle.MyPet.api.entity;

import de.Keyle.MyPet.api.Configuration;

/**
 * Marker for nether-native pet types that vanilla converts to a zombified
 * form when they spend too long in the Overworld (Hoglin → Zoglin, Piglin →
 * ZombifiedPiglin, PiglinBrute → ZombifiedPiglin). The
 * {@link #allowZombification()} default consults the per-pet preference
 * loaded from {@code MyPet.Pets.<Type>.AllowZombification} in
 * {@code pet-config.yml}; the default is {@code false} so pets stay their
 * original type.
 *
 * <p>The YAML row is auto-registered for every type that implements this marker
 * — adding a new convertible pet only requires implementing this interface.
 *
 * <p><b>Why "Zombifiable" and not the v3 "Shake" name:</b> the v3-era flag
 * was named {@code WillShake} after the cold-biome shake animation, but
 * vanilla couples the shake animation with the conversion timer through a
 * single {@code isImmuneToZombification} bit — there is no API path that
 * decouples them. The flag's actual consequence is the conversion, so the
 * marker and config row are named for that. The shake animation is
 * suppressed as a side effect when zombification is disallowed.
 */
public interface MyPetZombifiable extends MyPet {

    default boolean allowZombification() {
        return Configuration.MyPet.allowZombification(getPetType());
    }
}
