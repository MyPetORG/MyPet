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

package de.Keyle.MyPet.entity.types.pig;

import de.Keyle.MyPet.entity.EntitySize;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.util.itemstringinterpreter.ConfigItem;
import net.minecraft.server.v1_6_R2.*;

@EntitySize(width = 0.9F, height = 0.9F)
public class EntityMyPig extends EntityMyPet {
    public static ConfigItem GROW_UP_ITEM;

    public EntityMyPig(World world, MyPet myPet) {
        super(world, myPet);
    }

    @Override
    protected String getDeathSound() {
        return "mob.pig.death";
    }

    @Override
    protected String getHurtSound() {
        return "mob.pig.say";
    }

    protected String getLivingSound() {
        return "mob.pig.say";
    }

    public boolean handlePlayerInteraction(EntityHuman entityhuman) {
        if (super.handlePlayerInteraction(entityhuman)) {
            return true;
        }

        ItemStack itemStack = entityhuman.inventory.getItemInHand();

        if (getOwner().equals(entityhuman) && itemStack != null && canUseItem()) {
            if (itemStack.id == 329 && !((MyPig) myPet).hasSaddle() && getOwner().getPlayer().isSneaking()) {
                if (!entityhuman.abilities.canInstantlyBuild) {
                    --itemStack.count;
                }
                if (itemStack.count <= 0) {
                    entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                }
                ((MyPig) myPet).setSaddle(true);
                return true;
            } else if (itemStack.id == Item.SHEARS.id && ((MyPig) myPet).hasSaddle() && getOwner().getPlayer().isSneaking()) {
                ((MyPig) myPet).setSaddle(false);
                if (!entityhuman.abilities.canInstantlyBuild) {
                    EntityItem entityitem = this.a(new ItemStack(Item.SADDLE.id, 1, 1), 1.0F);
                    entityitem.motY += (double) (this.random.nextFloat() * 0.05F);
                    entityitem.motX += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                    entityitem.motZ += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                }
                makeSound("mob.sheep.shear", 1.0F, 1.0F);
                itemStack.damage(1, entityhuman);
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

    public boolean hasSaddle() {
        return ((MyPig) myPet).hasSaddle;
    }

    protected void initDatawatcher() {
        super.initDatawatcher();
        this.datawatcher.a(12, new Integer(0));     // age
        this.datawatcher.a(16, new Byte((byte) 0)); // saddle
    }

    public boolean isBaby() {
        return ((MyPig) myPet).isBaby;
    }

    public void setBaby(boolean flag) {
        if (flag) {
            this.datawatcher.watch(12, Integer.valueOf(Integer.MIN_VALUE));
        } else {
            this.datawatcher.watch(12, new Integer(0));
        }
        ((MyPig) myPet).isBaby = flag;
    }

    public void playStepSound() {
        makeSound("mob.pig.step", 0.15F, 1.0F);
    }

    public void setMyPet(MyPet myPet) {
        if (myPet != null) {
            super.setMyPet(myPet);

            this.setSaddle(((MyPig) myPet).hasSaddle());
            this.setBaby(((MyPig) myPet).isBaby());
        }
    }

    public void setSaddle(boolean flag) {
        if (flag) {
            this.datawatcher.watch(16, (byte) 1);
        } else {
            this.datawatcher.watch(16, (byte) 0);
        }
        ((MyPig) myPet).hasSaddle = flag;
    }
}