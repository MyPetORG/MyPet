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
import java.util.LinkedList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.CreatureType;
import org.bukkit.util.config.Configuration;

import de.Keyle.MyWolf.ConfigBuffer;
import de.Keyle.MyWolf.Skill.MyWolfExperience;

public class MyWolfConfig
{
	public static Configuration Config;

	public static Material LeashItem = Material.STRING;
	public static Material ControlItem = Material.STRING;
	public static int PickupRange = 2;
	public static int RespawnTimeFactor = 5;
	public static int RespawnTimeFixed = 0;
	public static int MaxLives = -1;
	public static int ExpFactor = 2;
	public static int NameColor = -1;

	public static void setStandart()
	{
		Config.setHeader("#### The names of the creaturetypes have to be in capital letters!!! Delete the creaturetypes you don't want to change to avoid errors. ####");
		
		setProperty("MyWolf.leash.item", 287);
		setProperty("MyWolf.control.item", 287);
		setProperty("MyWolf.pickup.range", 2);
		setProperty("MyWolf.respawntime.factor", 5);
		setProperty("MyWolf.respawntime.fixed", 0);
		setProperty("MyWolf.max.Lives", -1);
		setProperty("MyWolf.expfactor", 2);
		setProperty("MyWolf.namecolor", -1);

		if(Config.getKeys("MyWolf").contains("exp") == false)
		{
			setProperty("MyWolf.exp.SKELETON", 1.1);
			setProperty("MyWolf.exp.ZOMBIE", 1.1);
			setProperty("MyWolf.exp.SPIDER", 1.05);
			setProperty("MyWolf.exp.WOLF", 0.5);
			setProperty("MyWolf.exp.CREEPER", 1.55);
			setProperty("MyWolf.exp.GHAST", 0.85);
			setProperty("MyWolf.exp.PIG_ZOMBIE", 1.1);
			setProperty("MyWolf.exp.GIANT", 10.75);
			setProperty("MyWolf.exp.COW", 0.25);
			setProperty("MyWolf.exp.PIG", 0.25);
			setProperty("MyWolf.exp.CHICKEN", 0.25);
			setProperty("MyWolf.exp.SQUID", 0.25);
			setProperty("MyWolf.exp.SHEEP", 0.25);
		}

		if(Config.getKeys("MyWolf").contains("skills") == false)
		{
			List<String> list = new LinkedList<String>();
			list.add("InventorySmall");
			setProperty("MyWolf.skills.2", list);
			
			list = new LinkedList<String>();
			list.add("InventoryLarge");
			list.add("InventoryLarge");
			setProperty("MyWolf.skills.3", list);
			
			list = new LinkedList<String>();
			list.add("Pickup");
			setProperty("MyWolf.skills.5", list);
			
			list = new LinkedList<String>();
			list.add("Behavior");
			list.add("HP");
			list.add("HP");
			setProperty("MyWolf.skills.6", list);
		}

		Config.save();
	}

	public static void loadVariables()
	{
		LeashItem = MyWolfUtil.checkMaterial(Config.getInt("MyWolf.leash.item", 287), Material.STRING);
		ControlItem = MyWolfUtil.checkMaterial(Config.getInt("MyWolf.control.item", 287), Material.STRING);
		PickupRange = Config.getInt("MyWolf.pickup.range", 2);
		RespawnTimeFactor = Config.getInt("MyWolf.respawntime.factor", 5);
		RespawnTimeFixed = Config.getInt("MyWolf.respawntime.fixed", 0);
		MaxLives = Config.getInt("MyWolf.max.Lives", -1);
		ExpFactor = Config.getInt("MyWolf.expfactor", 2);
		NameColor = Config.getInt("MyWolf.namecolor", -1);
		NameColor = NameColor<=0xf?NameColor:-1;
		
		if(Config.getKeys("MyWolf.exp") != null)
		{
			for(String key : Config.getKeys("MyWolf.exp"))
			{
				double expval = Config.getDouble("MyWolf.exp." + key, -1.0);
				if(expval > -1)
				{
					MyWolfExperience.MobEXP.put(CreatureType.valueOf(key), expval);
				}
			}
		}
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
