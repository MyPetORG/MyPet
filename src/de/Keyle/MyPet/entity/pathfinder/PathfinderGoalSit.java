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
import de.Keyle.MyPet.entity.types.ocelot.EntityMyOcelot;
import de.Keyle.MyPet.entity.types.wolf.EntityMyWolf;
import net.minecraft.server.EntityLiving;
import net.minecraft.server.PathfinderGoal;

public class PathfinderGoalSit extends PathfinderGoal
{
    private EntityMyPet entityMyPet;
    private boolean sitting = false;

    public PathfinderGoalSit(EntityMyPet entityMyPet)
    {
        this.entityMyPet = entityMyPet;
        a(5);
    }

    public boolean a()
    {
        if (!(this.entityMyPet instanceof EntityMyOcelot) && !(this.entityMyPet instanceof EntityMyWolf))
        {
            return false;
        }
        if (this.entityMyPet.H())
        {
            return false;
        }
        if (!this.entityMyPet.onGround)
        {
            return false;
        }

        EntityLiving localEntityLiving = this.entityMyPet.getOwner();
        if (localEntityLiving == null)
        {
            return true;
        }

        if ((this.entityMyPet.e(localEntityLiving) < 144.0D) && (localEntityLiving.av() != null))
        {
            return false;
        }
        return this.sitting;
    }

    public void e()
    {
        this.entityMyPet.getNavigation().g();
        if (this.entityMyPet instanceof EntityMyOcelot)
        {
            ((EntityMyOcelot) this.entityMyPet).applySitting(true);
        }
        if (this.entityMyPet instanceof EntityMyWolf)
        {
            ((EntityMyWolf) this.entityMyPet).applySitting(true);
        }
    }

    public void c()
    {

        if (this.entityMyPet instanceof EntityMyOcelot)
        {
            ((EntityMyOcelot) this.entityMyPet).applySitting(false);
        }
        if (this.entityMyPet instanceof EntityMyWolf)
        {
            ((EntityMyWolf) this.entityMyPet).applySitting(false);
        }
    }

    public void setSitting(boolean sitting)
    {
        this.sitting = sitting;
    }

    public boolean isSitting()
    {
        return this.sitting;
    }

    public void toogleSitting()
    {
        this.sitting = !this.sitting;
    }
}