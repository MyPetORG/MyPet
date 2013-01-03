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

package de.Keyle.MyPet.entity.types.ocelot;

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.util.MyPetPlayer;
import net.minecraft.server.v1_4_6.NBTTagCompound;
import org.bukkit.entity.Ocelot.Type;

public class MyOcelot extends MyPet
{
    protected boolean isSitting = false;
    protected boolean isBaby = false;
    protected Type catType = Type.WILD_OCELOT;

    public MyOcelot(MyPetPlayer petOwner)
    {
        super(petOwner);
        this.petName = "Ocelot";
    }

    public boolean isSitting()
    {
        return isSitting;
    }

    public void setSitting(boolean flag)
    {
        if (status == PetState.Here)
        {
            ((CraftMyOcelot) craftMyPet).setSitting(flag);
        }
        this.isSitting = flag;
    }

    public Type getCatType()
    {
        return catType;
    }

    public void setCatType(Type value)
    {
        if (status == PetState.Here)
        {
            ((CraftMyOcelot) getCraftPet()).setCatType(value);
        }
        this.catType = value;
    }

    public boolean isBaby()
    {
        return isBaby;
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
        info.setInt("CatType", getCatType().getId());
        info.setBoolean("Sitting", isSitting());
        info.setBoolean("Baby", isBaby());
        return info;
    }

    @Override
    public void setExtendedInfo(NBTTagCompound info)
    {
        setCatType(Type.getType(info.getInt("CatType")));
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
        return "MyOcelot{owner=" + getOwner().getName() + ", name=" + petName + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + (skillTree != null ? skillTree.getName() : "-") + ", sitting=" + isSitting() + ", cattype=" + getCatType().name() + ", baby=" + isBaby() + "}";
    }
}