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
import org.bukkit.entity.Parrot;

public class MyParrot extends MyPet implements de.Keyle.MyPet.api.entity.types.MyParrot {

    /**
     * Variant stored by enum-name (e.g. "RED_BLUE", "BLUE", "GREEN") rather
     * than ordinal — drift-safe across Paper updates that reorder or extend
     * {@code Parrot.Variant}.
     */
    protected String variantName = "RED_BLUE";

    public MyParrot(MyPetPlayer petOwner) {
        super(petOwner);
    }

    public double getYSpawnOffset() {
        return 1;
    }

    /**
     * Returns the ordinal of the stored variant in the current runtime's
     * {@code Parrot.Variant} registry.
     */
    public int getVariant() {
        try {
            Parrot.Variant v = resolveBukkitVariant();
            return v != null ? v.ordinal() : 0;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    public void setVariant(int variant) {
        try {
            Parrot.Variant[] values = Parrot.Variant.values();
            int clamped = Math.min(values.length - 1, Math.max(0, variant));
            this.variantName = values[clamped].name();
        } catch (Throwable ignored) {
        }
        if (status == PetState.Here) {
            updateVisuals();
        }
    }

    public Parrot.Variant resolveBukkitVariant() {
        try {
            for (Parrot.Variant v : Parrot.Variant.values()) {
                if (v.name().equals(variantName)) return v;
            }
            Parrot.Variant[] values = Parrot.Variant.values();
            if (values.length > 0) return values[0];
        } catch (Throwable ignored) {
        }
        return null;
    }

    @Override
    public CompoundBinaryTag writeExtendedInfo() {
        CompoundBinaryTag info = super.writeExtendedInfo();
        info = info.putString("VariantName", variantName);
        return info;
    }

    @Override
    public void readExtendedInfo(CompoundBinaryTag info) {
        super.readExtendedInfo(info);
        if (info.keySet().contains("VariantName")) {
            String name = info.getString("VariantName");
            if (name != null && !name.isEmpty()) {
                this.variantName = name;
            }
        } else if (info.keySet().contains("Variant")) {
            setVariant(info.getInt("Variant"));
        }
    }
}
