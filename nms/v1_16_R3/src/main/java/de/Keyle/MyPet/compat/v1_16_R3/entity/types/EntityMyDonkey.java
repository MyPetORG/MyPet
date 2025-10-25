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

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyDonkey;
import de.Keyle.MyPet.compat.v1_16_R3.entity.EntityMyPet;
import net.minecraft.server.v1_16_R3.*;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.UUID;

import static de.Keyle.MyPet.compat.v1_16_R3.CompatManager.ENTITY_LIVING_broadcastItemBreak;

@EntitySize(width = 1.4F, height = 1.6F)
public class EntityMyDonkey extends EntityMyPet {

	protected static final DataWatcherObject<Boolean> AGE_WATCHER = DataWatcher.a(EntityMyDonkey.class, DataWatcherRegistry.i);
	protected static final DataWatcherObject<Byte> SADDLE_WATCHER = DataWatcher.a(EntityMyDonkey.class, DataWatcherRegistry.a);
	protected static final DataWatcherObject<Optional<UUID>> OWNER_WATCHER = DataWatcher.a(EntityMyDonkey.class, DataWatcherRegistry.o);
	private static final DataWatcherObject<Boolean> CHEST_WATCHER = DataWatcher.a(EntityMyDonkey.class, DataWatcherRegistry.i);

	int soundCounter = 0;
	int rearCounter = -1;

	public EntityMyDonkey(World world, MyPet myPet) {
		super(world, myPet);
		indirectRiding = true;
	}

	/**
	 * Possible visual horse effects:
	 * 4 saddle
	 * 8 chest
	 * 32 head down
	 * 64 rear
	 * 128 mouth open
	 */
	protected void applyVisual(int value, boolean flag) {
		int i = getDataWatcher().get(SADDLE_WATCHER);
		if (flag) {
			getDataWatcher().set(SADDLE_WATCHER, (byte) (i | value));
		} else {
			getDataWatcher().set(SADDLE_WATCHER, (byte) (i & (~value)));
		}
	}

	@Override
	public boolean attack(Entity entity) {
		boolean flag = false;
		try {
			flag = super.attack(entity);
			if (flag) {
				applyVisual(64, true);
				rearCounter = 10;
				this.makeSound("entity.donkey.angry", 1.0F, 1.0F);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return flag;
	}

	@Override
	public EnumInteractionResult handlePlayerInteraction(EntityHuman entityhuman, EnumHand enumhand, ItemStack itemStack) {
		if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).a()) {
			return EnumInteractionResult.CONSUME;
		}

		if (itemStack != null && canUseItem()) {
			if (itemStack.getItem() == Items.SADDLE && !getMyPet().hasSaddle() && getOwner().getPlayer().isSneaking() && canEquip()) {
				getMyPet().setSaddle(CraftItemStack.asBukkitCopy(itemStack));
				if (itemStack != ItemStack.b && !entityhuman.abilities.canInstantlyBuild) {
					itemStack.subtract(1);
					if (itemStack.getCount() <= 0) {
						entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, ItemStack.b);
					}
				}
				return EnumInteractionResult.CONSUME;
			} else if (itemStack.getItem() == Item.getItemOf(Blocks.CHEST) && getOwner().getPlayer().isSneaking() && !getMyPet().hasChest() && canEquip()) {
				getMyPet().setChest(CraftItemStack.asBukkitCopy(itemStack));
				if (itemStack != ItemStack.b && !entityhuman.abilities.canInstantlyBuild) {
					itemStack.subtract(1);
					if (itemStack.getCount() <= 0) {
						entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, ItemStack.b);
					}
				}
				return EnumInteractionResult.CONSUME;
			} else if (itemStack.getItem() == Items.SHEARS && getOwner().getPlayer().isSneaking() && canEquip()) {
				if (getMyPet().hasChest()) {
					EntityItem entityitem = new EntityItem(this.world, this.locX(), this.locY() + 1, this.locZ(), CraftItemStack.asNMSCopy(getMyPet().getChest()));
					entityitem.pickupDelay = 10;
					entityitem.setMot(entityitem.getMot().add(0, this.random.nextFloat() * 0.05F, 0));
					this.world.addEntity(entityitem);
				}
				if (getMyPet().hasSaddle()) {
					EntityItem entityitem = new EntityItem(this.world, this.locX(), this.locY() + 1, this.locZ(), CraftItemStack.asNMSCopy(getMyPet().getSaddle()));
					entityitem.pickupDelay = 10;
					entityitem.setMot(entityitem.getMot().add(0, this.random.nextFloat() * 0.05F, 0));
					this.world.addEntity(entityitem);
				}

				makeSound("entity.sheep.shear", 1.0F, 1.0F);
				getMyPet().setChest(null);
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
			} else if (Configuration.MyPet.Donkey.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
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
		getDataWatcher().register(SADDLE_WATCHER, (byte) 0);
		getDataWatcher().register(OWNER_WATCHER, Optional.empty());
		getDataWatcher().register(CHEST_WATCHER, false);
	}

	@Override
	public void updateVisuals() {
		getDataWatcher().set(AGE_WATCHER, getMyPet().isBaby());
		getDataWatcher().set(CHEST_WATCHER, getMyPet().hasChest());
		applyVisual(4, getMyPet().hasSaddle());
	}

	@Override
	protected String getDeathSound() {
		return "entity.donkey.death";
	}

	@Override
	protected String getHurtSound() {
		return "entity.donkey.hurt";
	}

	@Override
	protected String getLivingSound() {
		return "entity.donkey.ambient";
	}

	@Override
	public void onLivingUpdate() {
		boolean oldRiding = hasRider;
		super.onLivingUpdate();
		if (!hasRider) {
			if (rearCounter > -1 && rearCounter-- == 0) {
				applyVisual(64, false);
				rearCounter = -1;
			}
		}
		if (oldRiding != hasRider) {
			if (hasRider) {
				applyVisual(4, true);
			} else {
				applyVisual(4, getMyPet().hasSaddle());
			}
		}
	}


	@Override
	public void playStepSound(BlockPosition blockposition, IBlockData blockdata) {
		if (!blockdata.getMaterial().isLiquid()) {
			IBlockData blockdataUp = this.world.getType(blockposition.up());
			SoundEffectType soundeffecttype = blockdata.getStepSound();
			if (blockdataUp.getBlock() == Blocks.SNOW) {
				soundeffecttype = blockdata.getStepSound();
			}
			if (this.isVehicle()) {
				++this.soundCounter;
				if (this.soundCounter > 5 && this.soundCounter % 3 == 0) {
					this.playSound(SoundEffects.ENTITY_HORSE_GALLOP, soundeffecttype.getVolume() * 0.16F, soundeffecttype.getPitch());
				} else if (this.soundCounter <= 5) {
					this.playSound(SoundEffects.ENTITY_HORSE_STEP_WOOD, soundeffecttype.getVolume() * 0.16F, soundeffecttype.getPitch());
				}
			} else if (!blockdata.getMaterial().isLiquid()) {
				this.soundCounter += 1;
				playSound(SoundEffects.ENTITY_HORSE_STEP_WOOD, soundeffecttype.getVolume() * 0.16F, soundeffecttype.getPitch());
			} else {
				playSound(SoundEffects.ENTITY_HORSE_STEP, soundeffecttype.getVolume() * 0.16F, soundeffecttype.getPitch());
			}
		}
	}

	@Override
	public MyDonkey getMyPet() {
		return (MyDonkey) myPet;
	}
}