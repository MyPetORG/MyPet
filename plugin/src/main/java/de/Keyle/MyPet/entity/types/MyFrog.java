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
import org.bukkit.entity.Frog;

public class MyFrog extends MyPet implements de.Keyle.MyPet.api.entity.types.MyFrog {

    protected String variantName = "TEMPERATE";

    public MyFrog(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public CompoundBinaryTag writeExtendedInfo() {
        CompoundBinaryTag info = super.writeExtendedInfo();
        return info.putString("FrogTypeName", variantName);
    }

    @Override
    public void readExtendedInfo(CompoundBinaryTag info) {
        super.readExtendedInfo(info);
        if (info.keySet().contains("FrogTypeName")) {
            String name = info.getString("FrogTypeName");
            if (name != null && !name.isEmpty()) {
                this.variantName = name;
            }
        } else if (info.keySet().contains("FrogType")) {
            setFrogVariant(info.getInt("FrogType"));
        }
    }

    @Override
    public void setFrogVariant(int variant) {
        try {
            Frog.Variant[] values = Frog.Variant.values();
            int clamped = Math.min(values.length - 1, Math.max(0, variant));
            this.variantName = values[clamped].name();
        } catch (Throwable ignored) {
        }
        if (status == PetState.Here) {
            updateVisuals();
        }
    }

    public int getFrogVariant() {
        try {
            Frog.Variant v = resolveBukkitVariant();
            return v != null ? v.ordinal() : 0;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    public Frog.Variant resolveBukkitVariant() {
        try {
            for (Frog.Variant v : Frog.Variant.values()) {
                if (v.name().equals(variantName)) return v;
            }
            Frog.Variant[] values = Frog.Variant.values();
            if (values.length > 0) return values[0];
        } catch (Throwable ignored) {
        }
        return null;
    }
}
