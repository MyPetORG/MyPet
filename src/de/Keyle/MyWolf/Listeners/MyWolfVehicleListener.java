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

import de.Keyle.MyWolf.ConfigBuffer;
import de.Keyle.MyWolf.MyWolf.WolfState;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleEnterEvent;

public class MyWolfVehicleListener implements Listener
{
    @EventHandler(priority = EventPriority.LOW)
    public void onVehicleEnter(VehicleEnterEvent event)
    {
        if (event.isCancelled() || !(event.getVehicle() instanceof Minecart))
        {
            return;
        }
        if (event.getEntered() instanceof Wolf)
        {
            for (String owner : ConfigBuffer.mWolves.keySet())
            {
                if (ConfigBuffer.mWolves.get(owner).getID() == event.getEntered().getEntityId())
                {
                    event.setCancelled(true);
                    break;
                }
            }
        }
        if (event.getEntered() instanceof Player)
        {
            Player player = (Player) event.getEntered();
            if (ConfigBuffer.mWolves.containsKey(player.getName()))
            {
                if (ConfigBuffer.mWolves.get(player.getName()).Status == WolfState.Here && !ConfigBuffer.mWolves.get(player.getName()).isSitting())
                {
                    ConfigBuffer.mWolves.get(player.getName()).Wolf.setSitting(true);
                }
            }
        }
    }
}
