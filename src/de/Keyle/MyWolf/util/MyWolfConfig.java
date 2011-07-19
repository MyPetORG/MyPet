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

import java.util.Arrays;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.util.config.Configuration;

import de.Keyle.MyWolf.ConfigBuffer;

public class MyWolfConfig
{
	private static Configuration Config;

	public static Material LeashItem = Material.STRING;
	public static Material ControlItem = Material.STRING;
	public static int PickupRange = 2;
	public static int RespawnTimeFactor = 5;
	public static int MaxLives = -1;
	public static int InterfaceMapId = 0;
	public static int ExpFactor = 2;
	public static int NameColor = -1;

	public static void setStandart()
	{
		setProperty("MyWolf.leash.item", 287);
		setProperty("MyWolf.control.item", 287);
		setProperty("MyWolf.pickup.range", 2);
		setProperty("MyWolf.respawntimefactor", 5);
		setProperty("MyWolf.max.Lives", -1);
		setProperty("MyWolf.expfactor", 2);
		setProperty("MyWolf.namecolor", -1);

		Config.save();
	}

	public static void loadVariables()
	{
		LeashItem = MyWolfUtil.checkMaterial(Config.getInt("MyWolf.leash.item", 287), Material.STRING);
		ControlItem = MyWolfUtil.checkMaterial(Config.getInt("MyWolf.control.item", 287), Material.STRING);
		PickupRange = Config.getInt("MyWolf.pickup.range", 2);
		RespawnTimeFactor = Config.getInt("MyWolf.respawntimefactor", 5);
		MaxLives = Config.getInt("MyWolf.max.Lives", -1);
		InterfaceMapId = Config.getInt("MyWolf.max.Lives", -1);
		ExpFactor = Config.getInt("MyWolf.expfactor", 2);
		NameColor = Config.getInt("MyWolf.namecolor", -1);
		NameColor = NameColor<=0xf?NameColor:-1;
		

		if (Config.getKeys("MyWolf.skills") != null)
		{
			for (String lvl : Config.getKeys("MyWolf.skills"))
			{
				List<String> Skills = Arrays.asList(Config.getString("MyWolf.skills." + lvl.toString()).replace("[", "").replace("]", "").split("\\, "));
				ConfigBuffer.SkillPerLevel.put(Integer.parseInt(lvl), Skills);
			}
		}

	}

	public static void setProperty(String key, Object value)
	{
		if (Config.getProperty(key) == null)
		{
			Config.setProperty(key, value);
		}
	}
}
