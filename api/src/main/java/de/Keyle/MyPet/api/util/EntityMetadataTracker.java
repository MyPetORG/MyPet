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

package de.Keyle.MyPet.api.util;

import de.Keyle.MyPet.MyPetApi;
import org.bukkit.entity.Entity;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.*;

/**
 * Keeps track of the Bukkit metadata MyPet puts on entities it does not own (mobs a pet damaged,
 * projectiles fired for ranged leashing). Bukkit only drops metadata when it is removed explicitly,
 * so metadata on an entity that despawns or is unloaded instead of dying would stay in the server's
 * metadata store forever. Entities set through this class are cleaned up by a periodic sweep once
 * they have been gone for a while.
 * <p>
 * Must only be used from the main thread.
 */
public class EntityMetadataTracker {

    private static final Map<UUID, TrackedEntity> TRACKED = new HashMap<>();

    private EntityMetadataTracker() {
    }

    public static void setMetadata(Entity entity, String key, Object value) {
        entity.setMetadata(key, new FixedMetadataValue(MyPetApi.getPlugin(), value));
        TrackedEntity tracked = TRACKED.get(entity.getUniqueId());
        if (tracked == null) {
            tracked = new TrackedEntity(entity);
            TRACKED.put(entity.getUniqueId(), tracked);
        } else {
            // the entity may have been unloaded and reloaded as a new object
            tracked.entity = entity;
        }
        tracked.keys.add(key);
    }

    public static void removeMetadata(Entity entity, String key) {
        entity.removeMetadata(key, MyPetApi.getPlugin());
        TrackedEntity tracked = TRACKED.get(entity.getUniqueId());
        if (tracked != null) {
            tracked.keys.remove(key);
            if (tracked.keys.isEmpty()) {
                TRACKED.remove(entity.getUniqueId());
            }
        }
    }

    /**
     * Removes all tracked metadata from the given entity.
     */
    public static void removeAll(Entity entity) {
        TrackedEntity tracked = TRACKED.remove(entity.getUniqueId());
        if (tracked != null) {
            for (String key : tracked.keys) {
                entity.removeMetadata(key, MyPetApi.getPlugin());
            }
        }
    }

    /**
     * How many sweeps in a row an entity has to be missing before its metadata is dropped. The
     * sweep runs every minute, so a mob that is only unloaded keeps what pets did to it for ten
     * minutes, in case its owner comes back.
     */
    private static final int SWEEPS_BEFORE_REMOVAL = 10;

    /**
     * Removes the metadata of tracked entities that have been gone (dead, despawned or unloaded)
     * for {@link #SWEEPS_BEFORE_REMOVAL} sweeps in a row.
     * <p>
     * Bukkit stores entity metadata by UUID, so a mob that unloads and loads again comes back as a
     * new object that still carries the metadata. Removing it through the old, invalid object
     * would also remove it from the live mob, so a mob that is loaded again is picked up by its
     * UUID instead.
     *
     * @return the number of entities that were cleaned up
     */
    public static int sweep() {
        int removed = 0;
        Iterator<Map.Entry<UUID, TrackedEntity>> iterator = TRACKED.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, TrackedEntity> entry = iterator.next();
            TrackedEntity tracked = entry.getValue();
            if (tracked.entity.isValid()) {
                tracked.missingSweeps = 0;
                continue;
            }
            Entity loaded = findLoaded(entry.getKey());
            if (loaded != null) {
                tracked.entity = loaded;
                tracked.missingSweeps = 0;
                continue;
            }
            if (++tracked.missingSweeps < SWEEPS_BEFORE_REMOVAL) {
                continue;
            }
            for (String key : tracked.keys) {
                tracked.entity.removeMetadata(key, MyPetApi.getPlugin());
            }
            iterator.remove();
            removed++;
        }
        return removed;
    }

    private static Entity findLoaded(UUID uuid) {
        try {
            Entity entity = MyPetApi.getPlatformHelper().getEntityByUUID(uuid);
            return entity != null && entity.isValid() ? entity : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static class TrackedEntity {
        Entity entity;
        int missingSweeps = 0;
        final Set<String> keys = new HashSet<>(2);

        TrackedEntity(Entity entity) {
            this.entity = entity;
        }
    }
}
