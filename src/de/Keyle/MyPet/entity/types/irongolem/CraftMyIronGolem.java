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

package de.Keyle.MyPet.entity.types.irongolem;

import de.Keyle.MyPet.entity.types.CraftMyPet;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.EntityType;

public class CraftMyIronGolem extends CraftMyPet
{
    public CraftMyIronGolem(CraftServer server, EntityMyIronGolem ironGolem)
    {
        super(server, ironGolem);
    }

    @Override
    public EntityMyPet getHandle()
    {
        return (EntityMyIronGolem) entity;
    }

    @Override
    public String toString()
    {
        return "CraftMyIronGolem{isPet=" + getHandle().isMyPet() + ",owner=" + getOwner() + ",tame=" + isTamed() + ",sitting=" + isSitting() + "}";
    }

    public EntityType getType()
    {
        return EntityType.UNKNOWN;
    }
}