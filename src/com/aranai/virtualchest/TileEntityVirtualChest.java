/*
 * TileEntityVirtualChest is a simple class which overrides one method in TileEntityChest
 * The method is responsible for validating the selected chest against the world state.
 * For our purposes, the chest does not exist in the world, so we want to skip these checks.
 */
package com.aranai.virtualchest;

import net.minecraft.server.EntityHuman;
import net.minecraft.server.ItemStack;
import net.minecraft.server.TileEntityChest;

public class TileEntityVirtualChest extends TileEntityChest {
	
	public TileEntityVirtualChest()
	{
		super();
	}
	
	@Override
	public boolean a_(EntityHuman entityhuman) {
		/*
		 * For this proof of concept, we ALWAYS validate the chest.
		 * This behavior has not been thoroughly tested, and may cause unexpected results depending on the state of the player.
		 * 
		 * Depending on your purposes, you might want to change this.
		 * It would likely be preferable to enforce your business logic outside of this file instead, however.
		 */
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
