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
import de.Keyle.MyPet.api.entity.types.MyPanda;
import de.Keyle.MyPet.compat.v1_19_R1.entity.EntityMyPet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@EntitySize(width = 1.825F, height = 1.25F)
public class EntityMyPanda extends EntityMyPet {

	private static final EntityDataAccessor<Boolean> AGE_WATCHER = SynchedEntityData.defineId(EntityMyPanda.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> ASK_FOR_BAMBOO_TICKS_WATCHER = SynchedEntityData.defineId(EntityMyPanda.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> SNEEZE_PROGRESS_WATCHER = SynchedEntityData.defineId(EntityMyPanda.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> EATING_TICKS_WATCHER = SynchedEntityData.defineId(EntityMyPanda.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Byte> MAIN_GENE_WATCHER = SynchedEntityData.defineId(EntityMyPanda.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Byte> HIDDEN_GENE_WATCHER = SynchedEntityData.defineId(EntityMyPanda.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Byte> ACTIONS_WATCHER = SynchedEntityData.defineId(EntityMyPanda.class, EntityDataSerializers.BYTE);

	public EntityMyPanda(Level world, MyPet myPet) {
		super(world, myPet);
	}

	@Override
	protected String getMyPetDeathSound() {
		return "entity.panda.death";
	}

	@Override
	protected String getHurtSound() {
		return "entity.panda.hurt";
	}

	@Override
	protected String getLivingSound() {
		return "entity.panda.ambient";
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
		return InteractionResult.PASS;
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		getEntityData().define(AGE_WATCHER, false);
		getEntityData().define(ASK_FOR_BAMBOO_TICKS_WATCHER, 0);
		getEntityData().define(SNEEZE_PROGRESS_WATCHER, 0);
		getEntityData().define(MAIN_GENE_WATCHER, (byte) 0);
		getEntityData().define(HIDDEN_GENE_WATCHER, (byte) 0);
		getEntityData().define(ACTIONS_WATCHER, (byte) 0);
		getEntityData().define(EATING_TICKS_WATCHER, 0);
	}

	@Override
	public void updateVisuals() {
		this.getEntityData().set(AGE_WATCHER, getMyPet().isBaby());
		this.getEntityData().set(MAIN_GENE_WATCHER, (byte) getMyPet().getMainGene().ordinal());
		this.getEntityData().set(HIDDEN_GENE_WATCHER, (byte) getMyPet().getHiddenGene().ordinal());
	}

	/*
	 *  1   =
	 *  2   =
	 *  4   =  roll foward
	 *  8   =  sitting
	 */
	public void updateActionsWatcher(int i, boolean flag) {
		if (flag) {
			this.entityData.set(ACTIONS_WATCHER, (byte) (this.entityData.get(ACTIONS_WATCHER) | i));
		} else {
			this.entityData.set(ACTIONS_WATCHER, (byte) (this.entityData.get(ACTIONS_WATCHER) & ~i));
		}
	}

	@Override
	public MyPanda getMyPet() {
		return (MyPanda) myPet;
	}
}
