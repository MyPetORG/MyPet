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

package de.Keyle.MyPet.skill.skills.implementation.inventory;

import net.minecraft.server.v1_5_R1.EntityHuman;
import net.minecraft.server.v1_5_R1.IInventory;
import net.minecraft.server.v1_5_R1.ItemStack;
import org.bukkit.craftbukkit.v1_5_R1.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v1_5_R1.inventory.CraftItemStack;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;
import org.spout.nbt.ByteTag;
import org.spout.nbt.CompoundTag;
import org.spout.nbt.ListTag;

import java.util.ArrayList;
import java.util.List;

public class MyPetCustomInventory implements IInventory
{
    private String inventroyName;
    private List<ItemStack> items = new ArrayList<ItemStack>();
    private int size = 0;
    private int stackSize = 64;
    private List<HumanEntity> transaction = new ArrayList<HumanEntity>();

    public MyPetCustomInventory(String inventroyName, int size)
    {
        setName(inventroyName);
        setSize(size);
    }

    public int getSize()
    {
        return size;
    }

    public void setSize(int size)
    {
        this.size = size;
        for (int i = items.size() ; i < size ; i++)
        {
            items.add(i, null);
        }
    }

    public String getName()
    {
        return inventroyName;
    }

    public void setName(String name)
    {
        if (name.length() > 16)
        {
            name = name.substring(0, 16);
        }
        this.inventroyName = name;
    }

    public ItemStack getItem(int i)
    {
        if (i <= size)
        {
            return items.get(i);
        }
        return null;
    }

    public void setItem(int i, ItemStack itemStack)
    {
        if (i < items.size())
        {
            items.set(i, itemStack);
        }
        else
        {
            for (int x = items.size() ; x < i ; x++)
            {
                items.add(x, null);
            }
            items.add(i, itemStack);
        }
        update();
    }

    public int addItem(org.bukkit.inventory.ItemStack itemAdd)
    {
        if (itemAdd == null)
        {
            return 0;
        }
        itemAdd = itemAdd.clone();

        for (int i = 0 ; i < this.getSize() ; i++)
        {
            CraftItemStack craftItem = CraftItemStack.asCraftMirror(getItem(i));

            if (ItemStackComparator.compareItem(itemAdd, craftItem))
            {
                if (craftItem.getAmount() >= craftItem.getMaxStackSize())
                {
                    continue;
                }
                while (craftItem.getAmount() < craftItem.getMaxStackSize() && itemAdd.getAmount() > 0)
                {
                    craftItem.setAmount(craftItem.getAmount() + 1);
                    itemAdd.setAmount(itemAdd.getAmount() - 1);
                }
                if (itemAdd.getAmount() == 0)
                {
                    break;
                }
            }
        }
        if (itemAdd.getAmount() > 0)
        {
            for (int i = 0 ; i < this.getSize() ; i++)
            {
                if (getItem(i) == null)
                {
                    if (itemAdd.getAmount() <= itemAdd.getMaxStackSize())
                    {
                        setItem(i, CraftItemStack.asNMSCopy(itemAdd.clone()));
                        itemAdd.setAmount(0);
                        break;
                    }
                    else
                    {
                        CraftItemStack itemStack = (CraftItemStack) itemAdd.clone();
                        itemStack.setAmount(itemStack.getMaxStackSize());
                        setItem(i, CraftItemStack.asNMSCopy(itemStack));
                        itemAdd.setAmount(itemAdd.getAmount() - itemStack.getMaxStackSize());
                    }
                    if (itemAdd.getAmount() == 0)
                    {
                        break;
                    }
                }
            }
        }
        return itemAdd.getAmount();
    }

    public ItemStack splitStack(int i, int j)
    {
        if (i <= size && items.get(i) != null)
        {
            ItemStack itemStack;
            if (items.get(i).count <= j)
            {
                itemStack = items.get(i);
                items.set(i, null);
                return itemStack;
            }
            else
            {
                itemStack = items.get(i).a(j);
                if (items.get(i).count == 0)
                {
                    items.set(i, null);
                }
                return itemStack;
            }
        }
        return null;
    }

    public ItemStack[] getContents()
    {
        ItemStack[] itemStack = new ItemStack[getSize()];
        for (int i = 0 ; i < getSize() ; i++)
        {
            itemStack[i] = items.get(i);
        }
        return itemStack;
    }

    public CompoundTag save(CompoundTag compound)
    {
        List<CompoundTag> itemList = new ArrayList<CompoundTag>();
        for (int i = 0 ; i < this.items.size() ; i++)
        {
            ItemStack itemStack = this.items.get(i);
            if (itemStack != null)
            {
                CompoundTag item = ItemStackNBTConverter.ItemStackToCompund(itemStack);
                item.getValue().put("Slot", new ByteTag("Slot", (byte) i));
                itemList.add(item);
            }
        }
        compound.getValue().put("Items", new ListTag<CompoundTag>("Items", CompoundTag.class, itemList));
        return compound;
    }

    public void load(CompoundTag nbtTagCompound)
    {
        ListTag items = (ListTag) nbtTagCompound.getValue().get("Items");

        for (int i = 0 ; i < items.getValue().size() ; i++)
        {
            CompoundTag itemCompound = (CompoundTag) items.getValue().get(i);

            ItemStack itemStack = ItemStackNBTConverter.CompundToItemStack(itemCompound);
            setItem(((ByteTag) itemCompound.getValue().get("Slot")).getValue(), itemStack);
        }
    }

    public boolean a(EntityHuman entityHuman)
    {
        return true;
    }

    public void startOpen()
    {
    }

    public void onOpen(CraftHumanEntity who)
    {
        this.transaction.add(who);
    }

    public void onClose(CraftHumanEntity who)
    {
        this.transaction.remove(who);
        if (items.size() > this.size)
        {
            for (int counterOutside = items.size() - 1 ; counterOutside >= this.size ; counterOutside--)
            {
                if (items.get(counterOutside) != null)
                {
                    for (int counterInside = 0 ; counterInside < size ; counterInside++)
                    {
                        if (items.get(counterInside) == null)
                        {
                            items.set(counterInside, items.get(counterOutside));
                            items.set(counterOutside, null);
                        }
                    }
                }
                if (items.get(counterOutside) == null)
                {
                    items.remove(counterOutside);
                }
            }
        }
    }

    public void close()
    {
        for (HumanEntity humanEntity : transaction)
        {
            humanEntity.closeInventory();
        }
    }

    public List<HumanEntity> getViewers()
    {
        return this.transaction;
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
        if (items.get(i) != null)
        {
            ItemStack itemstack = items.get(i);

            items.set(i, null);
            return itemstack;
        }
        return null;
    }

    public void update()
    {
    }

    public boolean b(int paramInt, ItemStack paramItemStack)
    {
        return true;
    }

    public boolean c()
    {
        return true;
    }

    public void g()
    {
    }
}