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

package de.Keyle.MyPet.entity.types.wolf;

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.util.MyPetPlayer;
import net.minecraft.server.NBTTagCompound;

public class MyWolf extends MyPet
{
    private boolean isSitting = false;
    private int collarColor = 0;

    public MyWolf(MyPetPlayer petOwner)
    {
        super(petOwner);
        this.petName = "Wolf";
    }

    @Override
    public MyPetType getPetType()
    {
        return MyPetType.Wolf;
    }

    @Override
    public String toString()
    {
        return "MyWolf{owner=" + getOwner().getName() + ", name=" + petName + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + skillTree.getName() + "}";
    }

    public boolean isSitting()
    {
        if (status == PetState.Here)
        {
            return ((CraftMyWolf) craftPet).isSitting();
        }
        else
        {
            return isSitting;
        }
    }

    public void setSitting(boolean sitting)
    {
        if (status == PetState.Here)
        {
            ((CraftMyWolf) craftPet).setSitting(sitting);
            this.isSitting = sitting;
        }
        else
        {
            this.isSitting = sitting;
        }
    }

    public int getCollarColor()
    {
        if (status == PetState.Here)
        {
            return ((EntityMyWolf) craftPet.getHandle()).getCollarColor();
        }
        else
        {
            return collarColor;
        }
    }

    public void setCollarColor(int color)
    {
        if (status == PetState.Here)
        {
            ((EntityMyWolf) craftPet.getHandle()).setCollarColor(color);
            this.collarColor = color;
        }
        else
        {
            this.collarColor = color;
        }
    }

    @Override
    public NBTTagCompound getExtendedInfo()
    {
        NBTTagCompound info = new NBTTagCompound("Info");
        info.setBoolean("sitting", isSitting());
        info.setInt("collar", getCollarColor());
        return info;
    }

    @Override
    public void setExtendedInfo(NBTTagCompound info)
    {
        if (info.hasKey("sitting"))
        {
            setSitting(info.getBoolean("sitting"));
        }
        if (info.hasKey("collar"))
        {
            setCollarColor(info.getInt("collar"));
        }
    }
}