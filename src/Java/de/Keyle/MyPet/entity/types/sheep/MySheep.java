/*
 * Copyright (C) 2011-2013 Keyle
 *
 * This file is part of MyPet
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
 * along with MyPet. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.entity.types.sheep;

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.util.MyPetPlayer;
import net.minecraft.server.v1_4_6.NBTTagCompound;

public class MySheep extends MyPet
{
    protected int color = 0;
    protected boolean isSheared = false;
    protected boolean isBaby = false;

    public MySheep(MyPetPlayer petOwner)
    {
        super(petOwner);
        this.petName = "Sheep";
    }

    public void setColor(int value)
    {
        if (status == PetState.Here)
        {
            ((CraftMySheep) getCraftPet()).setColor(value);
        }
        this.color = value;
    }

    public int getColor()
    {
        return color;
    }

    public void setSheared(boolean flag)
    {
        if (status == PetState.Here)
        {
            ((CraftMySheep) getCraftPet()).setSheared(flag);
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
            ((CraftMySheep) getCraftPet()).setBaby(flag);
        }
        this.isBaby = flag;
    }

    @Override
    public NBTTagCompound getExtendedInfo()
    {
        NBTTagCompound info = new NBTTagCompound("Info");
        info.setInt("Color", getColor());
        info.setBoolean("Sheared", isSheared());
        info.setBoolean("Baby", isBaby());
        return info;
    }

    @Override
    public void setExtendedInfo(NBTTagCompound info)
    {
        setColor(info.getInt("Color"));
        setSheared(info.getBoolean("Sheared"));
        setBaby(info.getBoolean("Baby"));
    }

    @Override
    public MyPetType getPetType()
    {
        return MyPetType.Sheep;
    }

    @Override
    public String toString()
    {
        return "MySheep{owner=" + getOwner().getName() + ", name=" + petName + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + (skillTree != null ? skillTree.getName() : "-") + ", color=" + getColor() + ", sheared=" + isSheared() + ", baby=" + isBaby() + "}";
    }
}