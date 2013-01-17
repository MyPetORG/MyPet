/*
 * Copyright (C) 2011-2013 Keyle
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

package de.Keyle.MyPet.entity.ai.movement;

import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.util.MyPetUtil;
import net.minecraft.server.v1_4_R1.Navigation;
import net.minecraft.server.v1_4_R1.PathfinderGoal;
import org.bukkit.Location;

public class EntityAIFollowOwner extends PathfinderGoal
{
    private EntityMyPet petEntity;
    private float walkSpeed;
    private Navigation nav;
    private int setPathTimer = 0;
    private float stopDistance;
    private float startDistance;
    private float teleportDistance;
    private boolean nav_a_save;
    private EntityAIControl controlPathfinderGoal;

    public EntityAIFollowOwner(EntityMyPet entityMyPet, float walkSpeed, float startDistance, float stopDistance, float teleportDistance)
    {
        this.petEntity = entityMyPet;
        this.walkSpeed = walkSpeed;
        this.nav = entityMyPet.getNavigation();
        this.startDistance = startDistance * startDistance;
        this.stopDistance = stopDistance * stopDistance;
        this.teleportDistance = teleportDistance * teleportDistance;
    }

    /**
     * Checks whether this ai should be activated
     */
    public boolean a()
    {
        if (petEntity.petPathfinderSelector.hasGoal("Control"))
        {
            if (controlPathfinderGoal == null)
            {
                controlPathfinderGoal = (EntityAIControl) petEntity.petPathfinderSelector.getGoal("Control");
            }
        }
        if (!this.petEntity.canMove())
        {
            return false;
        }
        else if (this.petEntity.getGoalTarget() != null && this.petEntity.getGoalTarget().isAlive())
        {
            return false;
        }
        else if (this.petEntity.getOwner() == null)
        {
            return false;
        }
        else if (this.petEntity.e(this.petEntity.getOwner()) < this.startDistance)
        {
            return false;
        }
        else if (controlPathfinderGoal != null && controlPathfinderGoal.moveTo != null)
        {
            return false;
        }
        return true;
    }

    public boolean b()
    {
        if (controlPathfinderGoal.moveTo != null)
        {
            return false;
        }
        else if (this.nav.f())
        {
            return false;
        }
        else if (this.petEntity.getOwner() == null)
        {
            return false;
        }
        else if (this.petEntity.e(this.petEntity.getOwner()) < this.stopDistance)
        {
            return false;
        }
        else if (!this.petEntity.canMove())
        {
            return false;
        }
        else if (this.petEntity.getGoalTarget() != null && this.petEntity.getGoalTarget().isAlive())
        {
            return false;
        }
        return true;
    }

    public void c()
    {
        this.setPathTimer = 0;
        this.nav_a_save = this.nav.a();
        this.nav.a(false);
    }

    public void d()
    {
        this.nav.f();
        this.nav.a(this.nav_a_save);
    }

    public void e()
    {
        this.petEntity.getControllerLook().a(this.petEntity.getOwner(), 10.0F, (float) this.petEntity.bp());

        if (this.petEntity.canMove())
        {
            if (--this.setPathTimer <= 0)
            {
                this.setPathTimer = 10;

                if (!this.nav.a(this.petEntity.getOwner(), this.walkSpeed))
                {
                    Location ownerLocation = this.petEntity.getMyPet().getOwner().getPlayer().getLocation();
                    if (this.petEntity.e(this.petEntity.getOwner()) > this.teleportDistance && controlPathfinderGoal.moveTo == null && petEntity.goalTarget == null && MyPetUtil.canSpawn(ownerLocation, this.petEntity))
                    {
                        this.petEntity.setPositionRotation(ownerLocation.getX(), ownerLocation.getY(), ownerLocation.getZ(), this.petEntity.yaw, this.petEntity.pitch);
                        this.nav.a(this.petEntity.getOwner(), this.walkSpeed);
                    }
                }
            }
        }
    }
}