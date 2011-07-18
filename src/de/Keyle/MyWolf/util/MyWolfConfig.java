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
	Configuration Config;
	ConfigBuffer cb;

	public MyWolfConfig(Configuration cfg, ConfigBuffer cb)
	{
		Config = cfg;
		this.cb = cb;
	}

	public static Material WolfLeashItem = Material.STRING;
	public static Material WolfControlItem = Material.STRING;
	public static int WolfPickupRange = 2;
	public static int WolfRespawnTimeFactor = 5;
	public static int WolfMaxLives = -1;
	public static int WolfInterfaceMapId = 0;
	public static int WolfExpFactor = 2;

	public void setStandart()
	{
		setProperty("MyWolf.leash.item", 287);//String
		setProperty("MyWolf.control.item", 287);
		setProperty("MyWolf.pickup.range", 2); //2 Blocks range
		setProperty("MyWolf.respawntimefactor", 5); //5 seconds x MaxHP
		setProperty("MyWolf.max.Lives", -1); //no MaxLives
		//setProperty("MyWolf.mapid", 0); //no MaxLives
		setProperty("MyWolf.expfactor", 2); //2

		Config.save();
	}

	public void loadVariables()
	{
		WolfLeashItem = MyWolfUtil.checkMaterial(Config.getInt("MyWolf.leash.item", 287), Material.STRING);
		WolfControlItem = MyWolfUtil.checkMaterial(Config.getInt("MyWolf.control.item", 287), Material.STRING);
		WolfPickupRange = Config.getInt("MyWolf.pickup.range", 2);
		WolfRespawnTimeFactor = Config.getInt("MyWolf.respawntimefactor", 5);
		WolfMaxLives = Config.getInt("MyWolf.max.Lives", -1);
		WolfInterfaceMapId = Config.getInt("MyWolf.max.Lives", -1);
		WolfExpFactor = Config.getInt("MyWolf.expfactor", 2);

		if (Config.getKeys("MyWolf.skills") != null)
		{
			for (String lvl : Config.getKeys("MyWolf.skills"))
			{
				List<String> Skills = Arrays.asList(Config.getString("MyWolf.skills." + lvl.toString()).replace("[", "").replace("]", "").split("\\, "));
				ConfigBuffer.SkillPerLevel.put(Integer.parseInt(lvl), Skills);
			}
		}

	}

	public void setProperty(String key, Object value)
	{
		if (Config.getProperty(key) == null)
		{
			Config.setProperty(key, value);
		}
	}
}
