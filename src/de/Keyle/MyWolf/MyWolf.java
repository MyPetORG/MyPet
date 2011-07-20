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

/*
    1. make the health start at 8 and don't have that overcharge or whatever that goes above the set health like 18/10.
    4. fix the signal to attack in the snow. I think the layer of snow above the block is messing it up. It might have trouble climbing hills too or something.
    6. fix when the dog is sitting and you can't call it.
    7. is there anyway to modify the pathfinding to keep it from bumping you and running into fire?
*/

package de.Keyle.MyWolf;

import java.io.File;
import java.util.List;
import net.minecraft.server.ItemStack;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.Event;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

import de.Keyle.MyWolf.ConfigBuffer;
import de.Keyle.MyWolf.Listeners.*;
import de.Keyle.MyWolf.Skill.Skills.*;
import de.Keyle.MyWolf.Wolves.WolfState;
import de.Keyle.MyWolf.chatcommands.*;
import de.Keyle.MyWolf.util.MyWolfConfig;
import de.Keyle.MyWolf.util.MyWolfLanguage;
import de.Keyle.MyWolf.util.MyWolfUtil;
import de.Keyle.MyWolf.util.MyWolfPermissions;

public class MyWolf extends JavaPlugin
{

	ConfigBuffer cb;
	private MyWolfPlayerListener playerListener;
	private MyWolfEntityListener entityListener;
	private MyWolfInventoryListener inventoryListener;
	private MyWolfVehicleListener vehicleListener;
	private MyWolfLevelUpListener levelupListener;
	private MyWolfWorldListener worldListener;

	public static MyWolf Plugin;

	public void onDisable()
	{
		SaveWolves(ConfigBuffer.WolvesConfig);
		for (String owner : ConfigBuffer.mWolves.keySet())
		{
			if (ConfigBuffer.mWolves.get(owner).Status == WolfState.Here)
			{
				ConfigBuffer.mWolves.get(owner).removeWolf();
			}
		}
		getServer().getScheduler().cancelTasks(this);
		ConfigBuffer.mWolves.clear();
		ConfigBuffer.WolfChestOpened.clear();

		MyWolfUtil.Log.info("[MyWolf] Disabled");

		cb = null;
	}

	public void onEnable()
	{
		Plugin = this;

		playerListener = new MyWolfPlayerListener();
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_PORTAL, playerListener, Event.Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_JOIN, playerListener, Event.Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_MOVE, playerListener, Event.Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_QUIT, playerListener, Event.Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_INTERACT, playerListener, Event.Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_INTERACT_ENTITY, playerListener, Event.Priority.Normal, this);

		vehicleListener = new MyWolfVehicleListener();
		getServer().getPluginManager().registerEvent(Event.Type.VEHICLE_ENTER, vehicleListener, Event.Priority.Low, this);

		worldListener = new MyWolfWorldListener();
		getServer().getPluginManager().registerEvent(Event.Type.CHUNK_UNLOAD, worldListener, Event.Priority.Normal, this);

		entityListener = new MyWolfEntityListener();
		getServer().getPluginManager().registerEvent(Event.Type.ENTITY_DAMAGE, entityListener, Event.Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.ENTITY_TARGET, entityListener, Event.Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.ENTITY_DEATH, entityListener, Event.Priority.Normal, this);

		levelupListener = new MyWolfLevelUpListener();
		getServer().getPluginManager().registerEvent(Event.Type.CUSTOM_EVENT, levelupListener, Event.Priority.Normal, this);

		inventoryListener = new MyWolfInventoryListener();
		getServer().getPluginManager().registerEvent(Event.Type.CUSTOM_EVENT, inventoryListener, Event.Priority.Normal, this);

		MyWolfPermissions.setup();

