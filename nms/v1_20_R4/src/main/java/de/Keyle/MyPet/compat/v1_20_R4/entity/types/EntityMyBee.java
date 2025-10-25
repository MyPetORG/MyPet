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

package de.Keyle.MyPet.compat.v1_20_R4.entity.types;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyBee;
import de.Keyle.MyPet.api.skill.skills.Behavior;
import de.Keyle.MyPet.compat.v1_20_R4.entity.EntityMyFlyingPet;
import de.Keyle.MyPet.skill.skills.BehaviorImpl;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@EntitySize(width = 0.6F, height = 0.6f)
public class EntityMyBee extends EntityMyFlyingPet {

	private static final EntityDataAccessor<Boolean> AGE_WATCHER = SynchedEntityData.defineId(EntityMyBee.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Byte> BEE_STATUS_WATCHER = SynchedEntityData.defineId(EntityMyBee.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Integer> ANGER_WATCHER = SynchedEntityData.defineId(EntityMyBee.class, EntityDataSerializers.INT);

	protected boolean isAngry = false;

	public EntityMyBee(Level world, MyPet myPet) {
		super(world, myPet);
	}

	/**
	 * Returns the sound that is played when the MyPet dies
	 */
	@Override
	protected String getMyPetDeathSound() {
		return "entity.bee.death";
	}

	/**
	 * Returns the sound that is played when the MyPet get hurt
	 */
	@Override
	protected String getHurtSound() {
		return "entity.bee.hurt";
	}

	@Override
	protected String getLivingSound() {
		return "entity.bee.pollinate";
	}

	@Override
	public float getSoundSpeed() {
		return super.getSoundSpeed() * 0.95F;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(AGE_WATCHER, false);
		builder.define(BEE_STATUS_WATCHER, (byte) 0);
		builder.define(ANGER_WATCHER, 0);
	}

	@Override
	public void updateVisuals() {
		this.getEntityData().set(AGE_WATCHER, getMyPet().isBaby());
		this.getEntityData().set(ANGER_WATCHER, (getMyPet().isAngry() || isAngry) ? 1 : 0);
		this.setBeeStatus(8, getMyPet().hasNectar());
		this.setBeeStatus(4, getMyPet().hasStung());
	}

	/**
	 * Possible status flags:
	 * 8: Nectar
	 * 4: Stung
	 * 2: ?
	 */
	private void setBeeStatus(int status, boolean flag) {
		if (flag) {
			this.entityData.set(BEE_STATUS_WATCHER, (byte) (this.entityData.get(BEE_STATUS_WATCHER) | status));
		} else {
			this.entityData.set(BEE_STATUS_WATCHER, (byte) (this.entityData.get(BEE_STATUS_WATCHER) & ~status));
		}

	}

	@Override
	public MyBee getMyPet() {
		return (MyBee) myPet;
	}

	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();
		if (Configuration.MyPet.Bee.CAN_GLIDE) {
			if (!this.onGround && this.getDeltaMovement().y() < 0.0D) {
				this.setDeltaMovement(getDeltaMovement().multiply(1, 0.6D, 1));
			}
		}
	}
	
	@Override
	public InteractionResult handlePlayerInteraction(final Player entityhuman, InteractionHand enumhand, final ItemStack itemStack) {
		if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).consumesAction()) {
			return InteractionResult.CONSUME;
		}

		if (getOwner().equals(entityhuman) && itemStack != null && canUseItem()) {
			if (Configuration.MyPet.Bee.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
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
	protected void doMyPetTick() {
		super.doMyPetTick();
		BehaviorImpl skill = getMyPet().getSkills().get(BehaviorImpl.class);
		Behavior.BehaviorMode behavior = skill.getBehavior();
		if (behavior == Behavior.BehaviorMode.Aggressive) {
			if (!isAngry) {
				isAngry = true;
				this.updateVisuals();
			}
		} else {
			if (isAngry) {
				isAngry = false;
				this.updateVisuals();
			}
		}
	}
}
