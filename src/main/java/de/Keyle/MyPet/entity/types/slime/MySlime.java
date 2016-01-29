/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2016 Keyle
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

package de.Keyle.MyPet.entity.types.slime;

import de.Keyle.MyPet.api.entity.MyPetInfo;
import de.Keyle.MyPet.api.entity.MyPetSlimeSize;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagInt;
import org.bukkit.ChatColor;

import static org.bukkit.Material.SUGAR;

@MyPetInfo(food = {SUGAR})
public class MySlime extends MyPet implements MyPetSlimeSize {
    protected int size = 1;

    public MySlime(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public TagCompound writeExtendedInfo() {
        TagCompound info = super.writeExtendedInfo();
        info.getCompoundData().put("Size", new TagInt(getSize()));
        return info;
    }

    @Override
    public void readExtendedInfo(TagCompound info) {
        if (info.getCompoundData().containsKey("Size")) {
            setSize(info.getAs("Size", TagInt.class).getIntData());
        }
    }

    @Override
    public MyPetType getPetType() {
        return MyPetType.Slime;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int value) {
        value = Math.max(1, value);
        if (status == PetState.Here) {
            ((EntityMySlime) getCraftPet().getHandle()).setSize(value);
        }
        this.size = value;
    }

    @Override
    public String toString() {
        return "MySlime{owner=" + getOwner().getName() + ", name=" + ChatColor.stripColor(petName) + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + (skillTree != null ? skillTree.getName() : "-") + ", worldgroup=" + worldGroup + ", size=" + getSize() + "}";
    }
}