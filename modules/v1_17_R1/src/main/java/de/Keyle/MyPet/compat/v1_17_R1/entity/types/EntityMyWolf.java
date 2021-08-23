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
import net.minecraft.core.particles.Particles;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.util.MathHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemDye;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;
import org.bukkit.DyeColor;

import java.util.Optional;
import java.util.UUID;

@EntitySize(width = 0.6F, height = 0.64f)
public class EntityMyWolf extends EntityMyPet {

	private static final EntityDataAccessor<Boolean> AGE_WATCHER = SynchedEntityData.defineId(EntityMyWolf.class, EntityDataSerializers.BOOLEAN);
	protected static final EntityDataAccessor<Byte> SIT_WATCHER = SynchedEntityData.defineId(EntityMyWolf.class, EntityDataSerializers.BYTE);
	protected static final EntityDataAccessor<Optional<UUID>> OWNER_WATCHER = SynchedEntityData.defineId(EntityMyWolf.class, EntityDataSerializers.o);
	private static final EntityDataAccessor<Float> TAIL_WATCHER = SynchedEntityData.defineId(EntityMyWolf.class, EntityDataSerializers.c);
	private static final EntityDataAccessor<Boolean> UNUSED_WATCHER = SynchedEntityData.defineId(EntityMyWolf.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> COLLAR_COLOR_WATCHER = SynchedEntityData.defineId(EntityMyWolf.class, EntityDataSerializers.INT);

	protected boolean shaking;
	protected boolean isWet;
	protected float shakeCounter;

	public EntityMyWolf(Level world, MyPet myPet) {
		super(world, myPet);
	}

	public void applySitting(boolean sitting) {
		int i = this.getSynchedEntityData().get(SIT_WATCHER);
		if (sitting) {
			this.getEntityData().set(SIT_WATCHER, (byte) (i | 0x1));
		} else {
			this.getEntityData().set(SIT_WATCHER, (byte) (i & 0xFFFFFFFE));
		}
	}

	@Override
	protected String getDeathSound() {
		return "entity.wolf.death";
	}

	@Override
	protected String getHurtSound() {
		return "entity.wolf.hurt";
	}

	@Override
	protected String getLivingSound() {
		return this.Q.nextInt(5) == 0 ? (getHealth() * 100 / getMaxHealth() <= 25 ? "entity.wolf.whine" : "entity.wolf.pant") : "entity.wolf.ambient";
	}

	@Override
	public InteractionResult handlePlayerInteraction(EntityHuman entityhuman, InteractionHand enumhand, ItemStack itemStack) {
		if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).a()) {
			return InteractionResult.CONSUME;
		}

		if (getOwner().equals(entityhuman)) {
			if (itemStack != null && itemStack.getItem() != Items.a) {
				if (canUseItem()) {
					if (itemStack.getItem() instanceof ItemDye && ((ItemDye) itemStack.getItem()).d().ordinal() != getMyPet().getCollarColor().ordinal()) {
						if (getOwner().getPlayer().isSneaking()) {
							getMyPet().setCollarColor(DyeColor.values()[((ItemDye) itemStack.getItem()).d().ordinal()]);
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
		getEntityData().define(TAIL_WATCHER, 30F);
		getEntityData().define(UNUSED_WATCHER, false); // not used
		getEntityData().define(COLLAR_COLOR_WATCHER, 14);
	}

	@Override
	protected void initAttributes() {
		getAttributeInstance(GenericAttributes.a).setValue(20.0D);
	}

	@Override
	public void updateVisuals() {
		this.getEntityData().set(AGE_WATCHER, getMyPet().isBaby());

		byte b0 = this.getSynchedEntityData().get(SIT_WATCHER);
		if (getMyPet().isTamed()) {
			this.getEntityData().set(SIT_WATCHER, (byte) (b0 | 0x4));
		} else {
			this.getEntityData().set(SIT_WATCHER, (byte) (b0 & 0xFFFFFFFB));
		}

		b0 = this.getSynchedEntityData().get(SIT_WATCHER);
		if (getMyPet().isAngry()) {
			this.getEntityData().set(SIT_WATCHER, (byte) (b0 | 0x2));
		} else {
			this.getEntityData().set(SIT_WATCHER, (byte) (b0 & 0xFFFFFFFD));
		}

		this.getEntityData().set(COLLAR_COLOR_WATCHER, getMyPet().getCollarColor().ordinal());
	}

	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();
		if (this.isWet && !this.shaking && this.z) {
			this.shaking = true;
			this.shakeCounter = 0.0F;
			this.t.broadcastEntityEffect(this, (byte) 8);
		}

		if (isInWater()) // -> is in water
		{
			this.isWet = true;
			this.shaking = false;
			this.shakeCounter = 0.0F;
		} else if ((this.isWet || this.shaking) && this.shaking) {
			if (this.shakeCounter == 0.0F) {
				makeSound("entity.wolf.shake", 1.0F, (this.Q.nextFloat() - this.Q.nextFloat()) * 0.2F + 1.0F);
			}

			this.shakeCounter += 0.05F;
			if (this.shakeCounter - 0.05F >= 2.0F) {
				this.isWet = false;
				this.shaking = false;
				this.shakeCounter = 0.0F;
			}

			if (this.shakeCounter > 0.4F) {
				int i = (int) (MathHelper.sin((this.shakeCounter - 0.4F) * 3.141593F) * 7.0F);
				for (; i >= 0; i--) {
					float offsetX = (this.Q.nextFloat() * 2.0F - 1.0F) * this.getWidth() * 0.5F;
					float offsetZ = (this.Q.nextFloat() * 2.0F - 1.0F) * this.getWidth() * 0.5F;

					this.t.addParticle(Particles.ac, this.locX() + offsetX, this.locY() + 0.8F, this.locZ() + offsetZ, this.getMot().getX(), this.getMot().getY(), this.getMot().getZ());
				}
			}
		}

		float tailHeight = 30F * (getHealth() / getMaxHealth());
		if (this.getSynchedEntityData().get(TAIL_WATCHER) != tailHeight) {
			this.getEntityData().set(TAIL_WATCHER, tailHeight); // update tail height
		}
	}

	@Override
	public void playPetStepSound() {
		makeSound("entity.wolf.step", 0.15F, 1.0F);
	}

	@Override
	public void setHealth(float i) {
		super.setHealth(i);

		float tailHeight = 30F * (i / getMaxHealth());
		this.getEntityData().set(TAIL_WATCHER, tailHeight);
	}

	@Override
	public MyWolf getMyPet() {
		return (MyWolf) myPet;
	}
}
