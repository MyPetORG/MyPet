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

package de.Keyle.MyWolf.Listeners;

import org.bukkit.Material;
import org.bukkitcontrib.event.inventory.InventoryClickEvent;
import org.bukkitcontrib.event.inventory.InventoryCloseEvent;
import org.bukkitcontrib.event.inventory.InventoryListener;
import org.bukkitcontrib.inventory.CustomMCInventory;

import de.Keyle.MyWolf.ConfigBuffer;

public class MyWolfInventoryListener extends InventoryListener
{
	@Override
	public void onInventoryClose(InventoryCloseEvent event)
	{
		if (ConfigBuffer.WolfChestOpened.contains(event.getPlayer()) && ConfigBuffer.mWolves.containsKey(event.getPlayer().getName()))
		{
			ConfigBuffer.mWolves.get(event.getPlayer().getName()).Wolf.setSitting(false);
			ConfigBuffer.WolfChestOpened.remove(event.getPlayer());
		}
	}
	
	
	@Override
	public void onInventoryClick(InventoryClickEvent event)
	{
		if(event.getInventory() instanceof CustomMCInventory && event.getItem().getType() == Material.PISTON_MOVING_PIECE)
		{
			event.setCancelled(true);
		}
	}
}
