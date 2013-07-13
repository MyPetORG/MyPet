/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2013 Keyle
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

package de.Keyle.MyPet.entity.types.sheep;

import de.Keyle.MyPet.entity.MyPetInfo;
import de.Keyle.MyPet.entity.types.IMyPetBaby;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.util.MyPetPlayer;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.spout.nbt.ByteTag;
import org.spout.nbt.CompoundTag;
import org.spout.nbt.IntTag;
import org.spout.nbt.TagType;

import static org.bukkit.Material.WHEAT;

@MyPetInfo(food = {WHEAT})
public class MySheep extends MyPet implements IMyPetBaby
{
    protected DyeColor color = DyeColor.WHITE;
    protected boolean isSheared = false;
    protected boolean isBaby = false;

    public MySheep(MyPetPlayer petOwner)
    {
        super(petOwner);
    }

    public void setColor(DyeColor color)
    {
        if (status == PetState.Here)
        {
            ((EntityMySheep) getCraftPet().getHandle()).setColor(color.getDyeData());
        }
        this.color = color;
    }

    public DyeColor getColor()
    {
        return color;
    }

    public void setSheared(boolean flag)
    {
        if (status == PetState.Here)
        {
            ((EntityMySheep) getCraftPet().getHandle()).setSheared(flag);
        }
        this.isSheared = flag;
    }

    public boolean isSheared()
    {
        return isSheared;
    }

    public boolean isBaby()
    {
        return isBaby;
    }

    public void setBaby(boolean flag)
    {
        if (status == PetState.Here)
        {
            ((EntityMySheep) getCraftPet().getHandle()).setBaby(flag);
        }
        this.isBaby = flag;
    }

    @Override
    public CompoundTag getExtendedInfo()
    {
        CompoundTag info = super.getExtendedInfo();
        info.getValue().put("Color", new ByteTag("Color", getColor().getDyeData()));
        info.getValue().put("Sheared", new ByteTag("Sheared", isSheared()));
        info.getValue().put("Baby", new ByteTag("Baby", isBaby()));
        return info;
    }

    @Override
    public void setExtendedInfo(CompoundTag info)
    {
        if (info.getValue().containsKey("Color"))
        {
            byte data;
            if (info.getValue().get("Color").getType() == TagType.TAG_INT)
            {
                data = ((IntTag) info.getValue().get("Color")).getValue().byteValue();
            }
            else
            {
                data = ((ByteTag) info.getValue().get("Color")).getValue();
            }
            setColor(DyeColor.getByDyeData(data));
        }
        if (info.getValue().containsKey("Sheared"))
        {
            setSheared(((ByteTag) info.getValue().get("Sheared")).getBooleanValue());
        }
        if (info.getValue().containsKey("Baby"))
        {
            setBaby(((ByteTag) info.getValue().get("Baby")).getBooleanValue());
        }
    }

    @Override
    public MyPetType getPetType()
    {
        return MyPetType.Sheep;
    }

    @Override
    public String toString()
    {
        return "MySheep{owner=" + getOwner().getName() + ", name=" + ChatColor.stripColor(petName) + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + (skillTree != null ? skillTree.getName() : "-") + ", worldgroup=" + worldGroup + ", color=" + getColor() + ", sheared=" + isSheared() + ", baby=" + isBaby() + "}";
    }
}