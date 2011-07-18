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
import org.bukkit.util.Vector;
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

	private int RespawnTimer;
	public int DropTimer = -1;
	private int AgressiveTimer = -1;

	private boolean isSitting = false;

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
		Experience = new MyWolfExperience(MyWolfConfig.WolfExpFactor, this);
	}

	public void SetName(String Name)
	{
		this.Name = Name;
		ChatColor NameColor;
		if (getHealth() > HealthMax / 3 * 2)
		{
			NameColor = ChatColor.GREEN;
		}
		else if (getHealth() > HealthMax / 3 * 1)
		{
			NameColor = ChatColor.YELLOW;
		}
		else
		{
			NameColor = ChatColor.RED;
		}
		if (Status == WolfState.Here)
		{
			BukkitContrib.getAppearanceManager().setGlobalTitle(Wolf, NameColor + Name);
		}
	}

	public void SetName()
	{
		ChatColor NameColor;
		if (getHealth() > HealthMax / 3 * 2)
		{
			NameColor = ChatColor.GREEN;
		}
		else if (getHealth() > HealthMax / 3 * 1)
		{
			NameColor = ChatColor.YELLOW;
		}
		else
		{
			NameColor = ChatColor.RED;
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
		StopDropTimer();
		isSitting = Wolf.isSitting();
		HealthNow = Wolf.getHealth();
		Location = Wolf.getLocation();
		Status = WolfState.Despawned;
		((LivingEntity) Wolf).remove();
		Wolf = null;
	}

	public void AgressiveTimer()
	{
		if (AgressiveTimer != -1)
		{
			MyWolf.Plugin.getServer().getScheduler().cancelTask(AgressiveTimer);
			AgressiveTimer = -1;
		}
		if (MyWolfUtil.hasSkill(Abilities, "Behavior"))
		{
			AgressiveTimer = MyWolf.Plugin.getServer().getScheduler().scheduleSyncRepeatingTask(MyWolf.Plugin, new Runnable()
			{
				public void run()
				{
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
					else
					{
						MyWolf.Plugin.getServer().getScheduler().cancelTask(AgressiveTimer);
						AgressiveTimer = -1;
					}
				}
			}, 0L, 50L);
		}
	}

	public boolean createWolf(boolean sitting)
	{
		if (Wolf != null && Wolf.isDead() == false)
		{
			return false;
		}
		else
		{
			if (getOwner() != null && RespawnTime == 0)
			{
				Wolf = (Wolf) MyWolf.Plugin.getServer().getWorld(Location.getWorld().getName()).spawnCreature(Location, CreatureType.WOLF);
				Wolf.setOwner(getOwner());
				Wolf.setSitting(sitting);
				Location = Wolf.getLocation();
				Wolf.setHealth((int) HealthNow);
				ID = Wolf.getEntityId();

				Status = WolfState.Here;
				SetName();
				if (MyWolfPermissions.has(getOwner(), "mywolf.pickup") && MyWolfUtil.hasSkill(Abilities, "Pickup") == true)
				{
					DropTimer();
				}
				if (Behavior == BehaviorState.Agressive)
				{
					AgressiveTimer();
				}
				return true;
			}
			else if (RespawnTime > 0)
			{
				RespawnTimer();
				return false;
			}
		}
		return false;
	}

	public void createWolf(Wolf wolf)
	{
		Wolf = wolf;
		ID = Wolf.getEntityId();
		Location = Wolf.getLocation();
		Status = WolfState.Here;
		SetName();
		if (MyWolfPermissions.has(getOwner(), "mywolf.pickup") && MyWolfUtil.hasSkill(Abilities, "Pickup") == true)
		{
			DropTimer();
		}
		if (Behavior == BehaviorState.Agressive)
		{
			AgressiveTimer();
		}
	}

	public void setWolfHealth(double d)
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
		//if (isThere == true && isDead == false)
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
		//if (isThere == true && isDead == false)
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

	public void RespawnTimer()
	{
		Status = WolfState.Dead;
		if (RespawnTime == 0)
		{
			RespawnTime = (int) (HealthMax * MyWolfConfig.WolfRespawnTimeFactor);
		}
		RespawnTime++;
		getOwner().sendMessage(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_RespawnIn")).replace("%wolfname%", Name).replace("%time%", "" + (RespawnTime - 1)));
		RespawnTimer = MyWolf.Plugin.getServer().getScheduler().scheduleSyncRepeatingTask(MyWolf.Plugin, new Runnable()
		{

			public void run()
			{
				if (getOwner() != null)
				{
					RespawnTime--;
				}
				else
				{
					MyWolf.Plugin.getServer().getScheduler().cancelTask(RespawnTimer);
				}
				if (RespawnTime <= 0)
				{
					RespawnWolf();
					MyWolf.Plugin.getServer().getScheduler().cancelTask(RespawnTimer);
				}
			}
		}, 0L, 20L);
	}

	public boolean RespawnWolf()
	{
		//if (isDead == false)
		if (Status == WolfState.Dead)
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

	public void StopDropTimer()
	{
		if (DropTimer != -1)
		{
			MyWolf.Plugin.getServer().getScheduler().cancelTask(DropTimer);
			DropTimer = -1;
		}

	}

	public void DropTimer()
	{
		//if (isThere == true)
		if (Status == WolfState.Here)
		{
			if (DropTimer != -1)
			{
				StopDropTimer();
			}
			DropTimer = MyWolf.Plugin.getServer().getScheduler().scheduleSyncRepeatingTask(MyWolf.Plugin, new Runnable()
			{
				public void run()
				{
					//if (isThere == false || isDead == true || getOwner() == null)
					if (Status != WolfState.Here || getOwner() == null)
					{
						StopDropTimer();
					}
					else
					{
						if (getOwner() != null)
						{
							try
							{
								for (Entity e : Wolf.getWorld().getEntities())
								{
									if (e instanceof Item)
									{
										Item item = (Item) e;

										Vector distance = getLocation().toVector().add(new Vector(0.5, 0, 0.5)).subtract(item.getLocation().toVector());
										if (distance.lengthSquared() < 1.0 * MyWolfConfig.WolfPickupRange * MyWolfConfig.WolfPickupRange + 1)
										{
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
							}
							catch (Exception e)
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
