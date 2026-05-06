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

package de.Keyle.MyPet.entity.spawn;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.ComplexEntityPart;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.persistence.PersistentDataType;

/**
 * Manages the PDC marker used to tag real Bukkit mobs as MyPet pets.
 * <p>
 * The marker is a single byte under {@code mypet:pet}. Listeners and hook integrations
 * use this marker as a fast predicate instead of an instanceof check on a wrapper
 * type — pets are real vanilla mobs in v4, with no dedicated entity class.
 */
public final class PetEntityMarker {

    public static final NamespacedKey KEY = new NamespacedKey("mypet", "pet");

    private PetEntityMarker() {
    }

    public static void mark(Mob mob) {
        mob.getPersistentDataContainer().set(KEY, PersistentDataType.BYTE, (byte) 1);
    }

    public static boolean isMarked(Entity entity) {
        // Sub-parts of a ComplexLivingEntity (EnderDragon head/neck/body/tail/
        // wings) carry their own entity ID server-side. PlayerInteractEntityEvent
        // and friends fire with the part as the clicked entity, but the PDC
        // marker is set on the parent only — resolve so callers don't need to
        // know about parts.
        if (entity instanceof ComplexEntityPart part) {
            entity = part.getParent();
        }
        return entity != null && entity.getPersistentDataContainer().has(KEY, PersistentDataType.BYTE);
    }
}
