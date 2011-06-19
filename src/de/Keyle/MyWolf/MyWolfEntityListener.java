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

import net.minecraft.server.ItemStack;

import org.bukkit.ChatColor;
import org.bukkit.Material;
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

import de.Keyle.MyWolf.Wolves.InventoryType;
import de.Keyle.MyWolf.util.MyWolfUtil;

public class MyWolfEntityListener extends EntityListener
{
	private ConfigBuffer cb;

	public MyWolfEntityListener(ConfigBuffer cb)
	{
	  this.cb = cb;
	}

	@Override
	public void onEntityDamage(EntityDamageEvent event)
	{
		if ((event.getEntity() instanceof Wolf))
		{
			if (!(event instanceof EntityDamageByEntityEvent)) 
			{
			      return;
			}
			EntityDamageByEntityEvent e = (EntityDamageByEntityEvent)event;
			
			if(e.getDamager() instanceof Player)
			{
				Player player = (Player)e.getDamager();
				
				if(event.isCancelled() == false)
				{
					
					if(cb.mWolves.containsKey(player.getName()) == false)
					{
						if(cb.Permissions.has(player, "mywolf.leash") == false  || player.getItemInHand().getType() != cb.cv.WolfLeashItem)
						{
							return;
						}
						Wolf TargetWolf = (Wolf)event.getEntity();
						
						String OwnerOfTheWolf = ((CraftWolf) TargetWolf).getHandle().x();
						Player Attacker = (Player)e.getDamager();
						
						boolean isTarmed = TargetWolf.isTamed();
						
						if(isTarmed == true && OwnerOfTheWolf.equals(Attacker.getName()))
						{
							event.setCancelled(true);
							cb.mWolves.put(player.getName(), new Wolves(cb,player.getName()));
							cb.mWolves.get(player.getName()).createWolf((Wolf)event.getEntity());
							cb.Plugin.SaveWolves();
							player.sendMessage(MyWolfUtil.SetColors(cb.lv.Msg_AddLeash));
							//player.sendMessage(ChatColor.GREEN + "You take your wolf on the leash, he'll be a good wolf.");
						}
					}
					String WolfOwner = null;
					for ( String owner : cb.mWolves.keySet() )
			        {
						
						if(cb.mWolves.get( owner ).getID() == event.getEntity().getEntityId())
						{
							WolfOwner = owner;
							break;
						}
			        }
					if(WolfOwner != null && WolfOwner.equals(player.getName()))
					{
						Wolves wolf = cb.mWolves.get( WolfOwner );
						if(cb.mWolves.get(WolfOwner).getHealth() > cb.mWolves.get(WolfOwner).HealthMax)
						{
							cb.mWolves.get(WolfOwner).setWolfHealth(cb.mWolves.get(WolfOwner).HealthMax);
						}

						if(player.getItemInHand().getType() == cb.cv.WolfLeashItem && cb.cv.WolfLeashItemSneak == player.isSneaking())
						{
							if(cb.Permissions.has(player, "mywolf.info") == false)
							{
								return;
							}
							{
								String msg = "";//+ChatColor.AQUA + wolf.Name + ChatColor.WHITE + " HP:";
								if(wolf.getHealth() > wolf.HealthMax/3*2)
								{
									msg += "" + ChatColor.GREEN + wolf.getHealth() + ChatColor.WHITE + "/" + ChatColor.YELLOW + wolf.HealthMax+ ChatColor.WHITE;
								}
								else if(wolf.getHealth() > wolf.HealthMax/3*1)
								{
									msg += "" + ChatColor.YELLOW + wolf.getHealth() + ChatColor.WHITE + "/" + ChatColor.YELLOW + wolf.HealthMax+ ChatColor.WHITE;
								}
								else
								{
									msg += ""  +ChatColor.RED + wolf.getHealth() + ChatColor.WHITE + "/" + ChatColor.YELLOW + wolf.HealthMax+ ChatColor.WHITE;
								}
								//player.sendMessage(msg);
								player.sendMessage(MyWolfUtil.SetColors(cb.lv.Msg_HPinfo).replace("%wolfname%", wolf.Name).replace("%hp%", msg));
								if(wolf.MyWolf.isSitting())
								{
									event.setCancelled(true);
									wolf.isSitting = true;
									wolf.MyWolf.setSitting(true);
								}
								else
								{
									wolf.isSitting = false;
									event.setCancelled(true);
								}
							}
						}
						if(player.getItemInHand().getType() == cb.cv.WolfChestAddItem)
						{
							if(wolf.MyWolf.getEntityId() == event.getEntity().getEntityId())
							{
								if(cb.Permissions.has(player, "mywolf.chest.add.small") == false)
								{
									return;
								}
								else if(wolf.InventoryMode == InventoryType.SMALL && cb.Permissions.has(player, "mywolf.chest.add.large") == false)
								{
									return;
								}
								if(wolf.InventoryMode == InventoryType.NONE || wolf.InventoryMode == InventoryType.SMALL)
								{
									wolf.InventoryMode = (wolf.InventoryMode == InventoryType.NONE?InventoryType.SMALL:InventoryType.LARGE);
									if(player.getItemInHand().getAmount()>1)
									{
										player.getItemInHand().setAmount(player.getItemInHand().getAmount()-1);
									}
									else
									{
										player.getInventory().removeItem(player.getInventory().getItemInHand());
									}
									event.setCancelled(true);
									wolf.MyWolf.setSitting(true);
									player.sendMessage(MyWolfUtil.SetColors((wolf.InventoryMode == InventoryType.SMALL?cb.lv.Msg_AddChest:cb.lv.Msg_AddChestGreater)).replace("%wolfname%", wolf.Name));
									//player.sendMessage(ChatColor.AQUA+wolf.Name + ChatColor.WHITE + " has now an" + (wolf.InventoryMode == InventoryType.SMALL?"":" bigger") + " inventory.");
								}
									
								
							}
						}
						if(player.getItemInHand().getType() == cb.cv.WolfFoodLivesItem)
						{
							if(cb.cv.WolfMaxLives > -1 && wolf.MyWolf.getEntityId() == event.getEntity().getEntityId())
							{
								if(cb.Permissions.has(player, "mywolf.food.lives") == false)
								{
									return;
								}
								if(wolf.Lives < cb.cv.WolfMaxLives)
								{
									wolf.Lives += 1;
									if(player.getItemInHand().getAmount()>1)
									{
										player.getItemInHand().setAmount(player.getItemInHand().getAmount()-1);
									}
									else
									{
										player.getInventory().removeItem(player.getInventory().getItemInHand());
									}
									
									player.sendMessage(MyWolfUtil.SetColors(cb.lv.Msg_AddLive).replace("%wolfname%", wolf.Name));
									//player.sendMessage(ChatColor.GREEN + "+1 life for " + ChatColor.AQUA + wolf.Name);
								}
								else
								{
									player.sendMessage(MyWolfUtil.SetColors(cb.lv.Msg_MaxLives).replace("%wolfname%", wolf.Name).replace("%maxlives%", ""+cb.cv.WolfMaxLives));
									//player.sendMessage(ChatColor.AQUA + wolf.Name + ChatColor.RED + " has reached the maximum of " + cb.cv.WolfMaxLives + " lives.");
								}
								if(wolf.MyWolf.isSitting())
								{
									wolf.isSitting = true;
									wolf.MyWolf.setSitting(true);
								}
								else
								{
									wolf.isSitting = false;
								}
								event.setCancelled(true);
							}
						}
						if(player.getItemInHand().getType() == cb.cv.WolfPickupItem)
						{
							if(wolf.MyWolf.getEntityId() == event.getEntity().getEntityId())
							{
								if(cb.Permissions.has(player, "mywolf.pickup.add") == false)
								{
									return;
								}
								if(wolf.hasPickup == false)
								{
									wolf.hasPickup = true;
									if(player.getItemInHand().getAmount()>1)
									{
										player.getItemInHand().setAmount(player.getItemInHand().getAmount()-1);
									}
									else
									{
										player.getInventory().removeItem(player.getInventory().getItemInHand());
									}
									event.setCancelled(true);
									wolf.MyWolf.setSitting(true);
									wolf.DropTimer();
									player.sendMessage(MyWolfUtil.SetColors(cb.lv.Msg_AddPickup).replace("%wolfname%", wolf.Name).replace("%range%", ""+cb.cv.WolfPickupRange));
									//player.sendMessage(ChatColor.AQUA+wolf.Name + ChatColor.WHITE + " now pickup items in a range of" + cb.cv.WolfPickupRange + ".");
								}
									
								
							}
						}
						if(player.getItemInHand().getType() == cb.cv.WolfChestOpenItem && cb.cv.WolfChestOpenItemSneak == player.isSneaking())
						{
							if(wolf.MyWolf.getEntityId() == event.getEntity().getEntityId())
							{
								if(cb.Permissions.has(player, "mywolf.chest.open") == false)
								{
									return;
								}
								if(wolf.InventoryMode != InventoryType.NONE)
								{
									if(wolf.getLocation().getBlock().getType() != Material.STATIONARY_WATER && wolf.getLocation().getBlock().getType() != Material.WATER)
									{
										wolf.OpenInventory();
										if(wolf.isSitting() == false)
										{
											cb.WolfChestOpened.add(player);
										}
										wolf.MyWolf.setSitting(true);
									}
									else
									{
										player.sendMessage(cb.lv.Msg_InventorySwimming);
										//player.sendMessage("You can't open the inventory while the wolf is swimming!");
									}
									event.setCancelled(true);
								}
							}
						}
						if(player.getItemInHand().getType() == cb.cv.WolfFoodHPItem)
						{
							if(wolf.MyWolf.getEntityId() == event.getEntity().getEntityId())
							{
								if(cb.Permissions.has(player, "mywolf.food.hp") == false)
								{
									return;
								}
								if(wolf.HealthMax < cb.cv.WolfRespawnMaxHP)
								{
									String msg = "";//+ChatColor.AQUA+wolf.Name + ChatColor.WHITE + ":";
									wolf.HealthMax += 1;
									wolf.setWolfHealth(wolf.getHealth()+1);
									if(player.getItemInHand().getAmount()>1)
									{
										player.getItemInHand().setAmount(player.getItemInHand().getAmount()-1);
									}
									else
									{
										player.getInventory().removeItem(player.getInventory().getItemInHand());
									}
									
									if(wolf.getHealth() > wolf.HealthMax/3*2)
									{
										msg += " HP:" + ChatColor.GREEN + wolf.getHealth() + ChatColor.WHITE + "/" + ChatColor.YELLOW + wolf.HealthMax;
									}
									else if(wolf.getHealth() > wolf.HealthMax/3*1)
									{
										msg += " HP:" + ChatColor.YELLOW + wolf.getHealth() + ChatColor.WHITE + "/" + ChatColor.YELLOW + wolf.HealthMax;
									}
									else
									
									{
										msg += " HP:" + ChatColor.RED + wolf.getHealth() + ChatColor.WHITE + "/" + ChatColor.YELLOW + wolf.HealthMax;
									}
									
									player.sendMessage(MyWolfUtil.SetColors(cb.lv.Msg_AddHP.replace("%wolfname%", wolf.Name)));
									//player.sendMessage(ChatColor.GREEN + "+1 MaxHP for " + ChatColor.AQUA + wolf.Name);
									player.sendMessage(MyWolfUtil.SetColors(cb.lv.Msg_HPinfo.replace("%wolfname%", wolf.Name).replace("%hp%", msg)));
									//player.sendMessage(msg);
								}
								else
								{
									player.sendMessage(MyWolfUtil.SetColors(cb.lv.Msg_MaxHP).replace("%wolfname%", wolf.Name).replace("%maxlives%", ""+cb.cv.WolfRespawnMaxHP));
									//player.sendMessage(ChatColor.AQUA + wolf.Name + ChatColor.RED + " has reached the maximum of " + cb.cv.WolfRespawnMaxHP + "HP.");
								}
								if(wolf.MyWolf.isSitting())
								{
									wolf.isSitting = true;
									wolf.MyWolf.setSitting(true);
								}
								else
								{
									wolf.isSitting = false;
								}
								event.setCancelled(true);
							}	
						}
						
						/* --> for Armor
						if(event.isCancelled() == false)
						{
							for(ItemStack is : wolf.WolfInventory.getContents())
							{
								if(cb.ArmorList.containsKey(is.id))
								{
									event.setDamage(event.getDamage()-cb.ArmorList.get(is.id));
								}
							}
						}
						*/
						if(event.isCancelled() == false && event.getEntity().getWorld().getPVP() == false)
						{
							event.setCancelled(true);
						}
					}      	
				}
			}
		}
	}

