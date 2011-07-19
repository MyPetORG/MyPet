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

import java.util.HashMap;
import java.util.Map;

import de.Keyle.MyWolf.Skill.MyWolfExperience;
import de.Keyle.MyWolf.util.*;
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
import org.bukkitcontrib.BukkitContrib;

public class Wolves
{
	public String Name = "Wolf";
	public String Owner;
	public int ID;
	public double HealthMax = 6;
	public double HealthNow = HealthMax;
	public int Lives = 5;
	public Wolf Wolf;
	public int RespawnTime = 0;

	private int Timer = -1;

	private int SitTimer = 15;
	private boolean isSitting = false;
	public boolean isPickup = false;

	public boolean allowAttackPlayer = true;
	public boolean allowAttackMonster = false;

	public static enum BehaviorState
	{
		Normal, Friendly, Agressive, Raid;
	}

	public static enum WolfState
	{
		Dead, Despawned, Here;
	}

	public BehaviorState Behavior = BehaviorState.Normal;
	public WolfState Status = WolfState.Despawned;

	public MyWolfInventory[] Inventory = { new MyWolfInventory(), new MyWolfInventory() };
	public InventoryLargeChest LargeInventory = new InventoryLargeChest(Inventory[0].getName(), Inventory[0], Inventory[1]);

	private Location Location;

	public Map<String, Boolean> Abilities = new HashMap<String, Boolean>();
	public MyWolfExperience Experience;

	public Wolves(String Owner)
	{
		this.Owner = Owner;
		Experience = new MyWolfExperience(MyWolfConfig.ExpFactor, this);
	}

	public void SetName(String Name)
	{
		this.Name = Name;
		String NameColor;
		if(MyWolfConfig.NameColor >= 0 && MyWolfConfig.NameColor <= 0xf)
		{
			NameColor = "§" + MyWolfConfig.NameColor;
		}
		else
		{
			if (getHealth() > HealthMax / 3 * 2)
			{
				NameColor = ""+ChatColor.GREEN;
			}
			else if (getHealth() > HealthMax / 3 * 1)
			{
				NameColor = ""+ChatColor.YELLOW;
			}
			else
			{
				NameColor = ""+ChatColor.RED;
			}
		}
		if (Status == WolfState.Here)
		{
			BukkitContrib.getAppearanceManager().setGlobalTitle(Wolf, NameColor + Name);
		}
	}
	public void SetName()
	{
		String NameColor;
		if(MyWolfConfig.NameColor >= 0 && MyWolfConfig.NameColor <= 0xf)
		{
			NameColor = "§" + MyWolfConfig.NameColor;
		}
		else
		{
			if (getHealth() > HealthMax / 3 * 2)
			{
				NameColor = ""+ChatColor.GREEN;
			}
			else if (getHealth() > HealthMax / 3 * 1)
			{
				NameColor = ""+ChatColor.YELLOW;
			}
			else
			{
				NameColor = ""+ChatColor.RED;
			}
		}
		if (Status == WolfState.Here)
		{
			BukkitContrib.getAppearanceManager().setGlobalTitle(Wolf, NameColor + this.Name);
		}
	}

	public void OpenInventory()
	{
		EntityPlayer eh = ((CraftPlayer) getOwner()).getHandle();
		if (MyWolfUtil.hasSkill(Abilities, "InventoryLarge"))
		{
			eh.a(LargeInventory);
		}
		else if (MyWolfUtil.hasSkill(Abilities, "InventorySmall"))
		{
			eh.a(Inventory[0]);
		}
	}

	public void removeWolf()
	{
		StopTimer();
		isSitting = Wolf.isSitting();
		HealthNow = Wolf.getHealth();
		Location = Wolf.getLocation();
		Status = WolfState.Despawned;
		((LivingEntity) Wolf).remove();
	}
	
