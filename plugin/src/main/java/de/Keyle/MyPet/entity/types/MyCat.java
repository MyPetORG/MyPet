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
import lombok.Getter;
import org.bukkit.DyeColor;
import org.bukkit.entity.Cat.Type;

@Getter
public class MyCat extends MyPet implements de.Keyle.MyPet.api.entity.types.MyCat {

    protected boolean tamed = false;
    protected Type catType = Type.TABBY;
    protected DyeColor collarColor = DyeColor.RED;
    public MyCat(MyPetPlayer petOwner) {
        super(petOwner);
    }

    public void setCatType(Type value) {
        this.catType = value;
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }

    public void setCollarColor(DyeColor value) {
        this.collarColor = value;
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }

    public void setTamed(boolean flag) {
        this.tamed = flag;
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }

    @Override
    public CompoundBinaryTag writeExtendedInfo() {
        CompoundBinaryTag info = super.writeExtendedInfo();
        info = info.putInt("CatType", getCatType().ordinal());
        info = info.putInt("CollarColor", getCollarColor().ordinal());
        info = info.putBoolean("Tamed", isTamed());
        return info;
    }

    @Override
    public void readExtendedInfo(CompoundBinaryTag info) {
        super.readExtendedInfo(info);
        if (info.keySet().contains("CatType")) {
            Type leType = OwnCatType.values()[info.getInt("CatType")].getBukkitType();
            setCatType(leType);
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

    // Needed as some newer versions have integer -> Type mismatches
    enum OwnCatType {
        TABBY(Type.TABBY),
        BLACK(Type.BLACK),
        RED(Type.RED),
        SIAMESE(Type.SIAMESE),
        BRITISH_SHORTHAIR(Type.BRITISH_SHORTHAIR),
        CALICO(Type.CALICO),
        PERSIAN(Type.PERSIAN),
        RAGDOLL(Type.RAGDOLL),
        WHITE(Type.WHITE),
        JELLIE(Type.JELLIE),
        ALL_BLACK(Type.ALL_BLACK),
        ;

        private Type bukkitType;

        OwnCatType(Type type) {
            bukkitType = type;
        }

        public Type getBukkitType() {
            return bukkitType;
        }
    }
}