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
    private EntityMyPet petEntity;
    private EntityLiving petOwner;
    private World world;
    private float f;
    private Navigation nav;
    private int h;
    private float b;
    private float maxDistance;
    private boolean i;
    private PathfinderGoalControl controlPathfinderGoal;

    public PathfinderGoalFollowOwner(EntityMyPet entitytameableanimal, float speed, float maxDistance, float f2, PathfinderGoalControl Control)
    {
        this.controlPathfinderGoal = Control;
        this.petEntity = entitytameableanimal;
        this.world = entitytameableanimal.world;
        this.f = speed;
        this.nav = entitytameableanimal.getNavigation();
        this.maxDistance = maxDistance;
        this.b = f2;
        this.a(3);
    }

    /**
     * Checks whether this pathfinder should be activated
     */
    public boolean a()
    {
        EntityLiving entityLiving = this.petEntity.getOwner();

        if (entityLiving == null)
        {
            return false;
        }
        else if (this.petEntity.isSitting())
        {
            return false;
        }
        else if (this.petEntity.e(entityLiving) < (double) (this.maxDistance * this.maxDistance))
        {
            return false;
        }
        else if (controlPathfinderGoal.moveTo != null)
        {
            return false;
        }
        else
        {
            this.petOwner = entityLiving;
            return true;
        }
    }

    public boolean b()
    {
        return controlPathfinderGoal.moveTo == null && !this.nav.f() && this.petEntity.e(this.petOwner) > (double) (this.b * this.b) && !this.petEntity.isSitting();
    }

    public void e()
    {
        this.h = 0;
        this.i = this.nav.a();
        this.nav.a(false);
    }

    public void c()
    {
        this.petOwner = null;
        this.nav.f();
        this.nav.a(this.i);
    }

    public void d()
    {
        this.petEntity.getControllerLook().a(this.petOwner, 10.0F, (float) this.petEntity.bf());

        if (!this.petEntity.isSitting())
        {
            if (--this.h <= 0)
            {
                this.h = 10;

                if (!this.nav.a(this.petOwner, this.f))
                {
                    if (this.petEntity.e(this.petOwner) >= 144.0D && controlPathfinderGoal.moveTo == null && petEntity.goalTarget == null)
                    {
                        int i = MathHelper.floor(this.petOwner.locX) - 2;
                        int j = MathHelper.floor(this.petOwner.locZ) - 2;
                        int k = MathHelper.floor(this.petOwner.boundingBox.b);

                        for (int l = 0 ; l <= 4 ; ++l)
                        {
                            for (int i1 = 0 ; i1 <= 4 ; ++i1)
                            {
                                if ((l < 1 || i1 < 1 || l > 3 || i1 > 3) && this.world.t(i + l, k - 1, j + i1) && !this.world.t(i + l, k, j + i1) && !this.world.t(i + l, k + 1, j + i1))
                                {
                                    this.petEntity.setPositionRotation((double) ((float) (i + l) + 0.5F), (double) k, (double) ((float) (j + i1) + 0.5F), this.petEntity.yaw, this.petEntity.pitch);
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