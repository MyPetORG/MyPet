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

package de.Keyle.MyPet.compat.v1_16_R3.entity.types;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyRabbit;
import de.Keyle.MyPet.compat.v1_16_R3.entity.EntityMyPet;
import net.minecraft.server.v1_16_R3.*;

@EntitySize(width = 0.6F, height = 0.7F)
public class EntityMyRabbit extends EntityMyPet {

    private static final DataWatcherObject<Boolean> AGE_WATCHER = DataWatcher.a(EntityMyRabbit.class, DataWatcherRegistry.i);
    private static final DataWatcherObject<Integer> VARIANT_WATCHER = DataWatcher.a(EntityMyRabbit.class, DataWatcherRegistry.b);

    int jumpDelay;

    public EntityMyRabbit(World world, MyPet myPet) {
        super(world, myPet);
        this.jumpDelay = (this.random.nextInt(20) + 10);
    }

    @Override
    protected String getDeathSound() {
        return "entity.rabbit.death";
    }

    @Override
    protected String getHurtSound() {
        return "entity.rabbit.hurt";
    }

    @Override
    protected String getLivingSound() {
        return "entity.rabbit.ambient";
    }

    @Override
    public void playPetStepSound() {
        makeSound("entity.rabbit.jump", 1.0F, 1.0F);
    }

    @Override
    public EnumInteractionResult handlePlayerInteraction(EntityHuman entityhuman, EnumHand enumhand, ItemStack itemStack) {
        if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).a()) {
            return EnumInteractionResult.CONSUME;
        }

        if (getOwner().equals(entityhuman) && itemStack != null && canUseItem()) {
            if (Configuration.MyPet.Rabbit.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
                if (itemStack != ItemStack.b && !entityhuman.abilities.canInstantlyBuild) {
                    itemStack.subtract(1);
                    if (itemStack.getCount() <= 0) {
                        entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, ItemStack.b);
                    }
                }
                this.getMyPet().setBaby(false);
                return EnumInteractionResult.CONSUME;
            }
        }
        return EnumInteractionResult.PASS;
    }

    @Override
    public void initDatawatcher() {
        super.initDatawatcher();
        getDataWatcher().register(AGE_WATCHER, false); // is baby
        getDataWatcher().register(VARIANT_WATCHER, 0); // variant
    }

    @Override
    public void updateVisuals() {
        getDataWatcher().set(AGE_WATCHER, getMyPet().isBaby());
        getDataWatcher().set(VARIANT_WATCHER, (int) getMyPet().getVariant().getId());
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();

        if (this.onGround && getNavigation().k() != null && jumpDelay-- <= 0) {
            getControllerJump().jump();
            jumpDelay = (this.random.nextInt(10) + 10);
            if (getMyPetTarget() != null) {
                jumpDelay /= 3;
            }
            this.world.broadcastEntityEffect(this, (byte) 1);
        }
    }

    @Override
    public MyRabbit getMyPet() {
        return (MyRabbit) myPet;
    }
}