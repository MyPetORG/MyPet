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

import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.skill.skills.implementation.Ride;
import net.minecraft.server.v1_5_R1.*;

public class EntityAIRide extends PathfinderGoal
{
    private final EntityMyPet petEntity;
    private final float startSpeed;
    private MyPet myPet;
    private float currentSpeed = 0.0F;
    private boolean stopRiding = true;

    public EntityAIRide(EntityMyPet entityMyPet, float startSpeed)
    {
        this.petEntity = entityMyPet;
        this.startSpeed = startSpeed;
        myPet = petEntity.getMyPet();
        a(7);
    }

    public boolean a()
    {
        if (!myPet.getSkills().isSkillActive("Ride"))
        {
            return false;
        }
        else if (!this.petEntity.isAlive())
        {
            return false;
        }
        else if (this.petEntity.passenger == null)
        {
            return false;
        }
        else if (!petEntity.canMove())
        {
            return false;
        }
        else if (!(petEntity.passenger instanceof EntityPlayer))
        {
            return false;
        }
        return true;
    }

    public void c()
    {
        this.currentSpeed = 0.0F;
        this.petEntity.setRidden(true);
        petEntity.setSize(1);
    }

    public void d()
    {
        this.currentSpeed = 0.0F;
        this.petEntity.setRidden(false);
        petEntity.setSize();
    }

    public void e()
    {
        EntityHuman petRider = (EntityHuman) this.petEntity.passenger;

        if (petRider.isSneaking() && this.petEntity.onGround)
        {
            this.petEntity.motY += 0.5;
        }
        if (stopRiding)
        {
            return;
        }

        float totalSpeed = this.startSpeed + (((Ride) myPet.getSkills().getSkill("Ride")).getSpeed());

        float rotationDiff = MathHelper.g(petRider.yaw - this.petEntity.yaw) * 0.5F;
        if (rotationDiff > 5.0F)
        {
            rotationDiff = 5.0F;
        }
        if (rotationDiff < -5.0F)
        {
            rotationDiff = -5.0F;
        }

        this.petEntity.yaw = MathHelper.g(this.petEntity.yaw + rotationDiff);
        if (this.currentSpeed < totalSpeed)
        {
            this.currentSpeed += (totalSpeed - this.currentSpeed) * 0.01F;
        }
        if (this.currentSpeed > totalSpeed)
        {
            this.currentSpeed = totalSpeed;
        }

        int x = MathHelper.floor(this.petEntity.locX);
        int y = MathHelper.floor(this.petEntity.locY);
        int z = MathHelper.floor(this.petEntity.locZ);

        // Calculation of new Pathpoint
        float f3 = 0.91F;
        if (this.petEntity.onGround)
        {
            f3 = 0.5460001F;
            int belowEntityBlockID = this.petEntity.world.getTypeId(MathHelper.d(x), MathHelper.d(y) - 1, MathHelper.d(z));
            if (belowEntityBlockID > 0)
            {
                f3 = Block.byId[belowEntityBlockID].frictionFactor * 0.91F;
            }
        }
        float f4 = 0.1627714F / (f3 * f3 * f3);
        float f5 = MathHelper.sin(this.petEntity.yaw * 3.141593F / 180.0F);
        float f6 = MathHelper.cos(this.petEntity.yaw * 3.141593F / 180.0F);
        float f7 = this.petEntity.aI() * f4;
        float f8 = Math.max(this.currentSpeed, 1.0F);
        f8 = f7 / f8;
        float f9 = this.currentSpeed * f8;
        float f10 = -(f9 * f5);
        float f11 = f9 * f6;

        if (MathHelper.abs(f10) > MathHelper.abs(f11))
        {
            if (f10 < 0.0F)
            {
                f10 -= this.petEntity.width / 2.0F;
            }
            if (f10 > 0.0F)
            {
                f10 += this.petEntity.width / 2.0F;
            }
            f11 = 0.0F;
        }
        else
        {
            f10 = 0.0F;
            if (f11 < 0.0F)
            {
                f11 -= this.petEntity.width / 2.0F;
            }
            if (f11 > 0.0F)
            {
                f11 += this.petEntity.width / 2.0F;
            }
        }

        int n = MathHelper.floor(this.petEntity.locX + f10);
        int i1 = MathHelper.floor(this.petEntity.locZ + f11);

        PathPoint localPathPoint = new PathPoint(MathHelper.d(this.petEntity.width + 1.0F), MathHelper.d(this.petEntity.length + petRider.length + 1.0F), MathHelper.d(this.petEntity.width + 1.0F));

        if ((x != n) || (z != i1))
        {
            int blockAtEntityPos = this.petEntity.world.getData(x, y, z);
            int blockbelowEntityPos = this.petEntity.world.getData(x, y - 1, z);
            boolean isStep = checkForStep(blockAtEntityPos) || ((Block.byId[blockAtEntityPos] == null) && checkForStep(blockbelowEntityPos));

            if (!isStep && Pathfinder.a(this.petEntity, n, y, i1, localPathPoint, false, false, true) == 0 && Pathfinder.a(this.petEntity, x, y + 1, z, localPathPoint, false, false, true) == 1 && Pathfinder.a(this.petEntity, n, y + 1, i1, localPathPoint, false, false, true) == 1)
            {
                this.petEntity.getControllerJump().a();
            }
        }

        this.petEntity.e(0.0F, this.currentSpeed);
    }

    private boolean checkForStep(int blockId)
    {
        return Block.byId[blockId] != null && (Block.byId[blockId].d() == 10 || Block.byId[blockId] instanceof BlockStepAbstract);
    }

    public void stopRiding(boolean flag)
    {
        this.currentSpeed = 0.0F;
        this.stopRiding = flag;
    }

    public void toggleRiding()
    {
        if (this.petEntity.passenger != null)
        {
            this.currentSpeed = 0.0F;
            this.stopRiding = !this.stopRiding;
        }
    }

    public boolean canRide()
    {
        return !this.stopRiding;
    }
}