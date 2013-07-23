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

package de.Keyle.MyPet.entity.types.ocelot;

import de.Keyle.MyPet.entity.EntitySize;
import de.Keyle.MyPet.entity.ai.movement.MyPetAISit;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import net.minecraft.server.v1_6_R2.EntityHuman;
import net.minecraft.server.v1_6_R2.ItemStack;
import net.minecraft.server.v1_6_R2.World;
import org.bukkit.Material;
import org.bukkit.entity.Ocelot.Type;

@EntitySize(width = 0.6F, height = 0.8F)
public class EntityMyOcelot extends EntityMyPet
{
    public static int GROW_UP_ITEM = Material.POTION.getId();
    private MyPetAISit sitPathfinder;

    public EntityMyOcelot(World world, MyPet myPet)
    {
        super(world, myPet);
    }

    public void applySitting(boolean flag)
    {
        int i = this.datawatcher.getByte(16);
        if (flag)
        {
            this.datawatcher.watch(16, (byte) (i | 0x1));
        }
        else
        {
            this.datawatcher.watch(16, (byte) (i & 0xFFFFFFFE));
        }
        ((MyOcelot) myPet).isSitting = flag;
    }

    public boolean canMove()
    {
        return !isSitting();
    }

    public Type getCatType()
    {
        return ((MyOcelot) myPet).catType;
    }

    public void setCatType(int value)
    {
        this.datawatcher.watch(18, (byte) value);
        ((MyOcelot) myPet).catType = Type.getType(value);
    }

    protected String getDeathSound()
    {
        return "mob.cat.hitt";
    }

    protected String getHurtSound()
    {
        return "mob.cat.hitt";
    }

    protected String getLivingSound()
    {
        return !playIdleSound() ? null : this.random.nextInt(4) == 0 ? "mob.cat.purreow" : "mob.cat.meow";
    }

    public boolean handlePlayerInteraction(EntityHuman entityhuman)
    {
        if (super.handlePlayerInteraction(entityhuman))
        {
            return true;
        }

        ItemStack itemStack = entityhuman.inventory.getItemInHand();

        if (getOwner().equals(entityhuman))
        {
            if (itemStack != null && canUseItem() && getOwner().getPlayer().isSneaking())
            {
                if (itemStack.id == 351)
                {
                    if (itemStack.getData() == 11)
                    {
                        ((MyOcelot) myPet).setCatType(Type.WILD_OCELOT);
                        return true;
                    }
                    else if (itemStack.getData() == 0)
                    {
                        ((MyOcelot) myPet).setCatType(Type.BLACK_CAT);
                        return true;
                    }
                    else if (itemStack.getData() == 14)
                    {
                        ((MyOcelot) myPet).setCatType(Type.RED_CAT);
                        return true;
                    }
                    else if (itemStack.getData() == 7)
                    {
                        ((MyOcelot) myPet).setCatType(Type.SIAMESE_CAT);
                        return true;
                    }
                }
                else if (itemStack.id == GROW_UP_ITEM && canUseItem() && getOwner().getPlayer().isSneaking())
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
            this.sitPathfinder.toogleSitting();
            return true;
        }
        return false;
    }

    protected void initDatawatcher()
    {
        super.initDatawatcher();
        this.datawatcher.a(12, new Integer(0));     // age
        this.datawatcher.a(16, new Byte((byte) 0)); // tamed/sitting
        this.datawatcher.a(17, "");                 // ownername
        this.datawatcher.a(18, new Byte((byte) 0)); // cat type

    }

    public boolean isBaby()
    {
        return ((MyOcelot) myPet).isBaby;
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
        ((MyOcelot) myPet).isBaby = flag;
    }

    public boolean isSitting()
    {
        return this.sitPathfinder.isSitting();
    }

    public void setSitting(boolean flag)
    {
        this.sitPathfinder.setSitting(flag);
    }

    public void setMyPet(MyPet myPet)
    {
        if (myPet != null)
        {
            this.sitPathfinder = new MyPetAISit(this);

            super.setMyPet(myPet);

            this.setSitting(((MyOcelot) myPet).isSitting());
            this.setBaby(((MyOcelot) myPet).isBaby());
            this.setCatType(((MyOcelot) myPet).getCatType().getId());
        }
    }

    public void setPathfinder()
    {
        super.setPathfinder();
        petPathfinderSelector.addGoal("Sit", 2, sitPathfinder);
    }
}