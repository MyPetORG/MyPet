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

import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MySnowman;
import de.Keyle.MyPet.compat.v1_18_R1.CompatManager;
import de.Keyle.MyPet.compat.v1_18_R1.entity.EntityMyPet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

@EntitySize(width = 0.7F, height = 1.7F)
public class EntityMySnowman extends EntityMyPet {

	private static final EntityDataAccessor<Byte> SHEARED_WATCHER = SynchedEntityData.defineId(EntityMySnowman.class, EntityDataSerializers.BYTE);

	public EntityMySnowman(Level world, MyPet myPet) {
		super(world, myPet);
	}

	@Override
	public InteractionResult handlePlayerInteraction(Player entityhuman, InteractionHand enumhand, ItemStack itemStack) {
		if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).consumesAction()) {
			return InteractionResult.CONSUME;
		}

		if (getOwner().equals(entityhuman) && itemStack != null && canUseItem()) {
			if (itemStack.getItem() == Item.byBlock(Blocks.PUMPKIN) && getMyPet().isSheared() && entityhuman.isShiftKeyDown()) {
				getMyPet().setSheared(false);
				if (itemStack != ItemStack.EMPTY && !entityhuman.getAbilities().instabuild) {
					itemStack.shrink(1);
					if (itemStack.getCount() <= 0) {
						entityhuman.getInventory().setItem(entityhuman.getInventory().selected, ItemStack.EMPTY);
					}
				}
				return InteractionResult.CONSUME;
			} else if (itemStack.getItem() == Items.SHEARS && !getMyPet().isSheared() && entityhuman.isShiftKeyDown()) {
				getMyPet().setSheared(true);
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
			}
		}
		return InteractionResult.PASS;
	}

	@Override
	public void updateVisuals() {
		getEntityData().set(SHEARED_WATCHER, (byte) (getMyPet().isSheared() ? 0 : 16));
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();

		getEntityData().define(SHEARED_WATCHER, (byte) 16);
	}

	@Override
	protected String getMyPetDeathSound() {
		return "entity.snow_golem.death";
	}

	@Override
	protected String getHurtSound() {
		return "entity.snow_golem.hurt";
	}

	@Override
	protected String getLivingSound() {
		return "entity.snow_golem.ambient";
	}

	@Override
	public void playPetStepSound() {
		makeSound("block.snow.step", 0.15F, 1.0F);
	}

	@Override
	public MySnowman getMyPet() {
		return (MySnowman) myPet;
	}
}
