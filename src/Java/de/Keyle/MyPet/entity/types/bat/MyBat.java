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

package de.Keyle.MyPet.entity.types.bat;

import de.Keyle.MyPet.entity.MyPetInfo;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.util.MyPetPlayer;
import net.minecraft.server.v1_4_R1.NBTTagCompound;

import static org.bukkit.Material.SPIDER_EYE;

@MyPetInfo(food = {SPIDER_EYE})
public class MyBat extends MyPet
{
    boolean hanging = false;

    public MyBat(MyPetPlayer petOwner)
    {
        super(petOwner);
        this.petName = "Bat";
    }

    public void setHanging(boolean flag)
    {
        if (status == PetState.Here)
        {
            ((EntityMyBat) getCraftPet().getHandle()).setHanging(flag);
        }
        this.hanging = flag;
    }

    public boolean ishanging()
    {
        return hanging;
    }

    @Override
    public NBTTagCompound getExtendedInfo()
    {
        NBTTagCompound info = super.getExtendedInfo();
        info.setBoolean("Hanging", ishanging());
        return info;
    }

    @Override
    public void setExtendedInfo(NBTTagCompound info)
    {
        setHanging(info.getBoolean("Hanging"));
    }

    @Override
    public MyPetType getPetType()
    {
        return MyPetType.Bat;
    }

    @Override
    public String toString()
    {
        return "MyBat{owner=" + getOwner().getName() + ", name=" + petName + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + (skillTree != null ? skillTree.getName() : "-") + ",hanging=" + ishanging() + "}";
    }
}
