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
import de.Keyle.MyPet.api.entity.types.MyTurtle;
import de.Keyle.MyPet.compat.v1_16_R3.entity.EntityMyPet;
import net.minecraft.server.v1_16_R3.*;

@EntitySize(width = 1.2F, height = 0.4F)
public class EntityMyTurtle extends EntityMyPet {

	private static final DataWatcherObject<Boolean> AGE_WATCHER = DataWatcher.a(EntityMyTurtle.class, DataWatcherRegistry.i);
	private static final DataWatcherObject<BlockPosition> HOME_WATCHER = DataWatcher.a(EntityMyTurtle.class, DataWatcherRegistry.l);
	private static final DataWatcherObject<Boolean> HAS_EGG_WATCHER = DataWatcher.a(EntityMyTurtle.class, DataWatcherRegistry.i);
	private static final DataWatcherObject<Boolean> UNUSED_WATCHER_1 = DataWatcher.a(EntityMyTurtle.class, DataWatcherRegistry.i);
	private static final DataWatcherObject<BlockPosition> TRAVEL_POS_WATCHER = DataWatcher.a(EntityMyTurtle.class, DataWatcherRegistry.l);
	private static final DataWatcherObject<Boolean> UNUSED_WATCHER_2 = DataWatcher.a(EntityMyTurtle.class, DataWatcherRegistry.i);
	private static final DataWatcherObject<Boolean> UNUSED_WATCHER_3 = DataWatcher.a(EntityMyTurtle.class, DataWatcherRegistry.i);

	public EntityMyTurtle(World world, MyPet myPet) {
		super(world, myPet);
	}

	@Override
	protected String getDeathSound() {
		return "entity.turtle.death" + (isBaby() ? "_baby" : "");
	}

	@Override
	protected String getHurtSound() {
		return "entity.turtle.hurt" + (isBaby() ? "_baby" : "");
	}

	@Override
	protected String getLivingSound() {
		return "entity.turtle.ambient_land";
	}

	@Override
	public EnumInteractionResult handlePlayerInteraction(EntityHuman entityhuman, EnumHand enumhand, ItemStack itemStack) {
		if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).a()) {
			return EnumInteractionResult.CONSUME;
		}

		if (getOwner().equals(entityhuman) && itemStack != null && canUseItem()) {
			if (Configuration.MyPet.Turtle.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
				if (itemStack != ItemStack.b && !entityhuman.abilities.canInstantlyBuild) {
					itemStack.subtract(1);
					if (itemStack.getCount() <= 0) {
						entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, ItemStack.b);
					}
				}
				getMyPet().setBaby(false);
				return EnumInteractionResult.CONSUME;
			}
		}
		return EnumInteractionResult.PASS;
	}

	@Override
	protected void initDatawatcher() {
		super.initDatawatcher();
		getDataWatcher().register(AGE_WATCHER, false);
		getDataWatcher().register(HOME_WATCHER, BlockPosition.ZERO);
		getDataWatcher().register(HAS_EGG_WATCHER, false);
		getDataWatcher().register(TRAVEL_POS_WATCHER, BlockPosition.ZERO);
		getDataWatcher().register(UNUSED_WATCHER_2, false);
		getDataWatcher().register(UNUSED_WATCHER_3, false);
		getDataWatcher().register(UNUSED_WATCHER_1, false);
	}

	@Override
	public void updateVisuals() {
		getDataWatcher().set(AGE_WATCHER, getMyPet().isBaby());
	}

	@Override
	public MyTurtle getMyPet() {
		return (MyTurtle) myPet;
	}
}