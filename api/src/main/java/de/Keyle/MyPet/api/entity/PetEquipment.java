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

import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

/**
 * Marker for pet types that can wear or hold equipment (armor, weapons,
 * saddles). The equipment is persisted in the pet's NBT info blob and
 * re-applied by {@code PetVisualSyncer} after each spawn.
 * <p>
 * Not all slots are valid for every pet type — horses use BODY/SADDLE,
 * zombies use the full humanoid set. Implementations override
 * {@link #getAllowedSlotNames()} to declare which slots are available.
 */
public interface PetEquipment {

    /** Returns all currently equipped items, indexed by slot ordinal. */
    ItemStack[] getEquipment();

    /** Returns the item in the given slot, or {@code null} if empty. */
    ItemStack getEquipment(EquipmentSlot slot);

    /** Equips an item in the given slot (persisted across despawns). */
    void setEquipment(EquipmentSlot slot, ItemStack item);

    /** Drops all equipment as world items at the pet's location. */
    void dropEquipment();

    /**
     * Returns the set of equipment slot names this pet type can use.
     * Uses string names instead of EquipmentSlot enum values for version compatibility,
     * since SADDLE and BODY slots don't exist in older Bukkit versions.
     *
     * @return set of slot names (e.g., "HAND", "OFF_HAND", "HEAD", "CHEST", "LEGS", "FEET", "SADDLE", "BODY")
     */
    default Set<String> getAllowedSlotNames() {
        // Default: standard humanoid equipment slots
        return Set.of("HAND", "OFF_HAND", "HEAD", "CHEST", "LEGS", "FEET");
    }

    /**
     * Checks if this pet type can use the given equipment slot.
     *
     * @param slot the equipment slot to check
     * @return true if the pet can equip items in this slot
     */
    default boolean canUseSlot(EquipmentSlot slot) {
        return getAllowedSlotNames().contains(slot.name());
    }
}