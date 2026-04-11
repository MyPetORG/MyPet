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
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Villager;

@Getter
public class MyZombieVillager extends MyPet implements de.Keyle.MyPet.api.entity.types.MyZombieVillager {

    /** @see MyVillager#professionKey */
    protected String professionKey = "none";
    protected Villager.Type type = Villager.Type.PLAINS;
    protected int tradingLevel = 1;

    public MyZombieVillager(MyPetPlayer petOwner) {
        super(petOwner);
    }

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
        info = info.putString("ProfessionKey", professionKey);
        info = info.putString("VillagerTypeKey", type.getKey().getKey());
        info = info.putInt("TradingLevel", getTradingLevel());
        return info;
    }

    @Override
    public void readExtendedInfo(CompoundBinaryTag info) {
        super.readExtendedInfo(info);
        if (info.keySet().contains("ProfessionKey")) {
            String key = info.getString("ProfessionKey");
            if (key != null && !key.isEmpty()) {
                this.professionKey = key;
            }
        } else if (info.keySet().contains("Profession")) {
            setProfession(info.getInt("Profession"));
        }
        // Villager type — new (namespaced-key path) then legacy (int ordinal).
        // See MyVillager#readExtendedInfo for the legacy order rationale.
        if (info.keySet().contains("VillagerTypeKey")) {
            String key = info.getString("VillagerTypeKey");
            Villager.Type matched = resolveType(key);
            if (matched != null) {
                setType(matched);
            }
        } else if (info.keySet().contains("VillagerType")) {
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
        if (info.keySet().contains("TradingLevel")) {
            setTradingLevel(info.getInt("TradingLevel"));
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
    public void setTradingLevel(int level) {
        this.tradingLevel = Math.max(1, level);
        if (status == PetState.Here) {
            updateVisuals();
        }
    }
}
