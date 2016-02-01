/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2016 Keyle
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

package de.Keyle.MyPet.entity.types.blaze;

import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.ItemStack;
import net.minecraft.server.v1_8_R3.Items;
import net.minecraft.server.v1_8_R3.World;

@EntitySize(width = 0.6F, height = 1.7F)
public class EntityMyBlaze extends EntityMyPet {
    public EntityMyBlaze(World world, MyPet myPet) {
        super(world, myPet);
    }

    @Override
    protected String getDeathSound() {
        return "mob.blaze.death";
    }

    @Override
    protected String getHurtSound() {
        return "mob.blaze.hit";
    }

    protected String getLivingSound() {
        return "mob.blaze.breathe";
    }

    public boolean handlePlayerInteraction(EntityHuman entityhuman) {
        if (super.handlePlayerInteraction(entityhuman)) {
            return true;
        }

        ItemStack itemStack = entityhuman.inventory.getItemInHand();

        if (getOwner().equals(entityhuman) && itemStack != null && canUseItem()) {
            if (getMyPet().isOnFire() && itemStack.getItem() == Items.GLASS_BOTTLE && itemStack.getData() == 0 && getOwner().getPlayer().isSneaking()) {
                setOnFire(false);
                makeSound("random.fizz", 1.0F, 1.0F);
                if (!entityhuman.abilities.canInstantlyBuild) {
                    if (--itemStack.count <= 0) {
                        entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, new ItemStack(Items.GLASS_BOTTLE));
                    } else {
                        if (!entityhuman.inventory.pickup(new ItemStack(Items.GLASS_BOTTLE))) {
                            entityhuman.drop(new ItemStack(Items.GLASS_BOTTLE), true);
                        }
                    }
                }
                return true;
            } else if (!getMyPet().isOnFire() && itemStack.getItem() == Items.FLINT_AND_STEEL && getOwner().getPlayer().isSneaking()) {
                setOnFire(true);
                makeSound("fire.ignite", 1.0F, 1.0F);
                if (!entityhuman.abilities.canInstantlyBuild) {
                    itemStack.damage(1, entityhuman);
                }
                return true;
            }
        }
        return false;
    }

    protected void initDatawatcher() {
        super.initDatawatcher();
        getDataWatcher().a(16, new Byte((byte) 0)); // burning
    }

    public void setOnFire(boolean flag) {
        this.datawatcher.watch(16, (byte) (flag ? 1 : 0));
    }

    public void onLivingUpdate() {
        super.onLivingUpdate();

        if (!this.onGround && this.motY < 0.0D) {
            this.motY *= 0.6D;
        }
    }

    public void setMyPet(MyPet myPet) {
        if (myPet != null) {
            super.setMyPet(myPet);
            setOnFire(getMyPet().isOnFire());
        }
    }

    public MyBlaze getMyPet() {
        return (MyBlaze) myPet;
    }

    /**
     * -> disable falldamage
     */
    public void e(float f, float f1) {
    }
}