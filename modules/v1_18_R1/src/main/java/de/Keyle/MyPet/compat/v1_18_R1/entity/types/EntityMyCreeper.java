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

package de.Keyle.MyPet.compat.v1_18_R1.entity.types;

import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyCreeper;
import de.Keyle.MyPet.compat.v1_18_R1.entity.EntityMyPet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.Level;

@EntitySize(width = 0.6F, height = 1.9F)
public class EntityMyCreeper extends EntityMyPet {

	private static final EntityDataAccessor<Integer> FUSE_WATCHER = SynchedEntityData.defineId(EntityMyCreeper.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> POWERED_WATCHER = SynchedEntityData.defineId(EntityMyCreeper.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> UNUSED_WATCHER = SynchedEntityData.defineId(EntityMyCreeper.class, EntityDataSerializers.BOOLEAN);

	public EntityMyCreeper(Level world, MyPet myPet) {
		super(world, myPet);
	}

	@Override
	protected String getMyPetDeathSound() {
		return "entity.creeper.death";
	}

	@Override
	protected String getHurtSound() {
		return "entity.creeper.hurt";
	}

	@Override
	protected String getLivingSound() {
		return null;
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		getEntityData().define(FUSE_WATCHER, -1);
		getEntityData().define(POWERED_WATCHER, false);
		getEntityData().define(UNUSED_WATCHER, false);
	}

	@Override
	public void updateVisuals() {
		getEntityData().set(POWERED_WATCHER, getMyPet().isPowered());
	}

	@Override
	public MyCreeper getMyPet() {
		return (MyCreeper) myPet;
	}
}