	@Override
	public void onEntityDeath(EntityDeathEvent event)
	{
		if (event.getEntity() instanceof Wolf)
		{
			for ( String owner : cb.mWolves.keySet() )
	        {
				if(cb.mWolves.get(owner).ID == event.getEntity().getEntityId())
				{
					if(cb.cv.WolfMaxLives > -1)
					{
						cb.mWolves.get(owner).Lives -= 1;
						if(cb.mWolves.get(owner).Lives <= 0)
						{
							cb.mWolves.get(owner).StopDropTimer();
							for(ItemStack is : cb.mWolves.get(owner).LargeInventory.getContents())
							{
								cb.mWolves.get(owner).MyWolf.getWorld().dropItem(cb.mWolves.get(owner).getLocation(), new org.bukkit.inventory.ItemStack(is.id, is.count, (short)is.damage));
							}
							cb.mWolves.get(owner).getPlayer().sendMessage(MyWolfUtil.SetColors(cb.lv.Msg_WolfIsGone).replace("%wolfname%", cb.mWolves.get(owner).Name));
							//cb.mWolves.get(owner).getPlayer().sendMessage(ChatColor.AQUA + cb.mWolves.get(owner).Name + ChatColor.WHITE + " is " + ChatColor.RED + "gone" + ChatColor.WHITE + " and will never come back . . .");
							cb.mWolves.remove(cb.mWolves.get(owner).getPlayer().getName());
							cb.Plugin.SaveWolves();
						}
						else
						{
							cb.mWolves.get(owner).StopDropTimer();
							cb.mWolves.get(owner).isDead = true;
							cb.mWolves.get(owner).RespawnTimer();
						}
					}
					else
					{
						cb.mWolves.get(owner).StopDropTimer();
						cb.mWolves.get(owner).isDead = true;
						cb.mWolves.get(owner).RespawnTimer();
					}
					SendDeathMessage(event);
					break;
				}	        	
	        }
		}
	}
	
