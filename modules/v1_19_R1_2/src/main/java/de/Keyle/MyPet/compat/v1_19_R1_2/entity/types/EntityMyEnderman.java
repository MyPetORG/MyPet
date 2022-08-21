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

package de.Keyle.MyPet.compat.v1_19_R1_2.entity.types;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_19_R1.util.CraftMagicNumbers;

import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyEnderman;
import de.Keyle.MyPet.api.skill.skills.Behavior;
import de.Keyle.MyPet.compat.v1_19_R1_2.CompatManager;
import de.Keyle.MyPet.compat.v1_19_R1_2.entity.EntityMyPet;
import de.Keyle.MyPet.skill.skills.BehaviorImpl;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

@EntitySize(width = 0.6F, height = 2.55F)
public class EntityMyEnderman extends EntityMyPet {

	private static final EntityDataAccessor<Optional<BlockState>> BLOCK_WATCHER = SynchedEntityData.defineId(EntityMyEnderman.class, EntityDataSerializers.BLOCK_STATE);
	private static final EntityDataAccessor<Boolean> SCREAMING_WATCHER = SynchedEntityData.defineId(EntityMyEnderman.class, EntityDataSerializers.BOOLEAN);

	public EntityMyEnderman(Level world, MyPet myPet) {
		super(world, myPet);
	}

	@Override
	protected String getMyPetDeathSound() {
		return "entity.enderman.death";
	}

	@Override
	protected String getHurtSound() {
		return "entity.enderman.hurt";
	}

	@Override
	protected String getLivingSound() {
		return getMyPet().isScreaming() ? "entity.enderman.scream" : "entity.enderman.ambient";
	}

	@Override
	public InteractionResult handlePlayerInteraction(Player entityhuman, InteractionHand enumhand, ItemStack itemStack) {
		if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).consumesAction()) {
			return InteractionResult.CONSUME;
		}

		if (getOwner().equals(entityhuman) && itemStack != null && canUseItem()) {
			if (itemStack.getItem() == Items.SHEARS && getMyPet().hasBlock() && getOwner().getPlayer().isSneaking()) {
				ItemEntity entityitem = new ItemEntity(this.level, this.getX(), this.getY() + 1, this.getZ(), CraftItemStack.asNMSCopy(getMyPet().getBlock()));
				entityitem.pickupDelay = 10;
				entityitem.setDeltaMovement(entityitem.getDeltaMovement().add(0, this.random.nextFloat() * 0.05F, 0));

				makeSound("entity.sheep.shear", 1.0F, 1.0F);
				getMyPet().setBlock(null);
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
			} else if (getMyPet().getBlock() == null && Util.isBetween(1, 255, Item.getId(itemStack.getItem())) && getOwner().getPlayer().isSneaking()) {
				getMyPet().setBlock(CraftItemStack.asBukkitCopy(itemStack));
				if (itemStack != ItemStack.EMPTY && !entityhuman.getAbilities().instabuild) {
					itemStack.shrink(1);
					if (itemStack.getCount() <= 0) {
						entityhuman.getInventory().setItem(entityhuman.getInventory().selected, ItemStack.EMPTY);
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
		getEntityData().define(BLOCK_WATCHER, Optional.empty());
		getEntityData().define(SCREAMING_WATCHER, false);
	}

	@Override
	public void updateVisuals() {
		Optional<BlockState> block;
		if (getMyPet().getBlock() != null) {
			BlockState data = CraftMagicNumbers.getBlock(getMyPet().getBlock().getData());
			block = Optional.ofNullable(data);
		} else {
			block = Optional.empty();
		}
		getEntityData().set(BLOCK_WATCHER, block);
		getEntityData().set(SCREAMING_WATCHER, getMyPet().isScreaming());
	}

	@Override
	protected void doMyPetTick() {
		super.doMyPetTick();
		BehaviorImpl skill = getMyPet().getSkills().get(BehaviorImpl.class);
		Behavior.BehaviorMode behavior = skill.getBehavior();
		if (behavior == Behavior.BehaviorMode.Aggressive) {
			if (!getMyPet().isScreaming()) {
				getMyPet().setScreaming(true);
			}
		} else {
			if (getMyPet().isScreaming()) {
				getMyPet().setScreaming(false);
			}
		}
	}

	@Override
	public MyEnderman getMyPet() {
		return (MyEnderman) myPet;
	}
}
