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

import de.Keyle.MyPet.entity.types.CraftMyPet;
import org.bukkit.craftbukkit.v1_4_6.CraftServer;


public class CraftMyEnderman extends CraftMyPet
{
    public CraftMyEnderman(CraftServer server, EntityMyEnderman entityMyEnderman)
    {
        super(server, entityMyEnderman);
    }

    public short getBlockID()
    {
        return ((EntityMyEnderman) getHandle()).getBlockID();
    }

    public void setBlockID(short flag)
    {
        ((EntityMyEnderman) getHandle()).setBlockID(flag);
    }

    public short getBlockData()
    {
        return ((EntityMyEnderman) getHandle()).getBlockData();
    }

    public void setBlockData(short flag)
    {
        ((EntityMyEnderman) getHandle()).setBlockData(flag);
    }

    public boolean isScreaming()
    {
        return ((EntityMyEnderman) getHandle()).isScreaming();
    }

    public void setScreaming(boolean flag)
    {
        ((EntityMyEnderman) getHandle()).setScreaming(flag);
    }

    @Override
    public String toString()
    {
        return "CraftMyEnderman{isPet=" + getHandle().isMyPet() + ",owner=" + getOwner() + ",BlockID=" + getBlockID() + ",BlockData=" + getBlockID() + "}";
    }


}