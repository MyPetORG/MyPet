/*
 * Copyright (C) 2011-2012 Keyle
 *
 * This file is part of MyWolf
 *
 * MyWolf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyWolf is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyWolf. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyWolf.skill;

import de.Keyle.MyWolf.MyWolf;
import de.Keyle.MyWolf.util.Scheduler;
import net.minecraft.server.NBTTagCompound;

public class MyWolfGenericSkill implements Scheduler
{
    protected String Name;
    protected int Level = 0;
    protected MyWolf MWolf;

    protected MyWolfGenericSkill(String Name)
    {
        this.Name = Name;
    }

    public String getName()
    {
        return this.Name;
    }

    public void setMyWolf(MyWolf MWolf)
    {
        this.MWolf = MWolf;
    }

    public MyWolf getMyWolf()
    {
        return MWolf;
    }

    public int getLevel()
    {
        return this.Level;
    }

    public void setLevel(int level)
    {
        this.Level = level;
    }

    public void upgrade()
    {
        this.Level++;
    }

    public NBTTagCompound save()
    {
        return null;
    }

    public void load(NBTTagCompound nbtTagCompound)
    {
    }

    public void activate()
    {
    }

    public void schedule()
    {
    }
}