		getCommand("wolfname").setExecutor(new MyWolfName());
		getCommand("wolfcall").setExecutor(new MyWolfCall());
		getCommand("wolfstop").setExecutor(new MyWolfStop());
		getCommand("wolfrelease").setExecutor(new MyWolfRelease());
		getCommand("wolf").setExecutor(new MyWolfHelp());
		getCommand("wolfinventory").setExecutor(new MyWolfInventory());
		getCommand("wolfpickup").setExecutor(new MyWolfPickup());
		getCommand("wolfbehavior").setExecutor(new MyWolfBehavior());
		getCommand("wolfexp").setExecutor(new MyWolfEXP());

		new Inventory();
		new HP();
		new Live();
		new Pickup();
		new Behavior();

		MyWolfConfig.Config = this.getConfiguration();
		MyWolfConfig.setStandart();
		MyWolfConfig.loadVariables();

		ConfigBuffer.lv = new MyWolfLanguage(new Configuration(new File(this.getDataFolder().getPath() + File.separator + "lang.yml")));
		ConfigBuffer.lv.setStandart();
		ConfigBuffer.lv.loadVariables();

		ConfigBuffer.WolvesConfig = new Configuration(new File(this.getDataFolder().getPath() + File.separator + "Wolves.yml"));
		LoadWolves(ConfigBuffer.WolvesConfig);

