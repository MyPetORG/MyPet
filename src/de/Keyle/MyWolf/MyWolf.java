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

import java.io.File;
import java.util.List;

import net.minecraft.server.ItemStack;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

import de.Keyle.MyWolf.ConfigBuffer;
import de.Keyle.MyWolf.commands.*;


public class MyWolf extends JavaPlugin{
	
	ConfigBuffer cb = new ConfigBuffer(this);
    private final MyWolfPlayerListener playerListener = new MyWolfPlayerListener(cb);
    private final MyWolfEntityListener entityListener = new MyWolfEntityListener(cb);
    
    public void onDisable(){
    	
    	cb.Plugin.SaveWolves();
    	for ( String owner : cb.mWolves.keySet() )
        {
    		if(cb.mWolves.get(owner).isThere)
    		{
    			cb.mWolves.get(owner).removeWolf();
    		}
        }
        cb.log.info("[MyWolf] Disabled");
    }

    public void onEnable(){
    	
    	cb.Plugin = this;
    	
    	if (!cb.Permissions.setup())
        {
        	cb.log.info("[MyWolf] Permissions integration could not be enabled!");
        }
    	cb.Config = this.getConfiguration();

    	cb.cv.setProperty(cb.Config,"MyWolf.leash.item", 287);//String
    	cb.cv.setProperty(cb.Config,"MyWolf.chest.open.item", 340);//Book
    	cb.cv.setProperty(cb.Config,"MyWolf.chest.add", 54);//Chest
    	cb.cv.setProperty(cb.Config,"MyWolf.food.hp", 357); //Cookie
    	cb.cv.setProperty(cb.Config,"MyWolf.food.lives", 354); //Cake
    	cb.cv.setProperty(cb.Config,"MyWolf.control.item",287);
    	
    	cb.cv.setProperty(cb.Config,"MyWolf.leash.sneak", false);
    	cb.cv.setProperty(cb.Config,"MyWolf.chest.open.sneak", false);
    	cb.cv.setProperty(cb.Config,"MyWolf.control.sneak",false);
			
    	cb.cv.setProperty(cb.Config,"MyWolf.pickup.range", 2); //2 Blocks range
    	cb.cv.setProperty(cb.Config,"MyWolf.pickup.add", 331); //Redstone Dust
    	cb.cv.setProperty(cb.Config,"MyWolf.respawntimefactor", 5); //5 seconds x MaxHP
    	cb.cv.setProperty(cb.Config,"MyWolf.max.HP",20); //20 MaxHPWolfLives
    	cb.cv.setProperty(cb.Config,"MyWolf.max.Lives",-1); //no MaxLives
    	
    	cb.Config.save();

		cb.cv.WolfLeashItem = checkMaterial(cb.Config.getInt("MyWolf.leash.item",287),Material.STRING);
		cb.cv.WolfLeashItemSneak = cb.Config.getBoolean("MyWolf.leash.sneak",false);
		cb.cv.WolfControlItem = checkMaterial(cb.Config.getInt("MyWolf.control.item",287),Material.STRING);
		cb.cv.WolfControlItemSneak = cb.Config.getBoolean("MyWolf.control.sneak",false);
		cb.cv.WolfChestOpenItem = checkMaterial(cb.Config.getInt("MyWolf.chest.open.item",340),Material.BOOK);
		cb.cv.WolfChestOpenItemSneak = cb.Config.getBoolean("MyWolf.chest.open.sneak",false);
		cb.cv.WolfChestAddItem = checkMaterial(cb.Config.getInt("MyWolf.chest.add",54),Material.CHEST);
		cb.cv.WolfFoodHPItem = checkMaterial(cb.Config.getInt("MyWolf.food.hp",357),Material.COOKIE);
		cb.cv.WolfFoodLivesItem = checkMaterial(cb.Config.getInt("MyWolf.food.lives",354),Material.CAKE);
		
		
		
		cb.cv.WolfPickupRange = cb.Config.getInt("MyWolf.pickup.range",2);
		cb.cv.WolfPickupItem = checkMaterial(cb.Config.getInt("MyWolf.pickup.add",331),Material.REDSTONE);
		
		cb.cv.WolfRespawnTimeFactor = cb.Config.getInt("MyWolf.respawntimefactor",5);
		cb.cv.WolfRespawnMaxHP = cb.Config.getInt("MyWolf.max.HP",20);
		cb.cv.WolfMaxLives = cb.Config.getInt("MyWolf.max.Lives",-1);

    	cb.WolvesConfig = new Configuration(new File(this.getDataFolder().getPath() + File.separator + "Wolves.yml"));

		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_JOIN, playerListener, Event.Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_MOVE, playerListener, Event.Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_QUIT, playerListener, Event.Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.ENTITY_DEATH, entityListener, Event.Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_INTERACT, playerListener, Event.Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.ENTITY_DAMAGE, entityListener, Event.Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.ENTITY_TARGET, entityListener, Event.Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_INTERACT, playerListener, Event.Priority.Normal, this);
		//getServer().getPluginManager().registerEvent(Event.Type.ENTITY_MOVE, entityListener, Event.Priority.Normal, this); //for future
		
		
		
