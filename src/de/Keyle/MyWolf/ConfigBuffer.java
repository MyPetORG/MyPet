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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.config.Configuration;

import de.Keyle.MyWolf.util.MyWolfPermissions;

public class ConfigBuffer {
	
	public Logger log = Logger.getLogger("Minecraft");
	public Configuration WolvesConfig;
	public Configuration Config;
	public MyWolf Plugin;
	
	public ConfigVariables cv = new ConfigVariables();
	public MyWolfPermissions Permissions;
	
	public Map<String,Wolves> mWolves = new HashMap<String,Wolves>();

	public ConfigBuffer(MyWolf Plugin) {
		this.Plugin = Plugin;
		Permissions = new MyWolfPermissions(Plugin);
	}
	
	public boolean isNPC(Player p)
	{
		if(Plugin.getServer().getPluginManager().getPlugin("Citizens") != null)
		{
			return com.fullwall.Citizens.NPCs.NPCManager.isNPC(p);
		}
		return false;
	}
}


class ConfigVariables 
{
	public Material WolfLeashItem;
	public Material WolfChestOpenItem;
	public Material WolfChestAddItem;
	public Material WolfFoodHPItem;
	public Material WolfFoodLivesItem;
	public Material WolfPickupItem;
	public Material WolfControlItem;
	public int WolfPickupRange;
	public int WolfRespawnTimeFactor;
	public int WolfRespawnMaxHP;
	public int WolfMaxLives;
	public boolean WolfLeashItemSneak;
	public boolean WolfControlItemSneak;
	public boolean WolfChestOpenItemSneak;
	
	public void setProperty(Configuration cfg,String key,Object value)
	{
		if(cfg.getProperty(key) == null)
		{
			cfg.setProperty(key, value);
		}
	}
}