	private void SendDeathMessage(EntityDeathEvent event)
	{
		Wolves wolf = null;
		String Killer = MyWolfUtil.SetColors(cb.lv.Unknow);//"Unknown";
		for ( String owner : cb.mWolves.keySet() )
        {
			if(cb.mWolves.get(owner).ID == event.getEntity().getEntityId())
			{
				 wolf = cb.mWolves.get(owner);
			}
        }
		if(wolf != null)
		{
			
			//String KillMessage = "";
			if(event.getEntity().getLastDamageCause() instanceof EntityDamageByProjectileEvent)
			{
				EntityDamageByProjectileEvent e = (EntityDamageByProjectileEvent)event.getEntity().getLastDamageCause();
				if(event.getEntity().getLastDamageCause() instanceof Player)
				{
					if(((Player)e.getDamager()) == wolf.getPlayer())
					{
						Killer = MyWolfUtil.SetColors(cb.lv.You);//ChatColor.RED + "YOU!";
					}
					else
					{
						Killer = MyWolfUtil.SetColors(cb.lv.Player).replace("%player%", ((Player)e.getDamager()).getName());
					}
				}
				else if(event.getEntity().getLastDamageCause() instanceof Skeleton)
				{
					Killer = MyWolfUtil.SetColors(cb.lv.Skeleton);//"a Skeleton";
				}
				else if(event.getEntity().getLastDamageCause() instanceof Ghast)
				{
					Killer = MyWolfUtil.SetColors(cb.lv.Ghast);//"a Ghast";
				}
				//KillMessage = ChatColor.AQUA+ wolf.Name + ChatColor.WHITE + " was killed by " + Killer;
			}
			else if (event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent)
			{
				EntityDamageByEntityEvent e = (EntityDamageByEntityEvent)event.getEntity().getLastDamageCause();
				if(e.getDamager() instanceof Player)
				{
					if(((Player)e.getDamager()) == wolf.getPlayer())
					{
						Killer = MyWolfUtil.SetColors(cb.lv.You);//"YOU";
					}
					else
					{
						Killer = MyWolfUtil.SetColors(cb.lv.Player).replace("%player%", ((Player)e.getDamager()).getName());
					}
				}
				else if(e.getDamager() instanceof Zombie)
				{
					Killer = MyWolfUtil.SetColors(cb.lv.Zombie);//"a Zombie";
				}
				else if(e.getDamager() instanceof Creeper)
				{
					Killer = MyWolfUtil.SetColors(cb.lv.Creeper);//"a Creeper";
				}
				else if(e.getDamager() instanceof Spider)
				{
					Killer = MyWolfUtil.SetColors(cb.lv.Spider);//"a Spider";
				}
				else if(e.getDamager() instanceof Slime)
				{
					Killer = MyWolfUtil.SetColors(cb.lv.Slime);//"a Slime";
				}
				else if(e.getDamager() instanceof Giant)
				{
					Killer = MyWolfUtil.SetColors(cb.lv.Giant);//"a Giant";
				}
				else if(e.getDamager() instanceof Wolf)
				{
					Wolf w = (Wolf)e.getDamager();
					if(w.isTamed() == true)
					{
						Killer = MyWolfUtil.SetColors(cb.lv.Wolf).replace("%player%", ((CraftWolf) w).getHandle().x());;
						//Killer = "a Wolf of " + ((Player)w.getOwner()).getName();
						//for ( String owner : cb.mWolves.keySet() )
				        //{
							//f(cb.mWolves.get(owner).MyWolf == w)
							//{
								//Killer = "the wolf \"" +ChatColor.AQUA+ cb.mWolves.get(owner).Name + ChatColor.WHITE+ "\" of " + ((Player)w.getOwner()).getName();
								
							//}
				        //}
					}
					else
					{
						Killer = MyWolfUtil.SetColors(cb.lv.Wolf);//"a Wolf";
					}
				}
				//KillMessage = ChatColor.AQUA+ wolf.Name + ChatColor.WHITE + " was killed by " + Killer;
			}
			else if (event.getEntity().getLastDamageCause().getCause().equals(DamageCause.BLOCK_EXPLOSION))
			{
				Killer = MyWolfUtil.SetColors(cb.lv.Explosion);
				//KillMessage = ChatColor.AQUA+ wolf.Name + ChatColor.WHITE + " was killed by an Explosion";
			}
			else if (event.getEntity().getLastDamageCause().getCause().equals(DamageCause.DROWNING))
			{
				Killer = MyWolfUtil.SetColors(cb.lv.Drowning);
				//KillMessage = ChatColor.AQUA+ wolf.Name + ChatColor.WHITE + " drowned";
			}
			else if (event.getEntity().getLastDamageCause().getCause().equals(DamageCause.FALL))
			{
				Killer = MyWolfUtil.SetColors(cb.lv.Fall);
				//KillMessage = ChatColor.AQUA+ wolf.Name + ChatColor.WHITE + " died by falling down";
			}
			else if (event.getEntity().getLastDamageCause().getCause().equals(DamageCause.FIRE))
			{
				Killer = MyWolfUtil.SetColors(cb.lv.Fire);
				//KillMessage = ChatColor.AQUA+ wolf.Name + ChatColor.WHITE + " was killed by fire";
			}
			else if (event.getEntity().getLastDamageCause().getCause().equals(DamageCause.LAVA))
			{
				Killer = MyWolfUtil.SetColors(cb.lv.Lava);
				//KillMessage =ChatColor.AQUA+ wolf.Name + ChatColor.WHITE + " was killed by lava";
			}
			else if (event.getEntity().getLastDamageCause().getCause().equals(DamageCause.LIGHTNING))
			{
				Killer = MyWolfUtil.SetColors(cb.lv.Lightning);
				//KillMessage = ChatColor.AQUA+ wolf.Name + ChatColor.WHITE + " was killed by a lightning";
			}
			else if (event.getEntity().getLastDamageCause().getCause().equals(DamageCause.VOID))
			{
				Killer = MyWolfUtil.SetColors(cb.lv.kvoid);
				//KillMessage = ChatColor.AQUA+ wolf.Name + ChatColor.WHITE + " was killed by void";
			}
			wolf.getPlayer().sendMessage(MyWolfUtil.SetColors(cb.lv.Msg_DeathMessage).replace("%wolfname%", wolf.Name) + Killer);
		}
	}
	
	@Override
	public void onEntityTarget(EntityTargetEvent event) {
		if (!event.isCancelled()) {
			if (event.getEntity() instanceof Wolf && event.getTarget() instanceof Player)
			{
				for ( String owner : cb.mWolves.keySet() )
		        {
					if(cb.mWolves.get(owner).ID == event.getEntity().getEntityId())
					{
						if(event.getEntity().getWorld().getPVP() == false)
						{
							event.setCancelled(true);
						}
					}	        	
		        }
			}
		}
	}
}