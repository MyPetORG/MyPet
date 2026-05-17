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
 * Marker for {@link org.bukkit.entity.AbstractHorse}-based pets that
 * expose the {@code saddle} creation flag (puts a saddle in the mob's
 * inventory at spawn). The pet's Bukkit class must extend
 * {@link org.bukkit.entity.AbstractHorse}; otherwise the marker is a no-op.
 *
 * <p>{@code PetCreationOptions} auto-generates a {@code saddle} flag spec
 * for every pet that implements this marker.
 *
 * <p>Pig and Strider use a different saddle setter
 * ({@code setSaddle(boolean)} vs {@code getInventory().setSaddle(...)})
 * and don't go through this marker — they declare a per-pet
 * {@code saddle} flag spec in their {@code CREATION_SPECS} field instead.
 */
public interface PetSaddleable {
}
