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

package de.Keyle.MyPet.entity.types.chicken;

import de.Keyle.MyPet.entity.EntitySize;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.util.itemstringinterpreter.ConfigItem;
import net.minecraft.server.v1_6_R2.EntityHuman;
import net.minecraft.server.v1_6_R2.Item;
import net.minecraft.server.v1_6_R2.ItemStack;
import net.minecraft.server.v1_6_R2.World;

@EntitySize(width = 0.3F, height = 0.7F)
public class EntityMyChicken extends EntityMyPet
{
    public static boolean CAN_LAY_EGGS = true;
    public static ConfigItem GROW_UP_ITEM;

    private int nextEggTimer;

    public EntityMyChicken(World world, MyPet myPet)
    {
        super(world, myPet);
        nextEggTimer = (random.nextInt(6000) + 6000);
    }

    @Override
    protected String getDeathSound()
    {
        return "mob.chicken.hurt";
    }

    @Override
    protected String getHurtSound()
    {
        return "mob.chicken.hurt";
    }

    protected String getLivingSound()
    {
        return "mob.chicken.say";
    }

    public boolean handlePlayerInteraction(EntityHuman entityhuman)
    {
        if (super.handlePlayerInteraction(entityhuman))
        {
            return true;
        }

        ItemStack itemStack = entityhuman.inventory.getItemInHand();

        if (getOwner().equals(entityhuman) && itemStack != null)
        {
            if (GROW_UP_ITEM.compare(itemStack) && getOwner().getPlayer().isSneaking())
            {
                if (isBaby())
                {
                    if (!entityhuman.abilities.canInstantlyBuild)
                    {
                        if (--itemStack.count <= 0)
                        {
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

    protected void initDatawatcher()
    {
        super.initDatawatcher();
        this.datawatcher.a(12, new Integer(0)); // age
    }

    public boolean isBaby()
    {
        return ((MyChicken) myPet).isBaby;
    }

    public void setBaby(boolean flag)
    {
        if (flag)
        {
            this.datawatcher.watch(12, Integer.valueOf(Integer.MIN_VALUE));
        }
        else
        {
            this.datawatcher.watch(12, new Integer(0));
        }
        ((MyChicken) myPet).isBaby = flag;
    }

    public void onLivingUpdate()
    {
        super.onLivingUpdate();

        if (!this.onGround && this.motY < 0.0D)
        {
            this.motY *= 0.6D;
        }

        if (CAN_LAY_EGGS && !world.isStatic && canUseItem() && --nextEggTimer <= 0)
        {
            world.makeSound(this, "mob.chicken.plop", 1.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
            b(Item.EGG.id, 1);
            nextEggTimer = (random.nextInt(6000) + 6000);
        }
    }

    public void playStepSound()
    {
        makeSound("mob.chicken.step", 0.15F, 1.0F);
    }

    public void setMyPet(MyPet myPet)
    {
        if (myPet != null)
        {
            super.setMyPet(myPet);

            this.setBaby(((MyChicken) myPet).isBaby());
        }
    }
}