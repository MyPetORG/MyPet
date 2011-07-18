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

import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.CreatureType;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Giant;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Spider;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Zombie;
import org.bukkit.entity.PigZombie;
import org.bukkit.plugin.Plugin;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;

public class MyWolfUtil
{

	public static Logger Log = Logger.getLogger("Minecraft");
	private static Server server = org.bukkit.Bukkit.getServer();

	public static Server getServer()
	{
		return server;
	}

	public static String SetColors(String text)
	{
		return text.replace("%black%", "§0").replace("%navy%", "§1").replace("%green%", "§2").replace("%teal%", "§3").replace("%red%", "§4").replace("%purple%", "§5").replace("%gold%", "§6").replace("%silver%", "§7").replace("%gray%", "§8").replace("%blue%", "§9").replace("%lime%", "§a").replace("%aqua%", "§b").replace("%rose%", "§c").replace("%pink%", "§d").replace("%yellow%", "§e").replace("%white%", "§f");
	}

	public static Material checkMaterial(int itemid, Material defaultMaterial)
	{
		if (Material.getMaterial(itemid) == null)
		{
			return defaultMaterial;
		}
		else
		{
			return Material.getMaterial(itemid);
		}
	}

	public static boolean checkMaterial(int itemid)
	{
		if (Material.getMaterial(itemid) == null)
		{
			return false;
		}
		else
		{
			return true;
		}
	}

	public static boolean isInt(String number)
	{
		try
		{
			Integer.parseInt(number);
			return true;
		}
		catch (NumberFormatException nFE)
		{
			return false;
		}
	}

	public static boolean isNPC(Player player)
	{
		Plugin plugin = server.getPluginManager().getPlugin("Citizens");
		if (plugin != null)
		{
			int version = Integer.parseInt(plugin.getDescription().getVersion().replace(".", ""), 109);
			if (version <= 108)
			{
				return com.fullwall.Citizens.NPCs.NPCManager.isNPC(player);
			}
			else
			{
				return com.citizens.NPCs.NPCManager.isNPC(player);
			}
		}
		return false;
	}

	public static CreatureType getCreatureType(Entity entity)
	{
		if (entity instanceof Zombie)
		{
			return CreatureType.ZOMBIE;
		}
		else if (entity instanceof Spider)
		{
			return CreatureType.SPIDER;
		}
		else if (entity instanceof Skeleton)
		{
			return CreatureType.SKELETON;
		}
		else if (entity instanceof Wolf)
		{
			return CreatureType.WOLF;
		}
		else if (entity instanceof Creeper)
		{
			return CreatureType.CREEPER;
		}
		else if (entity instanceof Ghast)
		{
			return CreatureType.GHAST;
		}
		else if (entity instanceof PigZombie)
		{
			return CreatureType.PIG_ZOMBIE;
		}
		else if (entity instanceof Giant)
		{
			return CreatureType.GIANT;
		}
		else if (entity instanceof Slime)
		{
			return CreatureType.SLIME;
		}
		return null;
	}

	public static boolean getPVP(Location loc)
	{

		Plugin WG = server.getPluginManager().getPlugin("WorldGuard");
		if (WG != null)
		{
			if (WG.isEnabled())
			{
				WorldGuardPlugin WGP = (WorldGuardPlugin) WG;
				RegionManager mgr = WGP.getGlobalRegionManager().get(loc.getWorld());
				Vector pt = new com.sk89q.worldedit.Vector(loc.getX(), loc.getY(), loc.getZ());
				ApplicableRegionSet set = mgr.getApplicableRegions(pt);

				boolean pvp = set.allows(DefaultFlag.PVP);

				return pvp;
			}
		}
		return server.getWorld(loc.getWorld().getName()).getPVP();
	}

	public static boolean hasSkill(Map<String, Boolean> skills, String skill)
	{
		if (skills.containsKey(skill))
		{
			return skills.get(skill);
		}
		return false;
	}

	public static void sendMessage(Player player, String Message)
	{
		if (player != null && player.isOnline())
		{
			player.sendMessage(Message);
		}
	}

	public static double getDistance(Location loc1, Location loc2)
	{
		return Math.sqrt(Math.pow(loc1.getX() - loc2.getX(), 2.0D) + Math.pow(loc1.getZ() - loc2.getZ(), 2.0D));
	}
}
