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

import com.aranai.virtualchest.TileEntityVirtualChest;

import net.minecraft.server.EntityPlayer;
import net.minecraft.server.InventoryLargeChest;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.CreatureType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.util.Vector;
import org.bukkitcontrib.BukkitContrib;

public class Wolves {
	
	public ConfigBuffer cb;
	
	public String Name = "Wolf";
	public String Owner;
	public int ID;
	public int HealthMax = 6;
	public int HealthNow = HealthMax;
	public int Lives = 5;
	public Wolf MyWolf;
	public int RespawnTime = 0;
	private int RespawnTimer;
	private int DropTimer = -1;
	public Player player;
	public boolean isSitting = false;
	
	public enum InventoryType
	{
		NONE,
	    SMALL,
	    LARGE;
	}
	public InventoryType InventoryMode = InventoryType.NONE;
	public boolean hasPickup = false;
	
	public boolean allowAttackPlayer = true;
	public boolean allowAttackMonster = false;
	
	public TileEntityVirtualChest Inventory1 = new TileEntityVirtualChest();
	public TileEntityVirtualChest Inventory2 = new TileEntityVirtualChest();
	public InventoryLargeChest LargeInventory = new InventoryLargeChest(Inventory1.getName(), Inventory1, Inventory2);
	
	
	public boolean isThere = false;
	public boolean isDead = false;
	
	public Location Location;

	
	public Wolves(ConfigBuffer cb, String Owner) {
		this.cb = cb;				
		this.Owner = Owner;
	}
	
	public void SetName(String name)
	{
		Name = name;
		DisplayName();
	}
	
	private void DisplayName()
	{
		BukkitContrib.getAppearanceManager().setGlobalTitle(MyWolf, ChatColor.AQUA + Name);
	}
	
	public void OpenInventory()
	{
		EntityPlayer eh = ((CraftPlayer)getPlayer()).getHandle();
		if(InventoryMode == InventoryType.SMALL)
		{
			eh.a(Inventory1);
		}
		else if(InventoryMode == InventoryType.LARGE)
		{
			eh.a(LargeInventory);
		}
	}
	
	public void removeWolf()
	{
		StopDropTimer();
		isSitting = MyWolf.isSitting();
		HealthNow= MyWolf.getHealth();
		Location = MyWolf.getLocation();
		isThere = false;
		((LivingEntity) MyWolf).remove();
		MyWolf = null;
	}
	
	public boolean createWolf(boolean sitting)
	{
		if(MyWolf != null && MyWolf.isDead() == false)
		{
			return false;
		}
		else
		{
			if (getPlayer() != null && RespawnTime == 0)
			{
				MyWolf = (Wolf) cb.Plugin.getServer().getWorld(Location.getWorld().getName()).spawnCreature(Location,CreatureType.WOLF);
				MyWolf.setOwner(getPlayer());
				MyWolf.setSitting(sitting);
				Location = MyWolf.getLocation();
				MyWolf.setHealth(HealthNow);
		    	ID = MyWolf.getEntityId();
		    	
		    	isThere = true;
		    	isDead = false;
		    	DisplayName();
		    	if(cb.Permissions.has(getPlayer(), "mywolf.pickup") && hasPickup == true)
		    	{
		    		DropTimer();
		    	}
		    		
		    	return true;
			}
			else if(RespawnTime > 0)
			{
				RespawnTimer();
				return false;
			}
		}
		return false;
	}
	
	public void createWolf(Wolf wolf)
	{
		MyWolf = wolf;
    	ID = MyWolf.getEntityId();
    	Location = MyWolf.getLocation();
    	isThere = true;
    	isDead = false;
    	DisplayName();
    	if(cb.Permissions.has(getPlayer(), "mywolf.pickup") && hasPickup == true)
    	{
    		DropTimer();
    	}
	}
	
	public void setWolfHealth(int health)
	{
		if(health > HealthMax)
		{
			HealthNow = HealthMax;
		}
		else
		{
			HealthNow = health;
		}
		if (isThere == true)
		{
			MyWolf.setHealth(HealthNow);
		}
	}
	public int getHealth()
	{
		if (isThere == true && isDead == false)
		{
			return MyWolf.getHealth();
		}
		else
		{
			return HealthNow;
		}
	}
	
