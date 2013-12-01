/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2013 Keyle
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

package de.Keyle.MyPet.entity.types.cow;

import de.Keyle.MyPet.entity.EntitySize;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.util.ConfigItem;
import net.minecraft.server.v1_7_R1.EntityHuman;
import net.minecraft.server.v1_7_R1.ItemStack;
import net.minecraft.server.v1_7_R1.Items;
import net.minecraft.server.v1_7_R1.World;

@EntitySize(width = 0.9F, height = 1.3F)
public class EntityMyCow extends EntityMyPet {
    public static boolean CAN_GIVE_MILK = true;
    public static ConfigItem GROW_UP_ITEM;

    public EntityMyCow(World world, MyPet myPet) {
        super(world, myPet);
    }

    @Override
    protected String getDeathSound() {
        return "mob.cow.hurt";
    }

    @Override
    protected String getHurtSound() {
        return "mob.cow.hurt";
    }

    protected String getLivingSound() {
        return "mob.cow.say";
    }

    public boolean handlePlayerInteraction(EntityHuman entityhuman) {
        if (super.handlePlayerInteraction(entityhuman)) {
            return true;
        }

        ItemStack itemStack = entityhuman.inventory.getItemInHand();

        if (getOwner().equals(entityhuman) && itemStack != null && canUseItem()) {
            if (itemStack.getItem() == Items.BUCKET) {
                if (CAN_GIVE_MILK && !this.world.isStatic) {
                    ItemStack milkBucket = new ItemStack(Items.BUCKET, 1, 0);

                    entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, milkBucket);
                    return true;
                }
            } else if (GROW_UP_ITEM.compare(itemStack) && getOwner().getPlayer().isSneaking()) {
                if (isBaby()) {
                    if (!entityhuman.abilities.canInstantlyBuild) {
                        if (--itemStack.count <= 0) {
                            entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                        }
                    }
                    this.setBaby(false);
                    return true;
                }
            }
        }
        return false;
    }

    protected void initDatawatcher() {
        super.initDatawatcher();
        this.datawatcher.a(12, new Integer(0)); // age
    }

    public boolean isBaby() {
        return ((MyCow) myPet).isBaby;
    }

    public void setBaby(boolean flag) {
        if (flag) {
            this.datawatcher.watch(12, Integer.valueOf(Integer.MIN_VALUE));
        } else {
            this.datawatcher.watch(12, new Integer(0));
        }
        ((MyCow) myPet).isBaby = flag;
    }

    public void playStepSound() {
        makeSound("mob.cow.step", 0.15F, 1.0F);
    }

    public void setMyPet(MyPet myPet) {
        if (myPet != null) {
            super.setMyPet(myPet);

            this.setBaby(((MyCow) myPet).isBaby());
        }
    }
}