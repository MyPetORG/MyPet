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

package de.Keyle.MyPet.skill.skills.beacon;

import net.minecraft.server.v1_4_6.EntityHuman;
import net.minecraft.server.v1_4_6.IInventory;
import net.minecraft.server.v1_4_6.ItemStack;
import org.bukkit.craftbukkit.v1_4_6.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.List;

public class MyPetCustomBeaconInventory implements IInventory
{
    public List<HumanEntity> transaction = new ArrayList<HumanEntity>();
    private int maxStack = 64;
    private ItemStack tributeItem;

    // Inventory Methods --------------------------------------------------------------------------------------------

    public ItemStack[] getContents()
    {
        return null;
    }

    public void onOpen(CraftHumanEntity who)
    {
        this.transaction.add(who);
    }

    public void onClose(CraftHumanEntity who)
    {
        this.transaction.remove(who);
    }

    public List<HumanEntity> getViewers()
    {
        return this.transaction;
    }

    public InventoryHolder getOwner()
    {
        return null;
    }

    public int getSize()
    {
        return 1;
    }

    public ItemStack getItem(int slot)
    {
        return slot == 0 ? this.tributeItem : null;
    }

    public ItemStack splitStack(int slot, int amount)
    {
        if (slot == 0 && this.tributeItem != null)
        {
            if (amount >= this.tributeItem.count)
            {
                ItemStack itemstack = this.tributeItem;

                this.tributeItem = null;
                return itemstack;
            }
            this.tributeItem.count -= amount;
            return new ItemStack(this.tributeItem.id, amount, this.tributeItem.getData());
        }
        return null;
    }

    public ItemStack splitWithoutUpdate(int i)
    {
        if (i == 0 && this.tributeItem != null)
        {
            ItemStack itemstack = this.tributeItem;

            this.tributeItem = null;
            return itemstack;
        }
        return null;
    }

    public void setItem(int i, ItemStack itemStack)
    {
        if (i == 0)
        {
            this.tributeItem = itemStack;
        }
    }

    public String getName()
    {
        return "inventory.mypet.beacon";
    }

    public int getMaxStackSize()
    {
        return this.maxStack;
    }

    public void setMaxStackSize(int size)
    {
        this.maxStack = size;
    }

    public void update()
    {
    }

    public void startOpen()
    {
    }

    // Obfuscated Methods -------------------------------------------------------------------------------------------

    public boolean a_(EntityHuman entityHuman)
    {
        return true;
    }

    public void f()
    {
    }
}