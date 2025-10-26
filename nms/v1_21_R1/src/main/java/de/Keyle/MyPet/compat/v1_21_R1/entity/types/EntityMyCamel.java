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

package de.Keyle.MyPet.compat.v1_21_R1.entity.types;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyCamel;
import de.Keyle.MyPet.compat.v1_21_R1.entity.EntityMyPet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.bukkit.craftbukkit.v1_21_R1.inventory.CraftItemStack;

// TODO add jumping mechanic and second passenger (potentially)
@EntitySize(width = 0.9F, height = 1.7F)
public class EntityMyCamel extends EntityMyPet {

	private static final EntityDataAccessor<Boolean> AGE_WATCHER = SynchedEntityData.defineId(EntityMyCamel.class, EntityDataSerializers.BOOLEAN);
	protected static final EntityDataAccessor<Byte> SADDLE_CHEST_WATCHER = SynchedEntityData.defineId(EntityMyCamel.class, EntityDataSerializers.BYTE);
	public static final EntityDataAccessor<Boolean> DASH = SynchedEntityData.defineId(EntityMyCamel.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Long> LAST_POSE_CHANGE_TICK = SynchedEntityData.defineId(EntityMyCamel.class, EntityDataSerializers.LONG);

	private static final int SITDOWN_DURATION_TICKS = 40;
	private static final int STANDUP_DURATION_TICKS = 52;

	public EntityMyCamel(Level world, MyPet myPet) {
		super(world, myPet);
		indirectRiding = true;
	}

	public void applySitting(boolean sitting) {
		if (sitting) {
			this.sitDown();
		} else {
			this.standUp();
		}
	}

	public void sitDown() {
		this.playSound(SoundEvents.CAMEL_SIT, 1.0F, 1.0F);
		this.setPose(Pose.SITTING);
		this.resetLastPoseChangeTick(-this.level().getGameTime());
	}

	public void standUp() {
		this.playSound(SoundEvents.CAMEL_STAND, 1.0F, 1.0F);
		this.setPose(Pose.STANDING);
		this.resetLastPoseChangeTick(this.level().getGameTime());
	}

	@Override
	protected String getMyPetDeathSound() {
		return "entity.camel.death";
	}

	@Override
	protected String getHurtSound() {
		return "entity.camel.hurt";
	}

	@Override
	protected String getLivingSound() {
		return "entity.camel.ambient";
	}

	@Override
	public InteractionResult handlePlayerInteraction(final Player entityhuman, InteractionHand enumhand, final ItemStack itemStack) {
		if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).consumesAction()) {
			return InteractionResult.CONSUME;
		}

		if (getOwner().equals(entityhuman) && itemStack != null && canUseItem()) {
			if (itemStack.getItem() == Items.SADDLE && !getMyPet().hasSaddle() && getOwner().getPlayer().isSneaking()) {
				getMyPet().setSaddle(CraftItemStack.asBukkitCopy(itemStack));
				makeSound("entity.camel.saddle", 1.0F, 1.0F);
				if (itemStack != ItemStack.EMPTY && !entityhuman.getAbilities().instabuild) {
					itemStack.shrink(1);
					if (itemStack.getCount() <= 0) {
						entityhuman.getInventory().setItem(entityhuman.getInventory().selected, ItemStack.EMPTY);
					}
				}
				return InteractionResult.CONSUME;
			} else if (itemStack.getItem() == Items.SHEARS && getMyPet().hasSaddle() && getOwner().getPlayer().isSneaking()) {
				ItemEntity entityitem = new ItemEntity(this.level(), this.getX(), this.getY() + 1, this.getZ(), CraftItemStack.asNMSCopy(getMyPet().getSaddle()));
				entityitem.pickupDelay = 10;
				entityitem.setDeltaMovement(entityitem.getDeltaMovement().add(0, this.random.nextFloat() * 0.05F, 0));
				this.level().addFreshEntity(entityitem);

				makeSound("entity.sheep.shear", 1.0F, 1.0F);
				getMyPet().setSaddle(null);
				if (itemStack != ItemStack.EMPTY && !entityhuman.getAbilities().instabuild) {
					try {
						itemStack.hurtAndBreak(1, entityhuman, getSlotForHand(enumhand));
					} catch (Error e) {
						// TODO REMOVE
					}
				}

				return InteractionResult.CONSUME;
			} else if (Configuration.MyPet.Camel.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
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
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(AGE_WATCHER, false);
		builder.define(SADDLE_CHEST_WATCHER, (byte) 0);
		builder.define(DASH, false);

		builder.define(LAST_POSE_CHANGE_TICK, Math.max(0L, this.level().getGameTime() - 52L - 1L));
	}
	private void resetLastPoseChangeTick(long i) {
		this.getEntityData().set(LAST_POSE_CHANGE_TICK, i);
	}
	public long getPoseTime() {
		return this.level().getGameTime() - Math.abs((Long) this.getEntityData().get(LAST_POSE_CHANGE_TICK));
	}

	@Override
	public void updateVisuals() {
		this.getEntityData().set(AGE_WATCHER, getMyPet().isBaby());
		applyVisual(4, getMyPet().hasSaddle());
	}

	@Override
	public void playPetStepSound() {
		makeSound("entity.camel.step", 0.15F, 1.0F);
	}

	@Override
	public MyCamel getMyPet() {
		return (MyCamel) myPet;
	}

	/** Saddle things
	 * Possible visual camel effects:
	 * 4 saddle
	 */
	protected void applyVisual(int value, boolean flag) {
		int i = this.getEntityData().get(SADDLE_CHEST_WATCHER);
		if (flag) {
			this.getEntityData().set(SADDLE_CHEST_WATCHER, (byte) (i | value));
		} else {
			this.getEntityData().set(SADDLE_CHEST_WATCHER, (byte) (i & (~value)));
		}
	}
}
