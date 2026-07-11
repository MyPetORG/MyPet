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

package de.Keyle.MyPet.util.sound;

import de.Keyle.MyPet.api.entity.Pet;
import org.bukkit.World;
import org.bukkit.entity.Mob;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Entity-id → Pet index used by the sound packet listener. Populated by
 * {@link PetSoundLifecycleHook} on spawn / despawn and by
 * {@link PetSoundService}'s cold-path sweep at service start.
 *
 * <p>Backed by {@link ConcurrentHashMap} for lock-free reads on the
 * Netty I/O thread where the packet listener fires.
 */
public final class PetSoundRegistry {

    private static final Map<Integer, Pet> BY_ENTITY_ID = new ConcurrentHashMap<>();

    private PetSoundRegistry() {}

    public static void add(int entityId, Pet pet) {
        BY_ENTITY_ID.put(entityId, pet);
    }

    public static void remove(int entityId) {
        BY_ENTITY_ID.remove(entityId);
    }

    public static Pet find(int entityId) {
        return BY_ENTITY_ID.get(entityId);
    }

    public static void clear() {
        BY_ENTITY_ID.clear();
    }

    public static int size() {
        return BY_ENTITY_ID.size();
    }

    /**
     * Resolves a Pet by approximate world position. Used for positional sound
     * packets (which carry coordinates instead of an entity id). Iterates
     * this registry's own index (no allocation), O(active pets) per call.
     * Threshold is one block squared — sounds are emitted at the entity's
     * exact position and we tolerate a tick of drift.
     *
     * @return the closest matching pet, or {@code null} if none.
     */
    public static Pet findAtPosition(World world, double x, double y, double z) {
        if (world == null) return null;
        for (Pet pet : BY_ENTITY_ID.values()) {
            Mob mob = pet.getBukkitEntity();
            if (mob == null) continue;
            if (mob.getWorld() != world) continue;
            double dx = mob.getX() - x;
            double dy = mob.getY() - y;
            double dz = mob.getZ() - z;
            if (dx * dx + dy * dy + dz * dz < 1.0) return pet;
        }
        return null;
    }
}
