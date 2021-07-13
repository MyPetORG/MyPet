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

import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyEnderman;
import de.Keyle.MyPet.api.skill.skills.Behavior;
import de.Keyle.MyPet.compat.v1_17_R1.entity.EntityMyPet;
import de.Keyle.MyPet.skill.skills.BehaviorImpl;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.IBlockData;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_17_R1.util.CraftMagicNumbers;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

import static de.Keyle.MyPet.compat.v1_17_R1.CompatManager.ENTITY_LIVING_broadcastItemBreak;

@EntitySize(width = 0.6F, height = 2.55F)
public class EntityMyEnderman extends EntityMyPet {

	private static final DataWatcherObject<Optional<IBlockData>> BLOCK_WATCHER = DataWatcher.a(EntityMyEnderman.class, DataWatcherRegistry.h);
	private static final DataWatcherObject<Boolean> SCREAMING_WATCHER = DataWatcher.a(EntityMyEnderman.class, DataWatcherRegistry.i);

	public EntityMyEnderman(World world, MyPet myPet) {
		super(world, myPet);
	}

	@Override
	protected String getDeathSound() {
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
	public EnumInteractionResult handlePlayerInteraction(EntityHuman entityhuman, EnumHand enumhand, ItemStack itemStack) {
		if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).a()) {
			return EnumInteractionResult.b;
		}

		if (getOwner().equals(entityhuman) && itemStack != null && canUseItem()) {
			if (itemStack.getItem() == Items.pq && getMyPet().hasBlock() && getOwner().getPlayer().isSneaking()) {
				EntityItem entityitem = new EntityItem(this.t, this.locX(), this.locY() + 1, this.locZ(), CraftItemStack.asNMSCopy(getMyPet().getBlock()));
				entityitem.ap = 10;
				entityitem.setMot(entityitem.getMot().add(0, this.Q.nextFloat() * 0.05F, 0));

				makeSound("entity.sheep.shear", 1.0F, 1.0F);
				getMyPet().setBlock(null);
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
			} else if (getMyPet().getBlock() == null && Util.isBetween(1, 255, Item.getId(itemStack.getItem())) && getOwner().getPlayer().isSneaking()) {
				getMyPet().setBlock(CraftItemStack.asBukkitCopy(itemStack));
				if (itemStack != ItemStack.b && !entityhuman.getAbilities().d) {
					itemStack.subtract(1);
					if (itemStack.getCount() <= 0) {
						entityhuman.getInventory().setItem(entityhuman.getInventory().k, ItemStack.b);
					}
				}
				return EnumInteractionResult.b;
			}
		}
		return EnumInteractionResult.d;
	}

	@Override
	protected void initDatawatcher() {
		super.initDatawatcher();
		getDataWatcher().register(BLOCK_WATCHER, Optional.empty());
		getDataWatcher().register(SCREAMING_WATCHER, false);
	}

	@Override
	public void updateVisuals() {
		Optional<IBlockData> block;
		if (getMyPet().getBlock() != null) {
			IBlockData data = CraftMagicNumbers.getBlock(getMyPet().getBlock().getData());
			block = Optional.ofNullable(data);
		} else {
			block = Optional.empty();
		}
		getDataWatcher().set(BLOCK_WATCHER, block);
		getDataWatcher().set(SCREAMING_WATCHER, getMyPet().isScreaming());
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
