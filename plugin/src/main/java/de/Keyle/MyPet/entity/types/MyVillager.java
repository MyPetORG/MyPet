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

package de.Keyle.MyPet.entity.types;

import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.MyPet;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

@Getter
public class MyVillager extends MyPet implements de.Keyle.MyPet.api.entity.types.MyVillager {

    /**
     * Profession is stored as a NamespacedKey path (e.g. "armorer", "butcher")
     * so the value is drift-safe across Paper version upgrades that reorder
     * the VillagerProfession registry. The public {@link #getProfession()}
     * int-based API is preserved by converting to the current runtime's
     * ordinal on demand.
     */
    protected String professionKey = "none";
    protected Villager.Type type = Villager.Type.PLAINS;
    protected int level = 1;
    @Setter
    protected CompoundBinaryTag originalData = null;

    public MyVillager(MyPetPlayer petOwner) {
        super(petOwner);
    }

    /**
     * Returns the profession index within the current runtime's
     * {@code Villager.Profession} registry. For backward-API compatibility.
     */
    @Override
    public int getProfession() {
        try {
            Villager.Profession prof = Registry.VILLAGER_PROFESSION.get(
                    NamespacedKey.minecraft(professionKey));
            if (prof != null) return prof.ordinal();
        } catch (Throwable ignored) {
        }
        return 0;
    }

    public String getProfessionKey() {
        return professionKey;
    }

    @Override
    public void setProfession(int value) {
        try {
            Villager.Profession[] values = Villager.Profession.values();
            if (value >= 0 && value < values.length) {
                Villager.Profession prof = values[value];
                if (prof.getKey() != null) {
                    this.professionKey = prof.getKey().getKey();
                }
            }
        } catch (Throwable ignored) {
        }
        if (status == PetState.Here) {
            updateVisuals();
        }
    }

    public void setProfessionKey(String key) {
        if (key != null && !key.isEmpty()) {
            this.professionKey = key;
        }
        if (status == PetState.Here) {
            updateVisuals();
        }
    }

    @Override
    public CompoundBinaryTag writeExtendedInfo() {
        CompoundBinaryTag info = super.writeExtendedInfo();
        info = info.putString("ProfessionKey", professionKey)
                   .putString("VillagerTypeKey", type.getKey().getKey())
                   .putInt("VillagerLevel", this.getLevel());
        if (originalData != null) {
            info = info.put("OriginalData", originalData);
        }
        return info;
    }

    @Override
    public void readExtendedInfo(CompoundBinaryTag info) {
        super.readExtendedInfo(info);
        // Profession — new (key) then legacy (int ordinal)
        if (info.keySet().contains("ProfessionKey")) {
            String key = info.getString("ProfessionKey");
            if (key != null && !key.isEmpty()) {
                this.professionKey = key;
            }
        } else if (info.keySet().contains("Profession")) {
            setProfession(info.getInt("Profession"));
        }
        // Villager type — new (namespaced-key path) then legacy (int ordinal).
        // The stored VillagerTypeKey string (e.g. "plains", "desert") is
        // directly a path for Registry.VILLAGER_TYPE — no translation needed.
        if (info.keySet().contains("VillagerTypeKey")) {
            String key = info.getString("VillagerTypeKey");
            Villager.Type matched = resolveType(key);
            if (matched != null) {
                setType(matched);
            }
        } else if (info.keySet().contains("VillagerType")) {
            // Legacy path: int ordinal into the pre-v4 local Type enum
            // (order: Desert=0, Jungle=1, Plains=2, Savanna=3, Snow=4, Swamp=5, Taiga=6).
            // The local enum was deleted in favour of Villager.Type; use a hardcoded
            // order table so the legacy read doesn't depend on the Bukkit enum's
            // current ordinal layout.
            try {
                int ord = info.getInt("VillagerType");
                String[] legacyOrder = {"desert", "jungle", "plains", "savanna", "snow", "swamp", "taiga"};
                if (ord >= 0 && ord < legacyOrder.length) {
                    Villager.Type matched = resolveType(legacyOrder[ord]);
                    if (matched != null) {
                        setType(matched);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        if (info.keySet().contains("VillagerLevel")) {
            setLevel(info.getInt("VillagerLevel"));
        }
        if (info.keySet().contains("OriginalData")) {
            originalData = info.getCompound("OriginalData");
        }
    }

    private static Villager.Type resolveType(String keyPath) {
        if (keyPath == null || keyPath.isEmpty()) return null;
        try {
            return Registry.VILLAGER_TYPE.get(NamespacedKey.minecraft(keyPath.toLowerCase(java.util.Locale.ROOT)));
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public void setType(Villager.Type value) {
        if (value == null) return;
        this.type = value;
        if (status == PetState.Here) {
            updateVisuals();
        }
    }

    @Override
    public void setLevel(int level) {
        this.level = Math.max(1, level);
        if (status == PetState.Here) {
            updateVisuals();
        }
    }

    public boolean hasOriginalData() {
        return this.originalData != null;
    }

    @Override
    public void setEquipment(EquipmentSlot slot, ItemStack item) {
        if (slot != EquipmentSlot.HAND) {
            return;
        }
        super.setEquipment(slot, item);
    }
}
