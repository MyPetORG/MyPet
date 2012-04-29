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

import org.bukkit.entity.EntityType;

import java.util.Random;

public class MyWolfMonsterExpirience
{
    private int min;
    private int max;
    private EntityType entityType;
    private static Random random = new Random();

    public MyWolfMonsterExpirience(int min, int max, EntityType entityType)
    {
        if (max >= min)
        {
            this.max = max;
            this.min = min;
        }
        else if (max <= min)
        {
            this.max = min;
            this.min = max;
        }
        this.entityType = entityType;
    }

    public MyWolfMonsterExpirience(int exp, EntityType entityType)
    {
        this.max = exp;
        this.min = exp;
        this.entityType = entityType;
    }

    public double getRandomExp()
    {
        return max == min ? max : random.nextInt(max - min + 1) + min;
    }

    public int getMin()
    {
        return min;
    }

    public int getMax()
    {
        return max;
    }

    public EntityType getEntityType()
    {
        return entityType;
    }

    public void setMin(int min)
    {
        this.min = min;
        if (min > max)
        {
            max = min;
        }
    }

    public void setMax(int max)
    {
        this.max = max;
        if (max < min)
        {
            min = max;
        }
    }

    public void setExp(int exp)
    {
        max = (min = exp);
    }

    @Override
    public String toString()
    {
        return entityType.getName() + "{min=" + min + ", max=" + max + "}";
    }
}