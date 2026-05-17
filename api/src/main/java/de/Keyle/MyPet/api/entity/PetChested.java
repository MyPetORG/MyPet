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
 * Marker for pets that expose the {@code chest} creation flag (gives the
 * underlying mob a carried chest at spawn via
 * {@link org.bukkit.entity.ChestedHorse#setCarryingChest(boolean)}). The
 * pet's Bukkit class must implement
 * {@link org.bukkit.entity.ChestedHorse}; otherwise the marker is a no-op.
 *
 * <p>{@code PetCreationOptions} auto-generates a {@code chest} flag spec
 * for every pet that implements this marker, so no per-pet
 * {@code CREATION_SPECS} row is needed.
 */
public interface PetChested {
}
