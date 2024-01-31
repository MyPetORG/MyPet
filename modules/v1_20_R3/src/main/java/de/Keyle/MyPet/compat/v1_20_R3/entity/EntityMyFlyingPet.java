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

package de.Keyle.MyPet.compat.v1_20_R3.entity;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.compat.v1_20_R3.entity.ai.attack.MeleeAttack;
import de.Keyle.MyPet.compat.v1_20_R3.entity.ai.movement.MyPetFlyingMoveControl;
import de.Keyle.MyPet.compat.v1_20_R3.entity.ai.movement.MyPetRandomFly;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;

@EntitySize(width = 0.5F, height = 0.3f)
public abstract class EntityMyFlyingPet extends EntityMyPet {

	public EntityMyFlyingPet(Level world, MyPet myPet) {
		super(world, myPet);
		this.setPathfindingMalus(BlockPathTypes.WATER, -1.0f);
		this.switchMovement(new MyPetFlyingMoveControl(this, this.maxTurn));
	}

	@Override
	protected PathNavigation setSpecialNav() {
		return new FlyingPathNavigation(this, this.level());
	}

	//Disengage FallDamage
	@Override
	protected void checkFallDamage(double d0, boolean flag, BlockState iblockdata, BlockPos blockposition) {}

	@Override
	public void travel(Vec3 vec3d) {
		if (this.isControlledByLocalInstance()) {
			if (this.isInWater() || this.isInLava() || hasRider || this.isVehicle()) {
				super.travel(vec3d);
				return;
			} else {
				float f = 0.91F;

				if (this.onGround()) {
					f = this.level().getBlockState(this.getBlockPosBelowThatAffectsMyMovement()).getBlock().getFriction() * 0.91F;
				}

				float f1 = 0.16277137F / (f * f * f);

				f = 0.91F;
				if (this.onGround()) {
					f = this.level().getBlockState(this.getBlockPosBelowThatAffectsMyMovement()).getBlock().getFriction() * 0.91F;
				}

				this.moveRelative(this.onGround() ? 0.1F * f1 : 0.02F, vec3d);
				this.move(MoverType.SELF, this.getDeltaMovement());
				this.setDeltaMovement(this.getDeltaMovement().scale((double) f));
			}
		}

		this.calculateEntityAnimation(false);
	}

	@Override
	public void setPathfinder() {
		super.setPathfinder();
		petPathfinderSelector.addGoal("RandomFly", new MyPetRandomFly(this, (int) Configuration.Entity.MYPET_FOLLOW_START_DISTANCE));
		petPathfinderSelector.addGoal("MeleeAttack", new MeleeAttack(this, 0.7F, this.getBbWidth() + 1.3, 20));
	}
}
