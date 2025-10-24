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

package de.Keyle.MyPet.compat.v1_20_R1.entity.types;

import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MySlime;
import de.Keyle.MyPet.compat.v1_20_R1.entity.EntityMyPet;
import de.Keyle.MyPet.compat.v1_20_R1.entity.ai.attack.MeleeAttack;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;

@EntitySize(width = 0.51F, height = 0.51F)
public class EntityMySlime extends EntityMyPet {

	private static final EntityDataAccessor<Integer> SIZE_WATCHER = SynchedEntityData.defineId(EntityMySlime.class, EntityDataSerializers.INT);

	int jumpDelay;

	public EntityMySlime(Level world, MyPet myPet) {
		super(world, myPet);
		this.jumpDelay = (this.random.nextInt(20) + 10);
	}

	@Override
	protected String getMyPetDeathSound() {
		return "entity.slime.death";

	}

	@Override
	protected String getHurtSound() {
		return "entity.slime.hurt";
	}

	@Override
	protected String getLivingSound() {
		return null;
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		getEntityData().define(SIZE_WATCHER, 1); //size
	}

	@Override
	public void updateVisuals() {
		int size = Math.max(1, getMyPet().getSize());
		getEntityData().set(SIZE_WATCHER, size);
		this.refreshDimensions();
		if (petPathfinderSelector != null && petPathfinderSelector.hasGoal("MeleeAttack")) {
			petPathfinderSelector.replaceGoal("MeleeAttack", new MeleeAttack(this, 0.1F, 3 + (getMyPet().getSize() * 0.51), 20));
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
	public void onLivingUpdate() {
		super.onLivingUpdate();

		if (this.onGround && jumpDelay-- <= 0) {
			getJumpControl().jump();
			jumpDelay = (this.random.nextInt(20) + 50);
			makeSound("entity.slime.jump", 1.0F, ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F) / 0.8F);
		}
	}

	@Override
	public MySlime getMyPet() {
		return (MySlime) myPet;
	}

	@Override
	public void setPathfinder() {
		super.setPathfinder();
		petPathfinderSelector.replaceGoal("MeleeAttack", new MeleeAttack(this, 0.1F, 3 + (getMyPet().getSize() * 0.51), 20));
	}
}
