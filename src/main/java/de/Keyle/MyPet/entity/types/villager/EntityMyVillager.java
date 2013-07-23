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

package de.Keyle.MyPet.entity.types.villager;

import de.Keyle.MyPet.entity.EntitySize;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import net.minecraft.server.v1_6_R2.EntityHuman;
import net.minecraft.server.v1_6_R2.ItemStack;
import net.minecraft.server.v1_6_R2.World;
import org.bukkit.Material;

@EntitySize(width = 0.6F, height = 1.9F)
public class EntityMyVillager extends EntityMyPet
{
    public static int GROW_UP_ITEM = Material.POTION.getId();

    public EntityMyVillager(World world, MyPet myPet)
    {
        super(world, myPet);
    }

    public void setMyPet(MyPet myPet)
    {
        if (myPet != null)
        {
            super.setMyPet(myPet);

            this.setProfession(((MyVillager) myPet).getProfession());
            this.setBaby(((MyVillager) myPet).isBaby());
        }
    }

    public int getProfession()
    {
        return ((MyVillager) myPet).profession;
    }

    public void setProfession(int value)
    {
        this.datawatcher.watch(16, value);
        ((MyVillager) myPet).profession = value;
    }

    public boolean isBaby()
    {
        return ((MyVillager) myPet).isBaby;
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
        ((MyVillager) myPet).isBaby = flag;
    }

    protected void initDatawatcher()
    {
        super.initDatawatcher();
        this.datawatcher.a(12, new Integer(0)); // age
        this.datawatcher.a(16, new Integer(0)); // profession
    }

    public boolean handlePlayerInteraction(EntityHuman entityhuman)
    {
        if (super.handlePlayerInteraction(entityhuman))
        {
            return true;
        }

        ItemStack itemStack = entityhuman.inventory.getItemInHand();

        if (getOwner().equals(entityhuman) && itemStack != null && canUseItem())
        {
            if (itemStack.id == GROW_UP_ITEM && getOwner().getPlayer().isSneaking())
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

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    protected String getHurtSound()
    {
        return "mob.villager.defaulthurt";
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    protected String getDeathSound()
    {
        return "mob.villager.defaultdeath";
    }

    /**
     * Returns the default sound of the MyPet
     */
    protected String getLivingSound()
    {
        return !playIdleSound() ? null : "mob.villager.default";
    }
}