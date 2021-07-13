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
import de.Keyle.MyPet.api.entity.types.MyChicken;
import de.Keyle.MyPet.compat.v1_17_R1.entity.EntityMyPet;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;

@EntitySize(width = 0.4F, height = 0.7F)
public class EntityMyChicken extends EntityMyPet {

	private static final DataWatcherObject<Boolean> AGE_WATCHER = DataWatcher.a(EntityMyChicken.class, DataWatcherRegistry.i);

	private int nextEggTimer;

	public EntityMyChicken(World world, MyPet myPet) {
		super(world, myPet);
		nextEggTimer = (this.Q.nextInt(6000) + 6000);
	}

	@Override
	protected String getDeathSound() {
		return "entity.chicken.death";
	}

	@Override
	protected String getHurtSound() {
		return "entity.chicken.hurt";
	}

	@Override
	protected String getLivingSound() {
		return "entity.chicken.ambient";
	}

	@Override
	public EnumInteractionResult handlePlayerInteraction(EntityHuman entityhuman, EnumHand enumhand, ItemStack itemStack) {
		if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).a()) {
			return EnumInteractionResult.b;
		}

		if (getOwner().equals(entityhuman) && itemStack != null) {
			if (Configuration.MyPet.Chicken.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
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
	}

	@Override
	public void updateVisuals() {
		getDataWatcher().set(AGE_WATCHER, getMyPet().isBaby());
	}

	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();

		if (Configuration.MyPet.Chicken.CAN_GLIDE) {
			if (!this.z && this.getMot().getY() < 0.0D) {
				this.setMot(getMot().d(1, 0.6D, 1));
			}
		}

		if (Configuration.MyPet.Chicken.CAN_LAY_EGGS && canUseItem() && --nextEggTimer <= 0) {
			this.makeSound("entity.chicken.egg", 1.0F, (this.Q.nextFloat() - this.Q.nextFloat()) * 0.2F + 1.0F);
			a(Items.oo, 1);
			nextEggTimer = this.Q.nextInt(6000) + 6000;
		}
	}

	@Override
	public void playPetStepSound() {
		makeSound("entity.chicken.step", 0.15F, 1.0F);
	}

	@Override
	public MyChicken getMyPet() {
		return (MyChicken) myPet;
	}

	/**
	 * -> disable falldamage
	 */
	@Override
	public int d(float f, float f1) {
		if (!Configuration.MyPet.Chicken.CAN_GLIDE) {
			super.e(f, f1);
		}
		return 0;
	}
}
