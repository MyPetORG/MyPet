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

import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.compat.v1_19_R2.entity.EntityMyAquaticPet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.Level;

@EntitySize(width = 0.5F, height = 0.3f)
public class EntityMyCod extends EntityMyAquaticPet {

	private static final EntityDataAccessor<Boolean> FROM_BUCKET_WATCHER = SynchedEntityData.defineId(EntityMyCod.class, EntityDataSerializers.BOOLEAN);

	public EntityMyCod(Level world, MyPet myPet) {
		super(world, myPet);
	}

	@Override
	protected String getMyPetDeathSound() {
		return "entity.cod.death";
	}

	@Override
	protected String getHurtSound() {
		return "entity.cod.flop";
	}

	@Override
	protected String getLivingSound() {
		return "entity.cod.ambient";
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		getEntityData().define(FROM_BUCKET_WATCHER, false);
	}
}
