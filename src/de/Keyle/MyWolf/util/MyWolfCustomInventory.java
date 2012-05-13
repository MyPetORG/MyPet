/*
 * Copyright (C) 2011-2012 Keyle
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

package de.Keyle.MyWolf.util;

import de.Keyle.MyWolf.skill.skills.Inventory;
import net.minecraft.server.*;
import org.bukkit.OfflinePlayer;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.List;

public class MyWolfCustomInventory implements IInventory
{
    private String MyWolfInventroyName;
    private List<ItemStack> Items = new ArrayList<ItemStack>();
    private int Size = 0;
    private int stackSize = 64;

    public MyWolfCustomInventory(String Name, int Size)
    {
        setName(Name);
        setSize(Size);
    }

    public MyWolfCustomInventory(String Name)
    {
        setName(Name);
    }

    public int getSize()
    {
        return Items.size();
    }

    public void setSize(int Size)
    {
        this.Size = Size;
        for (int i = Items.size() ; i < Size ; i++)
        {
            Items.add(i, null);
        }
    }

    public String getName()
    {
        return MyWolfInventroyName;
    }

    public void setName(String Name)
    {
        if (Name.length() > 16)
        {
            Name = Name.substring(0, 16);
        }
        this.MyWolfInventroyName = Name;
    }

    public ItemStack getItem(int i)
    {
        if (i <= Size)
        {
            return Items.get(i);
        }
        return null;
    }

    public void setItem(int i, ItemStack itemStack)
    {
        if (i < Items.size())
        {
            Items.set(i, itemStack);
        }
        else
        {
            for (int x = Items.size() ; x < i ; x++)
            {
                Items.add(x, null);
            }
            Items.add(i, itemStack);
        }
    }

    public int addItem(org.bukkit.inventory.ItemStack item)
    {
        item = item.clone();
        int ItemID = item.getTypeId();
        int ItemDuarbility = item.getDurability();
        int ItemMaxStack = item.getMaxStackSize();

        for (int i = 0 ; i < this.getSize() ; i++)
        {
            if (getItem(i) != null && getItem(i).id == ItemID && getItem(i).getData() == ItemDuarbility && getItem(i).getEnchantments() == null && item.getEnchantments().size() == 0 && getItem(i).count < ItemMaxStack)
            {
                if (item.getAmount() >= ItemMaxStack - getItem(i).count)
                {
                    item.setAmount(item.getAmount() - (ItemMaxStack - getItem(i).count));
                    getItem(i).count = ItemMaxStack;
                }
                else
                {
                    getItem(i).count += item.getAmount();
                    item.setAmount(0);
                    break;
                }
            }
        }
        for (int i = 0 ; i < getSize() ; i++)
        {
            if (item.getAmount() <= 0)
            {
                break;
            }
            if (getItem(i) == null)
            {
                if (item.getAmount() <= ItemMaxStack)
                {
                    setItem(i, ((CraftItemStack) item.clone()).getHandle());
                    item.setAmount(0);
                    break;
                }
                else
                {
                    org.bukkit.inventory.ItemStack is = item.clone();
                    is.setAmount(is.getMaxStackSize());
                    setItem(i, ((CraftItemStack) is).getHandle());
                    item.setAmount(item.getAmount() - is.getMaxStackSize());
                }
            }
        }
        return item.getAmount();
    }

    public ItemStack splitStack(int i, int j)
    {
        if (i <= Size && Items.get(i) != null)
        {
            ItemStack itemstack;
            if (Items.get(i).count <= j)
            {
                itemstack = Items.get(i);
                Items.set(i, null);
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
        ItemStack[] itemStack = new ItemStack[getSize()];
        for (int i = 0 ; i < getSize() ; i++)
        {
            itemStack[i] = Items.get(i);
        }
        return itemStack;
    }

    public NBTTagCompound save(NBTTagCompound nbtTagCompound)
    {
        NBTTagList Items = new NBTTagList();
        for (int i = 0 ; i < this.Items.size() ; i++)
        {
            ItemStack itemStack = this.Items.get(i);
            if (itemStack != null)
            {
                NBTTagCompound Item = new NBTTagCompound();
                Item.setByte("Slot", (byte) i);
                itemStack.save(Item);
                Items.add(Item);
            }
        }
        nbtTagCompound.set("Items", Items);
        return nbtTagCompound;
    }

    public void load(NBTTagCompound nbtTagCompound)
    {
        NBTTagList Items = nbtTagCompound.getList("Items");

        for (int i = 0 ; i < Items.size() ; i++)
        {
            NBTTagCompound Item = (NBTTagCompound) Items.get(i);

            ItemStack itemStack = ItemStack.a(Item);
            setItem(Item.getByte("Slot"), itemStack);
        }
    }

    public boolean a(EntityHuman entityHuman)
    {
        return true;
    }

    public void onOpen(CraftHumanEntity craftHumanEntity)
    {
    }

    public void onClose(CraftHumanEntity craftHumanEntity)
    {
        OfflinePlayer OfflineP = MyWolfUtil.getOfflinePlayer(craftHumanEntity.getName());
        if (MyWolfList.hasMyWolf(OfflineP))
        {
            if (Inventory.WolfChestOpened.contains(OfflineP.getPlayer()))
            {
                MyWolfList.getMyWolf(OfflineP).setSitting(false);
                Inventory.WolfChestOpened.remove(OfflineP.getPlayer());
            }
        }
    }

    public List<HumanEntity> getViewers()
    {
        return null;
    }

    public InventoryHolder getOwner()
    {
        return null;
    }

    public int getMaxStackSize()
    {
        return stackSize;
    }

    public void setMaxStackSize(int i)
    {
        this.stackSize = i;
    }

    public ItemStack splitWithoutUpdate(int i)
    {
        return null;
    }

    public void update()
    {
    }

    public void f()
    {
    }

    public void g()
    {
    }
}