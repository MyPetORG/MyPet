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

import java.util.function.Supplier;

/**
 * Thread-scoped flag marking "the spawn happening right now is MyPet summoning a pet."
 * <p>
 * A source-driven pet (e.g. a MythicMob) is spawned by its provider plugin, not by
 * MyPet's own {@code world.spawn}/{@code spawnAt} call — so the entity carries no
 * {@link PetEntityMarker} yet when its {@code CreatureSpawnEvent} fires, and a region
 * plugin refusing that event cannot be told apart from an ordinary wild spawn. This
 * guard lets {@code PetEnvironmentListener#onCreatureSpawn} un-cancel that spawn on the
 * strength of "MyPet asked for this right now" instead of the marker.
 * <p>
 * A {@link ThreadLocal} rather than a plain static field: Folia runs region threads in
 * parallel, and a static flag would leak the "summon in progress" state across regions.
 * Always cleared in a {@code finally} — an exception leaving it set would reopen the
 * exact hole the old {@code fixMissingEntityType} hack left (spawn-cancel exemption
 * stuck on), just thread-scoped instead of world-scoped.
 */
public final class PetSpawnGuard {

    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);

    private PetSpawnGuard() {
    }

    public static boolean isActive() {
        return ACTIVE.get();
    }

    /**
     * Runs {@code supplier} with the guard active on the calling thread. The guard is
     * cleared in a {@code finally}, so it never stays set past this call even if
     * {@code supplier} throws.
     */
    public static <T> T runGuarded(Supplier<T> supplier) {
        ACTIVE.set(true);
        try {
            return supplier.get();
        } finally {
            ACTIVE.set(false);
        }
    }
}
