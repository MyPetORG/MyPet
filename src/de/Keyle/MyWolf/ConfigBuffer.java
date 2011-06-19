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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.util.config.Configuration;

import de.Keyle.MyWolf.util.MyWolfLanguageVariables;
import de.Keyle.MyWolf.util.MyWolfPermissions;
import de.Keyle.MyWolf.util.MyWolfUtil;

public class ConfigBuffer {
	
	public Logger log = Logger.getLogger("Minecraft");
	public Configuration WolvesConfig;
	public MyWolf Plugin;
	PluginDescriptionFile pdfFile;
	
	public ConfigVariables cv;
	public MyWolfLanguageVariables lv;
	public MyWolfPermissions Permissions;
	
	public Map<String,Wolves> mWolves = new HashMap<String,Wolves>();

	public List<Player> WolfChestOpened = new ArrayList<Player>();
	
	public ConfigBuffer(MyWolf Plugin) {
		this.Plugin = Plugin;
		pdfFile = Plugin.getDescription();
		Permissions = new MyWolfPermissions(this);
	}
}


class ConfigVariables extends Property
{
	Configuration Config;
	
	public ConfigVariables(Configuration cfg)
	{
		super(cfg);
		Config = cfg;
	}

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
	
	public void setStandart()
	{
		Config.setProperty("MyWolf.leash.item", 287);//String
    	Config.setProperty("MyWolf.chest.open.item", 340);//Book
    	Config.setProperty("MyWolf.chest.add", 54);//Chest
    	Config.setProperty("MyWolf.food.hp", 357); //Cookie
    	Config.setProperty("MyWolf.food.lives", 354); //Cake
    	Config.setProperty("MyWolf.control.item",287);
    	Config.setProperty("MyWolf.leash.sneak", false);
    	Config.setProperty("MyWolf.chest.open.sneak", false);
    	Config.setProperty("MyWolf.control.sneak",false);
    	Config.setProperty("MyWolf.pickup.range", 2); //2 Blocks range
    	Config.setProperty("MyWolf.pickup.add", 331); //Redstone Dust
    	Config.setProperty("MyWolf.respawntimefactor", 5); //5 seconds x MaxHP
    	Config.setProperty("MyWolf.max.HP",20); //20 MaxHPWolfLives
    	Config.setProperty("MyWolf.max.Lives",-1); //no MaxLives
    	
    	Config.save();
	}
	
	public void loadVariables()
	{
		WolfLeashItem = MyWolfUtil.checkMaterial(Config.getInt("MyWolf.leash.item",287),Material.STRING);
		WolfLeashItemSneak = Config.getBoolean("MyWolf.leash.sneak",false);
		WolfControlItem = MyWolfUtil.checkMaterial(Config.getInt("MyWolf.control.item",287),Material.STRING);
		WolfControlItemSneak = Config.getBoolean("MyWolf.control.sneak",false);
		WolfChestOpenItem = MyWolfUtil.checkMaterial(Config.getInt("MyWolf.chest.open.item",340),Material.BOOK);
		WolfChestOpenItemSneak = Config.getBoolean("MyWolf.chest.open.sneak",false);
		WolfChestAddItem = MyWolfUtil.checkMaterial(Config.getInt("MyWolf.chest.add",54),Material.CHEST);
		WolfFoodHPItem = MyWolfUtil.checkMaterial(Config.getInt("MyWolf.food.hp",357),Material.COOKIE);
		WolfFoodLivesItem = MyWolfUtil.checkMaterial(Config.getInt("MyWolf.food.lives",354),Material.CAKE);
		WolfPickupRange = Config.getInt("MyWolf.pickup.range",2);
		WolfPickupItem = MyWolfUtil.checkMaterial(Config.getInt("MyWolf.pickup.add",331),Material.REDSTONE);
		WolfRespawnTimeFactor = Config.getInt("MyWolf.respawntimefactor",5);
		WolfRespawnMaxHP = Config.getInt("MyWolf.max.HP",20);
		WolfMaxLives = Config.getInt("MyWolf.max.Lives",-1);
	}
}

class Property
{
	Configuration cfg;
	public Property(Configuration cfg)
	{
		this.cfg = cfg;
	}
	public void setProperty(String key,Object value)
	{
		if(cfg.getProperty(key) == null)
		{
			cfg.setProperty(key, value);
		}
	}
}