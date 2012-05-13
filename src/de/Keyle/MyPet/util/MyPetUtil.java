/*
 * Copyright (C) 2011-2012 Keyle
 *
 * This file is part of MyPet
 *
 * MyPet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyPet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyPet. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.util;

import com.massivecraft.factions.P;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.util.CombatUtil;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.util.logger.DebugLogger;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.logging.Logger;

public class MyPetUtil
{
    public static Server getServer()
    {
        return Bukkit.getServer();
    }

    public static OfflinePlayer getOfflinePlayer(String Name)
    {
        return getServer().getOfflinePlayer(Name);
    }

    public static Logger getLogger()
    {
        return MyPetPlugin.getPlugin().getLogger();
    }

    public static DebugLogger getDebugLogger()
    {
        return MyPetPlugin.getPlugin().getDebugLogger();
    }

    public static String setColors(String text)
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
        Plugin plugin = getServer().getPluginManager().getPlugin("Citizens");
        return plugin != null && net.citizensnpcs.api.CitizensManager.isNPC(player);
    }

    public static boolean canHurtWorldGuard(Player victim)
    {
        if (MyPetConfig.useWorldGuard && getServer().getPluginManager().isPluginEnabled("WorldGuard"))
        {
            Location loc = victim.getLocation();
            WorldGuardPlugin WGP = (WorldGuardPlugin) getServer().getPluginManager().getPlugin("WorldGuard");
            RegionManager mgr = WGP.getGlobalRegionManager().get(loc.getWorld());
            Vector pt = new Vector(loc.getX(), loc.getY(), loc.getZ());
            ApplicableRegionSet set = mgr.getApplicableRegions(pt);

            return set.allows(DefaultFlag.PVP);
        }
        return true;
    }

    public static boolean canHurtFactions(Player attacker, Player victim)
    {
        if (MyPetConfig.useFactions && MyPetUtil.getServer().getPluginManager().isPluginEnabled("Factions"))
        {
            EntityDamageByEntityEvent sub = new EntityDamageByEntityEvent(attacker, victim, EntityDamageEvent.DamageCause.CUSTOM, 0);
            return P.p.entityListener.canDamagerHurtDamagee(sub, false);
        }
        return true;
    }

    public static boolean canHurtTowny(Player attacker, Player defender)
    {
        boolean canHurt = true;
        if (MyPetConfig.useTowny && MyPetUtil.getServer().getPluginManager().isPluginEnabled("Towny"))
        {
            try
            {
                Class.forName("com.palmergames.bukkit.util.CombatUtil", false, null);
            }
            catch (ClassNotFoundException e)
            {
                return canHurt;
            }
            TownyWorld world = null;
            try
            {
                world = TownyUniverse.getDataSource().getWorld(defender.getWorld().getName());
            }
            catch (Exception ignored)
            {
            }
            if (CombatUtil.preventDamageCall(world, attacker, defender, attacker, defender))
            {
                canHurt = false;
            }
        }
        return canHurt;
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