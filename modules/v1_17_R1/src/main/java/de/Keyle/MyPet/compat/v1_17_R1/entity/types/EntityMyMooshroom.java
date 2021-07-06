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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyMooshroom;
import de.Keyle.MyPet.compat.v1_17_R1.entity.EntityMyPet;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Bukkit;

@EntitySize(width = 0.7F, height = 1.3F)
public class EntityMyMooshroom extends EntityMyPet {

	private static final DataWatcherObject<Boolean> AGE_WATCHER = DataWatcher.a(EntityMyMooshroom.class, DataWatcherRegistry.i);
	private static final DataWatcherObject<String> COLOR_WATCHER = DataWatcher.a(EntityMyMooshroom.class, DataWatcherRegistry.d);

	public EntityMyMooshroom(World world, MyPet myPet) {
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
	public EnumInteractionResult handlePlayerInteraction(EntityHuman entityhuman, EnumHand enumhand, ItemStack itemStack) {
		if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).a()) {
			return EnumInteractionResult.CONSUME;
		}

		if (itemStack != null) {
			if (itemStack.getItem().equals(Items.BOWL)) {
				if (!getOwner().equals(entityhuman) || !canUseItem() || !Configuration.MyPet.Mooshroom.CAN_GIVE_SOUP) {
					final int itemInHandIndex = entityhuman.inventory.itemInHandIndex;
					ItemStack is = new ItemStack(Items.MUSHROOM_STEW);
					final ItemStack oldIs = entityhuman.inventory.getItem(itemInHandIndex);
					entityhuman.inventory.setItem(itemInHandIndex, is);
					Bukkit.getScheduler().scheduleSyncDelayedTask(MyPetApi.getPlugin(), () -> entityhuman.inventory.setItem(itemInHandIndex, oldIs), 2L);

				} else {
					itemStack.subtract(1);
					if (itemStack.getCount() <= 0) {
						entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, new ItemStack(Items.MUSHROOM_STEW));
					} else {
						if (!entityhuman.inventory.pickup(new ItemStack(Items.MUSHROOM_STEW))) {
							entityhuman.drop(new ItemStack(Items.GLASS_BOTTLE), true);
						}
					}
					return EnumInteractionResult.CONSUME;
				}
			}
			if (getOwner().equals(entityhuman) && canUseItem()) {
				if (Configuration.MyPet.Mooshroom.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
					if (itemStack != ItemStack.b && !entityhuman.abilities.canInstantlyBuild) {
						itemStack.subtract(1);
						if (itemStack.getCount() <= 0) {
							entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, ItemStack.b);
						}
					}
					getMyPet().setBaby(false);
					return EnumInteractionResult.CONSUME;
				}
			}
		}
		return EnumInteractionResult.PASS;
	}

	@Override
	protected void initDatawatcher() {
		super.initDatawatcher();
		getDataWatcher().register(AGE_WATCHER, false);
		getDataWatcher().register(COLOR_WATCHER, "red");
	}

	@Override
	public void updateVisuals() {
		getDataWatcher().set(AGE_WATCHER, getMyPet().isBaby());
		getDataWatcher().set(COLOR_WATCHER, getMyPet().getType().getType());
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
