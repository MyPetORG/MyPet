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

package de.Keyle.MyPet.entity.types.blaze;

import de.Keyle.MyPet.entity.EntitySize;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import net.minecraft.server.v1_6_R2.EntityHuman;
import net.minecraft.server.v1_6_R2.Item;
import net.minecraft.server.v1_6_R2.ItemStack;
import net.minecraft.server.v1_6_R2.World;

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
            if (isOnFire() && itemStack.id == 373 && itemStack.getData() == 0 && getOwner().getPlayer().isSneaking()) {
                setOnFire(false);
                makeSound("random.fizz", 1.0F, 1.0F);
                if (!entityhuman.abilities.canInstantlyBuild) {
                    if (--itemStack.count <= 0) {
                        entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, new ItemStack(Item.GLASS_BOTTLE));
                    } else {
                        if (!entityhuman.inventory.pickup(new ItemStack(Item.GLASS_BOTTLE))) {
                            entityhuman.drop(new ItemStack(Item.GLASS_BOTTLE));
                        }
                    }
                }
            } else if (!isOnFire() && itemStack.id == Item.FLINT_AND_STEEL.id && getOwner().getPlayer().isSneaking()) {
                setOnFire(true);
                makeSound("fire.ignite", 1.0F, 1.0F);
                if (!entityhuman.abilities.canInstantlyBuild) {
                    itemStack.damage(1, entityhuman);
                }
            }
        }
        return false;
    }

    protected void initDatawatcher() {
        super.initDatawatcher();
        getDataWatcher().a(16, new Byte((byte) 0)); // burning
    }

    public boolean isOnFire() {
        return ((MyBlaze) myPet).isOnFire;
    }

    public void setOnFire(boolean flag) {
        this.datawatcher.watch(16, (byte) (flag ? 1 : 0));
        ((MyBlaze) myPet).isOnFire = flag;
    }

    public void setMyPet(MyPet myPet) {
        if (myPet != null) {
            super.setMyPet(myPet);
            setOnFire(((MyBlaze) myPet).isOnFire());
        }
    }
}