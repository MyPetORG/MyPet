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
public class MyBee extends MyPet implements de.Keyle.MyPet.api.entity.types.MyBee {

    protected boolean hasStung = false;
    protected boolean hasNectar = false;
    protected boolean angry = false;

    public MyBee(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public TagCompound writeExtendedInfo() {
        TagCompound info = super.writeExtendedInfo();
        info.getCompoundData().put("HasNectar", new TagByte(hasNectar()));
        info.getCompoundData().put("HasStung", new TagByte(hasStung()));
        info.getCompoundData().put("Angry", new TagByte(isAngry()));
        return info;
    }

    @Override
    public void readExtendedInfo(TagCompound info) {
        super.readExtendedInfo(info);
        if (info.containsKey("HasNectar")) {
            setHasNectar(info.getAs("HasNectar", TagByte.class).getBooleanData());
        }
        if (info.containsKey("HasStung")) {
            setHasStung(info.getAs("HasStung", TagByte.class).getBooleanData());
        }
        if (info.containsKey("Angry")) {
            setAngry(info.getAs("Angry", TagByte.class).getBooleanData());
        }
    }

    @Override
    public boolean hasNectar() {
        return false;
    }

    public void setHasNectar(boolean flag) {
        this.hasNectar = flag;
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }

    @Override
    public boolean hasStung() {
        return hasStung;
    }

    @Override
    public void setHasStung(boolean flag) {
        this.hasStung = flag;
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }

    @Override
    public void setAngry(boolean flag) {
        this.angry = flag;
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }
}