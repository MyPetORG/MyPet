/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2013 Keyle
 * MyPet is licensed under the GNU Lesser General Public License.
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.entity.types.pig;

import de.Keyle.MyPet.entity.MyPetInfo;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.util.MyPetPlayer;
import net.minecraft.server.v1_4_R1.NBTTagCompound;

import static org.bukkit.Material.CARROT_ITEM;

@MyPetInfo(food = {CARROT_ITEM})
public class MyPig extends MyPet
{
    protected boolean hasSaddle = false;
    protected boolean isBaby = false;

    public MyPig(MyPetPlayer petOwner)
    {
        super(petOwner);
        this.petName = "Pig";
    }

    public boolean hasSaddle()
    {
        return hasSaddle;
    }

    public void setSaddle(boolean saddle)
    {
        if (status == PetState.Here)
        {
            ((EntityMyPig) getCraftPet().getHandle()).setSaddle(saddle);
        }
        this.hasSaddle = saddle;
    }

    public boolean isBaby()
    {
        return isBaby;
    }

    public void setBaby(boolean flag)
    {
        if (status == PetState.Here)
        {
            ((EntityMyPig) getCraftPet().getHandle()).setBaby(flag);
        }
        this.isBaby = flag;
    }

    @Override
    public NBTTagCompound getExtendedInfo()
    {
        NBTTagCompound info = super.getExtendedInfo();
        info.setBoolean("Saddle", hasSaddle());
        info.setBoolean("Baby", isBaby());
        return info;
    }

    @Override
    public void setExtendedInfo(NBTTagCompound info)
    {
        if (info.hasKey("Saddle"))
        {
            setSaddle(info.getBoolean("Saddle"));
        }
        if (info.hasKey("Baby"))
        {
            setBaby(info.getBoolean("Baby"));
        }
    }

    @Override
    public MyPetType getPetType()
    {
        return MyPetType.Pig;
    }

    @Override
    public String toString()
    {
        return "MyPig{owner=" + getOwner().getName() + ", name=" + petName + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + (skillTree != null ? skillTree.getName() : "-") + ", saddle=" + hasSaddle() + ", baby=" + isBaby() + "}";
    }
}