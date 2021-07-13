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
import de.Keyle.MyPet.api.entity.types.MyIronGolem;
import de.Keyle.MyPet.compat.v1_17_R1.entity.EntityMyPet;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Blocks;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;

import java.lang.reflect.InvocationTargetException;

import static de.Keyle.MyPet.compat.v1_17_R1.CompatManager.ENTITY_LIVING_broadcastItemBreak;

@EntitySize(width = 1.4F, height = 2.7F)
public class EntityMyIronGolem extends EntityMyPet {

	protected static final DataWatcherObject<Byte> UNUSED_WATCHER = DataWatcher.a(EntityMyIronGolem.class, DataWatcherRegistry.a);

	int flowerCounter = 0;
	boolean flower = false;

	public EntityMyIronGolem(World world, MyPet myPet) {
		super(world, myPet);
	}

	@Override
	public boolean attack(Entity entity) {
		boolean flag = false;
		try {
			this.t.broadcastEntityEffect(this, (byte) 4);
			flag = super.attack(entity);
			if (Configuration.MyPet.IronGolem.CAN_TOSS_UP && flag) {
				entity.setMot(entity.getMot().add(0, 0.4000000059604645D, 0));
				this.makeSound("entity.iron_golem.attack", 1.0F, 1.0F);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return flag;
	}

	@Override
	protected String getDeathSound() {
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
	protected void initDatawatcher() {
		super.initDatawatcher();
		getDataWatcher().register(UNUSED_WATCHER, (byte) 0); // N/A
	}

	@Override
	public EnumInteractionResult handlePlayerInteraction(EntityHuman entityhuman, EnumHand enumhand, ItemStack itemStack) {
		if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).a()) {
			return EnumInteractionResult.b;
		}

		if (itemStack.getItem() == Items.mq) {
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
			if (itemStack.getItem() == Blocks.bw.getItem() && !getMyPet().hasFlower() && getOwner().getPlayer().isSneaking()) {
				getMyPet().setFlower(CraftItemStack.asBukkitCopy(itemStack));
				if (itemStack != ItemStack.b && !entityhuman.getAbilities().d) {
					itemStack.subtract(1);
					if (itemStack.getCount() <= 0) {
						entityhuman.getInventory().setItem(entityhuman.getInventory().k, ItemStack.b);
					}
				}
				return EnumInteractionResult.b;
			} else if (itemStack.getItem() == Items.pq && getMyPet().hasFlower() && getOwner().getPlayer().isSneaking()) {
				EntityItem entityitem = new EntityItem(this.t, this.locX(), this.locY() + 1, this.locZ(), CraftItemStack.asNMSCopy(getMyPet().getFlower()));
				entityitem.ap = 10;
				entityitem.setMot(entityitem.getMot().add(0, this.Q.nextFloat() * 0.05F, 0));
				this.t.addEntity(entityitem);

				makeSound("entity.sheep.shear", 1.0F, 1.0F);
				getMyPet().setFlower(null);
				if (itemStack != ItemStack.b && !entityhuman.getAbilities().d) {
					try {
						itemStack.damage(1, entityhuman, (entityhuman1) -> entityhuman1.broadcastItemBreak(enumhand));
					} catch (Error e) {
						// TODO REMOVE
						itemStack.damage(1, entityhuman, (entityhuman1) -> {
							try {
								ENTITY_LIVING_broadcastItemBreak.invoke(entityhuman1, enumhand);
							} catch (IllegalAccessException | InvocationTargetException ex) {
								ex.printStackTrace();
							}
						});
					}
				}

				return EnumInteractionResult.b;
			}
		}
		return EnumInteractionResult.d;
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
			this.t.broadcastEntityEffect(this, (byte) 11);
			flowerCounter = 300;
		}
	}
}
