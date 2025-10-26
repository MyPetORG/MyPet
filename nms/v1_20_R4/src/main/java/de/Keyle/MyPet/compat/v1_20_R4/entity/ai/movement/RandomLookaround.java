/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2020 Keyle
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

package de.Keyle.MyPet.compat.v1_20_R4.entity.ai.movement;

import de.Keyle.MyPet.api.entity.ai.AIGoal;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.compat.v1_20_R4.entity.EntityMyPet;

@Compat("v1_20_R4")
public class RandomLookaround implements AIGoal {

	protected EntityMyPet petEntity;
	protected double directionX;
	protected double directionZ;
	protected int ticksUntilStopLookingAround = 0;

	public RandomLookaround(EntityMyPet petEntity) {
		this.petEntity = petEntity;
	}

	@Override
	public boolean shouldStart() {
		if (this.petEntity.getMyPetTarget() != null && !this.petEntity.getMyPetTarget().isDead()) {
			return false;
		}
		if (this.petEntity.isVehicle()) {
			return false;
		}
		return this.petEntity.getRandom().nextFloat() < 0.02F;
	}

	@Override
	public boolean shouldFinish() {
		return this.ticksUntilStopLookingAround <= 0 || this.petEntity.isVehicle();
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
		this.petEntity.getLookControl().setLookAt(this.petEntity.getX() + this.directionX, this.petEntity.getY() + this.petEntity.getEyeHeight(), this.petEntity.getZ() + this.directionZ, this.petEntity.getMaxHeadXRot(), this.petEntity.getMaxHeadXRot());
	}
}
