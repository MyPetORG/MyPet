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

package de.Keyle.MyPet.compat.v1_19_R3.entity.types;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MySheep;
import de.Keyle.MyPet.compat.v1_19_R3.CompatManager;
import de.Keyle.MyPet.compat.v1_19_R3.entity.EntityMyPet;
import de.Keyle.MyPet.compat.v1_19_R3.entity.ai.movement.EatGrass;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

@EntitySize(width = 0.7F, height = 1.2349999f)
public class EntityMySheep extends EntityMyPet {

	private static final EntityDataAccessor<Boolean> AGE_WATCHER = SynchedEntityData.defineId(EntityMySheep.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Byte> COLOR_WATCHER = SynchedEntityData.defineId(EntityMySheep.class, EntityDataSerializers.BYTE);

	private static final Map<DyeColor, Block> colorMap = new HashMap<>();

	static {
		colorMap.put(DyeColor.WHITE, Blocks.WHITE_WOOL);
		colorMap.put(DyeColor.ORANGE, Blocks.ORANGE_WOOL);
		colorMap.put(DyeColor.MAGENTA, Blocks.MAGENTA_WOOL);
		colorMap.put(DyeColor.LIGHT_BLUE, Blocks.LIGHT_BLUE_WOOL);
		colorMap.put(DyeColor.YELLOW, Blocks.YELLOW_WOOL);
		colorMap.put(DyeColor.LIME, Blocks.LIME_WOOL);
		colorMap.put(DyeColor.PINK, Blocks.PINK_WOOL);
		colorMap.put(DyeColor.GRAY, Blocks.GRAY_WOOL);
		colorMap.put(DyeColor.LIGHT_GRAY, Blocks.LIGHT_GRAY_WOOL);
		colorMap.put(DyeColor.CYAN, Blocks.CYAN_WOOL);
		colorMap.put(DyeColor.PURPLE, Blocks.PURPLE_WOOL);
		colorMap.put(DyeColor.BLUE, Blocks.BLUE_WOOL);
		colorMap.put(DyeColor.BROWN, Blocks.BROWN_WOOL);
		colorMap.put(DyeColor.GREEN, Blocks.GREEN_WOOL);
		colorMap.put(DyeColor.RED, Blocks.RED_WOOL);
		colorMap.put(DyeColor.BLACK, Blocks.BLACK_WOOL);
	}

	public EntityMySheep(Level world, MyPet myPet) {
		super(world, myPet);
	}

	@Override
	protected String getMyPetDeathSound() {
		return "entity.sheep.death";
	}

	@Override
	protected String getHurtSound() {
		return "entity.sheep.hurt";
	}

	@Override
	protected String getLivingSound() {
		return "entity.sheep.ambient";
	}

	@Override
	public InteractionResult handlePlayerInteraction(Player entityhuman, InteractionHand enumhand, ItemStack itemStack) {
		if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).consumesAction()) {
			return InteractionResult.CONSUME;
		}

		if (getOwner().equals(entityhuman) && itemStack != null && canUseItem()) {
			if (itemStack.getItem() instanceof DyeItem && ((DyeItem) itemStack.getItem()).getDyeColor().ordinal() != getMyPet().getColor().ordinal() && !getMyPet().isSheared()) {
				getMyPet().setColor(org.bukkit.DyeColor.values()[((DyeItem) itemStack.getItem()).getDyeColor().ordinal()]);
				if (itemStack != ItemStack.EMPTY && !entityhuman.getAbilities().instabuild) {
					itemStack.shrink(1);
					if (itemStack.getCount() <= 0) {
						entityhuman.getInventory().setItem(entityhuman.getInventory().selected, ItemStack.EMPTY);
					}
				}
				return InteractionResult.CONSUME;
			} else if (itemStack.getItem() == Items.SHEARS && Configuration.MyPet.Sheep.CAN_BE_SHEARED && !getMyPet().isSheared()) {
				getMyPet().setSheared(true);
				int woolDropCount = 1 + this.random.nextInt(3);

				for (int j = 0; j < woolDropCount; ++j) {
					ItemEntity entityitem = new ItemEntity(this.level, this.getX(), this.getY() + 1, this.getZ(), new ItemStack(colorMap.get(DyeColor.values()[getMyPet().getColor().ordinal()])));
					entityitem.pickupDelay = 10;
					entityitem.setDeltaMovement(entityitem.getDeltaMovement().add(0, this.random.nextFloat() * 0.05F, 0));
					this.level.addFreshEntity(entityitem);
				}
				makeSound("entity.sheep.shear", 1.0F, 1.0F);
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
			} else if (Configuration.MyPet.Sheep.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
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
		getEntityData().define(COLOR_WATCHER, (byte) 0); // color/sheared
	}

	@Override
	public void updateVisuals() {
		this.getEntityData().set(AGE_WATCHER, getMyPet().isBaby());

		byte data = (byte) (getMyPet().isSheared() ? 16 : 0);
		this.getEntityData().set(COLOR_WATCHER, (byte) (data & 0xF0 | getMyPet().getColor().ordinal() & 0xF));
	}

	@Override
	public void playPetStepSound() {
		makeSound("entity.sheep.step", 0.15F, 1.0F);
	}

	@Override
	public MySheep getMyPet() {
		return (MySheep) myPet;
	}

	@Override
	public void setPathfinder() {
		super.setPathfinder();
		petPathfinderSelector.addGoal("EatGrass", new EatGrass(this));
	}
}
