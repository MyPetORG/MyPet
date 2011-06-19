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
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

import de.Keyle.MyWolf.ConfigBuffer;
import de.Keyle.MyWolf.Wolves.InventoryType;
import de.Keyle.MyWolf.commands.*;
import de.Keyle.MyWolf.util.MyWolfLanguageVariables;
import de.Keyle.MyWolf.util.MyWolfUtil;


public class MyWolf extends JavaPlugin{
	
	ConfigBuffer cb;
    private MyWolfPlayerListener playerListener;
    private MyWolfEntityListener entityListener;
    private MyWolfInventoryListener inventoryListener;
    private MyWolfVehicleListener vehicleListener;
    
    public void onDisable(){
    	
    	cb.Plugin.SaveWolves();
    	for ( String owner : cb.mWolves.keySet() )
        {
    		if(cb.mWolves.get(owner).isThere)
    		{
    			cb.mWolves.get(owner).removeWolf();
    		}
        }
    	cb.mWolves.clear();
    	cb.WolfChestOpened.clear();

        cb.log.info("[MyWolf] Disabled");
        
        cb = null;
    }

    public void onEnable(){
    	cb = new ConfigBuffer(this);
    	
    	playerListener = new MyWolfPlayerListener(cb);
    	getServer().getPluginManager().registerEvent(Event.Type.PLAYER_JOIN, playerListener, Event.Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_MOVE, playerListener, Event.Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_QUIT, playerListener, Event.Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_INTERACT, playerListener, Event.Priority.Normal, this);
		
		vehicleListener = new MyWolfVehicleListener(cb);
		getServer().getPluginManager().registerEvent(Event.Type.VEHICLE_ENTER, vehicleListener, Event.Priority.Low, this);
		
    	entityListener = new MyWolfEntityListener(cb);
    	getServer().getPluginManager().registerEvent(Event.Type.ENTITY_DAMAGE, entityListener, Event.Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.ENTITY_TARGET, entityListener, Event.Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.ENTITY_DEATH, entityListener, Event.Priority.Normal, this);
		
		if(cb.Plugin.getServer().getPluginManager().getPlugin("BukkitContrib") != null)
		{
			inventoryListener = new MyWolfInventoryListener(cb);
			getServer().getPluginManager().registerEvent(Event.Type.CUSTOM_EVENT, inventoryListener, Event.Priority.Normal, this);
		}
    	
    	cb.Permissions.setup();
    	
    	getCommand("wolfname").setExecutor(new MyWolfName(cb));
        getCommand("wolfcall").setExecutor(new MyWolfCall(cb));
        getCommand("wolfstop").setExecutor(new MyWolfStop(cb));
        getCommand("wolfrelease").setExecutor(new MyWolfRelease(cb));
        getCommand("wolf").setExecutor(new MyWolfInfo());

    	cb.cv = new ConfigVariables(this.getConfiguration());
    	cb.cv.setStandart();
		cb.cv.loadVariables();
		
		cb.lv = new MyWolfLanguageVariables(new Configuration(new File(this.getDataFolder().getPath() + File.separator + "lang.yml")));
		cb.lv.setStandart();		
		cb.lv.loadVariables();
		
		
    	cb.WolvesConfig = new Configuration(new File(this.getDataFolder().getPath() + File.separator + "Wolves.yml"));		
        cb.Plugin.LoadWolves();
        
        for(Player p : this.getServer().getOnlinePlayers())
        {
        	if(cb.mWolves.containsKey(p.getName()) && p.isOnline() == true)
        	cb.mWolves.get(p.getName()).createWolf(cb.mWolves.get(p.getName()).isSitting);
        }
        
        cb.log.info("["+cb.pdfFile.getName() + "] version " + cb.pdfFile.getVersion() + " ENABLED" );
    }						
	
