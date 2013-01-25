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

package de.Keyle.MyPet.entity.types.skeleton;

import de.Keyle.MyPet.entity.EntitySize;
import de.Keyle.MyPet.entity.EquipmentSlot;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import net.minecraft.server.v1_4_R1.*;

@EntitySize(width = 0.6F, height = 0.6F)
public class EntityMySkeleton extends EntityMyPet
{
    public EntityMySkeleton(World world, MyPet myPet)
    {
        super(world, myPet);
        this.texture = "/mob/skeleton.png";
    }

    public void setMyPet(MyPet myPet)
    {
        if (myPet != null)
        {
            super.setMyPet(myPet);
            MySkeleton mySkeleton = (MySkeleton) myPet;

            this.setWither(mySkeleton.isWither());
            for (EquipmentSlot slot : EquipmentSlot.values())
            {
                if (mySkeleton.getEquipment(slot) != null)
                {
                    setEquipment(slot.getSlotId(), mySkeleton.getEquipment(slot));
                }
            }
        }
    }

    public boolean isWither()
    {
        return this.datawatcher.getByte(13) == 1;
    }

    public void setWither(boolean flag)
    {
        this.datawatcher.watch(13, (byte) (flag ? 1 : 0));
        ((MySkeleton) myPet).isWither = flag;
    }

    public void setEquipment(int slot, ItemStack itemStack)
    {
        ((WorldServer) this.world).getTracker().a(this, new Packet5EntityEquipment(this.id, slot, itemStack));
        ((MySkeleton) myPet).equipment.put(EquipmentSlot.getSlotById(slot), itemStack);
    }

    public ItemStack getEquipment(int slot)
    {
        return ((MySkeleton) myPet).getEquipment(EquipmentSlot.getSlotById(slot));
    }

    public ItemStack[] getEquipment()
    {
        return ((MySkeleton) myPet).getEquipment();
    }

    public boolean checkForEquipment(ItemStack itemstack)
    {
        int slot = b(itemstack);
        if (slot == 0)
        {
            if (itemstack.getItem() instanceof ItemSword)
            {
                return true;
            }
            else if (itemstack.getItem() instanceof ItemAxe)
            {
                return true;
            }
            else if (itemstack.getItem() instanceof ItemSpade)
            {
                return true;
            }
            else if (itemstack.getItem() instanceof ItemHoe)
            {
                return true;
            }
            else if (itemstack.getItem() instanceof ItemPickaxe)
            {
                return true;
            }
            else
            {
                return false;
            }
        }
        return false;
    }

    // Obfuscated Methods -------------------------------------------------------------------------------------------

    protected void a()
    {
        super.a();
        this.datawatcher.a(13, new Byte((byte) 0)); // age
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
            if (itemStack.id == Item.SHEARS.id)
            {
                for (EquipmentSlot slot : EquipmentSlot.values())
                {
                    ItemStack itemInSlot = ((MySkeleton) myPet).getEquipment(slot);
                    if (itemInSlot != null)
                    {
                        EntityItem entityitem = this.a(itemInSlot.cloneItemStack(), 1.0F);
                        entityitem.motY += (double) (this.random.nextFloat() * 0.05F);
                        entityitem.motX += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                        entityitem.motZ += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                        setEquipment(slot.getSlotId(), null);
                    }
                }
                return true;
            }
            else if (checkForEquipment(itemStack) && getOwner().isSneaking())
            {
                EquipmentSlot slot = EquipmentSlot.getSlotById(b(itemStack));
                ItemStack itemInSlot = ((MySkeleton) myPet).getEquipment(slot);
                if (itemInSlot != null && !entityhuman.abilities.canInstantlyBuild)
                {
                    EntityItem entityitem = this.a(itemInSlot.cloneItemStack(), 1.0F);
                    entityitem.motY += (double) (this.random.nextFloat() * 0.05F);
                    entityitem.motX += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                    entityitem.motZ += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                }
                setEquipment(b(itemStack), itemStack.cloneItemStack());
                if (!entityhuman.abilities.canInstantlyBuild)
                {
                    --itemStack.count;
                }
                if (itemStack.count <= 0)
                {
                    entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                }
                return true;
            }
        }
        return false;
    }

    protected void a(int i, int j, int k, int l)
    {
        makeSound("mob.skeleton.step", 0.15F, 1.0F);
    }

    /**
     * Returns the default sound of the MyPet
     */
    protected String aY()
    {
        return !playIdleSound() ? "" : "mob.skeleton.say";
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    protected String aZ()
    {
        return "mob.skeleton.hurt";
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    protected String ba()
    {
        return "mob.skeleton.death";
    }
}