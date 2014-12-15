/*
 * This file is part of MyPet-1.8
 *
 * Copyright (C) 2011-2014 Keyle
 * MyPet-1.8 is licensed under the GNU Lesser General Public License.
 *
 * MyPet-1.8 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyPet-1.8 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.entity.types.rabbit;

import de.Keyle.MyPet.entity.MyPetInfo;
import de.Keyle.MyPet.entity.types.IMyPetBaby;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.entity.types.pig.EntityMyPig;
import de.Keyle.MyPet.util.ConfigItem;
import de.Keyle.MyPet.util.player.MyPetPlayer;
import de.keyle.knbt.TagByte;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagInt;
import org.bukkit.ChatColor;

import static org.bukkit.Material.CARROT;
import static org.bukkit.Material.RED_ROSE;

@MyPetInfo(food = {CARROT, RED_ROSE})
public class MyRabbit extends MyPet implements IMyPetBaby {
    public static ConfigItem GROW_UP_ITEM;

    protected boolean isBaby = false;
    protected byte variant = 0;

    public MyRabbit(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public MyPetType getPetType() {
        return MyPetType.Rabbit;
    }

    @Override
    public TagCompound getExtendedInfo() {
        TagCompound info = super.getExtendedInfo();
        info.getCompoundData().put("Variation", new TagByte(variant));
        info.getCompoundData().put("Baby", new TagByte(isBaby()));
        return info;
    }

    @Override
    public void setExtendedInfo(TagCompound info) {
        if (info.containsKeyAs("Variant", TagInt.class)) {
            setVariant((byte) info.getAs("Variant", TagInt.class).getIntData());
        }
        if (info.getCompoundData().containsKey("Baby")) {
            setBaby(info.getAs("Baby", TagByte.class).getBooleanData());
        }
    }

    public boolean isBaby() {
        return isBaby;
    }

    public void setBaby(boolean flag) {
        if (status == PetState.Here) {
            ((EntityMyPig) getCraftPet().getHandle()).setBaby(flag);
        }
        this.isBaby = flag;
    }

    public byte getVariant() {
        return variant;
    }

    public void setVariant(byte variant) {
        this.variant = variant;

        if (status == PetState.Here) {
            ((EntityMyRabbit) getCraftPet().getHandle()).setVariant(variant);
        }
    }

    @Override
    public String toString() {
        return "MyRabbit{owner=" + getOwner().getName() + ", name=" + ChatColor.stripColor(petName) + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + (skillTree != null ? skillTree.getName() : "-") + ", worldgroup=" + worldGroup + ", baby=" + isBaby() + ", variant=" + variant + "}";
    }
}