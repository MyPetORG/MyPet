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
import de.Keyle.MyPet.api.entity.types.MyArmadillo;
import de.Keyle.MyPet.compat.v1_21_R1.entity.EntityMyPet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@EntitySize(width = 0.7F, height = 0.65F)
public class EntityMyArmadillo extends EntityMyPet {

    private static final EntityDataAccessor<Boolean> AGE_WATCHER = SynchedEntityData.defineId(EntityMyArmadillo.class, EntityDataSerializers.BOOLEAN);
    // TODO use this :)
    private static final EntityDataAccessor<Armadillo.ArmadilloState> ARMADILLO_STATE_WATCHER = SynchedEntityData.defineId(EntityMyArmadillo.class, EntityDataSerializers.ARMADILLO_STATE);

    public EntityMyArmadillo(Level world, MyPet myPet) {
        super(world, myPet);
    }

    @Override
    protected String getLivingSound() {
        return "entity.armadillo.ambient";
    }

    @Override
    protected String getHurtSound() {
        return "entity.armadillo.hurt";
    }

    @Override
    protected String getMyPetDeathSound() {
        return "entity.armadillo.death";
    }

    @Override
    public InteractionResult handlePlayerInteraction(Player entityhuman, InteractionHand enumhand, ItemStack itemStack) {
        if (Configuration.MyPet.Armadillo.GROW_UP_ITEM.compare(itemStack) && ((MyArmadillo)getMyPet()).isBaby() && getOwner().getPlayer().isSneaking()) {
            if (itemStack != ItemStack.EMPTY && !entityhuman.getAbilities().instabuild) {
                itemStack.shrink(1);
                if (itemStack.getCount() <= 0) {
                    entityhuman.getInventory().setItem(entityhuman.getInventory().selected, ItemStack.EMPTY);
                }
            }
            ((MyArmadillo)getMyPet()).setBaby(false);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(AGE_WATCHER, false);
        builder.define(ARMADILLO_STATE_WATCHER, Armadillo.ArmadilloState.IDLE);
    }

    @Override
    public void playPetStepSound() {
        makeSound("entity.armadillo.step", 0.15F, 1.0F);
    }


    @Override
    public void updateVisuals() {
        this.getEntityData().set(AGE_WATCHER, ((MyArmadillo)getMyPet()).isBaby());
    }

    @Override
    public MyArmadillo getMyPet() {
        return (MyArmadillo) myPet;
    }
}