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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.compat.ParticleCompat;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyTropicalFish;
import de.Keyle.MyPet.compat.v1_17_R1.entity.EntityMyPet;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.world.level.World;

@EntitySize(width = 0.5F, height = 0.4f)
public class EntityMyTropicalFish extends EntityMyPet {

	private static final DataWatcherObject<Boolean> FROM_BUCKET_WATCHER = DataWatcher.a(EntityMyTropicalFish.class, DataWatcherRegistry.i);
	private static final DataWatcherObject<Integer> VARIANT_WATCHER = DataWatcher.a(EntityMyTropicalFish.class, DataWatcherRegistry.b);

	public EntityMyTropicalFish(World world, MyPet myPet) {
		super(world, myPet);
	}

	@Override
	protected String getDeathSound() {
		return "entity.tropical_fish.death";
	}

	@Override
	protected String getHurtSound() {
		return "entity.tropical_fish.hurt";
	}

	@Override
	protected String getLivingSound() {
		return "entity.tropical_fish.ambient";
	}

	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();
		if (!isInWater() && this.Q.nextBoolean()) {
			MyPetApi.getPlatformHelper().playParticleEffect(myPet.getLocation().get().add(0, 0.7, 0), ParticleCompat.WATER_SPLASH.get(), 0.2F, 0.2F, 0.2F, 0.5F, 10, 20);
		}
	}

	@Override
	public MyTropicalFish getMyPet() {
		return (MyTropicalFish) myPet;
	}

	@Override
	public void updateVisuals() {
		getDataWatcher().set(VARIANT_WATCHER, getMyPet().getVariant());
	}

	@Override
	protected void initDatawatcher() {
		super.initDatawatcher();
		getDataWatcher().register(FROM_BUCKET_WATCHER, false);
		getDataWatcher().register(VARIANT_WATCHER, 0);
	}
}
