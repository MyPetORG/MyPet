/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2013 Keyle
 * MyPet is licensed under the GNU Lesser General Public License.
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.entity.ai.movement;

import net.minecraft.server.v1_5_R1.EntityLiving;
import net.minecraft.server.v1_5_R1.PathfinderGoal;
import net.minecraft.server.v1_5_R1.World;
import org.bukkit.craftbukkit.v1_5_R1.event.CraftEventFactory;
import org.bukkit.event.entity.EntityTargetEvent;

public class EntityAIMeleeAttack extends PathfinderGoal
{
    private World petWorld;
    EntityLiving petEntity;
    EntityLiving targetEntity;
    double range;
    float walkSpeed;
    private int ticksUntilNextHitLeft = 0;
    private int ticksUntilNextHit;
    private int timeUntilNextNavigationUpdate;

    public EntityAIMeleeAttack(EntityLiving petEntity, float walkSpeed, double range, int ticksUntilNextHit)
    {
        this.petEntity = petEntity;
        this.petWorld = petEntity.world;
        this.walkSpeed = walkSpeed;
        this.range = range * range;
        this.ticksUntilNextHit = ticksUntilNextHit;
    }

    public boolean a()
    {
        EntityLiving targetEntity = this.petEntity.getGoalTarget();
        if (targetEntity == null)
        {
            return false;
        }
        if (!targetEntity.isAlive())
        {
            return false;
        }
        this.targetEntity = targetEntity;
        return this.petEntity.aD().canSee(targetEntity);
    }

    public boolean b()
    {
        if (this.petEntity.getGoalTarget() == null)
        {
            return false;
        }
        else if (this.targetEntity != this.petEntity.getGoalTarget())
        {
            return false;
        }
        else if (!this.targetEntity.isAlive())
        {
            return false;
        }
        return true;
    }

    public void c()
    {
        this.petEntity.getNavigation().a(this.targetEntity, this.walkSpeed);
        this.timeUntilNextNavigationUpdate = 0;
    }

    public void d()
    {
        EntityTargetEvent.TargetReason reason = targetEntity.isAlive() ? EntityTargetEvent.TargetReason.FORGOT_TARGET : EntityTargetEvent.TargetReason.TARGET_DIED;
        CraftEventFactory.callEntityTargetEvent(this.petEntity, null, reason);

        this.targetEntity = null;
        this.petEntity.getNavigation().g();
    }

    public void e()
    {
        this.petEntity.getControllerLook().a(targetEntity, 30.0F, 30.0F);
        if (((this.petEntity.aD().canSee(targetEntity))) && (--this.timeUntilNextNavigationUpdate <= 0))
        {
            this.timeUntilNextNavigationUpdate = (4 + this.petEntity.aE().nextInt(7));
            this.petEntity.getNavigation().a(targetEntity, this.walkSpeed);
        }
        if ((this.petEntity.e(targetEntity.locX, targetEntity.boundingBox.b, targetEntity.locZ) <= this.range) && (this.ticksUntilNextHitLeft-- <= 0))
        {
            this.ticksUntilNextHitLeft = ticksUntilNextHit;
            if (this.petEntity.bG() != null)
            {
                this.petEntity.bK();
            }
            this.petEntity.m(targetEntity);
        }
    }
}