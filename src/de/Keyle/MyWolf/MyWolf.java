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
    	
    	cb.Plugin.SaveWolfs();
    	for ( String owner : cb.mWolfs.keySet() )
        {
    		if(cb.mWolfs.get(owner).isThere)
    		{
    			cb.mWolfs.get(owner).removeWolf();
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
    	//cb.cv.setProperty(cb.Config,"MyWolf.WeaponItems",""); //
    	
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
		
		for ( String item : cb.Config.getString("MyWolf.WeaponItems", "").split("\\;") )
		{
			String[] itemvalues = item.split("\\,");
			if(itemvalues.length == 2 && isInt(itemvalues[0]) && isInt(itemvalues[1]))
			{
				cb.cv.WeaponList.put(Integer.parseInt(itemvalues[0]), Integer.parseInt(itemvalues[1]));
			}
		}

    	cb.WolfsConfig = new Configuration(new File(this.getDataFolder().getPath() + File.separator + "Wolfs.yml"));

		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_JOIN, playerListener, Event.Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_MOVE, playerListener, Event.Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_QUIT, playerListener, Event.Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.ENTITY_DEATH, entityListener, Event.Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_INTERACT, playerListener, Event.Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.ENTITY_DAMAGE, entityListener, Event.Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.ENTITY_TARGET, entityListener, Event.Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_INTERACT, playerListener, Event.Priority.Normal, this);
		//getServer().getPluginManager().registerEvent(Event.Type.ENTITY_MOVE, entityListener, Event.Priority.Normal, this); //for Future
		
		
		
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
        
        cb.Plugin.LoadWolfs();
        for(Player p : this.getServer().getOnlinePlayers())
        {
        	if(cb.mWolfs.containsKey(p.getName()) && p.isOnline() == true)
        	cb.mWolfs.get(p.getName()).createWolf(cb.mWolfs.get(p.getName()).isSitting);
        }
    }
    
						/*
						else if(commandname.equalsIgnoreCase("compass")){
							if(cb.Permissions != null && cb.Permissions.has(player, "mywolf.compass") == false)
							{
								return false;
							}
							if(args.length == 1 && args[0].equalsIgnoreCase("stop"))
							{
								player.sendMessage("Your compass targets the spawn point!");
								player.setCompassTarget(player.getWorld().getSpawnLocation());
							}
							else
							{
								player.sendMessage("Your compass targets the last location of the wolf!");
								player.setCompassTarget(cb.mWolfs.get(player.getName()).getLoc());
							}
							return true;
						}
						
						if(commandname.equalsIgnoreCase("attack")){
							if(cb.Permissions != null && cb.Permissions.has(player, "mywolf.attack") == false)
							{
								return false;
							}
							if(args.length == 1)
							{
								if(args[0].equalsIgnoreCase("deny"))
								{
									player.sendMessage("Your wolf will not attack!");
									cb.mWolfs.get(player.getName()).allowAttackMonster = false;
									cb.mWolfs.get(player.getName()).allowAttackPlayer = false;
								}
								else if(args[0].equalsIgnoreCase("allow"))
								{
									player.sendMessage("Your wolf will now attack!");
									cb.mWolfs.get(player.getName()).allowAttackMonster = true;
									cb.mWolfs.get(player.getName()).allowAttackPlayer = true;
								}
								else
								{
									player.sendMessage("Syntax: /wolf allow|deny [player|monster|all]");
								}
							}
							if(args.length == 2)
							{
								if(args[0].equalsIgnoreCase("deny"))
								{
									if(args[0].equalsIgnoreCase("player"))
									{
										player.sendMessage("Your wolf will not attack player!");
										cb.mWolfs.get(player.getName()).allowAttackPlayer = false;
									}
									else if(args[0].equalsIgnoreCase("monster"))
									{
										player.sendMessage("Your wolf will not attack monster!");
										cb.mWolfs.get(player.getName()).allowAttackMonster = false;
									}
									else if(args[0].equalsIgnoreCase("all"))
									{
										player.sendMessage("Your wolf will not attack!");
										cb.mWolfs.get(player.getName()).allowAttackMonster = false;
										cb.mWolfs.get(player.getName()).allowAttackPlayer = false;
									}
									else
									{
										player.sendMessage("Syntax: /wolf allow|deny [player|monster|all]");
									}
								}
								else if(args[0].equalsIgnoreCase("allow"))
								{
									if(args[0].equalsIgnoreCase("player"))
									{
										player.sendMessage("Your wolf will now attack player!");
										cb.mWolfs.get(player.getName()).allowAttackPlayer = true;
									}
									else if(args[0].equalsIgnoreCase("monster"))
									{
										player.sendMessage("Your wolf will now attack monster!");
										cb.mWolfs.get(player.getName()).allowAttackMonster = true;
									}
									else if(args[0].equalsIgnoreCase("all"))
									{
										player.sendMessage("Your wolf will now attack all!");
										cb.mWolfs.get(player.getName()).allowAttackMonster = true;
										cb.mWolfs.get(player.getName()).allowAttackPlayer = true;
									}
									else
									{
										player.sendMessage("Syntax: /wolf allow|deny [player|monster|all]");
									}
								}
								else
								{
									player.sendMessage("Syntax: /wolf allow|deny [player|monster|all]");
								}
							}
							
							cb.mWolfs.get(player.getName()).MyWolf.setTarget((LivingEntity)null);
						}
						*/
    
	
	
	public void LoadWolfs()
	{
		int anzahlWolfs = 0;
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
		boolean WolfSitting;
		boolean WolfhasInventory;
		boolean WolfhasPickup;
		
		cb.WolfsConfig.load();
		List<String>WolfList = cb.WolfsConfig.getKeys("Wolfs");
		if(WolfList != null)
		{
			for (String ownername: WolfList) 
			{
				invSlot = 0;
				WolfX = cb.WolfsConfig.getDouble("Wolfs."+ownername+".loc.X", 0);
				WolfY = cb.WolfsConfig.getDouble("Wolfs."+ownername+".loc.Y", 0);
				WolfZ = cb.WolfsConfig.getDouble("Wolfs."+ownername+".loc.Z", 0);
				WolfWorld = cb.WolfsConfig.getString("Wolfs."+ownername+".loc.world",cb.Plugin.getServer().getWorlds().get(0).getName());
				WolfHealthNow = cb.WolfsConfig.getInt("Wolfs."+ownername+".health.now", 6);
				WolfHealthMax = cb.WolfsConfig.getInt("Wolfs."+ownername+".health.max", 6);
				WolfLives = cb.WolfsConfig.getInt("Wolfs."+ownername+".health.lives", 3);
				WolfHealthRespawnTime = cb.WolfsConfig.getInt("Wolfs."+ownername+".health.respawntime", 0);
				WolfName = cb.WolfsConfig.getString("Wolfs."+ownername+".name", "Wolf");
				WolfSitting = cb.WolfsConfig.getBoolean("Wolfs."+ownername+".sitting", false);
				WolfhasInventory = cb.WolfsConfig.getBoolean("Wolfs."+ownername+".chest",false);
				WolfhasPickup = cb.WolfsConfig.getBoolean("Wolfs."+ownername+".pickup",false);
				
				if(WolfLives == 0)
				{
					continue;
				}
				if(cb.Plugin.getServer().getWorld(WolfWorld) == null)
				{
					cb.log.info("[MyWolf] World for wolf \"" + WolfName + "\" not found - skiped wolf");
				}
				
				cb.mWolfs.put(ownername, new Wolfs(cb,ownername));
				
				cb.mWolfs.get(ownername).WolfLocation = new Location(this.getServer().getWorld(WolfWorld), WolfX, WolfY, WolfZ);
				
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
				cb.mWolfs.get(ownername).HealthMax = WolfHealthMax;
				cb.mWolfs.get(ownername).setWolfHealth(WolfHealthNow);
				cb.mWolfs.get(ownername).RespawnTime = WolfHealthRespawnTime;
				cb.mWolfs.get(ownername).Name = WolfName;
				cb.mWolfs.get(ownername).isSitting = WolfSitting;
				cb.mWolfs.get(ownername).hasInventory = WolfhasInventory;
				cb.mWolfs.get(ownername).hasPickup = WolfhasPickup;
				
				for ( String item : cb.WolfsConfig.getString("Wolfs."+ownername+".inventory", "").split("\\;") )
				{
					String[] itemvalues = item.split("\\,");
					if(itemvalues.length == 3 && isInt(itemvalues[0]) && isInt(itemvalues[1]) && isInt(itemvalues[2]))
					{
						if(Material.getMaterial(Integer.parseInt(itemvalues[0])) != null)
						{
							if(Integer.parseInt(itemvalues[1])<=64)
							{
								cb.mWolfs.get(ownername).WolfInventory.setItem(invSlot,new ItemStack(Integer.parseInt(itemvalues[0]), Integer.parseInt(itemvalues[1]), Integer.parseInt(itemvalues[2])));
							}
						}
					}
					invSlot++;
				}
				anzahlWolfs++;
			}
		}
		cb.log.info("["+this.getDescription().getName() + "] " + anzahlWolfs + " Wolfs loaded" );
	}
	
	public void SaveWolfs()
	{
		cb.WolfsConfig.removeProperty("Wolfs");
		for ( String owner : cb.mWolfs.keySet() )
        {
			Wolfs wolf = cb.mWolfs.get( owner );
			String Items = "";
        	for ( ItemStack Item : cb.mWolfs.get(owner).WolfInventory.getContents() )
        	{
        		if(Item != null)
        		{
        			Items += Item.id + "," + Item.count + "," + Item.damage + ";";
        		}
        		else
        		{
        			Items += ",,;";
        		}
        	}
        	Items = Items.substring(0,Items.length()-1);
	
        	cb.WolfsConfig.setProperty("Wolfs."+owner+".loc.X", wolf.getLoc().getX());
        	cb.WolfsConfig.setProperty("Wolfs."+owner+".loc.Y", wolf.getLoc().getY());
        	cb.WolfsConfig.setProperty("Wolfs."+owner+".loc.Z", wolf.getLoc().getZ());
        	        	
        	cb.log.info(""+wolf.getLoc());
        	cb.WolfsConfig.setProperty("Wolfs."+owner+".loc.world", wolf.getLoc().getWorld().getName());
        	
        	cb.WolfsConfig.setProperty("Wolfs."+owner+".health.now", wolf.getHealth());
        	cb.WolfsConfig.setProperty("Wolfs."+owner+".health.max", wolf.HealthMax);
        	cb.WolfsConfig.setProperty("Wolfs."+owner+".health.lives", wolf.Lives);
        	cb.WolfsConfig.setProperty("Wolfs."+owner+".health.respawntime", wolf.RespawnTime);
        	cb.WolfsConfig.setProperty("Wolfs."+owner+".inventory", Items);	
			cb.WolfsConfig.setProperty("Wolfs."+owner+".name", wolf.Name);
			cb.WolfsConfig.setProperty("Wolfs."+owner+".sitting", wolf.isSitting());
			cb.WolfsConfig.setProperty("Wolfs."+owner+".chest", wolf.hasInventory);
			cb.WolfsConfig.setProperty("Wolfs."+owner+".pickup", wolf.hasPickup);
        }
		cb.WolfsConfig.save();
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