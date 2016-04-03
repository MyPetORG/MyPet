/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2016 Keyle
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

package de.Keyle.MyPet.compat.v1_8_R3.entity.types;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyRabbit;
import de.Keyle.MyPet.compat.v1_8_R3.entity.EntityMyPet;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.ItemStack;
import net.minecraft.server.v1_8_R3.World;

@EntitySize(width = 0.6F, height = 0.7F)
public class EntityMyRabbit extends EntityMyPet {
    int jumpDelay;

    public EntityMyRabbit(World world, MyPet myPet) {
        super(world, myPet);
        this.jumpDelay = (this.random.nextInt(20) + 10);
    }

    @Override
    protected String getDeathSound() {
        return "mob.rabbit.death";
    }

    @Override
    protected String getHurtSound() {
        return "mob.rabbit.hurt";
    }

    protected String getLivingSound() {
        return "mob.rabbit.idle";
    }

    @Override
    public void playStepSound() {
        makeSound("mob.rabbit.hop", 1.0F, 1.0F);
    }

    public boolean handlePlayerInteraction(EntityHuman entityhuman) {
        if (super.handlePlayerInteraction(entityhuman)) {
            return true;
        }

        ItemStack itemStack = entityhuman.inventory.getItemInHand();

        if (getOwner().equals(entityhuman) && itemStack != null && canUseItem()) {
            if (Configuration.MyPet.Rabbit.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
                if (!entityhuman.abilities.canInstantlyBuild) {
                    if (--itemStack.count <= 0) {
                        entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                    }
                }
                this.getMyPet().setBaby(false);
                return true;
            }
        }
        return false;
    }

    @Override
    public void initDatawatcher() {
        super.initDatawatcher();
        this.datawatcher.a(12, (byte) 0); // age
        this.datawatcher.a(18, (byte) 0); // variant
    }

    @Override
    public void updateVisuals() {
        if (getMyPet().isBaby()) {
            this.datawatcher.watch(12, Byte.MIN_VALUE);
        } else {
            this.datawatcher.watch(12, (byte) 0);
        }
        this.datawatcher.watch(18, getMyPet().getVariant().getId());
    }

    public void onLivingUpdate() {
        super.onLivingUpdate();

        if (this.onGround && getNavigation().j() != null && jumpDelay-- <= 0) {
            getControllerJump().a();
            jumpDelay = (this.random.nextInt(10) + 10);
            if (getTarget() != null) {
                jumpDelay /= 3;
            }
            this.world.broadcastEntityEffect(this, (byte) 1);
        }
    }

    public MyRabbit getMyPet() {
        return (MyRabbit) myPet;
    }
}