		for (Player p : this.getServer().getOnlinePlayers())
		{
			if (ConfigBuffer.mWolves.containsKey(p.getName()) && p.isOnline() == true)
			{
				ConfigBuffer.mWolves.get(p.getName()).createWolf(ConfigBuffer.mWolves.get(p.getName()).isSitting());
			}
		}
		MyWolfUtil.Log.info("[" + ConfigBuffer.pdfFile.getName() + "] version " + ConfigBuffer.pdfFile.getVersion() + " ENABLED");
	}

	public void LoadWolves(Configuration Config)
	{
		int anzahlWolves = 0;

		Config.load();
		List<String> WolfList = Config.getKeys("Wolves");
		if (WolfList != null)
		{
			for (String ownername : WolfList)
			{
				int invSlot = 0;
				double WolfX = Config.getDouble("Wolves." + ownername + ".loc.X", 0);
				double WolfY = Config.getDouble("Wolves." + ownername + ".loc.Y", 0);
				double WolfZ = Config.getDouble("Wolves." + ownername + ".loc.Z", 0);
				double WolfEXP = Config.getDouble("Wolves." + ownername + ".exp", 0);
				String WolfWorld = Config.getString("Wolves." + ownername + ".loc.world", getServer().getWorlds().get(0).getName());
				int WolfHealthNow = Config.getInt("Wolves." + ownername + ".health.now", 6);
				int WolfLives = Config.getInt("Wolves." + ownername + ".health.lives", 3);
				int WolfRespawnTime = Config.getInt("Wolves." + ownername + ".health.respawntime", 0);
				String WolfName = Config.getString("Wolves." + ownername + ".name", "Wolf");
				boolean Wolvesitting = Config.getBoolean("Wolves." + ownername + ".sitting", false);

				if (WolfLives == 0)
				{
					continue;
				}
				if (getServer().getWorld(WolfWorld) == null)
				{
					MyWolfUtil.Log.info("[MyWolf] World \"" + WolfWorld + "\" for " + ownername + "'s wolf \"" + WolfName + "\" not found - skiped wolf");
					continue;
				}

				ConfigBuffer.mWolves.put(ownername, new Wolves(ownername));

				ConfigBuffer.mWolves.get(ownername).setLocation(new Location(this.getServer().getWorld(WolfWorld), WolfX, WolfY, WolfZ));

				if (WolfLives > MyWolfConfig.MaxLives)
				{
					WolfLives = MyWolfConfig.MaxLives;
				}
				ConfigBuffer.mWolves.get(ownername).setHealth(WolfHealthNow);
				ConfigBuffer.mWolves.get(ownername).RespawnTime = WolfRespawnTime;
				if(WolfRespawnTime > 0)
				{
					ConfigBuffer.mWolves.get(ownername).Status = WolfState.Dead;
				}
				else
				{
					ConfigBuffer.mWolves.get(ownername).Status = WolfState.Despawned;
				}
				ConfigBuffer.mWolves.get(ownername).Name = WolfName;
				ConfigBuffer.mWolves.get(ownername).setSitting(Wolvesitting);
				ConfigBuffer.mWolves.get(ownername).Experience.setExp(WolfEXP);
				for (int i = 0; i < 2; i++)
				{
					invSlot = 0;
					for (String item : Config.getString("Wolves." + ownername + ".inventory." + (i + 1)).split("\\;"))
					{
						String[] itemvalues = item.split("\\,");
						if (itemvalues.length == 3 && MyWolfUtil.isInt(itemvalues[0]) && MyWolfUtil.isInt(itemvalues[1]) && MyWolfUtil.isInt(itemvalues[2]))
						{
							if (Material.getMaterial(Integer.parseInt(itemvalues[0])) != null)
							{
								if (Integer.parseInt(itemvalues[1]) <= 64)
								{
									ConfigBuffer.mWolves.get(ownername).Inventory[i].setItem(invSlot, new ItemStack(Integer.parseInt(itemvalues[0]), Integer.parseInt(itemvalues[1]), Integer.parseInt(itemvalues[2])));
								}
							}
						}
						invSlot++;
					}
				}
				anzahlWolves++;
			}
		}
		MyWolfUtil.Log.info("[" + this.getDescription().getName() + "] " + anzahlWolves + " wolf/wolves loaded");
	}

	public void SaveWolves(Configuration Config)
	{
		Config.removeProperty("Wolves");
		for (String owner : ConfigBuffer.mWolves.keySet())
		{
			Wolves wolf = ConfigBuffer.mWolves.get(owner);
			for (int i = 0; i < 2; i++)
			{
				String Items = "";
				if (ConfigBuffer.mWolves.get(owner).Inventory[i].getContents().length > 0)
				{
					for (ItemStack Item : ConfigBuffer.mWolves.get(owner).Inventory[i].getContents())
					{
						if (Item != null)
						{
							Items += Item.id + "," + Item.count + "," + Item.damage + ";";
						}
						else
						{
							Items += ",,;";
						}
					}
					Items = Items.substring(0, Items.length() - 1);
				}
				Config.setProperty("Wolves." + owner + ".inventory." + (i + 1), Items);
			}
			Config.setProperty("Wolves." + owner + ".loc.X", wolf.getLocation().getX());
			Config.setProperty("Wolves." + owner + ".loc.Y", wolf.getLocation().getY());
			Config.setProperty("Wolves." + owner + ".loc.Z", wolf.getLocation().getZ());
			Config.setProperty("Wolves." + owner + ".loc.world", wolf.getLocation().getWorld().getName());

			Config.setProperty("Wolves." + owner + ".health.now", wolf.getHealth());
			Config.setProperty("Wolves." + owner + ".health.lives", wolf.Lives);
			Config.setProperty("Wolves." + owner + ".health.respawntime", wolf.RespawnTime);
			Config.setProperty("Wolves." + owner + ".name", wolf.Name);
			Config.setProperty("Wolves." + owner + ".sitting", wolf.isSitting());
			Config.setProperty("Wolves." + owner + ".exp", wolf.Experience.getExp());
		}
		Config.save();
	}

	public static boolean isMyWolf(Wolf wolf)
	{
		for (Wolves w : ConfigBuffer.mWolves.values())
		{
			if (w.getID() == wolf.getEntityId())
			{
				return true;
			}
		}
		return false;
	}
	
	public static Wolves getMyWolf(Wolf wolf)
	{
		for (Wolves w : ConfigBuffer.mWolves.values())
		{
			if (w.getID() == wolf.getEntityId())
			{
				return w;
			}
		}
		return null;
	}
	public static Wolves getMyWolf(Player player)
	{
		return ConfigBuffer.mWolves.containsKey(player.getName())?ConfigBuffer.mWolves.get(player.getName()):null;
	}
}