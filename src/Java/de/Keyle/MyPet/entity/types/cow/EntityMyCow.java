/*
 * Copyright (C) 2011-2013 Keyle
 *
 * This file is part of MyPet
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
 * along with MyPet. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.entity.types.cow;

import de.Keyle.MyPet.entity.EntitySize;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import net.minecraft.server.v1_4_R1.EntityHuman;
import net.minecraft.server.v1_4_R1.Item;
import net.minecraft.server.v1_4_R1.ItemStack;
import net.minecraft.server.v1_4_R1.World;

@EntitySize(width = 0.9F, height = 1.3F)
public class EntityMyCow extends EntityMyPet
{
    public static boolean CAN_GIVE_MILK = true;

    public EntityMyCow(World world, MyPet myPet)
    {
        super(world, myPet);
        this.texture = "/mob/cow.png";
    }

    public void setMyPet(MyPet myPet)
    {
        if (myPet != null)
        {
            super.setMyPet(myPet);

            this.setBaby(((MyCow) myPet).isBaby());
        }
    }

    public boolean isBaby()
    {
        return this.datawatcher.getInt(12) < 0;
    }

    @SuppressWarnings("boxing")
    public void setBaby(boolean flag)
    {
        if (flag)
        {
            this.datawatcher.watch(12, new Integer(-24000));
        }
        else
        {
            this.datawatcher.watch(12, new Integer(0));
        }
        ((MyCow) myPet).isBaby = flag;
    }

    // Obfuscated Methods -------------------------------------------------------------------------------------------

    protected void a()
    {
        super.a();
        this.datawatcher.a(12, new Integer(0)); // age
    }

    /**
     * Is called when player rightclicks this MyPet
     * return:
     * true: there was a reaction on rightclick
     * false: no reaction on rightclick
     */
    public boolean a(EntityHuman entityhuman)
    {
        if (super.a(entityhuman))
        {
            return true;
        }

        ItemStack itemStack = entityhuman.inventory.getItemInHand();

        if (entityhuman == getOwner() && itemStack != null)
        {
            if (itemStack.id == Item.BUCKET.id)
            {
                if (CAN_GIVE_MILK && !this.world.isStatic)
                {
                    ItemStack milkBucket = new ItemStack(Item.BUCKET.id, 1, 0);

                    entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, milkBucket);
                    return true;
                }
            }
        }
        return false;
    }

    protected void a(int i, int j, int k, int l)
    {
        makeSound("mob.cow.step", 0.15F, 1.0F);
    }

    /**
     * Returns the default sound of the MyPet
     */
    protected String aY()
    {
        return !playIdleSound() ? "" : "mob.cow.say";
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    @Override
    protected String aZ()
    {
        return "mob.cow.hurt";
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    @Override
    protected String ba()
    {
        return "mob.cow.hurt";
    }
}