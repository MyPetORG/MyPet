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
import de.Keyle.MyPet.api.entity.types.MySheep;
import de.Keyle.MyPet.compat.v1_17_R1.entity.EntityMyPet;
import de.Keyle.MyPet.compat.v1_17_R1.entity.ai.movement.EatGrass;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.EnumColor;
import net.minecraft.world.item.ItemDye;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.bukkit.DyeColor;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import static de.Keyle.MyPet.compat.v1_17_R1.CompatManager.ENTITY_LIVING_broadcastItemBreak;

@EntitySize(width = 0.7F, height = 1.2349999f)
public class EntityMySheep extends EntityMyPet {

	private static final EntityDataAccessor<Boolean> AGE_WATCHER = SynchedEntityData.defineId(EntityMySheep.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Byte> COLOR_WATCHER = SynchedEntityData.defineId(EntityMySheep.class, EntityDataSerializers.BYTE);

	private static final Map<EnumColor, Block> colorMap = new HashMap<>();

	static {
		colorMap.put(EnumColor.a, Blocks.be);
		colorMap.put(EnumColor.b, Blocks.bf);
		colorMap.put(EnumColor.c, Blocks.bg);
		colorMap.put(EnumColor.d, Blocks.bh);
		colorMap.put(EnumColor.e, Blocks.bi);
		colorMap.put(EnumColor.f, Blocks.bj);
		colorMap.put(EnumColor.g, Blocks.bk);
		colorMap.put(EnumColor.h, Blocks.bl);
		colorMap.put(EnumColor.i, Blocks.bm);
		colorMap.put(EnumColor.j, Blocks.bn);
		colorMap.put(EnumColor.k, Blocks.bo);
		colorMap.put(EnumColor.l, Blocks.bp);
		colorMap.put(EnumColor.m, Blocks.bq);
		colorMap.put(EnumColor.n, Blocks.br);
		colorMap.put(EnumColor.o, Blocks.bs);
		colorMap.put(EnumColor.p, Blocks.bt);
	}

	public EntityMySheep(Level world, MyPet myPet) {
		super(world, myPet);
	}

	@Override
	protected String getDeathSound() {
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
	public InteractionResult handlePlayerInteraction(EntityHuman entityhuman, InteractionHand enumhand, ItemStack itemStack) {
		if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).a()) {
			return InteractionResult.CONSUME;
		}

		if (getOwner().equals(entityhuman) && itemStack != null && canUseItem()) {
			if (itemStack.getItem() instanceof ItemDye && ((ItemDye) itemStack.getItem()).d().ordinal() != getMyPet().getColor().ordinal() && !getMyPet().isSheared()) {
				getMyPet().setColor(DyeColor.values()[((ItemDye) itemStack.getItem()).d().ordinal()]);
				if (itemStack != ItemStack.EMPTY && !entityhuman.getAbilities().instabuild) {
					itemStack.shrink(1);
					if (itemStack.getCount() <= 0) {
						entityhuman.getInventory().setItem(entityhuman.getInventory().selected, ItemStack.EMPTY);
					}
				}
				return InteractionResult.CONSUME;
			} else if (itemStack.getItem() == Items.pq && Configuration.MyPet.Sheep.CAN_BE_SHEARED && !getMyPet().isSheared()) {
				getMyPet().setSheared(true);
				int woolDropCount = 1 + this.Q.nextInt(3);

				for (int j = 0; j < woolDropCount; ++j) {
					EntityItem entityitem = new EntityItem(this.t, this.locX(), this.locY() + 1, this.locZ(), new ItemStack(colorMap.get(EnumColor.values()[getMyPet().getColor().ordinal()])));
					entityitem.ap = 10;
					entityitem.setMot(entityitem.getMot().add(0, this.Q.nextFloat() * 0.05F, 0));
					this.t.addEntity(entityitem);
				}
				makeSound("entity.sheep.shear", 1.0F, 1.0F);
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
