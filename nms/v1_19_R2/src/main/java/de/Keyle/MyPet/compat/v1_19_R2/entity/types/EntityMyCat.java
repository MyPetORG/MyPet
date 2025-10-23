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

package de.Keyle.MyPet.compat.v1_19_R2.entity.types;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyCat;
import de.Keyle.MyPet.compat.v1_19_R2.entity.EntityMyPet;
import de.Keyle.MyPet.compat.v1_19_R2.util.VariantConverter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.CatVariant;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.bukkit.DyeColor;

import java.util.Optional;
import java.util.UUID;

@EntitySize(width = 0.6F, height = 0.8F)
public class EntityMyCat extends EntityMyPet {

	protected static final EntityDataAccessor<Boolean> AGE_WATCHER = SynchedEntityData.defineId(EntityMyCat.class, EntityDataSerializers.BOOLEAN);
	protected static final EntityDataAccessor<Byte> SIT_WATCHER = SynchedEntityData.defineId(EntityMyCat.class, EntityDataSerializers.BYTE);
	protected static final EntityDataAccessor<Optional<UUID>> OWNER_WATCHER = SynchedEntityData.defineId(EntityMyCat.class, EntityDataSerializers.OPTIONAL_UUID);
	protected static final EntityDataAccessor<CatVariant> VARIANT_WATCHER = SynchedEntityData.defineId(EntityMyCat.class, EntityDataSerializers.CAT_VARIANT);
	protected static final EntityDataAccessor<Boolean> UNUSED_WATCHER_1 = SynchedEntityData.defineId(EntityMyCat.class, EntityDataSerializers.BOOLEAN);
	protected static final EntityDataAccessor<Boolean> UNUSED_WATCHER_2 = SynchedEntityData.defineId(EntityMyCat.class, EntityDataSerializers.BOOLEAN);
	protected static final EntityDataAccessor<Integer> COLLAR_COLOR_WATCHER = SynchedEntityData.defineId(EntityMyCat.class, EntityDataSerializers.INT);

	public EntityMyCat(Level world, MyPet myPet) {
		super(world, myPet);
	}

	public void applySitting(boolean sitting) {
		byte i = this.getEntityData().get(SIT_WATCHER);
		if (sitting) {
			this.getEntityData().set(SIT_WATCHER, (byte) (i | 1));
		} else {
			this.getEntityData().set(SIT_WATCHER, (byte) (i & -2));
		}
	}

	@Override
	protected String getMyPetDeathSound() {
		return "entity.cat.death";
	}

	@Override
	protected String getHurtSound() {
		return "entity.cat.hurt";
	}

	@Override
	protected String getLivingSound() {
		return this.random.nextInt(4) == 0 ? "entity.cat.purr" : "entity.cat.ambient";
	}

	@Override
	public InteractionResult handlePlayerInteraction(Player entityhuman, InteractionHand enumhand, ItemStack itemStack) {
		if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).consumesAction()) {
			return InteractionResult.CONSUME;
		}

		if (getOwner().equals(entityhuman)) {
			if (itemStack != null && canUseItem() && getOwner().getPlayer().isSneaking()) {
				if (itemStack.getItem() instanceof DyeItem) {
					if (((DyeItem) itemStack.getItem()).getDyeColor().getId() != getMyPet().getCollarColor().ordinal()) {
						getMyPet().setCollarColor(DyeColor.values()[((DyeItem) itemStack.getItem()).getDyeColor().getId()]);
						if (itemStack != ItemStack.EMPTY && !entityhuman.getAbilities().instabuild) {
							itemStack.shrink(1);
							if (itemStack.getCount() <= 0) {
								entityhuman.getInventory().setItem(entityhuman.getInventory().selected, ItemStack.EMPTY);
							}
						}
						return InteractionResult.SUCCESS;
					}
				} else if (Configuration.MyPet.Ocelot.GROW_UP_ITEM.compare(itemStack) && canUseItem() && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
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
		return InteractionResult.PASS;
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		getEntityData().define(AGE_WATCHER, false);
		getEntityData().define(SIT_WATCHER, (byte) 0);
		getEntityData().define(OWNER_WATCHER, Optional.empty());
		getEntityData().define(VARIANT_WATCHER, (CatVariant) BuiltInRegistries.CAT_VARIANT.getOrThrow(CatVariant.BLACK));
		getEntityData().define(UNUSED_WATCHER_1, false);
		getEntityData().define(UNUSED_WATCHER_2, false);
		getEntityData().define(COLLAR_COLOR_WATCHER, 14);
	}

	@Override
	public void updateVisuals() {
		this.getEntityData().set(AGE_WATCHER, getMyPet().isBaby());
		this.getEntityData().set(VARIANT_WATCHER, VariantConverter.convertCatVariant(getMyPet().getCatType().ordinal()));
		this.getEntityData().set(COLLAR_COLOR_WATCHER, getMyPet().getCollarColor().ordinal());

		byte b0 = this.getEntityData().get(SIT_WATCHER);
		if (getMyPet().isTamed()) {
			this.getEntityData().set(SIT_WATCHER, (byte) (b0 | 0x4));
		} else {
			this.getEntityData().set(SIT_WATCHER, (byte) (b0 & 0xFFFFFFFB));
		}
	}

	@Override
	public MyCat getMyPet() {
		return (MyCat) myPet;
	}
}
