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
import de.Keyle.MyPet.api.entity.types.MyPiglin;
import de.Keyle.MyPet.compat.v1_21_R1.entity.EntityMyPet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

@EntitySize(width = 0.6F, height = 1.9F)
public class EntityMyPiglin extends EntityMyPet {

    //Funnily enough this one hasn't got the age_watcher before the No_Shake
    private static final EntityDataAccessor<Boolean> NO_SHAKE_WATCHER = SynchedEntityData.defineId(EntityMyPiglin.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> AGE_WATCHER = SynchedEntityData.defineId(EntityMyPiglin.class, EntityDataSerializers.BOOLEAN);

    public EntityMyPiglin(Level world, MyPet myPet) {
        super(world, myPet);
    }

    @Override
    protected String getMyPetDeathSound() {
        return "entity.piglin.death";
    }

    @Override
    protected String getHurtSound() {
        return "entity.piglin.hurt";
    }

    @Override
    protected String getLivingSound() {
        return "entity.piglin.ambient";
    }

    @Override
    public InteractionResult handlePlayerInteraction(Player entityhuman, InteractionHand enumhand, ItemStack itemStack) {
        if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).consumesAction()) {
            return InteractionResult.CONSUME;
        }

        if (getOwner().equals(entityhuman) && itemStack != null && itemStack.getItem() != Items.AIR) {
            if (Configuration.MyPet.Piglin.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
                if (!entityhuman.getAbilities().instabuild) {
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
        builder.define(NO_SHAKE_WATCHER, false);
        builder.define(AGE_WATCHER, false); // is baby
    }

    /**
     * Returns the speed of played sounds
     * The faster the higher the sound will be
     */
    @Override
    public float getSoundSpeed() {
        return super.getSoundSpeed() + 0.4F;
    }

    @Override
    public void updateVisuals() {
        this.getEntityData().set(AGE_WATCHER, getMyPet().isBaby());
        this.getEntityData().set(NO_SHAKE_WATCHER, getMyPet().isShakeImmune());
        super.updateVisuals();
    }

    @Override
    public MyPiglin getMyPet() {
        return (MyPiglin) myPet;
    }

    @Override
    protected boolean checkInteractCooldown() {
        boolean val = super.checkInteractCooldown();
        this.interactCooldown = 5;
        return val;
    }
}
