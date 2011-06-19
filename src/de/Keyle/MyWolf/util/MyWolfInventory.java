/*
 * TileEntityVirtualChest is a simple class which overrides one method in TileEntityChest
 * The method is responsible for validating the selected chest against the world state.
 * For our purposes, the chest does not exist in the world, so we want to skip these checks.
 */
package de.Keyle.MyWolf.util;

import net.minecraft.server.ItemStack;
import net.minecraft.server.TileEntityChest;

public class MyWolfInventory extends TileEntityChest {
	
	public MyWolfInventory()
	{
		super();
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
