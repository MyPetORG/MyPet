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

package de.Keyle.MyPet.skill.skills;

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.util.Scheduler;
import net.minecraft.server.NBTTagCompound;

public class MyPetGenericSkill implements Scheduler
{
    protected String skillName;
    protected int level = 0;
    protected int maxLevel = -1;
    protected MyPet myPet;

    protected MyPetGenericSkill(String name)
    {
        this.skillName = name;
    }

    protected MyPetGenericSkill(String name, int maxLevel)
    {
        this.skillName = name;
        this.maxLevel = maxLevel;
    }

    public String getName()
    {
        return this.skillName;
    }

    public void setMyPet(MyPet myPet)
    {
        this.myPet = myPet;
    }

    public MyPet getMyPet()
    {
        return myPet;
    }

    public int getLevel()
    {
        return this.level;
    }

    public void setLevel(int level)
    {
        if (level >= 0)
        {
            if(maxLevel != -1)
            {
                this.level = level > maxLevel ? maxLevel : level;
            }
            else
            {
                this.level = level;
            }
        }
        else
        {
            this.level = 0;
        }
    }

    public void upgrade()
    {
        if (maxLevel != -1)
        {
            if(this.level < maxLevel)
            {
                this.level++;
            }
        }
        else
        {
            level++;
        }
    }

    public void upgrade(int value)
    {
        if (value > 0)
        {
            value--;
            this.level += value;
            if (maxLevel != -1 && this.level > maxLevel)
            {
                level = maxLevel-1;
            }
            upgrade();
        }
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