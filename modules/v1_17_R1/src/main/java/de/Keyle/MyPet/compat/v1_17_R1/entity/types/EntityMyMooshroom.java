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

import org.bukkit.Bukkit;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyMooshroom;
import de.Keyle.MyPet.compat.v1_17_R1.entity.EntityMyPet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

@EntitySize(width = 0.7F, height = 1.3F)
public class EntityMyMooshroom extends EntityMyPet {

	private static final EntityDataAccessor<Boolean> AGE_WATCHER = SynchedEntityData.defineId(EntityMyMooshroom.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<String> COLOR_WATCHER = SynchedEntityData.defineId(EntityMyMooshroom.class, EntityDataSerializers.STRING);

	public EntityMyMooshroom(Level world, MyPet myPet) {
		super(world, myPet);
	}

	@Override
	protected String getDeathSound() {
		return "entity.cow.death";
	}

	@Override
	protected String getHurtSound() {
		return "entity.cow.hurt";
	}

	@Override
	protected String getLivingSound() {
		return "entity.cow.ambient";
	}

	@Override
	public InteractionResult handlePlayerInteraction(Player entityhuman, InteractionHand enumhand, ItemStack itemStack) {
		if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).consumesAction()) {
			return InteractionResult.CONSUME;
		}

		if (itemStack != null) {
			if (itemStack.getItem().equals(Items.BOWL)) {
				if (!getOwner().equals(entityhuman) || !canUseItem() || !Configuration.MyPet.Mooshroom.CAN_GIVE_SOUP) {
					final int itemInHandIndex = entityhuman.getInventory().selected;
					ItemStack is = new ItemStack(Items.MUSHROOM_STEW);
					final ItemStack oldIs = entityhuman.getInventory().getItem(itemInHandIndex);
					entityhuman.getInventory().setItem(itemInHandIndex, is);
					Bukkit.getScheduler().scheduleSyncDelayedTask(MyPetApi.getPlugin(), () -> entityhuman.getInventory().setItem(itemInHandIndex, oldIs), 2L);

				} else {
					itemStack.shrink(1);
					if (itemStack.getCount() <= 0) {
						entityhuman.getInventory().setItem(entityhuman.getInventory().selected, new ItemStack(Items.MUSHROOM_STEW));
					} else {
						if (!entityhuman.getInventory().add(new ItemStack(Items.MUSHROOM_STEW))) {
							entityhuman.drop(new ItemStack(Items.GLASS_BOTTLE), true);
						}
					}
					return InteractionResult.CONSUME;
				}
			}
			if (getOwner().equals(entityhuman) && canUseItem()) {
				if (Configuration.MyPet.Mooshroom.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
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
		}
		return InteractionResult.PASS;
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		getEntityData().define(AGE_WATCHER, false);
		getEntityData().define(COLOR_WATCHER, "red");
	}

	@Override
	public void updateVisuals() {
		this.getEntityData().set(AGE_WATCHER, getMyPet().isBaby());
		this.getEntityData().set(COLOR_WATCHER, getMyPet().getType().getType());	//TODO
	}

	@Override
	public void playPetStepSound() {
		makeSound("entity.cow.step", 0.15F, 1.0F);
	}

	@Override
	public MyMooshroom getMyPet() {
		return (MyMooshroom) myPet;
	}
}
