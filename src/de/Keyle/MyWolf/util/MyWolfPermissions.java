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

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;

import de.Keyle.MyWolf.MyWolf;

public class MyWolfPermissions
{
	MyWolf Plugin;
	private Object Permissions;
	private int Mode = -1;
	
	public MyWolfPermissions(MyWolf Plugin)
	{
		this.Plugin = Plugin;
	}
	
	public boolean has(Player player, String node)
	{
		if (this.Permissions == null)
		{
			return true;
		}
		else if (player == null)
		{
			return true;
		}
		else if (Mode == 0 && Permissions instanceof PermissionHandler)
		{
			return ((PermissionHandler) this.Permissions).has(player, node);
		}
		return false;
		
	}
	
	public boolean setup()
	{
    	Plugin testPresent = Plugin.getServer().getPluginManager().getPlugin("Permissions");
        if (testPresent != null)
        {
            Permissions = ((Permissions)testPresent).getHandler();
            Mode = 0;
            return true;
        }
        else
        {
            return false;
        }
    }
}