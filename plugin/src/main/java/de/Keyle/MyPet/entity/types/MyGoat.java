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

@Getter
public class MyGoat extends MyPet implements de.Keyle.MyPet.api.entity.types.MyGoat {

    protected boolean screaming = false;
    protected boolean leftHorn = true;
    protected boolean rightHorn = true;

    public MyGoat(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public CompoundBinaryTag writeExtendedInfo() {
        CompoundBinaryTag info = super.writeExtendedInfo();
        info = info.putBoolean("Screaming", isScreaming());
        info = info.putBoolean("LeftHorn", hasLeftHorn());
        info = info.putBoolean("RightHorn", hasRightHorn());
        return info;
    }

    @Override
    public void readExtendedInfo(CompoundBinaryTag info) {
        super.readExtendedInfo(info);
        if (info.keySet().contains("Screaming")) {
            setScreaming(info.getBoolean("Screaming"));
        }
        if (info.keySet().contains("LeftHorn")) {
            setLeftHorn(info.getBoolean("LeftHorn"));
        }
        if (info.keySet().contains("RightHorn")) {
            setRightHorn(info.getBoolean("RightHorn"));
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
            updateVisuals();
        }
    }

    public void setLeftHorn(boolean leftHorn) {
        this.leftHorn = leftHorn;
        if (status == PetState.Here) {
            updateVisuals();
        }
    }

    public void setRightHorn(boolean rightHorn) {
        this.rightHorn = rightHorn;
        if (status == PetState.Here) {
            updateVisuals();
        }
    }
}