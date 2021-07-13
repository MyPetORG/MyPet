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
import de.Keyle.MyPet.api.entity.types.MyPolarBear;
import de.Keyle.MyPet.compat.v1_17_R1.entity.EntityMyPet;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;

@EntitySize(width = 1.3F, height = 1.4F)
public class EntityMyPolarBear extends EntityMyPet {

	private static final DataWatcherObject<Boolean> AGE_WATCHER = DataWatcher.a(EntityMyPolarBear.class, DataWatcherRegistry.i);
	private static final DataWatcherObject<Boolean> REAR_WATCHER = DataWatcher.a(EntityMyPolarBear.class, DataWatcherRegistry.i);

	int rearCounter = -1;

	public EntityMyPolarBear(World world, MyPet myPet) {
		super(world, myPet);
	}

	@Override
	protected String getDeathSound() {
		return "entity.polar_bear.death";
	}

	@Override
	protected String getHurtSound() {
		return "entity.polar_bear.hurt";
	}

	@Override
	protected String getLivingSound() {
		return "entity.polar_bear.ambient" + (getMyPet().isBaby() ? "_baby" : "");
	}

	@Override
	public EnumInteractionResult handlePlayerInteraction(EntityHuman entityhuman, EnumHand enumhand, ItemStack itemStack) {
		if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).a()) {
			return EnumInteractionResult.b;
		}

		if (getOwner().equals(entityhuman) && itemStack != null && canUseItem()) {
			if (Configuration.MyPet.Cow.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
				if (itemStack != ItemStack.b && !entityhuman.getAbilities().d) {
					itemStack.subtract(1);
					if (itemStack.getCount() <= 0) {
						entityhuman.getInventory().setItem(entityhuman.getInventory().k, ItemStack.b);
					}
				}
				getMyPet().setBaby(false);
				return EnumInteractionResult.b;
			}
		}
		return EnumInteractionResult.d;
	}

	@Override
	protected void initDatawatcher() {
		super.initDatawatcher();
		getDataWatcher().register(AGE_WATCHER, false);
		getDataWatcher().register(REAR_WATCHER, false);

	}

	@Override
	public boolean attack(Entity entity) {
		boolean flag = false;
		try {
			flag = super.attack(entity);
			if (flag) {
				getDataWatcher().set(REAR_WATCHER, true);
				rearCounter = 10;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return flag;
	}

	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();
		if (rearCounter > -1 && rearCounter-- == 0) {
			getDataWatcher().set(REAR_WATCHER, false);
			rearCounter = -1;
		}
	}

	@Override
	public void updateVisuals() {
		getDataWatcher().set(AGE_WATCHER, getMyPet().isBaby());
	}

	@Override
	public void playPetStepSound() {
		makeSound("entity.polar_bear.step", 0.15F, 1.0F);
	}

	@Override
	public MyPolarBear getMyPet() {
		return (MyPolarBear) myPet;
	}
}
