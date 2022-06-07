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

package de.Keyle.MyPet.compat.v1_19_R1.entity.types;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyTurtle;
import de.Keyle.MyPet.compat.v1_19_R1.entity.EntityMyAquaticPet;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@EntitySize(width = 1.2F, height = 0.4F)
public class EntityMyTurtle extends EntityMyAquaticPet {

	private static final EntityDataAccessor<Boolean> AGE_WATCHER = SynchedEntityData.defineId(EntityMyTurtle.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<BlockPos> HOME_WATCHER = SynchedEntityData.defineId(EntityMyTurtle.class, EntityDataSerializers.BLOCK_POS);
	private static final EntityDataAccessor<Boolean> HAS_EGG_WATCHER = SynchedEntityData.defineId(EntityMyTurtle.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> UNUSED_WATCHER_1 = SynchedEntityData.defineId(EntityMyTurtle.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<BlockPos> TRAVEL_POS_WATCHER = SynchedEntityData.defineId(EntityMyTurtle.class, EntityDataSerializers.BLOCK_POS);
	private static final EntityDataAccessor<Boolean> UNUSED_WATCHER_2 = SynchedEntityData.defineId(EntityMyTurtle.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> UNUSED_WATCHER_3 = SynchedEntityData.defineId(EntityMyTurtle.class, EntityDataSerializers.BOOLEAN);

	public EntityMyTurtle(Level world, MyPet myPet) {
		super(world, myPet);
	}

	@Override
	protected String getMyPetDeathSound() {
		return "entity.turtle.death" + (isBaby() ? "_baby" : "");
	}

	@Override
	protected String getHurtSound() {
		return "entity.turtle.hurt" + (isBaby() ? "_baby" : "");
	}

	@Override
	protected String getLivingSound() {
		return "entity.turtle.ambient_land";
	}

	@Override
	public InteractionResult handlePlayerInteraction(Player entityhuman, InteractionHand enumhand, ItemStack itemStack) {
		if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).consumesAction()) {
			return InteractionResult.CONSUME;
		}

		if (getOwner().equals(entityhuman) && itemStack != null && canUseItem()) {
			if (Configuration.MyPet.Cow.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
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
	protected void defineSynchedData() {
		super.defineSynchedData();
		getEntityData().define(AGE_WATCHER, false);
		getEntityData().define(HOME_WATCHER, BlockPos.ZERO);
		getEntityData().define(HAS_EGG_WATCHER, false);
		getEntityData().define(TRAVEL_POS_WATCHER, BlockPos.ZERO);
		getEntityData().define(UNUSED_WATCHER_2, false);
		getEntityData().define(UNUSED_WATCHER_3, false);
		getEntityData().define(UNUSED_WATCHER_1, false);
	}

	@Override
	public void updateVisuals() {
		this.getEntityData().set(AGE_WATCHER, getMyPet().isBaby());
	}

	@Override
	public MyTurtle getMyPet() {
		return (MyTurtle) myPet;
	}
}
