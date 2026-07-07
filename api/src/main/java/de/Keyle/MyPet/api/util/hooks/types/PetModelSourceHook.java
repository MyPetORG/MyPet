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

package de.Keyle.MyPet.api.util.hooks.types;

import de.Keyle.MyPet.api.util.service.ServiceContainer;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;

import java.util.Optional;
import java.util.Set;

/**
 * A source of naturally-spawned modeled creatures (MythicMobs, ItemsAdder).
 * Detection feeds tame-time type resolution; {@code spawnSource} reconstructs
 * the genuine creature on create and release. Keyed on the pet type's configured
 * {@code Model.Id} (decoupled from the type name) — nothing is persisted per pet.
 */
public interface PetModelSourceHook extends ServiceContainer {

    /** Adoptable creature ids (MythicMob internal names / ItemsAdder entity namespaced ids); empty if unavailable. */
    Set<String> availableSources();

    /** Normalised source id (matchable to a PetType name) if {@code entity} is this provider's creature. */
    Optional<String> sourceIdOf(Entity entity);

    /**
     * Spawn the source creature whose id matches {@code typeId} and return it as a Bukkit
     * {@code Mob} so the caller can adopt it; empty if this provider does not own that id.
     */
    Optional<Mob> spawnSource(String typeId, Location location);
}
