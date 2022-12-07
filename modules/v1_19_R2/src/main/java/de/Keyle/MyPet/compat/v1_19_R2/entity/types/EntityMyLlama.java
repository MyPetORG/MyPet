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

package de.Keyle.MyPet.compat.v1_19_R2.entity.types;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyLlama;
import de.Keyle.MyPet.compat.v1_19_R2.CompatManager;
import de.Keyle.MyPet.compat.v1_19_R2.entity.EntityMyPet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WoolCarpetBlock;
import org.bukkit.craftbukkit.v1_19_R2.inventory.CraftItemStack;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.UUID;

@EntitySize(width = 0.9F, height = 1.87F)
public class EntityMyLlama extends EntityMyPet {

	private static final EntityDataAccessor<Boolean> AGE_WATCHER = SynchedEntityData.defineId(EntityMyLlama.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Byte> SADDLE_CHEST_WATCHER = SynchedEntityData.defineId(EntityMyLlama.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Optional<UUID>> OWNER_WATCHER = SynchedEntityData.defineId(EntityMyLlama.class, EntityDataSerializers.OPTIONAL_UUID);
	private static final EntityDataAccessor<Boolean> CHEST_WATCHER = SynchedEntityData.defineId(EntityMyLlama.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> STRENGTH_WATCHER = SynchedEntityData.defineId(EntityMyLlama.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> COLOR_WATCHER = SynchedEntityData.defineId(EntityMyLlama.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> VARIANT_WATCHER = SynchedEntityData.defineId(EntityMyLlama.class, EntityDataSerializers.INT);

	public EntityMyLlama(Level world, MyPet myPet) {
		super(world, myPet);
	}

	@Override
	protected String getMyPetDeathSound() {
		return "entity.llama.death";
	}

	@Override
	protected String getHurtSound() {
		return "entity.llama.hurt";
	}

	@Override
	protected String getLivingSound() {
		return "entity.llama.ambient";
	}

	@Override
	public InteractionResult handlePlayerInteraction(Player entityhuman, InteractionHand enumhand, ItemStack itemStack) {
		if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).consumesAction()) {
			return InteractionResult.CONSUME;
		}

		if (itemStack != null && canUseItem()) {
			if (itemStack.is(ItemTags.WOOL_CARPETS) && !getMyPet().hasDecor() && getOwner().getPlayer().isSneaking() && canEquip()) {
				getMyPet().setDecor(CraftItemStack.asBukkitCopy(itemStack));
				if (itemStack != ItemStack.EMPTY && !entityhuman.getAbilities().instabuild) {
					itemStack.shrink(1);
					if (itemStack.getCount() <= 0) {
						entityhuman.getInventory().setItem(entityhuman.getInventory().selected, ItemStack.EMPTY);
					}
				}
				return InteractionResult.CONSUME;
			} else if (itemStack.getItem() == Blocks.CHEST.asItem() && getOwner().getPlayer().isSneaking() && !getMyPet().hasChest() && !getMyPet().isBaby() && canEquip()) {
				getMyPet().setChest(CraftItemStack.asBukkitCopy(itemStack));
				if (itemStack != ItemStack.EMPTY && !entityhuman.getAbilities().instabuild) {
					itemStack.shrink(1);
					if (itemStack.getCount() <= 0) {
						entityhuman.getInventory().setItem(entityhuman.getInventory().selected, ItemStack.EMPTY);
					}
				}
				return InteractionResult.CONSUME;
			} else if (itemStack.getItem() == Items.SHEARS && getOwner().getPlayer().isSneaking() && canEquip()) {
				if (getMyPet().hasChest()) {
					ItemEntity entityitem = new ItemEntity(this.level, this.getX(), this.getY() + 1, this.getZ(), CraftItemStack.asNMSCopy(getMyPet().getChest()));
					entityitem.pickupDelay = 10;
					entityitem.setDeltaMovement(entityitem.getDeltaMovement().add(0, this.random.nextFloat() * 0.05F, 0));
					this.level.addFreshEntity(entityitem);
				}
				if (getMyPet().hasDecor()) {
					ItemEntity entityitem = new ItemEntity(this.level, this.getX(), this.getY() + 1, this.getZ(), CraftItemStack.asNMSCopy(getMyPet().getDecor()));
					entityitem.pickupDelay = 10;
					entityitem.setDeltaMovement(entityitem.getDeltaMovement().add(0, this.random.nextFloat() * 0.05F, 0));
					this.level.addFreshEntity(entityitem);
				}

				makeSound("entity.sheep.shear", 1.0F, 1.0F);
				getMyPet().setChest(null);
				getMyPet().setDecor(null);
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
			} else if (Configuration.MyPet.Llama.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
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
		getEntityData().define(SADDLE_CHEST_WATCHER, (byte) 0);    // saddle & chest
		getEntityData().define(OWNER_WATCHER, Optional.empty()); // owner
		getEntityData().define(CHEST_WATCHER, true);
		getEntityData().define(STRENGTH_WATCHER, 0);
		getEntityData().define(COLOR_WATCHER, 0);
		getEntityData().define(VARIANT_WATCHER, 0);
	}

	@Override
	public void updateVisuals() {
		this.getEntityData().set(CHEST_WATCHER, getMyPet().hasChest());
		this.getEntityData().set(AGE_WATCHER, getMyPet().isBaby());
		if (getMyPet().hasDecor()) {
			ItemStack is = CraftItemStack.asNMSCopy(getMyPet().getDecor());
			Block block = Block.byItem(is.getItem());
			int color = block instanceof WoolCarpetBlock ? ((WoolCarpetBlock) block).getColor().getId() : 0;
			this.getEntityData().set(COLOR_WATCHER, color);
		} else {
			this.getEntityData().set(COLOR_WATCHER, -1);
		}
		this.getEntityData().set(VARIANT_WATCHER, getMyPet().getVariant());
	}

	@Override
	public MyLlama getMyPet() {
		return (MyLlama) myPet;
	}
}
