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

import de.Keyle.MyPet.entity.types.CraftMyPet;
import org.bukkit.craftbukkit.v1_4_6.CraftServer;
import org.bukkit.entity.Ocelot.Type;

public class CraftMyOcelot extends CraftMyPet
{
    public CraftMyOcelot(CraftServer server, EntityMyOcelot entityMyOcelot)
    {
        super(server, entityMyOcelot);
    }

    public boolean isSitting()
    {
        return ((EntityMyOcelot) getHandle()).isSitting();
    }

    public void setSitting(boolean flag)
    {
        ((EntityMyOcelot) getHandle()).setSitting(flag);
    }

    public Type getCatType()
    {
        return Type.getType(((EntityMyOcelot) getHandle()).getCatType());
    }

    public void setCatType(Type catType)
    {
        ((EntityMyOcelot) getHandle()).setCatType(catType.getId());
    }

    public boolean isBaby()
    {
        return getHandle().isBaby();
    }

    public void setBaby(boolean flag)
    {
        ((EntityMyOcelot) getHandle()).setBaby(flag);
    }

    @Override
    public String toString()
    {
        return "CraftMyOcelot{isPet=" + getHandle().isMyPet() + ",owner=" + getOwner() + ",sitting=" + isSitting() + ",catType=" + getCatType().name() + ", baby=" + isBaby() + "}";
    }
}