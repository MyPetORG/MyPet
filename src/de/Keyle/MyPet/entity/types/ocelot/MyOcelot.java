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
    private static int startHP = 10;

    private int color = 0;

    public MyOcelot(MyPetPlayer petOwner)
    {
        super(petOwner);
        this.petName = "Ocelot";
    }

    public int getMaxHealth()
    {
        return startHP + (skillSystem.hasSkill("HP") ? skillSystem.getSkill("HP").getLevel() : 0);
    }

    public void setColor(int color)
    {
        this.color = color;
        if (status == PetState.Here)
        {
            ((EntityMyOcelot) craftPet.getHandle()).setCatType(color);
        }
    }

    public int getColor()
    {
        return color;
    }

    @Override
    public NBTTagCompound getExtendedInfo()
    {
        NBTTagCompound info = new NBTTagCompound("Info");
        info.setInt("Color", color);
        return info;
    }

    @Override
    public void setExtendedInfo(NBTTagCompound info)
    {
        setColor(info.getInt("Color"));
    }

    @Override
    public MyPetType getPetType()
    {
        return MyPetType.Ocelot;
    }

    @Override
    public String toString()
    {
        return "MyOcelot{owner=" + getOwner().getName() + ", name=" + petName + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + skillTree.getName() + "}";
    }

    public static void setStartHP(int hp)
    {
        startHP = hp;
    }

    public static int getStartHP()
    {
        return startHP;
    }
}