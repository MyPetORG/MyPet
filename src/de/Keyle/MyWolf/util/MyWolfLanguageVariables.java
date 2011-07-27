package de.Keyle.MyWolf.util;

import org.bukkit.util.config.Configuration;

public class MyWolfLanguageVariables
{
	public final Configuration Config;
	
	public MyWolfLanguageVariables(Configuration cfg)
	{
		Config = cfg;
	}
	public String Msg_AddLeash;
	public String Msg_HPinfo;
	public String Msg_AddChest;
	public String Msg_AddChestGreater;
	public String Msg_AddLive;
	public String Msg_MaxLives;
	public String Msg_AddPickup;
	public String Msg_AddHP;
	public String Msg_MaxHP;
	public String Msg_WolfIsGone;
	public String Msg_DeathMessage;
	public String Msg_RespawnIn;
	public String Msg_OnRespawn;
	public String Msg_Call;
	public String Msg_CallFirst;
	public String Msg_IsDead;
	public String Msg_DontHaveWolf;
	public String Msg_NewName;
	public String Msg_Release;
	public String Msg_StopAttack;
	public String Msg_InventorySwimming;
	public String Msg_CallDead;
	public String Msg_Name;
	
	public String Unknow;
	public String You;
	public String Zombie;
	public String Spider;
	public String Giant;
	public String Creeper;
	public String Slime;
	public String Wolf;
	public String Player;
	public String Drowning;
	public String Fall;
	public String Lava;
	public String Fire;
	public String Ghast;
	public String kvoid;
	public String Lightning;
	public String Skeleton;
	public String PlayerWolf;
	public String Explosion;
	
	
	
	public void setProperty(String key,Object value)
	{
		if(Config.getProperty(key) == null)
		{
			Config.setProperty(key, value);
		}
	}
	
	public void setStandart()
	{
		setProperty("MyWolf.Message.addleash", "%green%You take your wolf on the leash, he'll be a good wolf.");
		setProperty("MyWolf.Message.hpinfo", "%aqua%%wolfname%%white% HP:%hp%");
		setProperty("MyWolf.Message.addchest", "%aqua%%wolfname%%white% has now an inventory.");
		setProperty("MyWolf.Message.addlargechest", "%aqua%%wolfname%%white% has now a larger inventory.");
		setProperty("MyWolf.Message.addlive", "%green%+1 life for %aqua%%wolfname%");
		setProperty("MyWolf.Message.maxlives", "%aqua%%wolfname%%red% has reached the maximum of %maxlives% lives.");
		setProperty("MyWolf.Message.addpickup", "%aqua%%wolfname%%white% now pickup items in a range of %range%.");
		setProperty("MyWolf.Message.addhp", "%aqua%%wolfname%%white% +1 maxHP for %aqua%%wolfname%");
		setProperty("MyWolf.Message.maxhp", "%aqua%%wolfname%%red% has reached the maximum of %maxhp% HP.");
		setProperty("MyWolf.Message.wolfisgone", "%aqua%%wolfname%%white% is %red%gone%white% and will never come back . . .");
		setProperty("MyWolf.Message.deathmessage.text", "%aqua%%wolfname%%white% ");
		setProperty("MyWolf.Message.respawnin", "%aqua%%wolfname%%white% respawn in %gold%%time%%white% sec");
		setProperty("MyWolf.Message.onrespawn", "%aqua%%wolfname%%white% respawned");
		setProperty("MyWolf.Message.callwhendead", "%aqua%%wolfname%%white% is dead! and respawns in %gold%%time%%white% sec");
		setProperty("MyWolf.Message.call", "%aqua%%wolfname%%white% comes to you.");
		setProperty("MyWolf.Message.callfirst", "You must call your wolf first.");
		setProperty("MyWolf.Message.donthavewolf", "You don't have a wolf!");
		setProperty("MyWolf.Message.newname", "The name of your wolf is now: %aqua%%wolfname%");
		setProperty("MyWolf.Message.name", "The name of your wolf is: %aqua%%wolfname%");
		setProperty("MyWolf.Message.release", "%aqua%%wolfname%%white% is now %green$free%white% . . .");
		setProperty("MyWolf.Message.stopattack", "Your wolf should now %green%stop%white% attacking!");
		setProperty("MyWolf.Message.inventorywhileswimming", "You can't open the inventory while the wolf is swimming!");
		setProperty("MyWolf.Message.deathmessage.creeper", "was killed by a Creeper.");
		setProperty("MyWolf.Message.deathmessage.zombie", "was killed by a Zombie.");
		setProperty("MyWolf.Message.deathmessage.unknow", "was killed by an unknown source.");
		setProperty("MyWolf.Message.deathmessage.you", "was killed by %red%YOU.");
		setProperty("MyWolf.Message.deathmessage.spider", "was killed by a Spider.");
		setProperty("MyWolf.Message.deathmessage.giant", "was killed by a Giant.");
		setProperty("MyWolf.Message.deathmessage.slime", "was killed by a Slime.");
		setProperty("MyWolf.Message.deathmessage.ghast", "was killed by a Ghast.");
		setProperty("MyWolf.Message.deathmessage.wolf", "was killed by a Wolf.");
		setProperty("MyWolf.Message.deathmessage.player", "was killed by %player%.");
		setProperty("MyWolf.Message.deathmessage.drowning", "drowned.");
		setProperty("MyWolf.Message.deathmessage.fall", " died by falling down.");
		setProperty("MyWolf.Message.deathmessage.lightning", "was killed by lightning.");
		setProperty("MyWolf.Message.deathmessage.fire", "was killed by VOID.");
		setProperty("MyWolf.Message.deathmessage.skeleton", "was killed by a Skeleton.");
		setProperty("MyWolf.Message.deathmessage.playerwolf", "was killed by %player%'s Wolf.");
		setProperty("MyWolf.Message.deathmessage.explosion", "was killed by an explosion.");
		
		Config.save();
	}
	
