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

/**
 * Marker for pets that carry more than one rider in vanilla. Subset of
 * {@link PetNaturallyRideable}.
 *
 * <p>Drives the auto-registration of the
 * {@code MyPet.Pets.<Type>.AllowNonOwnerSecondaryMount} config flag
 * (default {@code true}) from {@code ConfigurationLoader.setDefault} —
 * the flag only appears in {@code pet-config.yml} for pets that actually
 * have a secondary seat to mount.
 *
 * <p>Vanilla seat-count enforcement (Camel: 2, HappyGhast: 4) is
 * delegated to the underlying Bukkit {@code Mob#canAddPassenger} check
 * which MyPet does not second-guess. The marker only signals "this pet
 * has more than one seat;" the actual capacity check happens at
 * {@code addPassenger} time.
 *
 * <p>Pure tag interface — no methods, no defaults. Initial implementers:
 * PetCamel (2 seats), PetCamelHusk (2 seats), PetHappyGhast (4 seats).
 */
public interface PetMultiPassenger {
}
