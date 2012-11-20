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

package de.Keyle.MyPet.entity.types.villager;

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.util.MyPetPlayer;
import net.minecraft.server.NBTTagCompound;

public class MyVillager extends MyPet
{
    protected int profession = 0;
    protected boolean isBaby = false;

    public MyVillager(MyPetPlayer petOwner)
    {
        super(petOwner);
        this.petName = "Villager";
    }

    public void setProfession(int value)
    {
        if (status == PetState.Here)
        {
            ((CraftMyVillager) getCraftPet()).setProfession(value);
        }
        this.profession = value;
    }

    public int getProfession()
    {
        if (status == PetState.Here)
        {
            return ((CraftMyVillager) getCraftPet()).getProfession();
        }
        else
        {
            return profession;
        }
    }

    public boolean isBaby()
    {
        if (status == PetState.Here)
        {
            return ((CraftMyVillager) getCraftPet()).isBaby();
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
            ((CraftMyVillager) getCraftPet()).setBaby(flag);
        }
        this.isBaby = flag;
    }

    @Override
    public NBTTagCompound getExtendedInfo()
    {
        NBTTagCompound info = new NBTTagCompound("Info");
        info.setInt("Profession", getProfession());
        info.setBoolean("Baby", isBaby());
        return info;
    }

    @Override
    public void setExtendedInfo(NBTTagCompound info)
    {
        setProfession(info.getInt("Profession"));
        setBaby(info.getBoolean("Baby"));
    }

    @Override
    public MyPetType getPetType()
    {
        return MyPetType.Villager;
    }

    @Override
    public String toString()
    {
        return "MyVillager{owner=" + getOwner().getName() + ", name=" + petName + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + skillTree.getName() + ", profession=" + getProfession() + ", baby=" + isBaby() + "}";
    }
}