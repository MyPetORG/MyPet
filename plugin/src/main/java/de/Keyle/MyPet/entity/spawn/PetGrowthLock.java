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
import org.bukkit.entity.Mob;
import org.bukkit.persistence.PersistentDataType;

/**
 * PDC helper for a baby pet's owner-chosen age-lock override (set in-game with a
 * golden dandelion on MC 26.1+).
 *
 * <p>Tri-state: an <b>absent</b> key means "no override — use the per-type
 * {@code PreventNaturalGrowup} config default"; a present byte {@code 1} means the
 * owner explicitly locked this pet (frozen), {@code 0} means the owner
 * explicitly unlocked it (ages normally). Stored in the mob's PDC so it
 * round-trips through {@code PetEntitySnapshot}'s full-NBT serialize/restore,
 * exactly like the {@code mypet:pet} marker — no DB column.
 */
public final class PetGrowthLock {

    public static final NamespacedKey OVERRIDE_KEY = new NamespacedKey("mypet", "age_lock_override");

    private PetGrowthLock() {
    }

    /**
     * The owner's per-pet age-lock override, or {@code null} if none is set
     * (the caller should fall back to the {@code PreventNaturalGrowup} config default).
     * {@code Boolean.TRUE} = locked/frozen, {@code Boolean.FALSE} = unlocked.
     */
    public static Boolean getOverride(Mob mob) {
        Byte value = mob.getPersistentDataContainer().get(OVERRIDE_KEY, PersistentDataType.BYTE);
        return value == null ? null : value == 1;
    }

    /** Records the owner's per-pet age-lock choice ({@code true} = locked/frozen). */
    public static void setOverride(Mob mob, boolean locked) {
        mob.getPersistentDataContainer().set(OVERRIDE_KEY, PersistentDataType.BYTE, (byte) (locked ? 1 : 0));
    }
}
