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

import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.MyPet;
import de.keyle.knbt.TagByte;
import de.keyle.knbt.TagCompound;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;

public class MyWolf extends MyPet implements de.Keyle.MyPet.api.entity.types.MyWolf {

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
        this.collarColor = value;
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }

    @Override
    public TagCompound writeExtendedInfo() {
        TagCompound info = super.writeExtendedInfo();
        info.getCompoundData().put("Baby", new TagByte(isBaby()));
        info.getCompoundData().put("Tamed", new TagByte(isTamed()));
        info.getCompoundData().put("Angry", new TagByte(isAngry()));
        info.getCompoundData().put("CollarColor", new TagByte(getCollarColor().ordinal()));
        return info;
    }

    @Override
    public void readExtendedInfo(TagCompound info) {
        if (info.containsKey("CollarColor")) {
            setCollarColor(DyeColor.values()[info.getAs("CollarColor", TagByte.class).getByteData()]);
        }
        if (info.containsKey("Tamed")) {
            setTamed(info.getAs("Tamed", TagByte.class).getBooleanData());
        }
        if (info.containsKey("Baby")) {
            setBaby(info.getAs("Baby", TagByte.class).getBooleanData());
        }
        if (info.containsKey("Angry")) {
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
        this.isAngry = flag;
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }

    public boolean isBaby() {
        return isBaby;
    }

    public void setBaby(boolean flag) {
        this.isBaby = flag;
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }

    public boolean isTamed() {
        return isTamed;
    }

    public void setTamed(boolean flag) {
        this.isTamed = flag;
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }

    @Override
    public String toString() {
        return "MyWolf{owner=" + getOwner().getName() + ", name=" + ChatColor.stripColor(petName) + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + (skilltree != null ? skilltree.getName() : "-") + ", worldgroup=" + worldGroup + ", collarcolor=" + getCollarColor() + ", baby=" + isBaby() + "}";
    }
}