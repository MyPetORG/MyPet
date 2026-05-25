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
 * Marker for pets that can be mounted in vanilla Minecraft. Used by the
 * cross-cutting ride-gating listeners ({@code RideInteractListener},
 * {@code PetMountGateListener}) to scope their gate logic to only the
 * pet types that vanilla considers rideable.
 *
 * <p>Drives the auto-registration of two per-pet config flags from
 * {@code ConfigurationLoader.setDefault}:
 *
 * <ul>
 *   <li>{@code MyPet.Pets.<Type>.RequireRideSkill} (default {@code true})</li>
 *   <li>{@code MyPet.Pets.<Type>.AllowNonOwnerPrimaryMount} (default {@code false})</li>
 * </ul>
 *
 * <p>Distinct from the MyPet Ride <em>skill</em> — this marker is about
 * whether vanilla's mount mechanism applies to the pet type, not whether
 * the Ride skill is granted to a specific pet.
 *
 * <p>Pure tag interface — no methods, no defaults. Pet types declare
 * {@code implements PetNaturallyRideable} on the matching {@code PetXxx}
 * class. Initial implementers: Horse, Donkey, Mule, SkeletonHorse,
 * ZombieHorse, Camel, CamelHusk, Pig, Strider, Llama, TraderLlama,
 * HappyGhast, Nautilus, ZombieNautilus.
 *
 * <p>Adding a future rideable Pet type is a one-line marker addition;
 * the YAML rows materialize automatically via the registration loop.
 */
public interface PetNaturallyRideable {
}
