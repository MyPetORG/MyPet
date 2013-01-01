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

package de.Keyle.MyPet.entity.types.slime;

import de.Keyle.MyPet.entity.types.CraftMyPet;
import org.bukkit.craftbukkit.v1_4_6.CraftServer;

public class CraftMySlime extends CraftMyPet
{
    public CraftMySlime(CraftServer server, EntityMySlime entityMySlime)
    {
        super(server, entityMySlime);
    }

    public int getSize()
    {
        return ((EntityMySlime) getHandle()).getSize();
    }

    public void setSize(int value)
    {
        ((EntityMySlime) getHandle()).setSize(value);
    }

    @Override
    public String toString()
    {
        return "CraftMySlime{isPet=" + getHandle().isMyPet() + ",owner=" + getOwner() + ",size=" + getSize() + "}";
    }
}