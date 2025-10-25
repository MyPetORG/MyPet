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
import de.Keyle.MyPet.api.entity.types.MyOcelot;
import de.Keyle.MyPet.compat.v1_16_R3.entity.EntityMyPet;
import net.minecraft.server.v1_16_R3.*;

@EntitySize(width = 0.6F, height = 0.8F)
public class EntityMyOcelot extends EntityMyPet {

	private static final DataWatcherObject<Boolean> AGE_WATCHER = DataWatcher.a(EntityMyOcelot.class, DataWatcherRegistry.i);
	private static final DataWatcherObject<Boolean> TRUSTING_WATCHER = DataWatcher.a(EntityMyOcelot.class, DataWatcherRegistry.i);

	public EntityMyOcelot(World world, MyPet myPet) {
		super(world, myPet);
	}

	@Override
	protected String getDeathSound() {
		return "entity.ocelot.death";
	}

	@Override
	protected String getHurtSound() {
		return "entity.ocelot.hurt";
	}

	@Override
	protected String getLivingSound() {
		return "entity.ocelot.ambient";
	}

	@Override
	public EnumInteractionResult handlePlayerInteraction(EntityHuman entityhuman, EnumHand enumhand, ItemStack itemStack) {
		if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).a()) {
			return EnumInteractionResult.CONSUME;
		}

		if (getOwner().equals(entityhuman)) {
			if (itemStack != null && canUseItem() && getOwner().getPlayer().isSneaking()) {
				if (Configuration.MyPet.Ocelot.GROW_UP_ITEM.compare(itemStack) && canUseItem() && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
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
		}
		return EnumInteractionResult.PASS;
	}

	@Override
	protected void initDatawatcher() {
		super.initDatawatcher();
		getDataWatcher().register(AGE_WATCHER, false);
		getDataWatcher().register(TRUSTING_WATCHER, false);
	}

	@Override
	public void updateVisuals() {
		getDataWatcher().set(AGE_WATCHER, getMyPet().isBaby());
	}

	@Override
	public MyOcelot getMyPet() {
		return (MyOcelot) myPet;
	}
}