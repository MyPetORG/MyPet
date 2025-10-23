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

public class MyBee extends MyPet implements de.Keyle.MyPet.api.entity.types.MyBee {

    protected boolean isBaby = false;
    protected boolean hasStrung = false;
    protected boolean hasNectar = false;
    protected boolean isAngry = false;

    public MyBee(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public TagCompound writeExtendedInfo() {
        TagCompound info = super.writeExtendedInfo();
        info.getCompoundData().put("Baby", new TagByte(isBaby()));
        info.getCompoundData().put("HasNectar", new TagByte(hasNectar()));
        info.getCompoundData().put("HasStung", new TagByte(hasStung()));
        info.getCompoundData().put("Angry", new TagByte(isAngry()));
        return info;
    }

    @Override
    public void readExtendedInfo(TagCompound info) {
        if (info.containsKey("Baby")) {
            setBaby(info.getAs("Baby", TagByte.class).getBooleanData());
        }
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
    public MyPetType getPetType() {
        return MyPetType.Bee;
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

    @Override
    public String toString() {
        return "MyBee{owner=" + getOwner().getName() + ", name=" + ChatColor.stripColor(petName) + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + (skilltree != null ? skilltree.getName() : "-") + ", worldgroup=" + worldGroup + ", baby=" + isBaby() + "}";
    }

    @Override
    public boolean hasNectar() {
        return hasNectar;
    }

    @Override
    public void setHasNectar(boolean flag) {
        this.hasNectar = flag;
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }

    @Override
    public boolean hasStung() {
        return hasStrung;
    }

    @Override
    public void setHasStung(boolean flag) {
        this.hasStrung = flag;
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }

    @Override
    public boolean isAngry() {
        return isAngry;
    }

    @Override
    public void setAngry(boolean flag) {
        this.isAngry = flag;
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }
}