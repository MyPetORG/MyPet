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

package de.Keyle.MyPet.entity.types.creeper;

import de.Keyle.MyPet.entity.types.CraftMyPet;
import org.bukkit.craftbukkit.v1_4_6.CraftServer;


public class CraftMyCreeper extends CraftMyPet
{
    public CraftMyCreeper(CraftServer server, EntityMyCreeper entityMyCreeper)
    {
        super(server, entityMyCreeper);
    }

    public boolean isPowered()
    {
        return ((EntityMyCreeper) getHandle()).isPowered();
    }

    public void setPowered(boolean sheared)
    {
        ((EntityMyCreeper) getHandle()).setPowered(sheared);
    }

    @Override
    public String toString()
    {
        return "CraftMyCreeper{isPet=" + getHandle().isMyPet() + ",owner=" + getOwner() + ",powered=" + isPowered() + "}";
    }
}