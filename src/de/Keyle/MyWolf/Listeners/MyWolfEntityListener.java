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

package de.Keyle.MyWolf.Listeners;

import net.minecraft.server.ItemStack;

import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.entity.CraftWolf;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Giant;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Spider;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageByProjectileEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.entity.EntityTargetEvent;

import de.Keyle.MyWolf.ConfigBuffer;
import de.Keyle.MyWolf.MyWolf;
import de.Keyle.MyWolf.Wolves;
import de.Keyle.MyWolf.Wolves.BehaviorState;
import de.Keyle.MyWolf.Wolves.WolfState;
import de.Keyle.MyWolf.util.MyWolfConfig;
import de.Keyle.MyWolf.util.MyWolfPermissions;
import de.Keyle.MyWolf.util.MyWolfUtil;
import de.Keyle.MyWolf.util.MyWolfLanguage;

public class MyWolfEntityListener extends EntityListener
{
	public void onEntityDamage(final EntityDamageEvent event)
	{
		if (event.getEntity() instanceof Wolf)
		{
			if (!(event instanceof EntityDamageByEntityEvent))
			{
				return;
			}
			EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;

			if (e.getDamager() instanceof Player)
			{
				Player player = (Player) e.getDamager();

				if (event.isCancelled() == false)
				{
					if (ConfigBuffer.mWolves.containsKey(player.getName()) == false)
					{
						if (MyWolfPermissions.has(player, "mywolf.leash") == false || player.getItemInHand().getType() != MyWolfConfig.LeashItem)
						{
							return;
						}
						Wolf TargetWolf = (Wolf) event.getEntity();

						String OwnerOfTheWolf = ((CraftWolf) TargetWolf).getHandle().getOwnerName();
						Player Attacker = (Player) e.getDamager();

						boolean isTarmed = TargetWolf.isTamed();

						if (isTarmed == true && OwnerOfTheWolf.equals(Attacker.getName()))
						{
							event.setCancelled(true);
							ConfigBuffer.mWolves.put(player.getName(), new Wolves(player.getName()));
							ConfigBuffer.mWolves.get(player.getName()).createWolf((Wolf) event.getEntity());
							MyWolf.Plugin.SaveWolves(ConfigBuffer.WolvesConfig);
							player.sendMessage(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_AddLeash")));
						}
					}
					String WolfOwner = null;
					for (String owner : ConfigBuffer.mWolves.keySet())
					{

						if (ConfigBuffer.mWolves.get(owner).getID() == event.getEntity().getEntityId())
						{
							WolfOwner = owner;
							break;
						}
					}
					if (WolfOwner != null && WolfOwner.equals(player.getName()))
					{
						Wolves wolf = ConfigBuffer.mWolves.get(WolfOwner);
						wolf.ResetSitTimer();
						wolf.SetName();
						if (player.getItemInHand().getType() == MyWolfConfig.LeashItem)
						{
							String msg;
							if (wolf.getHealth() > wolf.HealthMax / 3 * 2)
							{
								msg = "" + ChatColor.GREEN + wolf.getHealth() + ChatColor.WHITE + "/" + ChatColor.YELLOW + wolf.HealthMax + ChatColor.WHITE;
							}
							else if (wolf.getHealth() > wolf.HealthMax / 3 * 1)
							{
								msg = "" + ChatColor.YELLOW + wolf.getHealth() + ChatColor.WHITE + "/" + ChatColor.YELLOW + wolf.HealthMax + ChatColor.WHITE;
							}
							else
							{
								msg = "" + ChatColor.RED + wolf.getHealth() + ChatColor.WHITE + "/" + ChatColor.YELLOW + wolf.HealthMax + ChatColor.WHITE;
							}
							player.sendMessage(MyWolfUtil.SetColors("%wolfname% HP: %hp%").replace("%wolfname%", wolf.Name).replace("%hp%", msg));
							player.sendMessage(MyWolfUtil.SetColors("%wolfname%(Lv%lvl%) (%proz%%) EXP: %exp%/%reqexp%").replace("%wolfname%", wolf.Name).replace("%exp%", String.format("{0:F2}", wolf.Experience.getExp())).replace("%lvl%", "" + wolf.Experience.getLevel()).replace("%reqexp%", String.format("{0:F2}", wolf.Experience.getrequireEXP())).replace("%proz%", String.format("{0:F2}", wolf.Experience.getExp() * 100 / wolf.Experience.getrequireEXP())));
							if (wolf.Wolf.isSitting())
							{
								event.setCancelled(true);
								wolf.Wolf.setSitting(true);
							}
							else
							{
								event.setCancelled(true);
							}
						}
						/*
						if(cb.itemfunc.containsKey(player.getItemInHand().getType()))
						{
							if(cb.itemfunc.get(player.getItemInHand().getType()).size() != 0)
							{
								for(MyWolfFunction itemfunc :cb.itemfunc.get(player.getItemInHand().getType()))
								{
									if(MyWolfPermissions.has(player, "mywolf.item." + itemfunc.getName()))
									{
										if(itemfunc.run(wolf,null) == false)
										{
											event.setCancelled(true);
										}
									}
								}
							}
						}
						*/
					}
					for (Wolves wolf : ConfigBuffer.mWolves.values())
					{
						if (wolf.getID() == event.getEntity().getEntityId())
						{
							wolf.ResetSitTimer();
							if (wolf.getHealth() > wolf.HealthMax)
							{
								wolf.setHealth(wolf.HealthMax);
							}

							if (event.getDamage() < wolf.getHealth())
							{
								wolf.setHealth(wolf.getHealth() + event.getDamage());
								wolf.Demage(event.getDamage());
							}
							if (event.isCancelled() == false && MyWolfUtil.getPVP(event.getEntity().getLocation()) == false)
							{
								event.setCancelled(true);
							}
						}
					}
				}
			}
		}
	}

	@Override
	public void onEntityDeath(final EntityDeathEvent event)
	{
		if (event.getEntity() instanceof Wolf)
		{
			for (Wolves wolf : ConfigBuffer.mWolves.values())
			{
				if (wolf.getID() == event.getEntity().getEntityId())
				{
					if (MyWolfConfig.MaxLives > 0)
					{
						wolf.Lives -= 1;
						if (wolf.Lives <= 0)
						{
							for (ItemStack is : wolf.LargeInventory.getContents())
							{
								if (is != null)
								{
									wolf.Wolf.getWorld().dropItem(wolf.getLocation(), new org.bukkit.inventory.ItemStack(is.id, is.count, (short) is.damage));
								}
							}
							SendDeathMessage(event);
							wolf.sendMessageToOwner(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_WolfIsGone")).replace("%wolfname%", wolf.Name));
							ConfigBuffer.mWolves.remove(wolf.getOwner().getName());
							MyWolf.Plugin.SaveWolves(ConfigBuffer.WolvesConfig);
							return;
						}
					}
					wolf.Status = WolfState.Dead;
					wolf.RespawnTime  = wolf.Experience.getLevel() * MyWolfConfig.RespawnTimeFactor;
					SendDeathMessage(event);
					wolf.sendMessageToOwner(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_RespawnIn").replace("%wolfname%", wolf.Name).replace("%time%", ""+wolf.RespawnTime)));
					break;
				}
			}
		}
		else if (event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent)
		{
			if (((EntityDamageByEntityEvent) event.getEntity().getLastDamageCause()).getDamager() instanceof Wolf)
			{
				EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event.getEntity().getLastDamageCause();
				for (Wolves wolf : ConfigBuffer.mWolves.values())
				{
					if (wolf.getID() == e.getDamager().getEntityId())
					{
						if (MyWolfUtil.getCreatureType(e.getEntity()) != null)
						{
							wolf.Experience.addEXP(MyWolfUtil.getCreatureType(e.getEntity()));
						}
					}
				}
			}
		}
	}

	private void SendDeathMessage(final EntityDeathEvent event)
	{
		Wolves wolf = null;
		String Killer = MyWolfUtil.SetColors(MyWolfLanguage.getString("Unknow"));
		for (Wolves w : ConfigBuffer.mWolves.values())
		{
			if (w.getID() == event.getEntity().getEntityId())
			{
				wolf = w;
			}
		}
		if (wolf != null)
		{
			if (event.getEntity().getLastDamageCause() instanceof EntityDamageByProjectileEvent)
			{
				EntityDamageByProjectileEvent e = (EntityDamageByProjectileEvent) event.getEntity().getLastDamageCause();
				if (event.getEntity().getLastDamageCause() instanceof Player)
				{
					if (((Player) e.getDamager()) == wolf.getOwner())
					{
						Killer = MyWolfUtil.SetColors(MyWolfLanguage.getString("You"));
					}
					else
					{
						Killer = MyWolfUtil.SetColors(MyWolfLanguage.getString("Player")).replace("%player%", ((Player) e.getDamager()).getName());
					}
				}
				else if (event.getEntity().getLastDamageCause() instanceof Skeleton)
				{
					Killer = MyWolfUtil.SetColors(MyWolfLanguage.getString("Skeleton"));
				}
				else if (event.getEntity().getLastDamageCause() instanceof Ghast)
				{
					Killer = MyWolfUtil.SetColors(MyWolfLanguage.getString("Ghast"));
				}
			}
			else if (event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent)
			{
				EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event.getEntity().getLastDamageCause();
				if (e.getDamager() instanceof Player)
				{
					if (((Player) e.getDamager()) == wolf.getOwner())
					{
						Killer = MyWolfUtil.SetColors(MyWolfLanguage.getString("You"));
					}
					else
					{
						Killer = MyWolfUtil.SetColors(MyWolfLanguage.getString("Player")).replace("%player%", ((Player) e.getDamager()).getName());
					}
				}
				else if (e.getDamager() instanceof Zombie)
				{
					Killer = MyWolfUtil.SetColors(MyWolfLanguage.getString("Zombie"));
				}
				else if (e.getDamager() instanceof Creeper)
				{
					Killer = MyWolfUtil.SetColors(MyWolfLanguage.getString("Creeper"));
				}
				else if (e.getDamager() instanceof Spider)
				{
					Killer = MyWolfUtil.SetColors(MyWolfLanguage.getString("Spider"));
				}
				else if (e.getDamager() instanceof Slime)
				{
					Killer = MyWolfUtil.SetColors(MyWolfLanguage.getString("Slime"));
				}
				else if (e.getDamager() instanceof Giant)
				{
					Killer = MyWolfUtil.SetColors(MyWolfLanguage.getString("Giant"));
				}
				else if (e.getDamager() instanceof Wolf)
				{
					Wolf w = (Wolf) e.getDamager();
					if (w.isTamed() == true)
					{
						Killer = MyWolfUtil.SetColors(MyWolfLanguage.getString("Wolf")).replace("%player%", ((CraftWolf) w).getHandle().getOwnerName());;
						Killer = "a Wolf of " + ((Player) w.getOwner()).getName();
						for (String owner : ConfigBuffer.mWolves.keySet())
						{
							if (ConfigBuffer.mWolves.get(owner).getID() == w.getEntityId())
							{
								//Killer = "the wolf \"" +ChatColor.AQUA+ cb.mWolves.get(owner).Name + ChatColor.WHITE+ "\" of " + ((Player)w.getOwner()).getName();

							}
						}
					}
					else
					{
						Killer = MyWolfUtil.SetColors(MyWolfLanguage.getString("Wolf"));
					}
				}
			}
			else if (event.getEntity().getLastDamageCause().getCause().equals(DamageCause.BLOCK_EXPLOSION))
			{
				Killer = MyWolfUtil.SetColors(MyWolfLanguage.getString("Explosion"));
			}
			else if (event.getEntity().getLastDamageCause().getCause().equals(DamageCause.DROWNING))
			{
				Killer = MyWolfUtil.SetColors(MyWolfLanguage.getString("Drowning"));
			}
			else if (event.getEntity().getLastDamageCause().getCause().equals(DamageCause.FALL))
			{
				Killer = MyWolfUtil.SetColors(MyWolfLanguage.getString("Fall"));
			}
			else if (event.getEntity().getLastDamageCause().getCause().equals(DamageCause.FIRE))
			{
				Killer = MyWolfUtil.SetColors(MyWolfLanguage.getString("Fire"));
			}
			else if (event.getEntity().getLastDamageCause().getCause().equals(DamageCause.LAVA))
			{
				Killer = MyWolfUtil.SetColors(MyWolfLanguage.getString("Lava"));
			}
			else if (event.getEntity().getLastDamageCause().getCause().equals(DamageCause.LIGHTNING))
			{
				Killer = MyWolfUtil.SetColors(MyWolfLanguage.getString("Lightning"));
			}
			else if (event.getEntity().getLastDamageCause().getCause().equals(DamageCause.VOID))
			{
				Killer = MyWolfUtil.SetColors(MyWolfLanguage.getString("kvoid"));
			}
			wolf.getOwner().sendMessage(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_DeathMessage")).replace("%wolfname%", wolf.Name) + Killer);
		}
	}

	@Override
	public void onEntityTarget(final EntityTargetEvent event)
	{
		if (!event.isCancelled())
		{
			if (event.getEntity() instanceof Wolf)
			{
				for (Wolves Wolf : ConfigBuffer.mWolves.values())
				{
					if (Wolf.getID() == event.getEntity().getEntityId())
					{
						Wolf.ResetSitTimer();
						if (Wolf.Behavior == de.Keyle.MyWolf.Wolves.BehaviorState.Friendly)
						{
							event.setCancelled(true);
						}
						else if (Wolf.Behavior == BehaviorState.Raid)
						{
							if (event.getTarget() instanceof Player || (event.getTarget() instanceof Wolf && ((Wolf) event.getTarget()).isTamed() == true))
							{
								continue;
							}
						}
					}
				}
			}
		}
	}
}
