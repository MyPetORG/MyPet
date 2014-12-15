/*
 * This file is part of MyPet-1.8
 *
 * Copyright (C) 2011-2014 Keyle
 * MyPet-1.8 is licensed under the GNU Lesser General Public License.
 *
 * MyPet-1.8 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyPet-1.8 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.entity.types.rabbit;

import de.Keyle.MyPet.entity.EntitySize;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.pig.MyPig;
import net.minecraft.server.v1_8_R1.EntityHuman;
import net.minecraft.server.v1_8_R1.ItemStack;
import net.minecraft.server.v1_8_R1.World;

@EntitySize(width = 0.6F, length = 0.7F, height = 0.7F)
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
            if (MyPig.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
                if (!entityhuman.abilities.canInstantlyBuild) {
                    if (--itemStack.count <= 0) {
                        entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                    }
                }
                this.setBaby(false);
                return true;
            }
        }
        return false;
    }

    @Override
    public void initDatawatcher() {
        super.initDatawatcher();
        this.datawatcher.a(12, Byte.valueOf((byte) 0)); // age
        this.datawatcher.a(18, Byte.valueOf((byte) 0)); // variant
    }

    public void setBaby(boolean flag) {
        if (flag) {
            this.datawatcher.watch(12, Byte.valueOf(Byte.MIN_VALUE));
        } else {
            this.datawatcher.watch(12, new Byte((byte) 0));
        }
    }

    public void setVariant(byte variation) {
        this.datawatcher.watch(18, variation);
    }

    public void onLivingUpdate() {
        super.onLivingUpdate();

        if (this.onGround && getNavigation().j() != null && jumpDelay-- <= 0) {
            getControllerJump().a();
            jumpDelay = (this.random.nextInt(10) + 10);
            if (getGoalTarget() != null) {
                jumpDelay /= 3;
            }
            this.world.broadcastEntityEffect(this, (byte) 1);
        }
    }

    public void setMyPet(MyPet myPet) {
        if (myPet != null) {
            super.setMyPet(myPet);

            this.setVariant(getMyPet().getVariant());
            this.setBaby(getMyPet().isBaby());
        }
    }

    public MyRabbit getMyPet() {
        return (MyRabbit) myPet;
    }
}