	public void loadVariables()
	{
		Msg_AddLeash = Config.getString("MyWolf.Message.addleash", "%green%You take your wolf on the leash, he'll be a good wolf.");
		Msg_HPinfo = Config.getString("MyWolf.Message.hpinfo", "%aqua%%wolfname%%white% HP:%hp%");
		Msg_AddChest = Config.getString("MyWolf.Message.addchest", "%aqua%%wolfname%%white% has now an inventory.");
		Msg_AddChestGreater = Config.getString("MyWolf.Message.addlargechest", "%aqua%%wolfname%%white% has now a larger inventory.");
		Msg_AddLive = Config.getString("MyWolf.Message.addlive", "%green%+1 life for %aqua%%wolfname%");
		Msg_MaxLives = Config.getString("MyWolf.Message.maxlives", "%aqua%%wolfname%%red% has reached the maximum of %maxlives% lives.");
		Msg_AddPickup = Config.getString("MyWolf.Message.addpickup", "%aqua%%wolfname%%white% now pickup items in a range of %range%.");
		Msg_AddHP = Config.getString("MyWolf.Message.addhp", "%aqua%%wolfname%%white% +1 maxHP for %aqua%%wolfname%");
		Msg_MaxHP = Config.getString("MyWolf.Message.maxhp", "%aqua%%wolfname%%red% has reached the maximum of %maxhp% HP.");
		Msg_WolfIsGone = Config.getString("MyWolf.Message.wolfisgone", "%aqua%%wolfname%%white% is %red%gone%white% and will never come back . . .");
		Msg_DeathMessage = Config.getString("MyWolf.Message.deathmessage.text", "%aqua%%wolfname%%white% ");
		Msg_RespawnIn = Config.getString("MyWolf.Message.respawnin", "%aqua%%wolfname%%white% respawn in %gold%%time%%white% sec");
		Msg_OnRespawn = Config.getString("MyWolf.Message.onrespawn", "%aqua%%wolfname%%white% respawned");
		Msg_CallDead = Config.getString("MyWolf.Message.callwhendead", "%aqua%%wolfname%%white% is dead! and respawns in %gold%%time%%white% sec");
		Msg_Call = Config.getString("MyWolf.Message.call", "%aqua%%wolfname%%white% comes to you.");
		Msg_CallFirst = Config.getString("MyWolf.Message.callfirst", "You must call your wolf first.");
		Msg_DontHaveWolf = Config.getString("MyWolf.Message.donthavewolf", "You don't have a wolf!");
		Msg_NewName = Config.getString("MyWolf.Message.newname", "The name of your wolf is now: %aqua%%wolfname%");
		Msg_Name = Config.getString("MyWolf.Message.name", "The name of your wolf is: %aqua%%wolfname%");
		Msg_Release = Config.getString("MyWolf.Message.release", "%aqua%%wolfname%%white% is now %green$free%white% . . .");
		Msg_StopAttack = Config.getString("MyWolf.Message.stopattack", "%wolfname% should now %green%stop%white% attacking!");
		Msg_InventorySwimming = Config.getString("MyWolf.Message.inventorywhileswimming", "You can't open the inventory while the wolf is swimming!");
		Creeper = Config.getString("MyWolf.Message.deathmessage.creeper", "was killed by a Creeper.");
		Zombie = Config.getString("MyWolf.Message.deathmessage.zombie", "was killed by a Zombie.");
		Unknow = Config.getString("MyWolf.Message.deathmessage.unknow", "was killed by an unknown source.");
		You = Config.getString("MyWolf.Message.deathmessage.you", "was killed by %red%YOU.");
		Spider = Config.getString("MyWolf.Message.deathmessage.spider", "was killed by a Spider.");
		Skeleton = Config.getString("MyWolf.Message.deathmessage.skeleton", "was killed by a Skeleton.");
		Giant = Config.getString("MyWolf.Message.deathmessage.giant", "was killed by a Giant.");
		Slime = Config.getString("MyWolf.Message.deathmessage.slime", "was killed by a Slime.");
		Ghast = Config.getString("MyWolf.Message.deathmessage.ghast", "was killed by a Ghast.");
		Wolf = Config.getString("MyWolf.Message.deathmessage.wolf", "was killed by a Wolf.");
		PlayerWolf = Config.getString("MyWolf.Message.deathmessage.playerwolf", "was killed by %player%'s Wolf .");
		Player = Config.getString("MyWolf.Message.deathmessage.player", "was killed by %player%.");
		Drowning = Config.getString("MyWolf.Message.deathmessage.drowning", "drowned.");
		Explosion = Config.getString("MyWolf.Message.deathmessage.explosion", "was killed by an explosion.");
		Fall = Config.getString("MyWolf.Message.deathmessage.fall", " died by falling down.");
		Lightning = Config.getString("MyWolf.Message.deathmessage.lightning", "was killed by lightning.");
		kvoid = Config.getString("MyWolf.Message.deathmessage.fire", "was killed by VOID.");
	}
}