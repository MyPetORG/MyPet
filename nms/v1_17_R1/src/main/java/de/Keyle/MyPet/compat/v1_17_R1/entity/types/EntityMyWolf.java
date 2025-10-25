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
import de.Keyle.MyPet.api.entity.types.MyWolf;
import de.Keyle.MyPet.compat.v1_17_R1.entity.EntityMyPet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.bukkit.DyeColor;

import java.util.Optional;
import java.util.UUID;

@EntitySize(width = 0.6F, height = 0.64f)
public class EntityMyWolf extends EntityMyPet {

	private static final EntityDataAccessor<Boolean> AGE_WATCHER = SynchedEntityData.defineId(EntityMyWolf.class, EntityDataSerializers.BOOLEAN);
	protected static final EntityDataAccessor<Byte> SIT_WATCHER = SynchedEntityData.defineId(EntityMyWolf.class, EntityDataSerializers.BYTE);
	protected static final EntityDataAccessor<Optional<UUID>> OWNER_WATCHER = SynchedEntityData.defineId(EntityMyWolf.class, EntityDataSerializers.OPTIONAL_UUID);
	private static final EntityDataAccessor<Boolean> UNUSED_WATCHER = SynchedEntityData.defineId(EntityMyWolf.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> COLLAR_COLOR_WATCHER = SynchedEntityData.defineId(EntityMyWolf.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> ANGER_WATCHER = SynchedEntityData.defineId(EntityMyWolf.class, EntityDataSerializers.INT);

	protected boolean shaking;
	protected boolean isWet;
	protected float shakeCounter;

	public EntityMyWolf(Level world, MyPet myPet) {
		super(world, myPet);
	}

	public void applySitting(boolean sitting) {
		int i = this.getEntityData().get(SIT_WATCHER);
		if (sitting) {
			this.getEntityData().set(SIT_WATCHER, (byte) (i | 0x1));
		} else {
			this.getEntityData().set(SIT_WATCHER, (byte) (i & 0xFFFFFFFE));
		}
	}

	@Override
	protected String getMyPetDeathSound() {
		return "entity.wolf.death";
	}

	@Override
	protected String getHurtSound() {
		return "entity.wolf.hurt";
	}

	@Override
	protected String getLivingSound() {
		return this.random.nextInt(5) == 0 ? (getHealth() * 100 / getMaxHealth() <= 25 ? "entity.wolf.whine" : "entity.wolf.pant") : "entity.wolf.ambient";
	}

	@Override
	public InteractionResult handlePlayerInteraction(Player entityhuman, InteractionHand enumhand, ItemStack itemStack) {
		if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).consumesAction()) {
			return InteractionResult.CONSUME;
		}

		if (getOwner().equals(entityhuman)) {
			if (itemStack != null && itemStack.getItem() != Items.AIR) {
				if (canUseItem()) {
					if (itemStack.getItem() instanceof DyeItem && ((DyeItem) itemStack.getItem()).getDyeColor().ordinal() != getMyPet().getCollarColor().ordinal()) {
						if (getOwner().getPlayer().isSneaking()) {
							getMyPet().setCollarColor(DyeColor.values()[((DyeItem) itemStack.getItem()).getDyeColor().ordinal()]);
							if (itemStack != ItemStack.EMPTY && !entityhuman.getAbilities().instabuild) {
								itemStack.shrink(1);
								if (itemStack.getCount() <= 0) {
									entityhuman.getInventory().setItem(entityhuman.getInventory().selected, ItemStack.EMPTY);
								}
							}
							return InteractionResult.CONSUME;
						} else {
							this.getEntityData().set(COLLAR_COLOR_WATCHER, 0);
							updateVisuals();
						}
					} else if (Configuration.MyPet.Wolf.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
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
		}
		return InteractionResult.PASS;
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();

		getEntityData().define(AGE_WATCHER, false);
		getEntityData().define(SIT_WATCHER, (byte) 0);
		getEntityData().define(OWNER_WATCHER, Optional.empty());
		getEntityData().define(UNUSED_WATCHER, false); // not used
		getEntityData().define(COLLAR_COLOR_WATCHER, 14);
		getEntityData().define(ANGER_WATCHER, 0);
	}

	@Override
	protected void initAttributes() {
		getAttribute(Attributes.MAX_HEALTH).setBaseValue(20.0D);
	}

	@Override
	public void updateVisuals() {
		this.getEntityData().set(AGE_WATCHER, getMyPet().isBaby());

		byte b0 = this.getEntityData().get(SIT_WATCHER);
		if (getMyPet().isTamed()) {
			this.getEntityData().set(SIT_WATCHER, (byte) (b0 | 0x4));
		} else {
			this.getEntityData().set(SIT_WATCHER, (byte) (b0 & 0xFFFFFFFB));
		}

		if (getMyPet().isAngry()) {
			this.getEntityData().set(ANGER_WATCHER, 1);
		} else {
			this.getEntityData().set(ANGER_WATCHER, 0);
		}

		this.getEntityData().set(COLLAR_COLOR_WATCHER, getMyPet().getCollarColor().ordinal());
	}

	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();
		if (this.isWet && !this.shaking && this.onGround) {
			this.shaking = true;
			this.shakeCounter = 0.0F;
			this.level.broadcastEntityEvent(this, (byte) 8);
		}

		if (isInWater()) // -> is in water
		{
			this.isWet = true;
			this.shaking = false;
			this.shakeCounter = 0.0F;
		} else if ((this.isWet || this.shaking) && this.shaking) {
			if (this.shakeCounter == 0.0F) {
				makeSound("entity.wolf.shake", 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
			}

			this.shakeCounter += 0.05F;
			if (this.shakeCounter - 0.05F >= 2.0F) {
				this.isWet = false;
				this.shaking = false;
				this.shakeCounter = 0.0F;
			}

			if (this.shakeCounter > 0.4F) {
				int i = (int) (Mth.sin((this.shakeCounter - 0.4F) * 3.141593F) * 7.0F);
				for (; i >= 0; i--) {
					float offsetX = (this.random.nextFloat() * 2.0F - 1.0F) * this.getBbWidth() * 0.5F;
					float offsetZ = (this.random.nextFloat() * 2.0F - 1.0F) * this.getBbWidth() * 0.5F;

					this.level.addParticle(ParticleTypes.SPLASH, this.getX() + offsetX, this.getY() + 0.8F, this.getZ() + offsetZ, this.getDeltaMovement().x(), this.getDeltaMovement().y(), this.getDeltaMovement().z());
				}
			}
		}
	}

	@Override
	public void playPetStepSound() {
		makeSound("entity.wolf.step", 0.15F, 1.0F);
	}

	@Override
	public MyWolf getMyPet() {
		return (MyWolf) myPet;
	}
}
