package de.Keyle.MyWolf.util;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import de.Keyle.MyWolf.MyWolf;

public class MyWolfUtil extends de.Keyle.MyWolf.MyWolf
{
	public static String SetColors(String text)
	{
		return text.replace("%black%", "§0").replace("%navy%", "§1").replace("%green%", "§2").replace("%teal%", "§3").replace("%red%", "§4").replace("%purple%", "§5").replace("%gold%", "§6").replace("%silver%", "§7").replace("%gray%", "§8").replace("%blue%", "§9").replace("%lime%", "§a").replace("%aqua%", "§b").replace("%rose%", "§c").replace("%pink%", "§d").replace("%yellow%", "§e").replace("%white%", "§f");
	}
	
	public static Material checkMaterial(int itemid,Material defaultMaterial)
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
	
	public static boolean isInt(String number)
    {
    	try {
    		Integer.parseInt(number);
    		return true;
    	}
    	catch(NumberFormatException nFE) {
    		return false;
    	}
    }
	
	public static boolean isNPC(MyWolf Plugin, Player p)
	{
		if(Plugin.getServer().getPluginManager().getPlugin("Citizens") != null)
		{
			return com.fullwall.Citizens.NPCs.NPCManager.isNPC(p);
		}
		return false;
	}
}
