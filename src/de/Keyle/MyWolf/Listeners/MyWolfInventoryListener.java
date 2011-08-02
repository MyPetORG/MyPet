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

import de.Keyle.MyWolf.ConfigBuffer;
import org.getspout.spoutapi.event.inventory.InventoryCloseEvent;
import org.getspout.spoutapi.event.inventory.InventoryListener;

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
}
