/*
 * Copyright (C) 2011-2012 Keyle
 *
 * This file is part of MyWolf
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

import de.Keyle.MyWolf.ConfigBuffer;
import de.Keyle.MyWolf.MyWolf;
import de.Keyle.MyWolf.MyWolf.BehaviorState;
import de.Keyle.MyWolf.MyWolf.WolfState;
import de.Keyle.MyWolf.MyWolfPlugin;
import de.Keyle.MyWolf.Skill.MyWolfExperience;
import de.Keyle.MyWolf.util.MyWolfConfig;
import de.Keyle.MyWolf.util.MyWolfLanguage;
import de.Keyle.MyWolf.util.MyWolfPermissions;
import de.Keyle.MyWolf.util.MyWolfUtil;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.entity.CraftWolf;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class MyWolfEntityListener implements Listener
{
    @EventHandler()
    public void onEntityDamage(final EntityDamageEvent event)
    {
        if (!(event instanceof EntityDamageByEntityEvent))
        {
            return;
        }
        EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
        if (event.getEntity() instanceof Wolf)
        {
            if (e.getDamager() instanceof Player)
            {
                Player player = (Player) e.getDamager();

                if (!ConfigBuffer.mWolves.containsKey(player.getName()))
                {
                    if (!MyWolfPermissions.has(player, "MyWolf.leash") || player.getItemInHand().getType() != MyWolfConfig.LeashItem)
                    {
                        return;
                    }
                    Wolf TargetWolf = (Wolf) event.getEntity();

                    String OwnerOfTheWolf = ((CraftWolf) TargetWolf).getHandle().getOwnerName();
                    Player Attacker = (Player) e.getDamager();

                    boolean isTarmed = TargetWolf.isTamed();

                    if (isTarmed && OwnerOfTheWolf.equals(Attacker.getName()))
                    {
                        event.setCancelled(true);
                        ConfigBuffer.mWolves.put(player.getName(), new MyWolf(player.getName()));
                        ConfigBuffer.mWolves.get(player.getName()).createWolf((Wolf) event.getEntity());
                        MyWolfPlugin.Plugin.SaveWolves(ConfigBuffer.WolvesConfig);
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
                for (MyWolf wolf : ConfigBuffer.mWolves.values())
                {
                    if (wolf.getID() == event.getEntity().getEntityId())
                    {
                        wolf.ResetSitTimer();
                        if (wolf.getHealth() > wolf.HealthMax)
                        {
                            wolf.setHealth(wolf.HealthMax);
                        }

                        if (!event.isCancelled() && !MyWolfUtil.getPVP(event.getEntity().getLocation()))
                        {
                            event.setCancelled(true);
                        }
                        if (!event.isCancelled())
                        {
                            wolf.SetName(wolf.getHealth() - event.getDamage());
                            //wolf.updateHPbar(wolf.getHealth()-event.getDamage());
                        }
                        break;
                    }
                }
                if (WolfOwner != null && WolfOwner.equals(player.getName()))
                {
                    MyWolf wolf = ConfigBuffer.mWolves.get(WolfOwner);
                    wolf.ResetSitTimer();
                    wolf.SetName();
                    if (player.getItemInHand().getType() == MyWolfConfig.LeashItem)
                    {
                        String msg;
                        if (wolf.getHealth() > wolf.HealthMax / 3 * 2)
                        {
                            msg = "" + ChatColor.GREEN + wolf.getHealth() + ChatColor.WHITE + "/" + ChatColor.YELLOW + wolf.HealthMax + ChatColor.WHITE;
                        }
                        else if (wolf.getHealth() > wolf.HealthMax / 3)
                        {
                            msg = "" + ChatColor.YELLOW + wolf.getHealth() + ChatColor.WHITE + "/" + ChatColor.YELLOW + wolf.HealthMax + ChatColor.WHITE;
                        }
                        else
                        {
                            msg = "" + ChatColor.RED + wolf.getHealth() + ChatColor.WHITE + "/" + ChatColor.YELLOW + wolf.HealthMax + ChatColor.WHITE;
                        }
                        player.sendMessage(MyWolfUtil.SetColors("%aqua%%wolfname%%white% HP: %hp%").replace("%wolfname%", wolf.Name).replace("%hp%", msg));
                        if (MyWolfConfig.LevelSystem)
                        {
                            int lvl = wolf.Experience.getLevel();
                            double EXP = wolf.Experience.getActualEXP();
                            double reqEXP = wolf.Experience.getrequireEXP();
                            player.sendMessage(MyWolfUtil.SetColors("%aqua%%wolfname%%white% (Lv%lvl%) (%proz%%) EXP:%exp%/%reqexp%").replace("%wolfname%", wolf.Name).replace("%exp%", String.format("%1.2f", EXP)).replace("%lvl%", "" + lvl).replace("%reqexp%", String.format("%1.2f", reqEXP)).replace("%proz%", String.format("%1.2f", EXP * 100 / reqEXP)));
                        }
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
                }
            }
            else if(e.getDamager() instanceof Wolf)
            {
                for (MyWolf wolf : ConfigBuffer.mWolves.values())
                {
                    if (wolf.getID() == e.getDamager().getEntityId())
                    {
                        event.setDamage(event.getDamage() + wolf.DamageBonus);
                        break;
                    }
                }
            }
        }
    }

    @EventHandler()
    public void onEntityDeath(final EntityDeathEvent event)
    {
        if (event.getEntity() instanceof Wolf)
        {
            for (MyWolf wolf : ConfigBuffer.mWolves.values())
            {
                if (wolf.getID() == event.getEntity().getEntityId())
                {
                    wolf.Status = WolfState.Dead;
                    wolf.RespawnTime = MyWolfConfig.RespawnTimeFixed + (wolf.Experience.getLevel() * MyWolfConfig.RespawnTimeFactor);
                    SendDeathMessage(event);
                    wolf.sendMessageToOwner(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_RespawnIn").replace("%wolfname%", wolf.Name).replace("%time%", "" + wolf.RespawnTime)));
                    break;
                }
            }
        }
        if (MyWolfConfig.LevelSystem && event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent)
        {
            if (((EntityDamageByEntityEvent) event.getEntity().getLastDamageCause()).getDamager() instanceof Wolf)
            {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event.getEntity().getLastDamageCause();
                for (MyWolf wolf : ConfigBuffer.mWolves.values())
                {
                    if (wolf.getID() == e.getDamager().getEntityId())
                    {
                        if (MyWolfUtil.getCreatureType(e.getEntity()) != null)
                        {
                            if(MyWolfExperience.defaultEXPvalues)
                            {
                                wolf.Experience.addExp((double)event.getDroppedExp());
                            }
                            else
                            {
                                wolf.Experience.addEXP(MyWolfUtil.getCreatureType(e.getEntity()));
                            }
                        }
                    }
                }
            }
        }
    }

    private void SendDeathMessage(final EntityDeathEvent event)
    {
        MyWolf wolf = null;
        String Killer = MyWolfUtil.SetColors(MyWolfLanguage.getString("Unknow"));
        for (MyWolf w : ConfigBuffer.mWolves.values())
        {
            if (w.getID() == event.getEntity().getEntityId())
            {
                wolf = w;
            }
        }
        if (wolf != null)
        {
            if (event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent)
            {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event.getEntity().getLastDamageCause();
               
                //TODO: Add new mobs
                if (e.getDamager() instanceof Player)
                {
                    if (e.getDamager() == wolf.getOwner())
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
                else if (e.getDamager() instanceof Skeleton)
                {
                    Killer = MyWolfUtil.SetColors(MyWolfLanguage.getString("Skeleton"));
                }
                else if (e.getDamager() instanceof CaveSpider)
                {
                    Killer = MyWolfUtil.SetColors(MyWolfLanguage.getString("CaveSpider"));
                }
                else if (e.getDamager() instanceof Enderman)
                {
                    Killer = MyWolfUtil.SetColors(MyWolfLanguage.getString("Enderman"));
                }
                else if (e.getDamager() instanceof PigZombie)
                {
                    Killer = MyWolfUtil.SetColors(MyWolfLanguage.getString("PigZombie"));
                }
                else if (e.getDamager() instanceof Silverfish)
                {
                    Killer = MyWolfUtil.SetColors(MyWolfLanguage.getString("Silverfish"));
                }
                else if (e.getDamager() instanceof Wolf)
                {
                    Wolf w = (Wolf) e.getDamager();
                    if (w.isTamed())
                    {
                        Killer = MyWolfUtil.SetColors(MyWolfLanguage.getString("Wolf")).replace("%player%", ((CraftWolf) w).getHandle().getOwnerName());
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

    @EventHandler()
    public void onEntityTarget(final EntityTargetEvent event)
    {
        if (!event.isCancelled())
        {
            if (event.getEntity() instanceof Wolf)
            {
                for (MyWolf Wolf : ConfigBuffer.mWolves.values())
                {
                    if (Wolf.getID() == event.getEntity().getEntityId())
                    {
                        Wolf.ResetSitTimer();
                        if (Wolf.Behavior == de.Keyle.MyWolf.MyWolf.BehaviorState.Friendly)
                        {
                            event.setCancelled(true);
                        }
                        else if(event.getTarget() instanceof Player && ((Player)event.getTarget()).getName().equals(Wolf.getOwner().getName()))
                        {
                        	event.setCancelled(true);
                        }
                        else if (Wolf.Behavior == BehaviorState.Raid)
                        {
                            if (event.getTarget() instanceof Player || (event.getTarget() instanceof Wolf && ((Wolf) event.getTarget()).isTamed()))
                            {
                                event.setCancelled(true);
                            }
                        }
                    }
                }
            }
        }
    }
}