	public int getID()
	{
		if (isThere == true && isDead == false)
		{
			return MyWolf.getEntityId();
		}
		else
		{
			return ID;
		}
	}
	
	public Location getLocation()
	{
		if (isThere == true && isDead == false)
		{
			return MyWolf.getLocation();
		}
		else
		{
			return Location;
		}
	}
	
	public boolean isSitting()
	{
		if (isThere == true && isDead == false)
		{
			return MyWolf.isSitting();
		}
		else
		{
			return isSitting;
		}
	}
	
	public void RespawnTimer()
	{
		if(RespawnTime == 0)
		{
			RespawnTime = HealthMax*cb.cv.WolfRespawnTimeFactor;
		}
		getPlayer().sendMessage(ChatColor.AQUA+Name + ChatColor.WHITE + " respawn in "+ChatColor.GOLD+RespawnTime+ChatColor.WHITE +" sec");

		RespawnTimer = cb.Plugin.getServer().getScheduler().scheduleSyncRepeatingTask(cb.Plugin,new Runnable() 
		{
			
			public void run() {
				if (RespawnTime == 0)
				{
					RespawnWolf();
					cb.Plugin.getServer().getScheduler().cancelTask(RespawnTimer);
				}
				else
				{
					if(getPlayer() != null)
					{
						RespawnTime--;
					}
					else
					{
						cb.Plugin.getServer().getScheduler().cancelTask(RespawnTimer);
					}
				}
			}
		}, 0L,20L);
	}
	public boolean RespawnWolf()
	{
		if(isDead == false)
		{
			return false;
		}
		else
		{
			HealthNow = HealthMax;
			Location = getPlayer().getLocation();
			getPlayer().sendMessage(ChatColor.AQUA+Name + ChatColor.WHITE + " respawned");
			createWolf(false);
			RespawnTime = 0;
			return true;
		}
	}
	
	public void StopDropTimer()
	{
		if(DropTimer != -1)
		{
			cb.Plugin.getServer().getScheduler().cancelTask(DropTimer);
			DropTimer = -1;
		}
		
	}
	
	public void DropTimer()
	{
		if(isThere == true)
		{
			DropTimer = cb.Plugin.getServer().getScheduler().scheduleSyncRepeatingTask(cb.Plugin,new Runnable() 
			{
				
				public void run() {
					if (isThere == false || isDead == true || getPlayer() == null)
					{
						StopDropTimer();
					}
					else
					{
						if(getPlayer() != null)
						{
							try
							{
								for(Entity e : MyWolf.getWorld().getEntities())
								{
									if(e instanceof Item)
									{
										Item item = (Item)e;
										
										Vector distance = getLocation().toVector().add(new Vector(0.5,0,0.5)).subtract(item.getLocation().toVector());
										if(distance.lengthSquared() < 1.0*cb.cv.WolfPickupRange*cb.cv.WolfPickupRange + 1)
										{
											int amountleft = Inventory1.addItem(item);
											if(amountleft == 0)
											{
												e.remove();
											}
											else
											{
												if(item.getItemStack().getAmount() > amountleft)
												{
													item.getItemStack().setAmount(amountleft);
												}
												if(InventoryMode == InventoryType.LARGE)
												{
													amountleft = Inventory2.addItem(item);
													if(amountleft == 0)
													{
														e.remove();
													}
													else
													{
														if(item.getItemStack().getAmount() > amountleft)
														{
															item.getItemStack().setAmount(amountleft);
														}
													}
												}
											}
										}
									}
								}
							} 
							catch(Exception e)
							{
								System.out.println("Warning! An error occured!");
								e.printStackTrace();
							}
						}
						else
						{
							StopDropTimer();
						}
					}
				}
			}, 0L,20L);
		}
	}
	
	public Player getPlayer()
	{
		for(Player p: cb.Plugin.getServer().getOnlinePlayers())
		{
			if(p.getName().equals(Owner) && cb.isNPC(p) == false)
			{
				return p;
			}
		}
		return null;
	}
	
}