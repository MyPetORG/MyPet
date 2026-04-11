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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.MyPet;
import lombok.Getter;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.bukkit.DyeColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Cat.Type;

public class MyCat extends MyPet implements de.Keyle.MyPet.api.entity.types.MyCat {

    @Getter
    protected boolean tamed = false;
    /**
     * Storage uses the NamespacedKey path (e.g. "tabby", "black", "calico")
     * rather than an ordinal, so the variant is drift-safe across Paper
     * version upgrades that reorder the {@code Cat.Type} registry. The public
     * {@link #getCatType()} / {@link #setCatType(Type)} API is unchanged.
     */
    protected String catTypeKey = "tabby";
    @Getter
    protected DyeColor collarColor = DyeColor.RED;

    public MyCat(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public Type getCatType() {
        try {
            Type type = Registry.CAT_VARIANT.get(NamespacedKey.minecraft(catTypeKey));
            if (type != null) return type;
        } catch (Throwable ignored) {
        }
        try {
            return Registry.CAT_VARIANT.get(NamespacedKey.minecraft("tabby"));
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Override
    public void setCatType(Type value) {
        if (value != null && value.getKey() != null) {
            this.catTypeKey = value.getKey().getKey();
        }
        if (status == PetState.Here) {
            updateVisuals();
        }
    }

    public void setCollarColor(DyeColor value) {
        this.collarColor = value;
        if (status == PetState.Here) {
            updateVisuals();
        }
    }

    public void setTamed(boolean flag) {
        this.tamed = flag;
        if (status == PetState.Here) {
            updateVisuals();
        }
    }

    @Override
    public CompoundBinaryTag writeExtendedInfo() {
        CompoundBinaryTag info = super.writeExtendedInfo();
        info = info.putString("CatTypeKey", catTypeKey);
        info = info.putInt("CollarColor", getCollarColor().ordinal());
        info = info.putBoolean("Tamed", isTamed());
        return info;
    }

    @Override
    public void readExtendedInfo(CompoundBinaryTag info) {
        super.readExtendedInfo(info);
        if (info.keySet().contains("CatTypeKey")) {
            String key = info.getString("CatTypeKey");
            if (key != null && !key.isEmpty()) {
                this.catTypeKey = key;
            }
        } else if (info.keySet().contains("CatType")) {
            // Legacy format: int ordinal written by pre-fix writeExtendedInfo.
            // Migrate by looking up the name from the hardcoded legacy table.
            try {
                int ord = info.getInt("CatType");
                String legacyKey = legacyOrdinalKey(ord);
                if (legacyKey != null) {
                    this.catTypeKey = legacyKey;
                }
            } catch (Exception e) {
                MyPetApi.getLogger().warning("Failed to migrate legacy Cat variant ordinal: " + e.getMessage());
            }
        }
        if (info.keySet().contains("CollarColor")) {
            if (info.get("CollarColor") instanceof net.kyori.adventure.nbt.IntBinaryTag) {
                setCollarColor(DyeColor.values()[info.getInt("CollarColor")]);
            } else if (info.get("CollarColor") instanceof net.kyori.adventure.nbt.ByteBinaryTag) {
                setCollarColor(DyeColor.values()[info.getByte("CollarColor")]);
            }
        }
        if (info.keySet().contains("Tamed")) {
            setTamed(info.getBoolean("Tamed"));
        }
    }

    private static final String[] LEGACY_CAT_ORDINAL_KEYS = {
            "tabby", "black", "red", "siamese", "british_shorthair",
            "calico", "persian", "ragdoll", "white", "jellie", "all_black"
    };

    private static String legacyOrdinalKey(int ord) {
        if (ord < 0 || ord >= LEGACY_CAT_ORDINAL_KEYS.length) return null;
        return LEGACY_CAT_ORDINAL_KEYS[ord];
    }
}
