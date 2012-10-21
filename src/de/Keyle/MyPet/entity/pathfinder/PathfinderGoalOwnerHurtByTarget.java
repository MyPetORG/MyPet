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
import net.minecraft.server.EntityLiving;
import net.minecraft.server.PathfinderGoalTarget;

public class PathfinderGoalOwnerHurtByTarget extends PathfinderGoalTarget
{
    EntityMyPet entityMyPet;
    EntityLiving b;

    public PathfinderGoalOwnerHurtByTarget(EntityMyPet entityMyPet)
    {
        super(entityMyPet, 32.0F, false);
        this.entityMyPet = entityMyPet;
        a(1);
    }

    public boolean a()
    {
        EntityLiving localEntityLiving = this.entityMyPet.getOwner();
        if (localEntityLiving == null)
        {
            return false;
        }
        this.b = localEntityLiving.av();
        return a(this.b, false);
    }

    public void e()
    {
        this.d.b(this.b);
        super.e();
    }
}