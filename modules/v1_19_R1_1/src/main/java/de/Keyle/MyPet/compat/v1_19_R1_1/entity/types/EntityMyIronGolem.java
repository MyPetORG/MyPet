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

package de.Keyle.MyPet.compat.v1_19_R1_1.entity.types;

import java.lang.reflect.InvocationTargetException;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyIronGolem;
import de.Keyle.MyPet.compat.v1_19_R1_1.CompatManager;
import de.Keyle.MyPet.compat.v1_19_R1_1.entity.EntityMyPet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

@EntitySize(width = 1.4F, height = 2.7F)
public class EntityMyIronGolem extends EntityMyPet {

	protected static final EntityDataAccessor<Byte> UNUSED_WATCHER = SynchedEntityData.defineId(EntityMyIronGolem.class, EntityDataSerializers.BYTE);

	int flowerCounter = 0;
	boolean flower = false;

	public EntityMyIronGolem(Level world, MyPet myPet) {
		super(world, myPet);
	}

	@Override
	public boolean attack(Entity entity) {
		boolean flag = false;
		try {
			this.level.broadcastEntityEvent(this, (byte) 4);
			flag = super.attack(entity);
			if (Configuration.MyPet.IronGolem.CAN_TOSS_UP && flag) {
				entity.setDeltaMovement(entity.getDeltaMovement().add(0, 0.4000000059604645D, 0));
				this.makeSound("entity.iron_golem.attack", 1.0F, 1.0F);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return flag;
	}

	@Override
	protected String getMyPetDeathSound() {
		return "entity.iron_golem.death";
	}

	@Override
	protected String getHurtSound() {
		return "entity.iron_golem.hurt";
	}

	@Override
	protected String getLivingSound() {
		return null;
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		getEntityData().define(UNUSED_WATCHER, (byte) 0); // N/A
	}

	@Override
	public InteractionResult handlePlayerInteraction(Player entityhuman, InteractionHand enumhand, ItemStack itemStack) {
		if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).consumesAction()) {
			return InteractionResult.CONSUME;
		}

		if (itemStack.getItem() == Items.IRON_INGOT) {
			Bukkit.getScheduler().runTaskLater(MyPetApi.getPlugin(), () -> {
				if (getMyPet().getStatus() == MyPet.PetState.Here) {
					super.setHealth(this.getHealth() + 0.0001F);
				}
			}, 5L);
			Bukkit.getScheduler().runTaskLater(MyPetApi.getPlugin(), () -> {
				if (getMyPet().getStatus() == MyPet.PetState.Here) {
					super.setHealth(this.getHealth());
				}
			}, 10L);
		}
		if (getOwner().equals(entityhuman) && itemStack != null && canUseItem()) {
			if (itemStack.getItem() == Blocks.POPPY.asItem() && !getMyPet().hasFlower() && getOwner().getPlayer().isSneaking()) {
				getMyPet().setFlower(CraftItemStack.asBukkitCopy(itemStack));
				if (itemStack != ItemStack.EMPTY && !entityhuman.getAbilities().instabuild) {
					itemStack.shrink(1);
					if (itemStack.getCount() <= 0) {
						entityhuman.getInventory().setItem(entityhuman.getInventory().selected, ItemStack.EMPTY);
					}
				}
				return InteractionResult.CONSUME;
			} else if (itemStack.getItem() == Items.SHEARS && getMyPet().hasFlower() && getOwner().getPlayer().isSneaking()) {
				ItemEntity entityitem = new ItemEntity(this.level, this.getX(), this.getY() + 1, this.getZ(), CraftItemStack.asNMSCopy(getMyPet().getFlower()));
				entityitem.pickupDelay = 10;
				entityitem.setDeltaMovement(entityitem.getDeltaMovement().add(0, this.random.nextFloat() * 0.05F, 0));
				this.level.addFreshEntity(entityitem);

				makeSound("entity.sheep.shear", 1.0F, 1.0F);
				getMyPet().setFlower(null);
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
	public void updateVisuals() {
		flower = getMyPet().hasFlower();
		flowerCounter = 0;
	}

	@Override
	public MyIronGolem getMyPet() {
		return (MyIronGolem) myPet;
	}

	@Override
	public void playPetStepSound() {
		makeSound("entity.iron_golem.step", 1.0F, 1.0F);
	}

	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();
		if (this.flower && this.flowerCounter-- <= 0) {
			this.level.broadcastEntityEvent(this, (byte) 11);
			flowerCounter = 300;
		}
	}
}
