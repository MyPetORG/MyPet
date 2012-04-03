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

import net.minecraft.server.*;

public class PathfinderGoalFollowOwner extends PathfinderGoal
{
    private EntityTameableAnimal wolf;
    private EntityLiving owner;
    private World world;
    private float f;
    private Navigation nav;
    private int h;
    private float b;
    private float maxdistance;
    private boolean i;
    private PathfinderGoalControl Control;

    public PathfinderGoalFollowOwner(EntityTameableAnimal entitytameableanimal, float f, float maxdistance, float f2, PathfinderGoalControl Control)
    {
        this.Control = Control;
        this.wolf = entitytameableanimal;
        this.world = entitytameableanimal.world;
        this.f = f;
        this.nav = entitytameableanimal.al();
        this.maxdistance = maxdistance;
        this.b = f2;
        this.a(3);
    }

    public boolean a()
    {
        EntityLiving entityliving = this.wolf.getOwner();

        if (entityliving == null)
        {
            return false;
        }
        else if (this.wolf.isSitting())
        {
            return false;
        }
        else if (this.wolf.j(entityliving) < (double) (this.maxdistance * this.maxdistance))
        {
            return false;
        }
        else if (Control.moveTo != null)
        {
            return false;
        }
        else
        {
            this.owner = entityliving;
            return true;
        }
    }

    public boolean b()
    {
        return Control.moveTo == null && !this.nav.e() && this.wolf.j(this.owner) > (double) (this.b * this.b) && !this.wolf.isSitting();
    }

    public void c()
    {
        this.h = 0;
        this.i = this.wolf.al().a();
        this.wolf.al().a(false);
    }

    public void d()
    {
        this.owner = null;
        this.nav.f();
        this.wolf.al().a(this.i);
    }

    public void e()
    {
        this.wolf.getControllerLook().a(this.owner, 10.0F, (float) this.wolf.D());

        if (!this.wolf.isSitting())
        {
            if (--this.h <= 0)
            {
                this.h = 10;

                if (!this.nav.a(this.owner, this.f))
                {
                    if (this.wolf.j(this.owner) >= 144.0D && Control.moveTo == null)
                    {
                        int i = MathHelper.floor(this.owner.locX) - 2;
                        int j = MathHelper.floor(this.owner.locZ) - 2;
                        int k = MathHelper.floor(this.owner.boundingBox.b);

                        for (int l = 0 ; l <= 4 ; ++l)
                        {
                            for (int i1 = 0 ; i1 <= 4 ; ++i1)
                            {
                                if ((l < 1 || i1 < 1 || l > 3 || i1 > 3) && this.world.e(i + l, k - 1, j + i1) && !this.world.e(i + l, k, j + i1) && !this.world.e(i + l, k + 1, j + i1))
                                {
                                    this.wolf.setPositionRotation((double) ((float) (i + l) + 0.5F), (double) k, (double) ((float) (j + i1) + 0.5F), this.wolf.yaw, this.wolf.pitch);
                                    this.nav.f();
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