		getCommand("wolfname").setExecutor(new MyWolfName(cb));
		MyWolfCall cewc = new MyWolfCall(cb);
        getCommand("wolfcall").setExecutor(cewc);
        getCommand("wc").setExecutor(cewc);
        MyWolfStop cews = new MyWolfStop(cb);
        getCommand("wolfstop").setExecutor(cews);
        getCommand("ws").setExecutor(cews);
        getCommand("wolfrelease").setExecutor(new MyWolfRelease(cb));
        getCommand("wolf").setExecutor(new MyWolfInfo());

        PluginDescriptionFile pdfFile = this.getDescription();
        cb.log.info("["+pdfFile.getName() + "] version " + pdfFile.getVersion() + " ENABLED" );
        
        cb.Plugin.LoadWolves();
        for(Player p : this.getServer().getOnlinePlayers())
        {
        	if(cb.mWolves.containsKey(p.getName()) && p.isOnline() == true)
        	cb.mWolves.get(p.getName()).createWolf(cb.mWolves.get(p.getName()).isSitting);
        }
    }						
	
	public void LoadWolves()
	{
		int anzahlWolves = 0;
		int invSlot = 0;
		double WolfX;
		double WolfY;
		double WolfZ;
		String WolfWorld;
		int WolfHealthNow;
		int WolfHealthMax;
		int WolfLives;
		int WolfHealthRespawnTime;
		String WolfName;
		boolean Wolvesitting;
		boolean WolfhasInventory;
		boolean WolfhasPickup;
		
		cb.WolvesConfig.load();
		List<String>WolfList = cb.WolvesConfig.getKeys("Wolves");
		if(WolfList != null)
		{
			for (String ownername: WolfList) 
			{
				invSlot = 0;
				WolfX = cb.WolvesConfig.getDouble("Wolves."+ownername+".loc.X", 0);
				WolfY = cb.WolvesConfig.getDouble("Wolves."+ownername+".loc.Y", 0);
				WolfZ = cb.WolvesConfig.getDouble("Wolves."+ownername+".loc.Z", 0);
				WolfWorld = cb.WolvesConfig.getString("Wolves."+ownername+".loc.world",cb.Plugin.getServer().getWorlds().get(0).getName());
				WolfHealthNow = cb.WolvesConfig.getInt("Wolves."+ownername+".health.now", 6);
				WolfHealthMax = cb.WolvesConfig.getInt("Wolves."+ownername+".health.max", 6);
				WolfLives = cb.WolvesConfig.getInt("Wolves."+ownername+".health.lives", 3);
				WolfHealthRespawnTime = cb.WolvesConfig.getInt("Wolves."+ownername+".health.respawntime", 0);
				WolfName = cb.WolvesConfig.getString("Wolves."+ownername+".name", "Wolf");
				Wolvesitting = cb.WolvesConfig.getBoolean("Wolves."+ownername+".sitting", false);
				WolfhasInventory = cb.WolvesConfig.getBoolean("Wolves."+ownername+".chest",false);
				WolfhasPickup = cb.WolvesConfig.getBoolean("Wolves."+ownername+".pickup",false);
				
				if(WolfLives == 0)
				{
					continue;
				}
				if(cb.Plugin.getServer().getWorld(WolfWorld) == null)
				{
					cb.log.info("[MyWolf] World \"" + WolfWorld + "\" for " + ownername + "'s wolf \"" + WolfName + "\" not found - skiped wolf");
					continue;
				}
				
				cb.mWolves.put(ownername, new Wolves(cb,ownername));
				
				cb.mWolves.get(ownername).Location = new Location(this.getServer().getWorld(WolfWorld), WolfX, WolfY, WolfZ);
				
				if(WolfLives > cb.cv.WolfMaxLives)
				{
					WolfLives = cb.cv.WolfMaxLives;
				}
				if(WolfHealthMax > cb.cv.WolfRespawnMaxHP)
				{
					WolfHealthMax = cb.cv.WolfRespawnMaxHP;
				}
				if(WolfHealthNow > WolfHealthMax)
				{
					WolfHealthNow = WolfHealthMax;
				}
				if(WolfHealthRespawnTime==0 && WolfHealthNow <= 0)
				{
					WolfHealthNow = WolfHealthMax;
				}
				cb.mWolves.get(ownername).HealthMax = WolfHealthMax;
				cb.mWolves.get(ownername).setWolfHealth(WolfHealthNow);
				cb.mWolves.get(ownername).RespawnTime = WolfHealthRespawnTime;
				cb.mWolves.get(ownername).Name = WolfName;
				cb.mWolves.get(ownername).isSitting = Wolvesitting;
				cb.mWolves.get(ownername).hasInventory = WolfhasInventory;
				cb.mWolves.get(ownername).hasPickup = WolfhasPickup;
				
				for ( String item : cb.WolvesConfig.getString("Wolves."+ownername+".inventory", "").split("\\;") )
				{
					String[] itemvalues = item.split("\\,");
					if(itemvalues.length == 3 && isInt(itemvalues[0]) && isInt(itemvalues[1]) && isInt(itemvalues[2]))
					{
						if(Material.getMaterial(Integer.parseInt(itemvalues[0])) != null)
						{
							if(Integer.parseInt(itemvalues[1])<=64)
							{
								cb.mWolves.get(ownername).Inventory.setItem(invSlot,new ItemStack(Integer.parseInt(itemvalues[0]), Integer.parseInt(itemvalues[1]), Integer.parseInt(itemvalues[2])));
							}
						}
					}
					invSlot++;
				}
				anzahlWolves++;
			}
		}
		cb.log.info("["+this.getDescription().getName() + "] " + anzahlWolves + " wolf/wolves loaded" );
	}
	
	public void SaveWolves()
	{
		cb.WolvesConfig.removeProperty("Wolves");
		for ( String owner : cb.mWolves.keySet() )
        {
			Wolves wolf = cb.mWolves.get( owner );
			String Items = "";
			if(cb.mWolves.get(owner).Inventory.getContents().length > 0)
        	for ( ItemStack Item : cb.mWolves.get(owner).Inventory.getContents() )
        	{
        		if( Item!=null)
        		{
        			Items += Item.id + "," + Item.count + "," + Item.damage + ";";
        		}
        		else
        		{
        			Items += ",,;";
        		}
        	}
			if(cb.mWolves.get(owner).Inventory.getContents().length > 0)
			{
				Items = Items.substring(0,Items.length()-1);
			}
	
        	cb.WolvesConfig.setProperty("Wolves."+owner+".loc.X", wolf.getLocation().getX());
        	cb.WolvesConfig.setProperty("Wolves."+owner+".loc.Y", wolf.getLocation().getY());
        	cb.WolvesConfig.setProperty("Wolves."+owner+".loc.Z", wolf.getLocation().getZ());
        	cb.WolvesConfig.setProperty("Wolves."+owner+".loc.world", wolf.getLocation().getWorld().getName());
        	
        	cb.WolvesConfig.setProperty("Wolves."+owner+".health.now", wolf.getHealth());
        	cb.WolvesConfig.setProperty("Wolves."+owner+".health.max", wolf.HealthMax);
        	cb.WolvesConfig.setProperty("Wolves."+owner+".health.lives", wolf.Lives);
        	cb.WolvesConfig.setProperty("Wolves."+owner+".health.respawntime", wolf.RespawnTime);
        	cb.WolvesConfig.setProperty("Wolves."+owner+".inventory", Items);	
			cb.WolvesConfig.setProperty("Wolves."+owner+".name", wolf.Name);
			cb.WolvesConfig.setProperty("Wolves."+owner+".sitting", wolf.isSitting());
			cb.WolvesConfig.setProperty("Wolves."+owner+".chest", wolf.hasInventory);
			cb.WolvesConfig.setProperty("Wolves."+owner+".pickup", wolf.hasPickup);
        }
		cb.WolvesConfig.save();
	}
	
	public boolean isInt(String number)
    {
    	try {
    		Integer.parseInt(number);
    		return true;
    	}
    	catch(NumberFormatException nFE) {
    		return false;
    	}
    }
	
	public Material checkMaterial(int itemid,Material defaultMaterial)
	{
		if(Material.getMaterial(itemid) == null)
		{
			return defaultMaterial;
		}
		else
		{
			return Material.getMaterial(itemid);
		}
	}
}