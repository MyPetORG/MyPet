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

package de.Keyle.MyPet.compat.v1_19_R2.entity.types;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.compat.v1_19_R2.entity.EntityMyPet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.Level;

@EntitySize(width = 0.5F, height = 0.45f)
public class EntityMyBat extends EntityMyPet {

	private static final EntityDataAccessor<Byte> HANGING_WATCHER = SynchedEntityData.defineId(EntityMyBat.class, EntityDataSerializers.BYTE);

	public EntityMyBat(Level world, MyPet myPet) {
		super(world, myPet);

	}

	/**
	 * Returns the sound that is played when the MyPet dies
	 */
	@Override
	protected String getMyPetDeathSound() {
		return "entity.bat.death";
	}

	/**
	 * Returns the sound that is played when the MyPet get hurt
	 */
	@Override
	protected String getHurtSound() {

		return "entity.bat.hurt";
	}

	@Override
	protected String getLivingSound() {
		return "entity.bat.ambient";
	}

	@Override
	public float getSoundSpeed() {
		return super.getSoundSpeed() * 0.95F;
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		getEntityData().define(HANGING_WATCHER, (byte) 0xFFFFFFFE); // hanging
	}

	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();
		if (Configuration.MyPet.Bat.CAN_GLIDE) {
			if (!this.onGround && this.getDeltaMovement().y() < 0.0D) {
				this.setDeltaMovement(getDeltaMovement().multiply(1, 0.6D, 1));
			}
		}
	}

	/**
	 * -> disable falldamage
	 */
	@Override
	public int calculateFallDamage(float f, float f1) {
		if (!Configuration.MyPet.Bat.CAN_GLIDE) {
			super.calculateFallDamage(f, f1);
		}
		return 0;
	}
}
