/*
* Copyright (C) 2011-2012 Keyle
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

import com.sk89q.worldedit.Vector;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.*;
import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Map;
import java.util.logging.Logger;

public class MyWolfUtil
{

    public static final Logger Log = Logger.getLogger("Minecraft");
    private static final Server server = org.bukkit.Bukkit.getServer();

    public static Server getServer()
    {
        return server;
    }

    public static String SetColors(String text)
    {
        return text.replace("%black%", "" + ChatColor.BLACK).replace("%darkgreen%", "" + ChatColor.DARK_GREEN).replace("%green%", "" + ChatColor.GREEN).replace("%aqua%", "" + ChatColor.AQUA).replace("%red%", "" + ChatColor.RED).replace("%lightpurple%", "" + ChatColor.LIGHT_PURPLE).replace("%gold%", "" + ChatColor.GOLD).replace("%darkgray%", "" + ChatColor.DARK_GRAY).replace("%gray%", "" + ChatColor.GRAY).replace("%blue%", "" + ChatColor.BLUE).replace("%darkaqua%", "" + ChatColor.DARK_AQUA).replace("%darkblue%", "" + ChatColor.DARK_BLUE).replace("%darkpurple%", "" + ChatColor.DARK_PURPLE).replace("%darkred%", "" + ChatColor.DARK_RED).replace("%yellow%", "" + ChatColor.YELLOW).replace("%white%", "" + ChatColor.WHITE);
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
        return Material.getMaterial(itemid) != null;
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
        return plugin != null && net.citizensnpcs.api.CitizensManager.isNPC(player);
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
        else if (entity instanceof Cow)
        {
            return CreatureType.COW;
        }
        else if (entity instanceof Pig)
        {
            return CreatureType.PIG;
        }
        else if (entity instanceof Chicken)
        {
            return CreatureType.CHICKEN;
        }
        else if (entity instanceof Squid)
        {
            return CreatureType.SQUID;
        }
        else if (entity instanceof Sheep)
        {
            return CreatureType.SHEEP;
        }
        else if(entity instanceof CaveSpider)
        {
            return CreatureType.CAVE_SPIDER;
        }
        else if(entity instanceof Blaze)
        {
            return CreatureType.BLAZE;
        }
        else if(entity instanceof EnderDragon)
        {
            return CreatureType.ENDER_DRAGON;
        }
        else if(entity instanceof Enderman)
        {
            return CreatureType.ENDERMAN;
        }
        else if(entity instanceof MagmaCube)
        {
            return CreatureType.MAGMA_CUBE;
        }
        else if(entity instanceof MushroomCow)
        {
            return CreatureType.MUSHROOM_COW;
        }
        else if(entity instanceof Silverfish)
        {
            return CreatureType.SILVERFISH;
        }
        else if(entity instanceof Snowman)
        {
            return CreatureType.SNOWMAN;
        }
        else if(entity instanceof Villager)
        {
            return CreatureType.VILLAGER;
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
                Vector pt = new Vector(loc.getX(), loc.getY(), loc.getZ());
                ApplicableRegionSet set = mgr.getApplicableRegions(pt);

                return set.allows(DefaultFlag.PVP);
            }
        }
        return server.getWorld(loc.getWorld().getName()).getPVP();
    }

    public static boolean hasSkill(Map<String, Boolean> skills, String skill)
    {
    	if(!MyWolfConfig.LevelSystem)
    		return true;
    	
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

    public static String readFileAsString(String filePath) throws java.io.IOException
    {
        StringBuilder fileData = new StringBuilder(1000);
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        char[] buf = new char[1024];
        int numRead;
        while ((numRead = reader.read(buf)) != -1)
        {
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
            buf = new char[1024];
        }
        reader.close();
        return fileData.toString();
    }
}
