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
import net.minecraft.world.entity.Entity;

@Compat("v1_20_R4")
public class LookAtPlayer implements AIGoal {

	private final EntityMyPet petEntity;
	protected Entity targetPlayer;
	private final float range;
	private int ticksUntilStopLooking;
	private final float lookAtPlayerChance;

	public LookAtPlayer(EntityMyPet petEntity, float range) {
		this.petEntity = petEntity;
		this.range = range;
		this.lookAtPlayerChance = 0.02F;
	}

	public LookAtPlayer(EntityMyPet petEntity, float range, float lookAtPlayerChance) {
		this.petEntity = petEntity;
		this.range = range;
		this.lookAtPlayerChance = lookAtPlayerChance;
	}

	@Override
	public boolean shouldStart() {
		if (this.petEntity.getRandom().nextFloat() >= this.lookAtPlayerChance) {
			return false;
		}
		if (this.petEntity.getMyPetTarget() != null && !this.petEntity.getMyPetTarget().isDead()) {
			return false;
		}
		if (this.petEntity.isVehicle()) {
			return false;
		}
		this.targetPlayer = this.petEntity.level().getNearestPlayer(this.petEntity, this.range);
		return this.targetPlayer != null;
	}

	@Override
	public boolean shouldFinish() {
		if (!this.targetPlayer.isAlive()) {
			return true;
		}
		if (this.petEntity.distanceToSqr(this.targetPlayer) > this.range) {
			return true;
		}
		if (this.petEntity.isVehicle()) {
			return true;
		}
		return this.ticksUntilStopLooking <= 0;
	}

	@Override
	public void start() {
		this.ticksUntilStopLooking = (40 + this.petEntity.getRandom().nextInt(40));
	}

	@Override
	public void finish() {
		this.targetPlayer = null;
	}

	@Override
	public void tick() {
		this.petEntity.getLookControl().setLookAt(this.targetPlayer.getX(), this.targetPlayer.getY() + this.targetPlayer.getEyeHeight(), this.targetPlayer.getZ(), petEntity.getMaxHeadXRot(), this.petEntity.getMaxHeadXRot());
		this.ticksUntilStopLooking -= 1;
	}
}
