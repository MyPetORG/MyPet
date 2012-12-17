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

package de.Keyle.MyPet.entity.types.zombie;

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.util.MyPetPlayer;
import net.minecraft.server.v1_4_5.NBTTagCompound;

public class MyZombie extends MyPet
{
    protected boolean isBaby = false;
    protected boolean isVillager = false;

    public MyZombie(MyPetPlayer petOwner)
    {
        super(petOwner);
        this.petName = "Zombie";
    }

    public boolean isBaby()
    {
        if (status == PetState.Here)
        {
            return ((CraftMyZombie) getCraftPet()).isBaby();
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
            ((CraftMyZombie) getCraftPet()).setBaby(flag);
        }
        this.isBaby = flag;
    }

    public boolean isVillager()
    {
        if (status == PetState.Here)
        {
            return ((CraftMyZombie) getCraftPet()).isVillager();
        }
        else
        {
            return isVillager;
        }
    }

    public void setVillager(boolean flag)
    {
        if (status == PetState.Here)
        {
            ((CraftMyZombie) getCraftPet()).setVillager(flag);
        }
        this.isVillager = flag;
    }

    @Override
    public NBTTagCompound getExtendedInfo()
    {
        NBTTagCompound info = new NBTTagCompound("Info");
        info.setBoolean("Baby", isBaby);
        info.setBoolean("Villager", isVillager);
        return info;
    }

    @Override
    public void setExtendedInfo(NBTTagCompound info)
    {
        setBaby(info.getBoolean("Baby"));
        setVillager(info.getBoolean("Villager"));
    }

    @Override
    public MyPetType getPetType()
    {
        return MyPetType.Zombie;
    }

    @Override
    public String toString()
    {
        return "MyZombie{owner=" + getOwner().getName() + ", name=" + petName + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + skillTree.getName() + ", villager=" + isVillager() + ", baby=" + isBaby() + "}";
    }
}