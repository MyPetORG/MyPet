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

package Java.MyPet.entity.types.zombie;

import Java.MyPet.entity.types.CraftMyPet;
import org.bukkit.craftbukkit.CraftServer;

public class CraftMyZombie extends CraftMyPet
{
    public CraftMyZombie(CraftServer server, EntityMyZombie entityMyZombie)
    {
        super(server, entityMyZombie);
    }

    public boolean isBaby()
    {
        return getHandle().isBaby();
    }

    public void setBaby(boolean flag)
    {
        ((EntityMyZombie) getHandle()).setBaby(flag);
    }

    public boolean isVillager()
    {
        return ((EntityMyZombie) getHandle()).isVillager();
    }

    public void setVillager(boolean flag)
    {
        ((EntityMyZombie) getHandle()).setVillager(flag);
    }

    @Override
    public String toString()
    {
        return "CraftMyZombie{isPet=" + getHandle().isMyPet() + ",owner=" + getOwner() + ", villager=" + isVillager() + ", baby=" + isBaby() + "}";
    }
}