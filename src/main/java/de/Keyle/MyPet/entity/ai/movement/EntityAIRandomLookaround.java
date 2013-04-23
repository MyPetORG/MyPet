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

import de.Keyle.MyPet.entity.ai.EntityAIGoal;
import de.Keyle.MyPet.entity.types.EntityMyPet;

public class EntityAIRandomLookaround extends EntityAIGoal
{
    private EntityMyPet petEntity;
    private double directionX;
    private double directionZ;
    private int ticksUntilStopLookingAround = 0;

    public EntityAIRandomLookaround(EntityMyPet petEntity)
    {
        this.petEntity = petEntity;
    }

    @Override
    public boolean shouldStart()
    {
        return this.petEntity.aE().nextFloat() < 0.02F;
    }

    @Override
    public boolean shouldFinish()
    {
        return this.ticksUntilStopLookingAround >= 0;
    }

    @Override
    public void start()
    {
        double circumference = 6.283185307179586D * this.petEntity.aE().nextDouble();
        this.directionX = Math.cos(circumference);
        this.directionZ = Math.sin(circumference);
        this.ticksUntilStopLookingAround = (20 + this.petEntity.aE().nextInt(20));
    }

    @Override
    public void tick()
    {
        this.ticksUntilStopLookingAround -= 1;
        this.petEntity.getControllerLook().a(this.petEntity.locX + this.directionX, this.petEntity.locY + this.petEntity.getHeadHeight(), this.petEntity.locZ + this.directionZ, 10.0F, this.petEntity.bs());
    }
}
