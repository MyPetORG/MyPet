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

package de.Keyle.MyPet.compat.v1_19_R2.entity;

import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.compat.v1_19_R2.entity.ai.navigation.MyAquaticPetPathNavigation;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@EntitySize(width = 0.5F, height = 0.3f)
public abstract class EntityMyAquaticPet extends EntityMyPet {

	public EntityMyAquaticPet(Level world, MyPet myPet) {
		super(world, myPet);
	}

	@Override
	protected PathNavigation setSpecialNav() {
		return new MyAquaticPetPathNavigation(this, this.level);
	}

	@Override
	public boolean specialFloat() {
		if(this.isInWater()) {
	    	return true;
		}
		return false;
	}

	@Override
	public boolean canBreatheUnderwater() {
		return true;
	}

	@Override
	public boolean rideableUnderWater() {
		return true;
	}

	@Override	//Special riding for Underwater
	protected void ride(double motionSideways, double motionForward, double motionUpwards, float speedModifier) {
		float speed;

		if(this.isEyeInFluid(FluidTags.WATER)) {	//No floating, just riding
			double minY;
			minY = this.getBoundingBox().minY;

			float friction = 0.91F;
			if (this.onGround) {
				friction = this.level.getBlockState(new BlockPos(Mth.floor(this.getX()), Mth.floor(minY) - 1, Mth.floor(this.getZ()))).getBlock().getFriction() * 0.91F;
			}

			speed = speedModifier * (0.16277136F / (friction * friction * friction));
			this.moveRelative(speed, new Vec3(motionSideways, motionUpwards, motionForward));

			double motX = this.getDeltaMovement().x();
			double motY = this.getDeltaMovement().y();
			double motZ = this.getDeltaMovement().z();

			Vec3 mot = new Vec3(motX, motY, motZ);

			this.move(MoverType.SELF, mot);

			motY -= 0.1D;
			motY *= 0.6D;

			motY *= 0.9800000190734863D;
			motX *= friction;
			motZ *= friction;
			this.setDeltaMovement(motX, motY, motZ);

			this.startRiding(this, false);
		} else { //Call normal riding when not in water
			super.ride(motionSideways, motionForward, motionUpwards, speedModifier);
		}
	}
}
