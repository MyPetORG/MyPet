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

package de.Keyle.MyPet.compat.v1_19_R1_1.entity.types;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyPhantom;
import de.Keyle.MyPet.compat.v1_19_R1_1.entity.EntityMyPet;
import de.Keyle.MyPet.compat.v1_19_R1_1.entity.ai.attack.MeleeAttack;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;

@EntitySize(width = 0.51F, height = 0.51F)
public class EntityMyPhantom extends EntityMyPet {

	private static final EntityDataAccessor<Integer> SIZE_WATCHER = SynchedEntityData.defineId(EntityMyPhantom.class, EntityDataSerializers.INT);

	public EntityMyPhantom(Level world, MyPet myPet) {
		super(world, myPet);
	}

	@Override
	protected String getMyPetDeathSound() {
		return "entity.phantom.death";
	}

	@Override
	protected String getHurtSound() {
		return "entity.phantom.hurt";
	}

	@Override
	protected String getLivingSound() {
		return "entity.phantom.ambient";
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();

		getEntityData().define(SIZE_WATCHER, 0);
	}

	@Override
	public void updateVisuals() {
		int size = Math.max(1, getMyPet().getSize());
		getEntityData().set(SIZE_WATCHER, size);
		this.refreshDimensions();
		if (petPathfinderSelector != null && petPathfinderSelector.hasGoal("MeleeAttack")) {
			petPathfinderSelector.replaceGoal("MeleeAttack", new MeleeAttack(this, 0.1F, 3 + (getMyPet().getSize() * 0.2), 20));
		}
	}

	@Override
	public net.minecraft.world.entity.EntityDimensions getDimensions(Pose entitypose) {
		EntitySize es = this.getClass().getAnnotation(EntitySize.class);
		if (es != null) {
			int size = Math.max(1, getMyPet().getSize());
			float width = es.width();
			float height = Float.isNaN(es.height()) ? width : es.height();
			return new net.minecraft.world.entity.EntityDimensions(width * size, height * size, false);
		}
		return super.getDimensions(entitypose);
	}

	@Override
	public MyPhantom getMyPet() {
		return (MyPhantom) myPet;
	}

	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();

		if (Configuration.MyPet.Phantom.CAN_GLIDE) {
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
		if (!Configuration.MyPet.Phantom.CAN_GLIDE) {
			super.calculateFallDamage(f, f1);
		}
		return 0;
	}
}
