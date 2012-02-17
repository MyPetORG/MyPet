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

package de.Keyle.MyWolf.Listeners;

import de.Keyle.MyWolf.MyWolfPlugin;
import de.Keyle.MyWolf.util.MyWolfList;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.getspout.spoutapi.event.inventory.InventoryCloseEvent;

public class MyWolfInventoryListener implements Listener
{
    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClose(InventoryCloseEvent event)
    {
        if (MyWolfPlugin.WolfChestOpened.contains(event.getPlayer()) && MyWolfList.hasMyWolf(event.getPlayer()))
        {
            MyWolfList.getMyWolf(event.getPlayer()).setSitting(false);
            MyWolfPlugin.WolfChestOpened.remove(event.getPlayer());
        }
        if(MyWolfPlugin.OpenMyWolfChests.contains(event.getPlayer()))
        {
            MyWolfPlugin.OpenMyWolfChests.remove(event.getPlayer());
        }
    }
}
