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
import de.Keyle.MyPet.api.entity.types.MyTraderLlama;
import de.Keyle.MyPet.compat.v1_17_R1.entity.EntityMyPet;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.tags.TagsItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockCarpet;
import net.minecraft.world.level.block.Blocks;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.UUID;

import static de.Keyle.MyPet.compat.v1_17_R1.CompatManager.ENTITY_LIVING_broadcastItemBreak;

@EntitySize(width = 0.9F, height = 1.87F)
public class EntityMyTraderLlama extends EntityMyPet {

	private static final EntityDataAccessor<Boolean> AGE_WATCHER = SynchedEntityData.defineId(EntityMyTraderLlama.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Byte> SADDLE_CHEST_WATCHER = SynchedEntityData.defineId(EntityMyTraderLlama.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Optional<UUID>> OWNER_WATCHER = SynchedEntityData.defineId(EntityMyTraderLlama.class, EntityDataSerializers.o);
	private static final EntityDataAccessor<Boolean> CHEST_WATCHER = SynchedEntityData.defineId(EntityMyTraderLlama.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> STRENGTH_WATCHER = SynchedEntityData.defineId(EntityMyTraderLlama.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> COLOR_WATCHER = SynchedEntityData.defineId(EntityMyTraderLlama.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> VARIANT_WATCHER = SynchedEntityData.defineId(EntityMyTraderLlama.class, EntityDataSerializers.INT);

	public EntityMyTraderLlama(Level world, MyPet myPet) {
		super(world, myPet);
	}

	@Override
	protected String getDeathSound() {
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
	public InteractionResult handlePlayerInteraction(EntityHuman entityhuman, InteractionHand enumhand, ItemStack itemStack) {
		if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).a()) {
			return InteractionResult.CONSUME;
		}

		if (itemStack != null && canUseItem()) {
			if (TagsItem.g.isTagged(itemStack.getItem()) && !getMyPet().hasDecor() && getOwner().getPlayer().isSneaking() && canEquip()) {
				getMyPet().setDecor(CraftItemStack.asBukkitCopy(itemStack));
				if (itemStack != ItemStack.EMPTY && !entityhuman.getAbilities().instabuild) {
					itemStack.shrink(1);
					if (itemStack.getCount() <= 0) {
						entityhuman.getInventory().setItem(entityhuman.getInventory().selected, ItemStack.EMPTY);
					}
				}
				return InteractionResult.CONSUME;
			} else if (itemStack.getItem() == Blocks.bX.getItem() && getOwner().getPlayer().isSneaking() && !getMyPet().hasChest() && !getMyPet().isBaby() && canEquip()) {
				getMyPet().setChest(CraftItemStack.asBukkitCopy(itemStack));
				if (itemStack != ItemStack.EMPTY && !entityhuman.getAbilities().instabuild) {
					itemStack.shrink(1);
					if (itemStack.getCount() <= 0) {
						entityhuman.getInventory().setItem(entityhuman.getInventory().selected, ItemStack.EMPTY);
					}
				}
				return InteractionResult.CONSUME;
			} else if (itemStack.getItem() == Items.pq && getOwner().getPlayer().isSneaking() && canEquip()) {
				if (getMyPet().hasChest()) {
					EntityItem entityitem = new EntityItem(this.t, this.locX(), this.locY() + 1, this.locZ(), CraftItemStack.asNMSCopy(getMyPet().getChest()));
					entityitem.ap = 10;
					entityitem.setMot(entityitem.getMot().add(0, this.Q.nextFloat() * 0.05F, 0));
					this.t.addEntity(entityitem);
				}
				if (getMyPet().hasDecor()) {
					EntityItem entityitem = new EntityItem(this.t, this.locX(), this.locY() + 1, this.locZ(), CraftItemStack.asNMSCopy(getMyPet().getDecor()));
					entityitem.ap = 10;
					entityitem.setMot(entityitem.getMot().add(0, this.Q.nextFloat() * 0.05F, 0));
					this.t.addEntity(entityitem);
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
								ENTITY_LIVING_broadcastItemBreak.invoke(entityhuman1, enumhand);
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
			Block block = Block.asBlock(is.getItem());
			int color = block instanceof BlockCarpet ? ((BlockCarpet) block).c().getColorIndex() : 0;
			this.getEntityData().set(COLOR_WATCHER, color);
		} else {
			this.getEntityData().set(COLOR_WATCHER, -1);
		}
		this.getEntityData().set(VARIANT_WATCHER, getMyPet().getVariant());
	}

	@Override
	public MyTraderLlama getMyPet() {
		return (MyTraderLlama) myPet;
	}
}
