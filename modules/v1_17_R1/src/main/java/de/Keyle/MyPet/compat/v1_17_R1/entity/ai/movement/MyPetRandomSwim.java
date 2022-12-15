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

package de.Keyle.MyPet.compat.v1_17_R1.entity.ai.movement;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.compat.v1_17_R1.entity.EntityMyPet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.entity.ai.util.RandomPos;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;

@Compat("v1_18_R1")
public class MyPetRandomSwim extends MyPetRandomStroll {

	protected EntityMyPet petEntity;

	public MyPetRandomSwim(EntityMyPet petEntity, int startDistance) {
		super(petEntity, startDistance);
		this.petEntity = petEntity;
	}

	@Override
	public boolean shouldFinish() {
		if(!petEntity.isInWaterOrBubble())
			return super.shouldFinish();

		if (controlPathfinderGoal.moveTo != null) {
			return true;
		} else if (this.petEntity.getOwner() == null) {
			return true;
		} else if (this.petEntity.distanceToSqr(owner) > this.startDistance) {
			return true;
		} else if (!this.petEntity.canMove()) {
			return true;
		} else if (moveTo == null){
			return true;
		} else if (MyPetApi.getPlatformHelper().distance(petEntity.getMyPet().getLocation().get(), moveTo) < 2.5) {
			return true;
		}else if (timeToMove <= 0) {
			return true;
		}else return this.petEntity.getMyPetTarget() != null && !this.petEntity.getMyPetTarget().isDead();
	}

	@Override
	protected Vec3 getPosition() {
		if(petEntity.isInWaterOrBubble() && owner.isInWaterOrBubble()) {
			Vec3 leVec = makeWaterPos(this.petEntity, 10, 7);
			return leVec;
		}
		return super.getPosition();
	}

	private Vec3 makeWaterPos(PathfinderMob mobby, int i, int j) {
		Vec3 vec3d = leGen(mobby, i, j);
		Vec3 vec4d = vec3d.add(0,1,0);

		for (int k = 0;
			 vec3d != null && !mobby.level.getBlockState(new BlockPos(vec3d)).isPathfindable(mobby.level, new BlockPos(vec3d), PathComputationType.WATER)
					 && !mobby.level.getBlockState(new BlockPos(vec4d)).getMaterial().isLiquid()
					 && k++ < 10; vec3d = leGen(mobby, i, j)) {
			;
		}

		return vec3d;
	}

	private static Vec3 leGen(PathfinderMob mobby, int i, int j) {
		return RandomPos.generateRandomPos(mobby, () -> {
			BlockPos blockPos = RandomPos.generateRandomDirection(mobby.getRandom(), i, j);
			return generateRandomPosTowardDirection(mobby, i, GoalUtils.mobRestricted(mobby, i), blockPos);
		});
	}

	private static BlockPos generateRandomPosTowardDirection(PathfinderMob entitycreature, int i, boolean flag, BlockPos blockposition) {
		BlockPos lePos = RandomPos.generateRandomPosTowardDirection(entitycreature, i, entitycreature.getRandom(), blockposition);
		return !GoalUtils.isOutsideLimits(lePos, entitycreature) && !GoalUtils.isRestricted(flag, entitycreature, lePos) && !GoalUtils.isNotStable(entitycreature.getNavigation(), lePos) ? lePos : null;
	}

	@Override
	protected void applySpeed() {
		if(petEntity.isInWaterOrBubble()) {
			double walkSpeed = owner.getAbilities().walkingSpeed+0.3f;
			nav.getParameters().addSpeedModifier("RandomStroll", walkSpeed);
		} else {
			super.applySpeed();
		}
	}
}
