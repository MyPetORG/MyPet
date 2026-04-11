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
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.bukkit.entity.Fox.Type;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class MyFox extends MyPet implements de.Keyle.MyPet.api.entity.types.MyFox {

    /**
     * Storage is by enum name (e.g. "RED", "SNOW") rather than ordinal so the
     * value is drift-safe if Paper reorders or adds {@code Fox.Type} variants.
     */
    protected String foxTypeName = Type.RED.name();

    public MyFox(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public Type getFoxType() {
        try {
            return Type.valueOf(foxTypeName);
        } catch (Throwable ignored) {
            return Type.RED;
        }
    }

    public void setFoxType(Type value) {
        if (value != null) {
            this.foxTypeName = value.name();
        }
        if (status == PetState.Here) {
            updateVisuals();
        }
    }

    @Override
    public CompoundBinaryTag writeExtendedInfo() {
        CompoundBinaryTag info = super.writeExtendedInfo();
        info = info.putString("FoxTypeName", foxTypeName);
        return info;
    }

    @Override
    public void readExtendedInfo(CompoundBinaryTag info) {
        super.readExtendedInfo(info);
        if (info.keySet().contains("FoxTypeName")) {
            String name = info.getString("FoxTypeName");
            if (name != null && !name.isEmpty()) {
                try {
                    Type.valueOf(name); // validate
                    this.foxTypeName = name;
                } catch (Throwable ignored) {
                }
            }
        } else if (info.keySet().contains("FoxType")) {
            // Legacy format: int ordinal. Migrate via current runtime's values().
            try {
                int ord = info.getInt("FoxType");
                Type[] values = Type.values();
                if (ord >= 0 && ord < values.length) {
                    this.foxTypeName = values[ord].name();
                }
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public void setEquipment(EquipmentSlot slot, ItemStack item) {
        if (slot != EquipmentSlot.HAND) {
            return;
        }
        super.setEquipment(slot, item);
    }
}
