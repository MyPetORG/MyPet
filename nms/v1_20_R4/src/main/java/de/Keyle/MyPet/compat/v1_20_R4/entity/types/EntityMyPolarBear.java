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

package de.Keyle.MyPet.compat.v1_20_R4.entity.types;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyPolarBear;
import de.Keyle.MyPet.compat.v1_20_R4.entity.EntityMyPet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@EntitySize(width = 1.3F, height = 1.4F)
public class EntityMyPolarBear extends EntityMyPet {

	private static final EntityDataAccessor<Boolean> AGE_WATCHER = SynchedEntityData.defineId(EntityMyPolarBear.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> REAR_WATCHER = SynchedEntityData.defineId(EntityMyPolarBear.class, EntityDataSerializers.BOOLEAN);

	int rearCounter = -1;

	public EntityMyPolarBear(Level world, MyPet myPet) {
		super(world, myPet);
	}

	@Override
	protected String getMyPetDeathSound() {
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
	public InteractionResult handlePlayerInteraction(Player entityhuman, InteractionHand enumhand, ItemStack itemStack) {
		if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).consumesAction()) {
			return InteractionResult.CONSUME;
		}

		if (getOwner().equals(entityhuman) && itemStack != null && canUseItem()) {
			if (Configuration.MyPet.PolarBear.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
				if (itemStack != ItemStack.EMPTY && !entityhuman.getAbilities().instabuild) {
					itemStack.shrink(1);
					if (itemStack.getCount() <= 0) {
						entityhuman.getInventory().setItem(entityhuman.getInventory().selected, ItemStack.EMPTY);
					}
				}
				getMyPet().setBaby(false);
				return InteractionResult.CONSUME;
			}
		}
		return InteractionResult.PASS;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(AGE_WATCHER, false);
		builder.define(REAR_WATCHER, false);

	}

	@Override
	public boolean attack(Entity entity) {
		boolean flag = false;
		try {
			flag = super.attack(entity);
			if (flag) {
				this.getEntityData().set(REAR_WATCHER, true);
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
			this.getEntityData().set(REAR_WATCHER, false);
			rearCounter = -1;
		}
	}

	@Override
	public void updateVisuals() {
		this.getEntityData().set(AGE_WATCHER, getMyPet().isBaby());
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
