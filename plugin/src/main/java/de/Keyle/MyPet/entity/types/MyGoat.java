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
import lombok.Getter;

@Getter
public class MyGoat extends MyPet implements de.Keyle.MyPet.api.entity.types.MyGoat {

    protected boolean screaming = false;
    protected boolean leftHorn = true;
    protected boolean rightHorn = true;

    public MyGoat(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public TagCompound writeExtendedInfo() {
        TagCompound info = super.writeExtendedInfo();
        info.getCompoundData().put("Screaming", new TagByte(isScreaming()));
        info.getCompoundData().put("LeftHorn", new TagByte(hasLeftHorn()));
        info.getCompoundData().put("RightHorn", new TagByte(hasRightHorn()));
        return info;
    }

    @Override
    public void readExtendedInfo(TagCompound info) {
        super.readExtendedInfo(info);
        if (info.containsKey("Screaming")) {
            setScreaming(info.getAs("Screaming", TagByte.class).getBooleanData());
        }
        if (info.containsKey("LeftHorn")) {
            setLeftHorn(info.getAs("LeftHorn", TagByte.class).getBooleanData());
        }
        if (info.containsKey("RightHorn")) {
            setRightHorn(info.getAs("RightHorn", TagByte.class).getBooleanData());
        }
    }

    // Manual getters needed - Lombok generates isLeftHorn()/isRightHorn() but API requires hasLeftHorn()/hasRightHorn()
    public boolean hasLeftHorn() {
        return leftHorn;
    }

    public boolean hasRightHorn() {
        return rightHorn;
    }

    public void setScreaming(boolean flag) {
        this.screaming = flag;
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }

    public void setLeftHorn(boolean leftHorn) {
        this.leftHorn = leftHorn;
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }

    public void setRightHorn(boolean rightHorn) {
        this.rightHorn = rightHorn;
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }
}