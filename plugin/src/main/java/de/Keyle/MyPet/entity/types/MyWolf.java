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
import org.bukkit.DyeColor;

@Getter
public class MyWolf extends MyPet implements de.Keyle.MyPet.api.entity.types.MyWolf {

    protected boolean tamed = false;
    protected boolean angry = false;
    protected DyeColor collarColor = DyeColor.RED;
    @Setter
    protected String variant = "pale";

    public MyWolf(MyPetPlayer petOwner) {
        super(petOwner);
    }

    public void setCollarColor(DyeColor value) {
        this.collarColor = value;
        if (status == PetState.Here) {
            updateVisuals();
        }
    }

    @Override
    public CompoundBinaryTag writeExtendedInfo() {
        CompoundBinaryTag info = super.writeExtendedInfo();
        info = info.putBoolean("Tamed", isTamed());
        info = info.putBoolean("Angry", isAngry());
        info = info.putByte("CollarColor", (byte) getCollarColor().ordinal());
        info = info.putString("Variant", getVariant());
        return info;
    }

    @Override
    public void readExtendedInfo(CompoundBinaryTag info) {
        super.readExtendedInfo(info);
        if (info.keySet().contains("CollarColor")) {
            setCollarColor(DyeColor.values()[info.getByte("CollarColor")]);
        }
        if (info.keySet().contains("Tamed")) {
            setTamed(info.getBoolean("Tamed"));
        }
        if (info.keySet().contains("Angry")) {
            setAngry(info.getBoolean("Angry"));
        }
        if (info.keySet().contains("Variant")) {
            setVariant(info.getString("Variant"));
        }
    }

    public void setAngry(boolean flag) {
        this.angry = flag;
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
}