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

package de.Keyle.MyPet.compat.v1_17_R1.entity.types;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyWither;
import de.Keyle.MyPet.compat.v1_17_R1.entity.EntityMyPet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.Level;

@EntitySize(width = 1.9F, height = 3.5F)
public class EntityMyWither extends EntityMyPet {

	private static final EntityDataAccessor<Integer> TARGET_WATCHER = SynchedEntityData.defineId(EntityMyWither.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> UNUSED_WATCHER_1 = SynchedEntityData.defineId(EntityMyWither.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> UNUSED_WATCHER_2 = SynchedEntityData.defineId(EntityMyWither.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> INVULNERABILITY_WATCHER = SynchedEntityData.defineId(EntityMyWither.class, EntityDataSerializers.INT);

	public EntityMyWither(Level world, MyPet myPet) {
		super(world, myPet);
	}

	@Override
	protected String getDeathSound() {
		return "entity.wither.death";
	}

	@Override
	protected String getHurtSound() {
		return "entity.wither.hurt";
	}

	@Override
	protected String getLivingSound() {
		return "entity.wither.ambient";
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		getEntityData().define(TARGET_WATCHER, 0);
		getEntityData().define(UNUSED_WATCHER_1, 0);
		getEntityData().define(UNUSED_WATCHER_2, 0);
		getEntityData().define(INVULNERABILITY_WATCHER, 0);
	}

	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();
		if (Configuration.MyPet.Wither.CAN_GLIDE) {
			if (!this.onGround && this.getDeltaMovement().y() < 0.0D) {
				this.setDeltaMovement(getDeltaMovement().multiply(1, 0.6D, 1));
			}
		}
	}

	@Override
	public void updateVisuals() {
		getEntityData().set(INVULNERABILITY_WATCHER, getMyPet().isBaby() ? 600 : 0);
	}

	/**
	 * -> disable falldamage
	 */
	@Override
	public int calculateFallDamage(float f, float f1) {
		if (!Configuration.MyPet.Wither.CAN_GLIDE) {
			super.e(f, f1);
		}
		return 0;
	}

	@Override
	public MyWither getMyPet() {
		return (MyWither) myPet;
	}
}
