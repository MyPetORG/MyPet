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

package de.Keyle.MyPet.entity.types.sheep;

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.util.MyPetPlayer;
import net.minecraft.server.NBTTagCompound;

public class MySheep extends MyPet
{
    int color = 0;
    boolean sheared = false;

    public MySheep(MyPetPlayer petOwner)
    {
        super(petOwner);
        this.petName = "Sheep";
    }

    public void setColor(int color)
    {
        this.color = color;
        if (status == PetState.Here)
        {
            ((EntityMySheep) this.getPet().getHandle()).setColor(color);
        }
    }

    public int getColor()
    {
        return color;
    }

    public void setSheared(boolean sheared)
    {
        this.sheared = sheared;
        if (status == PetState.Here)
        {
            ((EntityMySheep) this.getPet().getHandle()).setSheared(sheared);
        }
    }

    public boolean isSheared()
    {
        if (sheared != ((EntityMySheep) this.getPet().getHandle()).isSheared())
        {
            sheared = !sheared;
        }
        return sheared;
    }

    @Override
    public NBTTagCompound getExtendedInfo()
    {
        NBTTagCompound info = new NBTTagCompound("Info");
        info.setInt("Color", color);
        info.setBoolean("Sheared", sheared);
        return info;
    }

    @Override
    public void setExtendedInfo(NBTTagCompound info)
    {
        if (info.hasKey("Color"))
        {
            setColor(info.getInt("Color"));
        }
        if (info.hasKey("Sheared"))
        {
            setSheared(info.getBoolean("Sheared"));
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
        return "MySheep{owner=" + getOwner().getName() + ", name=" + petName + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + skillTree.getName() + ", color=" + getColor() + ", sheared=" + isSheared() + "}";
    }
}