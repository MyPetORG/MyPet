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

package Java.MyPet.entity.types.mooshroom;

import Java.MyPet.util.MyPetPlayer;
import Java.MyPet.entity.types.MyPet;
import Java.MyPet.entity.types.MyPetType;
import net.minecraft.server.NBTTagCompound;

public class MyMooshroom extends MyPet
{
    protected boolean isBaby = false;

    public MyMooshroom(MyPetPlayer petOwner)
    {
        super(petOwner);
        this.petName = "Mooshroom";
    }

    public boolean isBaby()
    {
        if (status == PetState.Here)
        {
            return ((CraftMyMooshroom) getCraftPet()).isBaby();
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
            ((CraftMyMooshroom) getCraftPet()).setBaby(flag);
        }
        this.isBaby = flag;
    }

    @Override
    public NBTTagCompound getExtendedInfo()
    {
        NBTTagCompound info = new NBTTagCompound("Info");
        info.setBoolean("Baby", isBaby());
        return info;
    }

    @Override
    public void setExtendedInfo(NBTTagCompound info)
    {
        setBaby(info.getBoolean("Baby"));
    }

    @Override
    public MyPetType getPetType()
    {
        return MyPetType.Mooshroom;
    }

    @Override
    public String toString()
    {
        return "MyMooshroom{owner=" + getOwner().getName() + ", name=" + petName + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + skillTree.getName() + ", baby=" + isBaby() + "}";
    }
}