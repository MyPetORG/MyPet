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

package de.Keyle.MyPet.api.behavior;

import de.Keyle.MyPet.api.entity.Pet;
import org.bukkit.entity.Mob;
import org.bukkit.event.Event;

/**
 * Functional handler invoked by the central dispatcher when a {@link Event}
 * fires on a pet whose type matches a registered {@link PetBehavior}.
 *
 * <p>The handler receives the typed event plus the already-matched pet and
 * its live Bukkit {@link Mob} — no {@code instanceof PetEntityMarker} check,
 * no {@code MyPetApi.getPetManager().getPetFromEntity(...)} call, no class
 * cast. The dispatcher does all of that before invocation.
 */
@FunctionalInterface
public interface PetEventHandler<E extends Event> {
    void accept(E event, Pet pet, Mob mob);
}
