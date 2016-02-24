/*
 * This file is part of mypet-v1_8_R3
 *
 * Copyright (C) 2011-2016 Keyle
 * mypet-v1_8_R3 is licensed under the GNU Lesser General Public License.
 *
 * mypet-v1_8_R3 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mypet-v1_8_R3 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.compat.v1_8_R3.entity.ai.movement;

import de.Keyle.MyPet.api.entity.ai.AIGoal;
import de.Keyle.MyPet.compat.v1_8_R3.entity.EntityMyPet;

public class RandomLookaround extends AIGoal {
    private EntityMyPet petEntity;
    private double directionX;
    private double directionZ;
    private int ticksUntilStopLookingAround = 0;

    public RandomLookaround(EntityMyPet petEntity) {
        this.petEntity = petEntity;
    }

    @Override
    public boolean shouldStart() {
        if (this.petEntity.getGoalTarget() != null && this.petEntity.getGoalTarget().isAlive()) {
            return false;
        }
        if (this.petEntity.passenger != null) {
            return false;
        }
        return this.petEntity.getRandom().nextFloat() < 0.02F;
    }

    @Override
    public boolean shouldFinish() {
        return this.ticksUntilStopLookingAround <= 0 || this.petEntity.passenger != null;
    }

    @Override
    public void start() {
        double circumference = 6.283185307179586D * this.petEntity.getRandom().nextDouble();
        this.directionX = Math.cos(circumference);
        this.directionZ = Math.sin(circumference);
        this.ticksUntilStopLookingAround = (20 + this.petEntity.getRandom().nextInt(20));
    }

    @Override
    public void tick() {
        this.ticksUntilStopLookingAround--;
        this.petEntity.getControllerLook().a(this.petEntity.locX + this.directionX, this.petEntity.locY + this.petEntity.getHeadHeight(), this.petEntity.locZ + this.directionZ, 10.0F, this.petEntity.bQ());
    }
}