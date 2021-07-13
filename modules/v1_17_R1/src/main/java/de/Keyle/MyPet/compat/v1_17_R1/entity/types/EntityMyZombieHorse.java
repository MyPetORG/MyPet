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
import de.Keyle.MyPet.api.entity.types.MyZombieHorse;
import de.Keyle.MyPet.compat.v1_17_R1.entity.EntityMyPet;
import net.minecraft.core.BlockPosition;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.IJumpable;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundEffectType;
import net.minecraft.world.level.block.state.IBlockData;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.UUID;

import static de.Keyle.MyPet.compat.v1_17_R1.CompatManager.ENTITY_LIVING_broadcastItemBreak;

@EntitySize(width = 1.4F, height = 1.6F)
public class EntityMyZombieHorse extends EntityMyPet implements IJumpable {

	protected static final DataWatcherObject<Boolean> AGE_WATCHER = DataWatcher.a(EntityMyZombieHorse.class, DataWatcherRegistry.i);
	protected static final DataWatcherObject<Byte> SADDLE_CHEST_WATCHER = DataWatcher.a(EntityMyZombieHorse.class, DataWatcherRegistry.a);
	protected static final DataWatcherObject<Optional<UUID>> OWNER_WATCHER = DataWatcher.a(EntityMyZombieHorse.class, DataWatcherRegistry.o);

	int soundCounter = 0;
	int rearCounter = -1;

	public EntityMyZombieHorse(World world, MyPet myPet) {
		super(world, myPet);
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
		int i = getDataWatcher().get(SADDLE_CHEST_WATCHER);
		if (flag) {
			getDataWatcher().set(SADDLE_CHEST_WATCHER, (byte) (i | value));
		} else {
			getDataWatcher().set(SADDLE_CHEST_WATCHER, (byte) (i & (~value)));
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
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return flag;
	}

	@Override
	protected String getDeathSound() {
		return "entity.zombie_horse.death";
	}

	@Override
	protected String getHurtSound() {
		return "entity.zombie_horse.hurt";
	}

	@Override
	protected String getLivingSound() {
		return "entity.zombie_horse.ambient";
	}


	@Override
	public EnumInteractionResult handlePlayerInteraction(EntityHuman entityhuman, EnumHand enumhand, ItemStack itemStack) {
		if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).a()) {
			return EnumInteractionResult.b;
		}

		if (itemStack != null && canUseItem()) {
			if (itemStack.getItem() == Items.lL && !getMyPet().hasSaddle() && getOwner().getPlayer().isSneaking() && canEquip()) {
				getMyPet().setSaddle(CraftItemStack.asBukkitCopy(itemStack));
				if (itemStack != ItemStack.b && !entityhuman.getAbilities().d) {
					itemStack.subtract(1);
					if (itemStack.getCount() <= 0) {
						entityhuman.getInventory().setItem(entityhuman.getInventory().k, ItemStack.b);
					}
				}
				return EnumInteractionResult.b;
			} else if (itemStack.getItem() == Items.pq && getOwner().getPlayer().isSneaking() && canEquip()) {
				if (getMyPet().hasSaddle()) {
					EntityItem entityitem = new EntityItem(this.t, this.locX(), this.locY() + 1, this.locZ(), CraftItemStack.asNMSCopy(getMyPet().getSaddle()));
					entityitem.ap = 10;
					entityitem.setMot(entityitem.getMot().add(0, this.Q.nextFloat() * 0.05F, 0));
					this.t.addEntity(entityitem);
				}

				makeSound("entity.sheep.shear", 1.0F, 1.0F);
				getMyPet().setSaddle(null);
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
			} else if (Configuration.MyPet.ZombieHorse.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
				if (itemStack != ItemStack.b && !entityhuman.getAbilities().d) {
					itemStack.subtract(1);
					if (itemStack.getCount() <= 0) {
						entityhuman.getInventory().setItem(entityhuman.getInventory().k, ItemStack.b);
					}
				}
				getMyPet().setBaby(false);
				return EnumInteractionResult.b;
			}
		}
		return EnumInteractionResult.d;
	}

	@Override
	protected void initDatawatcher() {
		super.initDatawatcher();
		getDataWatcher().register(AGE_WATCHER, false);
		getDataWatcher().register(SADDLE_CHEST_WATCHER, (byte) 0);
		getDataWatcher().register(OWNER_WATCHER, Optional.empty());
	}

	@Override
	public void updateVisuals() {
		getDataWatcher().set(AGE_WATCHER, getMyPet().isBaby());
		applyVisual(4, getMyPet().hasSaddle());
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
			IBlockData blockdataUp = this.t.getType(blockposition.up());
			SoundEffectType soundeffecttype = blockdata.getStepSound();
			if (blockdataUp.getBlock() == Blocks.cK) {
				soundeffecttype = blockdata.getStepSound();
			}
			if (this.isVehicle()) {
				++this.soundCounter;
				if (this.soundCounter > 5 && this.soundCounter % 3 == 0) {
					this.playSound(SoundEffects.iB, soundeffecttype.getVolume() * 0.15F, soundeffecttype.getPitch());
				} else if (this.soundCounter <= 5) {
					this.playSound(SoundEffects.iH, soundeffecttype.getVolume() * 0.15F, soundeffecttype.getPitch());
				}
			} else if (!blockdata.getMaterial().isLiquid()) {
				this.soundCounter += 1;
				playSound(SoundEffects.iH, soundeffecttype.getVolume() * 0.15F, soundeffecttype.getPitch());
			} else {
				playSound(SoundEffects.iG, soundeffecttype.getVolume() * 0.15F, soundeffecttype.getPitch());
			}
		}
	}

	@Override
	public MyZombieHorse getMyPet() {
		return (MyZombieHorse) myPet;
	}

	@Override
	public void a(int i) {
		// I don't know.
	}

	/* Jump power methods */
	@Override
	public boolean a() {
		return true;
	}

	@Override
	public void b(int i) {
		this.jumpPower = i;
	}

	@Override
	public void b() {
	}
}
