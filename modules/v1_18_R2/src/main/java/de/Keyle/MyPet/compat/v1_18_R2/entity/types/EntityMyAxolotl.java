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

package de.Keyle.MyPet.compat.v1_18_R2.entity.types;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyAxolotl;
import de.Keyle.MyPet.compat.v1_18_R2.entity.EntityMyAquaticPet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@EntitySize(width = 0.7F, height = 1.3F)
public class EntityMyAxolotl extends EntityMyAquaticPet {

    private static final EntityDataAccessor<Boolean> AGE_WATCHER = SynchedEntityData.defineId(EntityMyAxolotl.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> VARIANT_WATCHER = SynchedEntityData.defineId(EntityMyAxolotl.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> PLAYING_DEAD_WATCHER = SynchedEntityData.defineId(EntityMyAxolotl.class, EntityDataSerializers.BOOLEAN);

    public EntityMyAxolotl(Level world, MyPet myPet) {
        super(world, myPet);
    }

    @Override
    protected String getMyPetDeathSound() {
        return "entity.axolotl.death";
    }

    @Override
    protected String getHurtSound() {
        return "entity.axolotl.hurt";
    }

    @Override
    protected String getLivingSound() {
        return "entity.axolotl.idle_air";
    }
    
    @Override
	public InteractionResult handlePlayerInteraction(final Player entityhuman, InteractionHand enumhand, final ItemStack itemStack) {
		if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).consumesAction()) {
			return InteractionResult.CONSUME;
		}

		if (getOwner().equals(entityhuman) && itemStack != null && canUseItem()) {
			if (Configuration.MyPet.Hoglin.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
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
        getEntityData().define(VARIANT_WATCHER, 0);
        getEntityData().define(PLAYING_DEAD_WATCHER, false);
    }

    @Override
    public void tick() {
        super.tick();
        if(this.getDeltaMovement().x() <= 0.0 && this.getDeltaMovement().y() <= 0.0 && this.getDeltaMovement().z() <= 0.0) {
            this.getEntityData().set(PLAYING_DEAD_WATCHER, getMyPet().getHealth() <= 1);
        } else if(this.getEntityData().get(PLAYING_DEAD_WATCHER).booleanValue()) {
            this.getEntityData().set(PLAYING_DEAD_WATCHER, false);
        }
    }

    @Override
    public void updateVisuals() {
        this.getEntityData().set(AGE_WATCHER, getMyPet().isBaby());
        this.getEntityData().set(VARIANT_WATCHER, getMyPet().getVariant());
    }

    @Override
    public void playPetStepSound() {
        makeSound("entity.axolotl.splash", 0.15F, 1.0F);
    }

    @Override
    public MyAxolotl getMyPet() {
        return (MyAxolotl) myPet;
    }
}
