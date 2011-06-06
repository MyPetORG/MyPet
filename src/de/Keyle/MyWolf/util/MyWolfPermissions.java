package de.Keyle.MyWolf.util;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;

import de.Keyle.MyWolf.MyWolf;

public class MyWolfPermissions
{
	MyWolf Plugin;
	private Object Permissions;
	private int Mode = -1;
	
	public MyWolfPermissions(MyWolf Plugin)
	{
		this.Plugin = Plugin;
	}
	
	public boolean has(Player player, String node)
	{
		if (this.Permissions == null)
		{
			return true;
		}
		else if (player == null)
		{
			return true;
		}
		else if (Mode == 0 && Permissions instanceof PermissionHandler)
		{
			return ((PermissionHandler) this.Permissions).has(player, node);
		}
		return false;
		
	}
	
	public boolean setup()
	{
    	Plugin testPresent = Plugin.getServer().getPluginManager().getPlugin("Permissions");
        if (testPresent != null)
        {
            Permissions = ((Permissions)testPresent).getHandler();
            Mode = 0;
            return true;
        }
        else
        {
            return false;
        }
    }
}