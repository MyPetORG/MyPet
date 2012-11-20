/*
 * Copyright (C) 2011-2012 Keyle
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

package de.Keyle.MyPet.entity.types.ocelot;

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.util.MyPetPlayer;
import net.minecraft.server.NBTTagCompound;

public class MyOcelot extends MyPet
{
    protected boolean isSitting = false;
    protected boolean isBaby = false;
    protected int catType = 0;

    public MyOcelot(MyPetPlayer petOwner)
    {
        super(petOwner);
        this.petName = "Ocelot";
    }

    public boolean isSitting()
    {
        if (status == PetState.Here)
        {
            return ((CraftMyOcelot) craftMyPet).isSitting();
        }
        else
        {
            return isSitting;
        }
    }

    public void setSitting(boolean flag)
    {
        if (status == PetState.Here)
        {
            ((CraftMyOcelot) craftMyPet).setSitting(flag);
        }
        this.isSitting = flag;
    }

    public int getCatType()
    {
        if (status == PetState.Here)
        {
            return ((CraftMyOcelot) getCraftPet()).getCatType().getId();
        }
        else
        {
            return catType;
        }
    }

    public void setCatType(int value)
    {
        if (status == PetState.Here)
        {
            ((EntityMyOcelot) craftMyPet.getHandle()).setCatType(value);
        }
        this.catType = value;
    }

    public boolean isBaby()
    {
        if (status == PetState.Here)
        {
            return ((CraftMyOcelot) getCraftPet()).isBaby();
        }
        else
        {
            return isBaby;
        }
    }

    public void setBaby(boolean flag)
    {
        if (status == PetState.Here)
        {
            ((CraftMyOcelot) getCraftPet()).setBaby(flag);
        }
        this.isBaby = flag;
    }

    @Override
    public NBTTagCompound getExtendedInfo()
    {
        NBTTagCompound info = new NBTTagCompound("Info");
        info.setInt("CatType", getCatType());
        info.setBoolean("Sitting", isSitting());
        info.setBoolean("Baby", isBaby());
        return info;
    }

    @Override
    public void setExtendedInfo(NBTTagCompound info)
    {
        setCatType(info.getInt("CatType"));
        setSitting(info.getBoolean("Sitting"));
        setBaby(info.getBoolean("Baby"));
    }

    @Override
    public MyPetType getPetType()
    {
        return MyPetType.Ocelot;
    }

    @Override
    public String toString()
    {
        return "MyOcelot{owner=" + getOwner().getName() + ", name=" + petName + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + skillTree.getName() + ", sitting=" + isSitting() + ", cattype=" + getCatType() + ", baby=" + isBaby() + "}";
    }
}