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

package de.Keyle.MyPet.entity.types.enderman;


import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.util.MyPetPlayer;
import net.minecraft.server.v1_4_R1.NBTTagCompound;

public class MyEnderman extends MyPet
{

    short BlockID = 0;
    short BlockData = 0;
    public boolean isScreaming = false;

    public MyEnderman(MyPetPlayer petOwner)
    {
        super(petOwner);
        this.petName = "Enderman";
    }

    public short getBlockID()
    {
        return BlockID;
    }

    public void setBlockID(short flag)
    {
        if (status == PetState.Here)
        {
            ((EntityMyEnderman) getCraftPet().getHandle()).setBlockID(flag);
        }
        this.BlockID = flag;
    }

    public short getBlockData()
    {
        return BlockData;
    }

    public void setBlockData(short flag)
    {
        if (status == PetState.Here)
        {
            ((EntityMyEnderman) getCraftPet().getHandle()).setBlockData(flag);
        }
        this.BlockData = flag;
    }

    public boolean isScreaming()
    {
        return isScreaming;
    }

    public void setScreaming(boolean flag)
    {
        if (status == PetState.Here)
        {
            ((EntityMyEnderman) getCraftPet().getHandle()).setScreaming(flag);
        }
        this.isScreaming = flag;
    }

    @Override
    public NBTTagCompound getExtendedInfo()
    {
        NBTTagCompound info = new NBTTagCompound("Info");
        info.setShort("BlockID", getBlockID());
        info.setShort("BlockData", getBlockData());
        //info.setBoolean("Screaming", isScreaming());
        return info;
    }

    @Override
    public void setExtendedInfo(NBTTagCompound info)
    {
        setBlockID(info.getShort("BlockID"));
        setBlockData(info.getShort("BlockData"));
        //setScreaming(info.getBoolean("Screaming"));
    }

    @Override
    public MyPetType getPetType()
    {
        return MyPetType.Enderman;
    }

    @Override
    public String toString()
    {
        return "MyEnderman{owner=" + getOwner().getName() + ", name=" + petName + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + (skillTree != null ? skillTree.getName() : "-") + ",BlockID=" + getBlockID() + ",BlockData=" + getBlockData() + "}";
    }

}