	public void LoadWolves()
	{
		
		int anzahlWolves = 0;

		cb.WolvesConfig.load();
		List<String>WolfList = cb.WolvesConfig.getKeys("Wolves");
		if(WolfList != null)
		{
			for (String ownername: WolfList) 
			{
				int invSlot = 0;
				double WolfX = cb.WolvesConfig.getDouble("Wolves."+ownername+".loc.X", 0);
				double WolfY = cb.WolvesConfig.getDouble("Wolves."+ownername+".loc.Y", 0);
				double WolfZ = cb.WolvesConfig.getDouble("Wolves."+ownername+".loc.Z", 0);
				String WolfWorld = cb.WolvesConfig.getString("Wolves."+ownername+".loc.world",cb.Plugin.getServer().getWorlds().get(0).getName());
				int WolfHealthNow = cb.WolvesConfig.getInt("Wolves."+ownername+".health.now", 6);
				int WolfHealthMax = cb.WolvesConfig.getInt("Wolves."+ownername+".health.max", 6);
				int WolfLives = cb.WolvesConfig.getInt("Wolves."+ownername+".health.lives", 3);
				int WolfHealthRespawnTime = cb.WolvesConfig.getInt("Wolves."+ownername+".health.respawntime", 0);
				String WolfName = cb.WolvesConfig.getString("Wolves."+ownername+".name", "Wolf");
				boolean Wolvesitting = cb.WolvesConfig.getBoolean("Wolves."+ownername+".sitting", false);
				String WolfInventoryMode = cb.WolvesConfig.getString("Wolves."+ownername+".chest","NONE");
				boolean WolfhasPickup = cb.WolvesConfig.getBoolean("Wolves."+ownername+".pickup",false);
				
				if(WolfInventoryMode.equals("NONE") == false && WolfInventoryMode.equals("SMALL") == false && WolfInventoryMode.equals("LARGE") == false)
				{
					WolfInventoryMode = WolfInventoryMode.equalsIgnoreCase("true")?"SMALL":"NONE";
				}
				
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
				cb.mWolves.get(ownername).InventoryMode = InventoryType.valueOf(WolfInventoryMode);
				cb.mWolves.get(ownername).hasPickup = WolfhasPickup;
				
				String inv1 = cb.WolvesConfig.getString("Wolves."+ownername+".inventory");

				if(inv1.startsWith("{"))
				{
					inv1 = cb.WolvesConfig.getString("Wolves."+ownername+".inventory.1");
				}
				for (int i = 0;i<2;i++)
				{
					invSlot = 0;
					if(i==0 || (i>0 && cb.mWolves.get(ownername).InventoryMode == InventoryType.LARGE))
					{
						for ( String item : (i==0?inv1:cb.WolvesConfig.getString("Wolves."+ownername+".inventory.2", "")).split("\\;") )
						{
							String[] itemvalues = item.split("\\,");
							if(itemvalues.length == 3 && MyWolfUtil.isInt(itemvalues[0]) && MyWolfUtil.isInt(itemvalues[1]) && MyWolfUtil.isInt(itemvalues[2]))
							{
								if(Material.getMaterial(Integer.parseInt(itemvalues[0])) != null)
								{
									if(Integer.parseInt(itemvalues[1])<=64)
									{
										cb.mWolves.get(ownername).Inventory[i].setItem(invSlot,new ItemStack(Integer.parseInt(itemvalues[0]), Integer.parseInt(itemvalues[1]), Integer.parseInt(itemvalues[2])));
									}
								}
							}
							invSlot++;
						}
					}
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
			for (int i = 0;i<2;i++)
			{
				String Items = "";
				if(cb.mWolves.get(owner).Inventory[i].getContents().length > 0)
				{
		        	for ( ItemStack Item : cb.mWolves.get(owner).Inventory[i].getContents() )
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
		        	Items = Items.substring(0,Items.length()-1);
				}
				cb.WolvesConfig.setProperty("Wolves."+owner+".inventory."+(i+1), Items);
			}
        	cb.WolvesConfig.setProperty("Wolves."+owner+".loc.X", wolf.getLocation().getX());
        	cb.WolvesConfig.setProperty("Wolves."+owner+".loc.Y", wolf.getLocation().getY());
        	cb.WolvesConfig.setProperty("Wolves."+owner+".loc.Z", wolf.getLocation().getZ());
        	cb.WolvesConfig.setProperty("Wolves."+owner+".loc.world", wolf.getLocation().getWorld().getName());
        	
        	cb.WolvesConfig.setProperty("Wolves."+owner+".health.now", wolf.getHealth());
        	cb.WolvesConfig.setProperty("Wolves."+owner+".health.max", wolf.HealthMax);
        	cb.WolvesConfig.setProperty("Wolves."+owner+".health.lives", wolf.Lives);
        	cb.WolvesConfig.setProperty("Wolves."+owner+".health.respawntime", wolf.RespawnTime);
			cb.WolvesConfig.setProperty("Wolves."+owner+".name", wolf.Name);
			cb.WolvesConfig.setProperty("Wolves."+owner+".sitting", wolf.isSitting());
			cb.WolvesConfig.setProperty("Wolves."+owner+".chest", wolf.InventoryMode.name());
			cb.WolvesConfig.setProperty("Wolves."+owner+".pickup", wolf.hasPickup);
        }
		cb.WolvesConfig.save();
	}
}