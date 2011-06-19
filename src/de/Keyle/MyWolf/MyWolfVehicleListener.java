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

package de.Keyle.MyWolf;

import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleListener;

public class MyWolfVehicleListener extends VehicleListener{
	
	private ConfigBuffer cb;
	
	public MyWolfVehicleListener(ConfigBuffer cb) {
		this.cb = cb;
    }
    
	@Override
	public void onVehicleEnter(VehicleEnterEvent event) {
		if (event.isCancelled() || !(event.getVehicle() instanceof Minecart)) {
			return;
		}
		if(event.getEntered() instanceof Wolf)
		{
			for ( String owner : cb.mWolves.keySet() )
	        {
				if(cb.mWolves.get( owner ).MyWolf == (Wolf)event.getEntered())
				{
					event.setCancelled(true);
					break;
				}
	        }
		}
		if(event.getEntered() instanceof Player)
		{
			Player player = (Player)event.getEntered();
			if(cb.mWolves.containsKey(player.getName()))
			{
				if(cb.mWolves.get(player.getName()).isThere == true && cb.mWolves.get(player.getName()).isDead == false && cb.mWolves.get(player.getName()).isSitting() == false)
				{
					cb.mWolves.get(player.getName()).MyWolf.setSitting(true);
				}
			}
		}
	}
}