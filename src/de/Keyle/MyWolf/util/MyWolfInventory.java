/*
* Copyright (C) 2011 Keyle
*
* This file is part of MyWolf.
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
import net.minecraft.server.ItemStack;
import net.minecraft.server.TileEntityChest;

public class MyWolfInventory extends TileEntityChest {
	
	public MyWolfInventory()
	{
		super();
	}
	
	@Override
	public boolean a_(EntityHuman entityhuman)
	{
		return true;
	}
	
	public int addItem(org.bukkit.entity.Item item)
	{
		int itemID = item.getItemStack().getTypeId();
		int itemAmount = item.getItemStack().getAmount();
		int itemDurability = item.getItemStack().getDurability();
		int itemMaxStack = item.getItemStack().getMaxStackSize();		
		ItemStack[] items = this.getContents();
		for(net.minecraft.server.ItemStack i : items)
		{
			if(i != null && i.id == itemID && i.damage == itemDurability && i.count <itemMaxStack)
			{
				if(itemAmount >= itemMaxStack - i.count)
				{
					itemAmount = itemAmount-(itemMaxStack - i.count);
					i.count = itemMaxStack;
				}
				else
				{
					i.count = i.count + itemAmount;
					itemAmount = 0;
				}
			}
			if(itemAmount == 0)
			{
				break;
			}
		}
		for(int i =0;i<items.length;i++)
		{
			if(itemAmount == 0)
			{
				break;
			}
			if(items[i] == null)
			{
				if(itemAmount<=itemMaxStack)
				{
					this.setItem(i, new net.minecraft.server.ItemStack(itemID, itemAmount, itemDurability));
					itemAmount = 0;
				}
				else
				{
					this.setItem(i, new net.minecraft.server.ItemStack(itemID, itemMaxStack, itemDurability));
					itemAmount -= itemMaxStack;
				}
			}
		}
		return itemAmount;
	}
}
