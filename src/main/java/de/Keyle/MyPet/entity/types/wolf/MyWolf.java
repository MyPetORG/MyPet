/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2014 Keyle
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

package de.Keyle.MyPet.entity.types.wolf;

import de.Keyle.MyPet.entity.MyPetInfo;
import de.Keyle.MyPet.entity.types.IMyPetBaby;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.util.ConfigItem;
import de.Keyle.MyPet.util.MyPetPlayer;
import de.keyle.knbt.TagByte;
import de.keyle.knbt.TagCompound;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;

import static de.Keyle.MyPet.entity.types.MyPet.LeashFlag.Tamed;
import static org.bukkit.Material.RAW_BEEF;
import static org.bukkit.Material.RAW_CHICKEN;

@MyPetInfo(food = {RAW_BEEF, RAW_CHICKEN}, leashFlags = {Tamed})
public class MyWolf extends MyPet implements IMyPetBaby {
    public static ConfigItem GROW_UP_ITEM;

    protected boolean isBaby = false;
    protected boolean isTamed = false;
    protected boolean isAngry = false;
    protected DyeColor collarColor = DyeColor.RED;

    public MyWolf(MyPetPlayer petOwner) {
        super(petOwner);
    }

    public DyeColor getCollarColor() {
        return collarColor;
    }

    public void setCollarColor(DyeColor value) {
        if (status == PetState.Here) {
            ((EntityMyWolf) getCraftPet().getHandle()).setCollarColor(value.getDyeData());
        }
        this.collarColor = value;
    }

    @Override
    public TagCompound getExtendedInfo() {
        TagCompound info = super.getExtendedInfo();
        info.getCompoundData().put("Baby", new TagByte(isBaby()));
        info.getCompoundData().put("Tamed", new TagByte(isTamed()));
        info.getCompoundData().put("Angry", new TagByte(isAngry()));
        info.getCompoundData().put("CollarColor", new TagByte(getCollarColor().getDyeData()));
        return info;
    }

    @Override
    public void setExtendedInfo(TagCompound info) {
        if (info.getCompoundData().containsKey("CollarColor")) {
            setCollarColor(DyeColor.getByDyeData(info.getAs("CollarColor", TagByte.class).getByteData()));
        }
        if (info.getCompoundData().containsKey("Tamed")) {
            setTamed(info.getAs("Tamed", TagByte.class).getBooleanData());
        }
        if (info.getCompoundData().containsKey("Baby")) {
            setBaby(info.getAs("Baby", TagByte.class).getBooleanData());
        }
        if (info.getCompoundData().containsKey("Angry")) {
            setAngry(info.getAs("Angry", TagByte.class).getBooleanData());
        }
    }

    @Override
    public MyPetType getPetType() {
        return MyPetType.Wolf;
    }

    public boolean isAngry() {
        return isAngry;
    }

    public void setAngry(boolean flag) {
        if (status == PetState.Here) {
            ((EntityMyWolf) getCraftPet().getHandle()).setAngry(flag);
        }
        this.isAngry = flag;
    }

    public boolean isBaby() {
        return isBaby;
    }

    public void setBaby(boolean flag) {
        if (status == PetState.Here) {
            ((EntityMyWolf) getCraftPet().getHandle()).setBaby(flag);
        }
        this.isBaby = flag;
    }

    public boolean isTamed() {
        return isTamed;
    }

    public void setTamed(boolean flag) {
        if (status == PetState.Here) {
            ((EntityMyWolf) getCraftPet().getHandle()).setTamed(flag);
        }
        this.isTamed = flag;
    }

    @Override
    public String toString() {
        return "MyWolf{owner=" + getOwner().getName() + ", name=" + ChatColor.stripColor(petName) + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + (skillTree != null ? skillTree.getName() : "-") + ", worldgroup=" + worldGroup + ", collarcolor=" + getCollarColor() + ", baby=" + isBaby() + "}";
    }
}