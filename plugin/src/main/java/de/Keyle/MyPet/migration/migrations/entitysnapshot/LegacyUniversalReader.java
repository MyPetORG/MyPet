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

package de.Keyle.MyPet.migration.migrations.entitysnapshot;

import de.Keyle.MyPet.MyPetApi;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import org.bukkit.Material;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Donkey;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Mule;
import org.bukkit.entity.SkeletonHorse;
import org.bukkit.entity.TraderLlama;
import org.bukkit.entity.ZombieHorse;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Universal legacy-NBT keys shared across every pet type: the {@code Equipment}
 * list and the {@code Baby} flag. Previously implemented in the base
 * {@code Pet#readExtendedInfo}; relocated here so the production class is
 * legacy-free.
 *
 * <p>Owned by the EntitySnapshot migration; will be deleted at v5.
 */
public final class LegacyUniversalReader {

    private LegacyUniversalReader() {
    }

    public static void apply(Mob mob, CompoundBinaryTag info) {
        applyEquipment(mob, info);
        applyBaby(mob, info);
    }

    private static void applyEquipment(Mob mob, CompoundBinaryTag info) {
        if (!info.keySet().contains("Equipment")) return;
        ListBinaryTag list = info.getList("Equipment", BinaryTagTypes.COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundBinaryTag itemTag = list.getCompound(i);
            String slotName = itemTag.getString("Slot");
            if (slotName.isEmpty()) continue;
            ItemStack item;
            try {
                item = LegacyNbtItemDecoder.decode(itemTag);
            } catch (Exception e) {
                MyPetApi.getLogger().warning("LegacyUniversalReader: bad Equipment item tag");
                continue;
            }
            applySlot(mob, slotName, item);
        }
    }

    private static void applySlot(Mob mob, String slotName, ItemStack item) {
        // Horse-family special slots — these slot names don't exist as
        // EquipmentSlot enum values across all supported MC versions.
        if (mob instanceof AbstractHorse horse) {
            if ("SADDLE".equals(slotName)) {
                if (item != null && item.getType() == Material.SADDLE) {
                    horse.getInventory().setSaddle(item);
                }
                return;
            }
            if ("BODY".equals(slotName)) {
                if (horse instanceof Horse h) {
                    h.getInventory().setArmor(item);
                } else if (horse instanceof Llama l) {
                    if (item != null && item.getType().name().endsWith("CARPET")) {
                        l.getInventory().setDecor(item);
                    }
                } else if (horse instanceof TraderLlama tl) {
                    if (item != null && item.getType().name().endsWith("CARPET")) {
                        tl.getInventory().setDecor(item);
                    }
                }
                // Donkey / Mule / SkeletonHorse / ZombieHorse have no body armor slot.
                return;
            }
            if ("CHEST".equals(slotName) && (horse instanceof Donkey || horse instanceof Mule)) {
                // No-op: chest contents would need ItemStack[] handling; legacy
                // schema didn't store a real chest inventory under the Equipment list.
                return;
            }
            // SkeletonHorse / ZombieHorse fall through with no special handling.
            if (horse instanceof SkeletonHorse || horse instanceof ZombieHorse) {
                return;
            }
        }

        // Standard slots. Wrapping in try/catch skips slot names that don't
        // exist as EquipmentSlot enum values (e.g. between MC versions).
        try {
            EquipmentSlot slot = EquipmentSlot.valueOf(slotName);
            EntityEquipment eq = mob.getEquipment();
            if (eq != null) {
                eq.setItem(slot, item);
            }
        } catch (IllegalArgumentException ignored) {
        }
    }

    private static void applyBaby(Mob mob, CompoundBinaryTag info) {
        if (!info.keySet().contains("Baby")) return;
        if (mob instanceof Ageable a) {
            if (info.getBoolean("Baby")) {
                a.setBaby();
            } else {
                a.setAdult();
            }
        }
    }
}
