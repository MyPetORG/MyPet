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

package de.Keyle.MyPet.entity.pathfinder;

import de.Keyle.MyPet.entity.types.EntityMyPet;
import net.minecraft.server.*;

public class PathfinderGoalFollowOwner extends PathfinderGoal
{
    private EntityMyPet pet;
    private EntityLiving owner;
    private World world;
    private float f;
    private Navigation nav;
    private int h;
    private float b;
    private float maxdistance;
    private boolean i;
    private PathfinderGoalControl Control;

    public PathfinderGoalFollowOwner(EntityMyPet entitytameableanimal, float speed, float maxdistance, float f2, PathfinderGoalControl Control)
    {
        this.Control = Control;
        this.pet = entitytameableanimal;
        this.world = entitytameableanimal.world;
        this.f = speed;
        this.nav = entitytameableanimal.getNavigation();
        this.maxdistance = maxdistance;
        this.b = f2;
        this.a(3);
    }

    /**
     * Checks whether this pathfinder should be activated
     */
    public boolean a()
    {
        EntityLiving entityliving = this.pet.getOwner();

        if (entityliving == null)
        {
            return false;
        }
        else if (this.pet.isSitting())
        {
            return false;
        }
        else if (this.pet.e(entityliving) < (double) (this.maxdistance * this.maxdistance))
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
        return Control.moveTo == null && !this.nav.f() && this.pet.e(this.owner) > (double) (this.b * this.b) && !this.pet.isSitting();
    }

    public void e()
    {
        this.h = 0;
        this.i = this.nav.a();
        this.nav.a(false);
    }

    public void c()
    {
        this.owner = null;
        this.nav.f();
        this.nav.a(this.i);
    }

    public void d()
    {
        this.pet.getControllerLook().a(this.owner, 10.0F, (float) this.pet.bf());

        if (!this.pet.isSitting())
        {
            if (--this.h <= 0)
            {
                this.h = 10;

                if (!this.nav.a(this.owner, this.f))
                {
                    if (this.pet.e(this.owner) >= 144.0D && Control.moveTo == null && pet.Goaltarget == null)
                    {
                        int i = MathHelper.floor(this.owner.locX) - 2;
                        int j = MathHelper.floor(this.owner.locZ) - 2;
                        int k = MathHelper.floor(this.owner.boundingBox.b);

                        for (int l = 0 ; l <= 4 ; ++l)
                        {
                            for (int i1 = 0 ; i1 <= 4 ; ++i1)
                            {
                                if ((l < 1 || i1 < 1 || l > 3 || i1 > 3) && this.world.t(i + l, k - 1, j + i1) && !this.world.t(i + l, k, j + i1) && !this.world.t(i + l, k + 1, j + i1))
                                {
                                    this.pet.setPositionRotation((double) ((float) (i + l) + 0.5F), (double) k, (double) ((float) (j + i1) + 0.5F), this.pet.yaw, this.pet.pitch);
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