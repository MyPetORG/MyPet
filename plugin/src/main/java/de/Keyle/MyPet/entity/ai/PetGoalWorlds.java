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

package de.Keyle.MyPet.entity.ai;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

/**
 * World checks for goals that compare distances against an owner, target, or
 * destination held across ticks.
 *
 * <p>{@link Location#distanceSquared(Location)} throws {@link IllegalArgumentException}
 * when the two locations are in different worlds, so any goal that caches an entity or
 * location on one tick and measures against it on a later tick must check first — the
 * referenced entity can portal or be teleported away in between.
 *
 * <p>{@code Bukkit.isOwnedByCurrentRegion(...)} is not a substitute: it only tracks Folia
 * region ownership and does not imply same-world on regular Paper.
 */
public final class PetGoalWorlds {

    private PetGoalWorlds() {
    }

    /** Returns {@code true} when the two entities are in different worlds. */
    public static boolean isCrossWorld(Entity a, Entity b) {
        return !a.getWorld().equals(b.getWorld());
    }

    /** Returns {@code true} when the entity and the location are in different worlds. */
    public static boolean isCrossWorld(Entity entity, Location location) {
        return !entity.getWorld().equals(location.getWorld());
    }
}
