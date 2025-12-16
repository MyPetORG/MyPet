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
import de.keyle.knbt.TagByte;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagString;
import lombok.Getter;
import lombok.Setter;
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
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }

    @Override
    public TagCompound writeExtendedInfo() {
        TagCompound info = super.writeExtendedInfo();
        info.getCompoundData().put("Tamed", new TagByte(isTamed()));
        info.getCompoundData().put("Angry", new TagByte(isAngry()));
        info.getCompoundData().put("CollarColor", new TagByte(getCollarColor().ordinal()));
        info.getCompoundData().put("Variant", new TagString(getVariant()));
        return info;
    }

    @Override
    public void readExtendedInfo(TagCompound info) {
        super.readExtendedInfo(info);
        if (info.containsKey("CollarColor")) {
            setCollarColor(DyeColor.values()[info.getAs("CollarColor", TagByte.class).getByteData()]);
        }
        if (info.containsKey("Tamed")) {
            setTamed(info.getAs("Tamed", TagByte.class).getBooleanData());
        }
        if (info.containsKey("Angry")) {
            setAngry(info.getAs("Angry", TagByte.class).getBooleanData());
        }
        if (info.containsKey("Variant")) {
            setVariant(info.getAs("Variant", TagString.class).getStringData());
        }
    }

    public void setAngry(boolean flag) {
        this.angry = flag;
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
}