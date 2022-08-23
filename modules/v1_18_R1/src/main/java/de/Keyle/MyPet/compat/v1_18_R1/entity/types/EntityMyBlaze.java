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

import java.lang.reflect.InvocationTargetException;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyBlaze;
import de.Keyle.MyPet.compat.v1_18_R1.CompatManager;
import de.Keyle.MyPet.compat.v1_18_R1.entity.EntityMyPet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

@EntitySize(width = 0.6F, height = 1.7F)
public class EntityMyBlaze extends EntityMyPet {

	private static final EntityDataAccessor<Byte> BURNING_WATCHER = SynchedEntityData.defineId(EntityMyBlaze.class, EntityDataSerializers.BYTE);

	public EntityMyBlaze(Level world, MyPet myPet) {
		super(world, myPet);
	}

	@Override
	protected String getMyPetDeathSound() {
		return "entity.blaze.death";
	}

	@Override
	protected String getHurtSound() {
		return "entity.blaze.hurt";
	}

	@Override
	protected String getLivingSound() {
		return "entity.blaze.ambient";
	}

	@Override
	public InteractionResult handlePlayerInteraction(Player entityhuman, InteractionHand enumhand, ItemStack itemStack) {
		if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack) == InteractionResult.CONSUME) {
			return InteractionResult.CONSUME;
		}

		if (getOwner().equals(entityhuman) && itemStack != null && canUseItem()) {
			if (getMyPet().isOnFire() && itemStack.getItem() == Items.WATER_BUCKET && getOwner().getPlayer().isSneaking()) {
				getMyPet().setOnFire(false);
				makeSound("block.fire.extinguish", 1.0F, 1.0F);
				if (itemStack != ItemStack.EMPTY && !entityhuman.getAbilities().flying) {
					itemStack.shrink(1);
					if (itemStack.getCount() <= 0) {
						entityhuman.getInventory().setItem(entityhuman.getInventory().selected, new ItemStack(Items.BUCKET));
					} else {
						if (!entityhuman.getInventory().add(new ItemStack(Items.BUCKET))) {
							entityhuman.drop(new ItemStack(Items.BUCKET), true);
						}
					}
				}
				return InteractionResult.CONSUME;
			} else if (!getMyPet().isOnFire() && itemStack.getItem() == Items.FLINT_AND_STEEL && getOwner().getPlayer().isSneaking()) {
				getMyPet().setOnFire(true);
				makeSound("item.flintandsteel.use", 1.0F, 1.0F);
				if (itemStack != ItemStack.EMPTY && !entityhuman.getAbilities().instabuild) {
					try {
						itemStack.hurtAndBreak(1, entityhuman, (entityhuman1) -> entityhuman1.broadcastBreakEvent(enumhand));
					} catch (Error e) {
						// TODO REMOVE
						itemStack.hurtAndBreak(1, entityhuman, (entityhuman1) -> {
							try {
								CompatManager.ENTITY_LIVING_broadcastItemBreak.invoke(entityhuman1, enumhand);
							} catch (IllegalAccessException | InvocationTargetException ex) {
								ex.printStackTrace();
							}
						});
					}
				}
				return InteractionResult.CONSUME;
			}
		}
		return InteractionResult.PASS;
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		getEntityData().define(BURNING_WATCHER, (byte) 0);
	}

	@Override
	public void updateVisuals() {
		getEntityData().set(BURNING_WATCHER, (byte) (getMyPet().isOnFire() ? 1 : 0));
	}

	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();
		if (Configuration.MyPet.Blaze.CAN_GLIDE) {
			if (!this.onGround && this.getDeltaMovement().y() < 0.0D) {
				this.setDeltaMovement(getDeltaMovement().multiply(1, 0.6D, 1));
			}
		}
	}

	@Override
	public MyBlaze getMyPet() {
		return (MyBlaze) myPet;
	}

	/**
	 * -> disable falldamage
	 */
	@Override
	public int calculateFallDamage(float f, float f1) {
		if (!Configuration.MyPet.Blaze.CAN_GLIDE) {
			super.calculateFallDamage(f, f1);
		}
		return 0;
	}

	@Override
	protected boolean checkInteractCooldown() {
		boolean val = super.checkInteractCooldown();
		this.interactCooldown = 5;
		return val;
	}
}
