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

package de.Keyle.MyPet.skill.skills;

import de.Keyle.MyPet.api.entity.Pet;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-pet mutual exclusion for the autonomous "go do a chore" skills — Mining, Lumberjack,
 * Fishing, Sniff. It lets a pet visibly commit to one task at a time (walk to the tree, chop
 * it, collect) instead of trying to mine, chop, fish and sniff on the same tick. The holder is
 * the skill instance currently working; it must release when its operation finishes or aborts.
 *
 * <p>All access for a given pet happens on that pet's own region thread (every skill ticks
 * through the pet's {@code EntityScheduler}), so no per-pet locking is needed — the map is
 * concurrent only because different pets tick on different threads. Entries exist only while a
 * pet is mid-chore, so the map self-trims.
 */
public final class PetWorkFocus {

    /** Ticks a work skill lingers at the site after finishing so the Pickup skill can grab the drops. */
    static final long PICKUP_LINGER_TICKS = 20L;

    private static final Map<UUID, Object> HOLDER = new ConcurrentHashMap<>();
    /**
     * Sticky claim: a skill mid multi-cycle job (e.g. Lumberjack felling a whole tree one bite at a
     * time) reserves the pet so other work skills defer until it releases — see {@link #reserve}.
     */
    private static final Map<UUID, Object> RESERVED = new ConcurrentHashMap<>();

    private PetWorkFocus() {
    }

    /** True if this pet is mid-chore at all (actively working or reserved) — used to pause follow-navigation. */
    public static boolean isWorking(Pet pet) {
        return HOLDER.containsKey(pet.getUUID()) || RESERVED.containsKey(pet.getUUID());
    }

    /** True if the pet is busy for a skill other than {@code skill} — actively working this tick, or reserved. */
    static boolean isBusy(Pet pet, Object skill) {
        Object holder = HOLDER.get(pet.getUUID());
        if (holder != null && holder != skill) {
            return true;
        }
        Object reserved = RESERVED.get(pet.getUUID());
        return reserved != null && reserved != skill;
    }

    /** Grabs the focus for {@code skill} if it is free (or already held by it); false if another holds it. */
    static boolean acquire(Pet pet, Object skill) {
        Object current = HOLDER.putIfAbsent(pet.getUUID(), skill);
        return current == null || current == skill;
    }

    /** Releases the focus, but only if {@code skill} is the current holder. */
    static void release(Pet pet, Object skill) {
        HOLDER.remove(pet.getUUID(), skill);
    }

    /** Reserves the pet for a multi-cycle job so other work skills defer until {@link #clearReservation}. */
    static void reserve(Pet pet, Object skill) {
        RESERVED.put(pet.getUUID(), skill);
    }

    /** Drops {@code skill}'s standing reservation (only if it holds it). */
    static void clearReservation(Pet pet, Object skill) {
        RESERVED.remove(pet.getUUID(), skill);
    }
}
