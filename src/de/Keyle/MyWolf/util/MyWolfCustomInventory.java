/*
 * Copyright (C) 2011-2012 Keyle
 *
 * This file is part of MyWolf
 *
 * MyWolf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyWolf is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyWolf. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyWolf.util;

import net.minecraft.server.EntityHuman;
import net.minecraft.server.IInventory;
import net.minecraft.server.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class MyWolfCustomInventory implements IInventory
{
    protected String MyWolfInventroyName;
    protected List<ItemStack> Items = new ArrayList<ItemStack>();
    protected int Size = 0;

    public MyWolfCustomInventory(String Name, int Size)
    {
        setName(Name);
        setSize(Size);
    }
    
    public int getSize()
    {
        return Items.size();
    }

    public void setSize(int Size)
    {
        this.Size = Size;
        for(int i = Items.size();i<Size;i++)
        {
            Items.add(i,null);
        }
    }

    public String getName()
    {
        return MyWolfInventroyName;
    }

    public void setName(String Name)
    {
        if(Name.length()>16)
        {
            Name = Name.substring(0,16);
        }
        this.MyWolfInventroyName = Name;
    }

    public ItemStack getItem(int i)
    {
        if(i<=Size)
        {
            return Items.get(i);
        }
        return null;
    }

    public void setItem(int i, ItemStack itemStack)
    {
        if(i<=Size)
        {
            Items.set(i,itemStack);
        }
    }

    public int addItem(org.bukkit.inventory.ItemStack item)
    {
        int ItemID = item.getTypeId();
        int ItemDuarbility = item.getDurability();
        int ItemAmount = item.getAmount();
        int ItemMaxStack = item.getMaxStackSize();

        for (int i = 0 ; i < this.getSize() ; i++)
        {
            if (getItem(i) != null && getItem(i).id == ItemID && getItem(i).getData() == ItemDuarbility && getItem(i).count < ItemMaxStack)
            {
                if (ItemAmount >= ItemMaxStack - getItem(i).count)
                {
                    ItemAmount = ItemAmount - (ItemMaxStack - getItem(i).count);
                    getItem(i).count = ItemMaxStack;
                }
                else
                {
                    getItem(i).count += ItemAmount;
                    ItemAmount = 0;
                    break;
                }
            }
        }
        for (int i = 0 ; i < getSize() ; i++)
        {
            if (ItemAmount <= 0)
            {
                break;
            }
            if (getItem(i) == null)
            {
                if (ItemAmount <= ItemMaxStack)
                {
                    setItem(i, new ItemStack(ItemID, ItemAmount, ItemDuarbility));
                    ItemAmount = 0;
                }
                else
                {
                    setItem(i, new ItemStack(ItemID, ItemMaxStack, ItemDuarbility));
                    ItemAmount -= ItemMaxStack;
                }
            }
        }
        return ItemAmount;
    }

    public ItemStack splitStack(int i, int j)
    {
        if (i<=Size && Items.get(i) != null)
        {
            ItemStack itemstack;
            if (Items.get(i).count <= j)
            {
                itemstack = Items.get(i);
                Items.set(i,null);
                return itemstack;
            }
            else
            {
                itemstack = Items.get(i).a(j);
                if (Items.get(i).count == 0)
                {
                    Items.set(i, null);
                }
                return itemstack;
            }
        }
        return null;
    }

    public ItemStack[] getContents()
    {
        return (ItemStack[])Items.toArray();
    }

    public int getMaxStackSize()
    {
        return 64;
    }

    public void update()
    {
    }

    public boolean a(EntityHuman entityHuman)
    {
        return true;
    }

    public void f()
    {
    }

    public void g()
    {
    }
}
