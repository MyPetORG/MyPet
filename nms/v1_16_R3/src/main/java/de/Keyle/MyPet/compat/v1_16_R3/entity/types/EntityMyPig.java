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

package de.Keyle.MyPet.compat.v1_16_R3.entity.types;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyPig;
import de.Keyle.MyPet.compat.v1_16_R3.entity.EntityMyPet;
import net.minecraft.server.v1_16_R3.*;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.InvocationTargetException;

import static de.Keyle.MyPet.compat.v1_16_R3.CompatManager.ENTITY_LIVING_broadcastItemBreak;

@EntitySize(width = 0.7F, height = 0.9F)
public class EntityMyPig extends EntityMyPet {

	private static final DataWatcherObject<Boolean> AGE_WATCHER = DataWatcher.a(EntityMyPig.class, DataWatcherRegistry.i);
	private static final DataWatcherObject<Boolean> SADDLE_WATCHER = DataWatcher.a(EntityMyPig.class, DataWatcherRegistry.i);

	public EntityMyPig(World world, MyPet myPet) {
		super(world, myPet);
		indirectRiding = true;
	}

	@Override
	protected String getDeathSound() {
		return "entity.pig.death";
	}

	@Override
	protected String getHurtSound() {
		return "entity.pig.hurt";
	}

	@Override
	protected String getLivingSound() {
		return "entity.pig.ambient";
	}

	@Override
	public EnumInteractionResult handlePlayerInteraction(final EntityHuman entityhuman, EnumHand enumhand, final ItemStack itemStack) {
		if (enumhand == EnumHand.OFF_HAND) {
			if (itemStack != null) {
				if (itemStack.getItem() == Items.LEAD) {
					((WorldServer) this.world).getChunkProvider().broadcastIncludingSelf(this, new PacketPlayOutAttachEntity(this, null));
					entityhuman.a(EnumHand.OFF_HAND, ItemStack.b);
					new BukkitRunnable() {
						@Override
						public void run() {
							if (entityhuman instanceof EntityPlayer) {
								entityhuman.a(EnumHand.OFF_HAND, itemStack);
								Player p = (Player) entityhuman.getBukkitEntity();
								if (!p.isOnline()) {
									p.saveData();
								}
							}
						}
					}.runTaskLater(MyPetApi.getPlugin(), 5);
				}
			}
			return EnumInteractionResult.CONSUME;
		}

		if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).a()) {
			return EnumInteractionResult.CONSUME;
		}

		if (getOwner().equals(entityhuman) && itemStack != null && canUseItem()) {
			if (itemStack.getItem() == Items.SADDLE && !getMyPet().hasSaddle() && getOwner().getPlayer().isSneaking()) {
				getMyPet().setSaddle(CraftItemStack.asBukkitCopy(itemStack));
				if (itemStack != ItemStack.b && !entityhuman.abilities.canInstantlyBuild) {
					itemStack.subtract(1);
					if (itemStack.getCount() <= 0) {
						entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, ItemStack.b);
					}
				}
				return EnumInteractionResult.CONSUME;
			} else if (itemStack.getItem() == Items.SHEARS && getMyPet().hasSaddle() && getOwner().getPlayer().isSneaking()) {
				EntityItem entityitem = new EntityItem(this.world, this.locX(), this.locY() + 1, this.locZ(), CraftItemStack.asNMSCopy(getMyPet().getSaddle()));
				entityitem.pickupDelay = 10;
				entityitem.setMot(entityitem.getMot().add(0, this.random.nextFloat() * 0.05F, 0));
				this.world.addEntity(entityitem);

				makeSound("entity.sheep.shear", 1.0F, 1.0F);
				getMyPet().setSaddle(null);
				if (itemStack != ItemStack.b && !entityhuman.abilities.canInstantlyBuild) {
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

				return EnumInteractionResult.CONSUME;
			} else if (Configuration.MyPet.Pig.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
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
		return EnumInteractionResult.PASS;
	}

	@Override
	protected void initDatawatcher() {
		super.initDatawatcher();
		getDataWatcher().register(AGE_WATCHER, false);
		getDataWatcher().register(SADDLE_WATCHER, false); // saddle
	}

	@Override
	public void updateVisuals() {
		getDataWatcher().set(AGE_WATCHER, getMyPet().isBaby());
		getDataWatcher().set(SADDLE_WATCHER, getMyPet().hasSaddle());
	}

	@Override
	public void playPetStepSound() {
		makeSound("entity.pig.step", 0.15F, 1.0F);
	}

	@Override
	public MyPig getMyPet() {
		return (MyPig) myPet;
	}
}