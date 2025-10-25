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

package de.Keyle.MyPet.compat.v1_16_R3.entity.types;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyWither;
import de.Keyle.MyPet.compat.v1_16_R3.entity.EntityMyPet;
import net.minecraft.server.v1_16_R3.DataWatcher;
import net.minecraft.server.v1_16_R3.DataWatcherObject;
import net.minecraft.server.v1_16_R3.DataWatcherRegistry;
import net.minecraft.server.v1_16_R3.World;

@EntitySize(width = 1.9F, height = 3.5F)
public class EntityMyWither extends EntityMyPet {

	private static final DataWatcherObject<Integer> TARGET_WATCHER = DataWatcher.a(EntityMyWither.class, DataWatcherRegistry.b);
	private static final DataWatcherObject<Integer> UNUSED_WATCHER_1 = DataWatcher.a(EntityMyWither.class, DataWatcherRegistry.b);
	private static final DataWatcherObject<Integer> UNUSED_WATCHER_2 = DataWatcher.a(EntityMyWither.class, DataWatcherRegistry.b);
	private static final DataWatcherObject<Integer> INVULNERABILITY_WATCHER = DataWatcher.a(EntityMyWither.class, DataWatcherRegistry.b);

	public EntityMyWither(World world, MyPet myPet) {
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
	protected void initDatawatcher() {
		super.initDatawatcher();
		getDataWatcher().register(TARGET_WATCHER, 0);
		getDataWatcher().register(UNUSED_WATCHER_1, 0);
		getDataWatcher().register(UNUSED_WATCHER_2, 0);
		getDataWatcher().register(INVULNERABILITY_WATCHER, 0);
	}

	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();
		if (Configuration.MyPet.Wither.CAN_GLIDE) {
			if (!this.onGround && this.getMot().y < 0.0D) {
				this.setMot(getMot().d(1, 0.6D, 1));
			}
		}
	}

	@Override
	public void updateVisuals() {
		getDataWatcher().set(INVULNERABILITY_WATCHER, getMyPet().isBaby() ? 600 : 0);
	}

	/**
	 * -> disable falldamage
	 */
	@Override
	public int e(float f, float f1) {
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