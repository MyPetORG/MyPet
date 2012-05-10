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

package de.Keyle.MyWolf.entity.pathfinder;

import de.Keyle.MyWolf.entity.types.wolf.MyWolf;
import de.Keyle.MyWolf.util.MyWolfUtil;
import net.minecraft.server.PathfinderGoal;

public class PathfinderGoalTest extends PathfinderGoal
{
    MyWolf MWolf;
    boolean s = true;
    boolean d = true;
    int c = 0;
    int e = 0;

    public PathfinderGoalTest()
    {

    }

    public boolean b()
    {
        MyWolfUtil.getLogger().info("--- target: " + d + " ---");
        e = e > 50 ? 0 : e;
        if (++e > 30)
        {
            d = !d;
        }
        return d;
    }

    @Override
    public boolean a()
    {
        MyWolfUtil.getLogger().info("--- a: " + s + " ---");
        c = c > 50 ? 0 : c;
        return ++c > 50 ? s = !s : s;
    }

    public void c()
    {
        MyWolfUtil.getLogger().info("--- c ---");
    }

    public void d()
    {
        MyWolfUtil.getLogger().info("--- d ---");
    }
}