	public boolean RespawnWolf()
	{
		if (Status == WolfState.Here)
		{
			return false;
		}
		else
		{
			HealthNow = HealthMax;
			Location = getOwner().getLocation();
			getOwner().sendMessage(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_OnRespawn")).replace("%wolfname%", Name));
			createWolf(false);
			RespawnTime = 0;
			return true;
		}
	}
	
	public void createWolf(boolean sitting)
	{
		if (Status == WolfState.Here || getOwner() == null )
		{
			return;
		}
		else
		{
			if(RespawnTime <= 0)
			{
				Wolf = (Wolf) MyWolf.Plugin.getServer().getWorld(Location.getWorld().getName()).spawnCreature(Location, CreatureType.WOLF);
				Wolf.setOwner(getOwner());
				Wolf.setSitting(sitting);
				Location = Wolf.getLocation();
				Wolf.setHealth((int) HealthNow);
				ID = Wolf.getEntityId();

				Status = WolfState.Here;
				SetName();
			}
			Timer();
		}
	}
	public void createWolf(Wolf wolf)
	{
		Wolf = wolf;
		ID = Wolf.getEntityId();
		Location = Wolf.getLocation();
		Status = WolfState.Here;
		SetName();
		Timer();
	}

	public void setHealth(double d)
	{
		if (d > HealthMax)
		{
			HealthNow = HealthMax;
		}
		else
		{
			HealthNow = d;
		}
		if (Status == WolfState.Here)
		{
			Wolf.setHealth((int) HealthNow);
		}
		SetName();
	}
	public double getHealth()
	{
		if (Status == WolfState.Here)
		{
			return Wolf.getHealth();
		}
		else
		{
			return HealthNow;
		}
	}

	public double Demage(double Demage)
	{
		if (Status == WolfState.Here)
		{
			HealthNow -= Demage;
			Wolf.setHealth((int) (HealthNow + 0.5));
		}
		return HealthNow;
	}

	public int getID()
	{
		if (Status == WolfState.Here)
		{
			return Wolf.getEntityId();
		}
		else
		{
			return ID;
		}
	}

	public Location getLocation()
	{
		if (Status == WolfState.Here)
		{
			return Wolf.getLocation();
		}
		else
		{
			return Location;
		}
	}
	public void setLocation(Location loc)
	{
		this.Location = loc;
		if(Status == WolfState.Here)
		{
			Wolf.teleport(loc);
		}
	}

	public boolean isSitting()
	{
		if (Status == WolfState.Here)
		{
			return Wolf.isSitting();
		}
		else
		{
			return isSitting;
		}
	}
	public void setSitting(boolean sitting)
	{
		if (Status == WolfState.Here)
		{
			Wolf.setSitting(sitting);
			this.isSitting = sitting;
		}
		else
		{
			this.isSitting = sitting;
		}
	}

	public void ResetSitTimer()
	{
		SitTimer = 15;
	}
	
	public void StopTimer()
	{
		if (Timer != -1)
		{
			MyWolf.Plugin.getServer().getScheduler().cancelTask(Timer);
			Timer = -1;
		}
	}
	public void Timer()
	{
		if (Status != WolfState.Despawned)
		{
			if (Timer != -1)
			{
				StopTimer();
			}
			Timer = MyWolf.Plugin.getServer().getScheduler().scheduleSyncRepeatingTask(MyWolf.Plugin, new Runnable()
			{
				public void run()
				{
					if (Status == WolfState.Despawned || getOwner() == null)
					{
						StopTimer();
					}
					else
					{
						if(Status == WolfState.Here)
						{
							SitTimer--;
							if(SitTimer <= 0)
							{
								Wolf.setSitting(true);
							}
							if (isPickup)
							{
								for (Entity e : Wolf.getNearbyEntities(MyWolfConfig.PickupRange, MyWolfConfig.PickupRange, MyWolfConfig.PickupRange))
								{
									if (e instanceof Item)
									{
										Item item = (Item) e;

										int amountleft = Inventory[0].addItem(item);
										if (amountleft == 0)
										{
											e.remove();
										}
										else
										{
											if (item.getItemStack().getAmount() > amountleft)
											{
												item.getItemStack().setAmount(amountleft);
											}
											if (MyWolfUtil.hasSkill(Abilities, "InventoryLarge"))
											{
												amountleft = Inventory[1].addItem(item);
												if (amountleft == 0)
												{
													e.remove();
												}
												else
												{
													if (item.getItemStack().getAmount() > amountleft)
													{
														item.getItemStack().setAmount(amountleft);
													}
												}
											}
										}
									}
								}
							}
							if (Behavior == BehaviorState.Agressive)
							{
								if (Wolf.getTarget() == null)
								{
									for (Entity e : Wolf.getNearbyEntities(10, 10, 10))
									{
										if (MyWolfUtil.getCreatureType(e) != null)
										{
											Wolf.setTarget((LivingEntity) e);
										}
									}
								}
							}
						}
						if(Status == WolfState.Dead)
						{	
							RespawnTime--;
							if (RespawnTime <= 0)
							{
								RespawnWolf();
							}
						}
					}
				}
			}, 0L, 20L);
		}
	}

	public Player getOwner()
	{
		for (Player p : MyWolf.Plugin.getServer().getOnlinePlayers())
		{
			if (p.getName().equals(Owner) && MyWolfUtil.isNPC(p) == false)
			{
				return p;
			}
		}
		return null;
	}

	public void sendMessageToOwner(String Text)
	{
		if (getOwner() != null)
		{
			getOwner().sendMessage(Text);
		}
	}
}
