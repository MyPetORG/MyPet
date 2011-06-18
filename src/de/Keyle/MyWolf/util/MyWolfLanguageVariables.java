package de.Keyle.MyWolf.util;

import org.bukkit.util.config.Configuration;

public class MyWolfLanguageVariables
{
	public Configuration Config;
	
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
}