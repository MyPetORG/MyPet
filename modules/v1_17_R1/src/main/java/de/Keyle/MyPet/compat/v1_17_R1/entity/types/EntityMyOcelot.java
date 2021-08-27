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
import de.Keyle.MyPet.api.entity.types.MyOcelot;
import de.Keyle.MyPet.compat.v1_17_R1.entity.EntityMyPet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@EntitySize(width = 0.6F, height = 0.8F)
public class EntityMyOcelot extends EntityMyPet {

	private static final EntityDataAccessor<Boolean> TRUSTING_WATCHER = SynchedEntityData.defineId(EntityMyOcelot.class, EntityDataSerializers.BOOLEAN);

	public EntityMyOcelot(Level world, MyPet myPet) {
		super(world, myPet);
	}

	@Override
	protected String getMyPetDeathSound() {
		return "entity.ocelot.death";
	}

	@Override
	protected String getHurtSound() {
		return "entity.ocelot.hurt";
	}

	@Override
	protected String getLivingSound() {
		return "entity.ocelot.ambient";
	}

	@Override
	public InteractionResult handlePlayerInteraction(Player entityhuman, InteractionHand enumhand, ItemStack itemStack) {
		if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).consumesAction()) {
			return InteractionResult.CONSUME;
		}

		if (getOwner().equals(entityhuman)) {
			if (itemStack != null && canUseItem() && getOwner().getPlayer().isSneaking()) {
				if (Configuration.MyPet.Ocelot.GROW_UP_ITEM.compare(itemStack) && canUseItem() && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
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
		return InteractionResult.CONSUME;
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		getEntityData().define(TRUSTING_WATCHER, false);
	}

	@Override
	public void updateVisuals() {
		if(getMyPet().isBaby()) {
			this.setAge(-1);
		} else {
			this.setAge(1);
		}
	}

	@Override
	public MyOcelot getMyPet() {
		return (MyOcelot) myPet;
	}
}
