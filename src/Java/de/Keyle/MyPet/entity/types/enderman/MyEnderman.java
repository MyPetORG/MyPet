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

package de.Keyle.MyPet.entity.types.enderman;


import de.Keyle.MyPet.entity.MyPetInfo;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.util.MyPetPlayer;
import net.minecraft.server.v1_4_R1.NBTTagCompound;

import static org.bukkit.Material.SOUL_SAND;

@MyPetInfo(food = {SOUL_SAND})
public class MyEnderman extends MyPet
{

    int BlockID = 0;
    int BlockData = 0;
    public boolean isScreaming = false;

    public MyEnderman(MyPetPlayer petOwner)
    {
        super(petOwner);
        this.petName = "Enderman";
    }

    public int getBlockID()
    {
        return BlockID;
    }

    public int getBlockData()
    {
        return BlockData;
    }

    public void setBlock(int id, int data)
    {
        if (status == PetState.Here)
        {
            ((EntityMyEnderman) getCraftPet().getHandle()).setBlock(id, data);
        }
        this.BlockID = id;
        this.BlockData = data;
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
        NBTTagCompound info = super.getExtendedInfo();
        info.setInt("BlockID", getBlockID());
        info.setInt("BlockData", getBlockData());
        //info.setBoolean("Screaming", isScreaming());
        return info;
    }

    @Override
    public void setExtendedInfo(NBTTagCompound info)
    {
        int id, data;
        if (info.get("BlockID").getTypeId() == 2)
        {
            id = info.getShort("BlockID");
            data = info.getShort("BlockData");
        }
        else
        {
            id = info.getInt("BlockID");
            data = info.getInt("BlockData");
        }
        setBlock(id, data);
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
