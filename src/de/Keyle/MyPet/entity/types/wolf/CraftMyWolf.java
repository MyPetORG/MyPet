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

package de.Keyle.MyPet.entity.types.wolf;

import de.Keyle.MyPet.entity.types.CraftMyPet;
import org.bukkit.DyeColor;
import org.bukkit.craftbukkit.CraftServer;

public class CraftMyWolf extends CraftMyPet
{
    public CraftMyWolf(CraftServer server, EntityMyWolf entityMyWolf)
    {
        super(server, entityMyWolf);
    }

    public boolean isBaby()
    {
        return getHandle().isBaby();
    }

    public void setBaby(boolean flag)
    {
        ((EntityMyWolf) getHandle()).setBaby(flag);
    }

    public boolean isTamed()
    {
        return ((EntityMyWolf) getHandle()).isTamed();
    }

    public void setTamed(boolean flag)
    {
        ((EntityMyWolf) getHandle()).setTamed(flag);
    }

    public boolean isAngry()
    {
        return ((EntityMyWolf) getHandle()).isAngry();
    }

    public void setAngry(boolean flag)
    {
        ((EntityMyWolf) getHandle()).setAngry(flag);
    }

    public boolean isSitting()
    {
        return ((EntityMyWolf) getHandle()).isSitting();
    }

    public void setSitting(boolean flag)
    {
        ((EntityMyWolf) getHandle()).setSitting(flag);
    }

    public DyeColor getCollarColor()
    {
        return DyeColor.getByData((byte) ((EntityMyWolf) getHandle()).getCollarColor());
    }

    public void setCollarColor(DyeColor value)
    {
        ((EntityMyWolf) getHandle()).setCollarColor(value.getData());
    }

    @Override
    public String toString()
    {
        return "CraftMyWolf{isPet=" + getHandle().isMyPet() + ",owner=" + getOwner() + ",sitting=" + isSitting() + ",color=" + getCollarColor() + ", yngry=" + isAngry() + ", baby=" + isBaby() + "}";
    }
}