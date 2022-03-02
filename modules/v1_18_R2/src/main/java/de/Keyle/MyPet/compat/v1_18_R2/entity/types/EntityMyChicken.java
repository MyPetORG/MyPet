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

package de.Keyle.MyPet.compat.v1_18_R2.entity.types;

import java.util.UUID;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyChicken;
import de.Keyle.MyPet.compat.v1_18_R2.entity.EntityMyPet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

@EntitySize(width = 0.4F, height = 0.7F)
public class EntityMyChicken extends EntityMyPet {

	private static final EntityDataAccessor<Boolean> AGE_WATCHER = SynchedEntityData.defineId(EntityMyChicken.class, EntityDataSerializers.BOOLEAN);

	private int nextEggTimer;

	public EntityMyChicken(Level world, MyPet myPet) {
		super(world, myPet);
		nextEggTimer = (this.random.nextInt(6000) + 6000);
	}

	@Override
	protected String getMyPetDeathSound() {
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
	public InteractionResult handlePlayerInteraction(Player entityhuman, InteractionHand enumhand, ItemStack itemStack) {
		if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).consumesAction()) {
			return InteractionResult.CONSUME;
		}

		if (getOwner().equals(entityhuman) && itemStack != null) {
			if (Configuration.MyPet.Chicken.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
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
	protected void defineSynchedData() {
		super.defineSynchedData();
		getEntityData().define(AGE_WATCHER, false);
	}

	@Override
	public void updateVisuals() {
		this.getEntityData().set(AGE_WATCHER, getMyPet().isBaby());
	}

	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();

		if (Configuration.MyPet.Chicken.CAN_GLIDE) {
			if (!this.onGround && this.getDeltaMovement().y() < 0.0D) {
				this.setDeltaMovement(getDeltaMovement().multiply(1, 0.6D, 1));
			}
		}

		if (Configuration.MyPet.Chicken.CAN_LAY_EGGS && canUseItem() && --nextEggTimer <= 0) {
			this.makeSound("entity.chicken.egg", 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
			spawnAtLocation(Items.EGG, 1);
			nextEggTimer = this.random.nextInt(6000) + 6000;
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
	public int calculateFallDamage(float f, float f1) {
		if (!Configuration.MyPet.Chicken.CAN_GLIDE) {
			super.calculateFallDamage(f, f1);
		}
		return 0;
	}
}
