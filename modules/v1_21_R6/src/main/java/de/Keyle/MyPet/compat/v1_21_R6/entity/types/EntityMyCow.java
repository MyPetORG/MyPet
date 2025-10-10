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

package de.Keyle.MyPet.compat.v1_21_R6.entity.types;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyCow;
import de.Keyle.MyPet.compat.v1_21_R6.entity.EntityMyPet;
import de.Keyle.MyPet.compat.v1_21_R6.util.VariantConverter;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.CowVariant;
import net.minecraft.world.entity.animal.CowVariants;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_21_R6.CraftRegistry;

@EntitySize(width = 0.7F, height = 1.3F)
public class EntityMyCow extends EntityMyPet {

	private static final EntityDataAccessor<Boolean> AGE_WATCHER = SynchedEntityData.defineId(EntityMyCow.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Holder<CowVariant>> VARIANT_WATCHER = SynchedEntityData.defineId(EntityMyCow.class, EntityDataSerializers.COW_VARIANT);

	public EntityMyCow(Level world, MyPet myPet) {
		super(world, myPet);
	}

	@Override
	protected String getMyPetDeathSound() {
		return "entity.cow.death";
	}

	@Override
	protected String getHurtSound() {
		return "entity.cow.hurt";
	}

	@Override
	protected String getLivingSound() {
		return "entity.cow.ambient";
	}

	@Override
	public InteractionResult handlePlayerInteraction(Player entityhuman, InteractionHand enumhand, ItemStack itemStack) {
		if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).consumesAction()) {
			return InteractionResult.CONSUME;
		}

		if (getOwner().equals(entityhuman) && itemStack != null && canUseItem()) {
			if (itemStack.getItem() == Items.BUCKET && Configuration.MyPet.Cow.CAN_GIVE_MILK) {
				ItemStack milkBucket = new ItemStack(Items.MILK_BUCKET);
				itemStack.shrink(1);
				if (itemStack.getCount() <= 0) {
					entityhuman.getInventory().setItem(entityhuman.getInventory().getSelectedSlot(), milkBucket);
				} else {
					if(!entityhuman.getInventory().add(milkBucket)) {
						entityhuman.drop(milkBucket, true);
					}
				}
				return InteractionResult.CONSUME;
			} else if (Configuration.MyPet.Cow.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
				if (itemStack != ItemStack.EMPTY && !entityhuman.getAbilities().instabuild) {
					itemStack.shrink(1);
					if (itemStack.getCount() <= 0) {
						entityhuman.getInventory().setItem(entityhuman.getInventory().getSelectedSlot(), ItemStack.EMPTY);
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

		Registry<CowVariant> registry = CraftRegistry.getMinecraftRegistry(Registries.COW_VARIANT);
		builder.define(VARIANT_WATCHER, registry.wrapAsHolder(VariantConverter.COW_REGISTRY.getOrThrow(CowVariants.TEMPERATE).value()));
	}

	@Override
	public void updateVisuals() {
		this.getEntityData().set(AGE_WATCHER, getMyPet().isBaby());

		Registry<CowVariant> registry = CraftRegistry.getMinecraftRegistry(Registries.COW_VARIANT);
		this.getEntityData().set(VARIANT_WATCHER, registry.wrapAsHolder(VariantConverter.convertCowVariant(getMyPet().getVariant())));
	}

	@Override
	public void playPetStepSound() {
		makeSound("entity.cow.step", 0.15F, 1.0F);
	}

	@Override
	public MyCow getMyPet() {
		return (MyCow) myPet;
	}
}