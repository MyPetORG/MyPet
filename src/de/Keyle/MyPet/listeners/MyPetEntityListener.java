/*
 * Copyright (C) 2011-2012 Keyle
 *
 * This file is part of MyPet
 *
 * MyPet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyPet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyPet. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.listeners;

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.entity.types.wolf.CraftMyWolf;
import de.Keyle.MyPet.entity.types.wolf.MyWolf;
import de.Keyle.MyPet.event.MyPetLeashEvent;
import de.Keyle.MyPet.skill.skills.Behavior;
import de.Keyle.MyPet.skill.skills.Poison;
import de.Keyle.MyPet.util.*;
import net.minecraft.server.EntityWolf;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.entity.CraftWolf;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class MyPetEntityListener implements Listener
{
    @EventHandler
    public void onEntityDamage(final EntityDamageEvent event)
    {
        if (!(event instanceof EntityDamageByEntityEvent) || event.isCancelled())
        {
            return;
        }
        EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
        if (event.getEntity() instanceof Wolf)
        {
            if (e.getDamager() instanceof Player)
            {
                Player damager = (Player) e.getDamager();

                if (!MyPetList.hasMyPet(damager) && !(event.getEntity() instanceof CraftMyWolf))
                {
                    if (!MyPetPermissions.has(damager, "MyPet.user.leash") || damager.getItemInHand().getType() != MyPetConfig.LeashItem)
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
                        MyWolf MPet = new MyWolf(damager);
                        MyPetUtil.getServer().getPluginManager().callEvent(new MyPetLeashEvent(MPet));
                        MyPetList.addMyPet(MPet);
                        MPet.createWolf((Wolf) event.getEntity());
                        MyPetUtil.getDebugLogger().info("New Wolf leashed:");
                        MyPetUtil.getDebugLogger().info("   " + MPet.toString());
                        MyPetPlugin.getPlugin().saveWolves(MyPetPlugin.NBTWolvesFile);
                        damager.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_AddLeash")));
                    }
                }
                if (MyPetList.isMyPet(event.getEntity().getEntityId()))
                {
                    MyWolf MPet = MyPetList.getMyPet(event.getEntity().getEntityId());
                    MPet.ResetSitTimer();
                    if (damager.getItemInHand().getType() == MyPetConfig.LeashItem)
                    {
                        String msg;
                        if (MPet.getHealth() > MPet.getMaxHealth() / 3 * 2)
                        {
                            msg = "" + ChatColor.GREEN + MPet.getHealth() + ChatColor.WHITE + "/" + ChatColor.YELLOW + MPet.getMaxHealth() + ChatColor.WHITE;
                        }
                        else if (MPet.getHealth() > MPet.getMaxHealth() / 3)
                        {
                            msg = "" + ChatColor.YELLOW + MPet.getHealth() + ChatColor.WHITE + "/" + ChatColor.YELLOW + MPet.getMaxHealth() + ChatColor.WHITE;
                        }
                        else
                        {
                            msg = "" + ChatColor.RED + MPet.getHealth() + ChatColor.WHITE + "/" + ChatColor.YELLOW + MPet.getMaxHealth() + ChatColor.WHITE;
                        }
                        damager.sendMessage(MyPetUtil.setColors("%aqua%%wolfname%%white% HP: %hp%").replace("%wolfname%", MPet.Name).replace("%hp%", msg));
                        if (MyPetConfig.LevelSystem)
                        {
                            int lvl = MPet.getExperience().getLevel();
                            double EXP = MPet.getExperience().getCurrentExp();
                            double reqEXP = MPet.getExperience().getRequiredExp();
                            damager.sendMessage(MyPetUtil.setColors("%aqua%%wolfname%%white% (Lv%lvl%) (%proz%%) EXP:%exp%/%reqexp%").replace("%wolfname%", MPet.Name).replace("%exp%", String.format("%1.2f", EXP)).replace("%lvl%", "" + lvl).replace("%reqexp%", String.format("%1.2f", reqEXP)).replace("%proz%", String.format("%1.2f", EXP * 100 / reqEXP)));
                        }

                        if (MPet.Wolf.isSitting())
                        {
                            MPet.Wolf.setSitting(true);
                        }
                        event.setCancelled(true);
                    }
                    if (!event.getEntity().getLocation().getWorld().getPVP())
                    {
                        event.setCancelled(true);
                    }
                    if (!MyPetUtil.canHurtWorldGuard(MPet.getOwner().getPlayer()))
                    {
                        event.setCancelled(true);
                    }
                    if (!MyPetUtil.canHurtFactions(damager, MPet.getOwner().getPlayer()))
                    {
                        event.setCancelled(true);
                    }
                    if (!MyPetUtil.canHurtTowny(damager, MPet.getOwner().getPlayer()))
                    {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDamageResult(EntityDamageEvent event)
    {
        if (!(event instanceof EntityDamageByEntityEvent) || event.isCancelled())
        {
            return;
        }
        EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
        if (event.getEntity() instanceof LivingEntity)
        {
            if (e.getDamager() instanceof Player)
            {
                Player damager = (Player) e.getDamager();
                if (MyPetList.hasMyPet(damager))
                {
                    MyWolf MPet = MyPetList.getMyPet(damager);
                    if (MPet.Status == PetState.Here && event.getEntity() != MPet.Wolf)
                    {
                        MyPetList.getMyPet(damager).Wolf.getHandle().Goaltarget = ((CraftLivingEntity) event.getEntity()).getHandle();
                    }

                }
                else if (e.getDamager() instanceof CraftMyWolf)
                {
                    MyWolf MPet = ((CraftMyWolf) e.getDamager()).getHandle().getMyWolf();
                    if (MPet.getSkillSystem().hasSkill("Poison"))
                    {
                        Poison poison = (Poison) MPet.getSkillSystem().getSkill("Poison");
                        if (poison.getPoison())
                        {
                            PotionEffect effect = new PotionEffect(PotionEffectType.POISON, 5, 1);
                            ((LivingEntity) event.getEntity()).addPotionEffect(effect);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityDeath(final EntityDeathEvent event)
    {
        if (event.getEntity() instanceof CraftMyWolf)
        {
            if (MyPetList.isMyPet(event.getEntity().getEntityId()))
            {
                MyWolf MPet = MyPetList.getMyPet(event.getEntity().getEntityId());
                MPet.Status = PetState.Dead;
                MPet.RespawnTime = MyPetConfig.RespawnTimeFixed + (MPet.getExperience().getLevel() * MyPetConfig.RespawnTimeFactor);
                if (event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent)
                {
                    EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event.getEntity().getLastDamageCause();
                    if (!(e.getDamager() instanceof Player && MPet.getOwner() != e.getDamager()))
                    {
                        event.setDroppedExp(0);
                    }
                }
                SendDeathMessage(event);
                MPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_RespawnIn").replace("%wolfname%", MPet.Name).replace("%time%", "" + MPet.RespawnTime)));
            }
        }
        if (MyPetConfig.LevelSystem && event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent)
        {
            if (((EntityDamageByEntityEvent) event.getEntity().getLastDamageCause()).getDamager() instanceof CraftMyWolf)
            {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event.getEntity().getLastDamageCause();
                if (MyPetList.isMyPet(e.getDamager().getEntityId()))
                {
                    MyWolf MPet = MyPetList.getMyPet(e.getDamager().getEntityId());
                    event.setDroppedExp(MPet.getExperience().addExp(e.getEntity().getType()));
                }
            }
        }
    }

    @EventHandler
    public void onEntityTarget(final EntityTargetEvent event)
    {
        if (!event.isCancelled())
        {
            if (event.getEntity() instanceof Wolf)
            {
                if (MyPetList.isMyPet(event.getEntity().getEntityId()))
                {
                    MyWolf MPet = MyPetList.getMyPet(event.getEntity().getEntityId());
                    MPet.ResetSitTimer();
                    if (MPet.getSkillSystem().hasSkill("Behavior"))
                    {
                        Behavior behavior = (Behavior) MPet.getSkillSystem().getSkill("Behavior");
                        if (behavior.getLevel() > 0)
                        {
                            if (behavior.getBehavior() == Behavior.BehaviorState.Friendly)
                            {
                                event.setCancelled(true);
                            }
                            else if (event.getTarget() instanceof Player && ((Player) event.getTarget()).getName().equals(MPet.getOwner().getName()))
                            {
                                event.setCancelled(true);
                            }
                            else if (behavior.getBehavior() == Behavior.BehaviorState.Raid)
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

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntitySpawn(CreatureSpawnEvent event)
    {
        if (event.getEntity() instanceof CraftMyWolf)
        {
            CraftMyWolf MWolf = (CraftMyWolf) event.getEntity();
            if (!MWolf.getHandle().isMyWolf())
            {
                event.setCancelled(true);
                net.minecraft.server.World mcWorld = ((CraftWorld) event.getLocation().getWorld()).getHandle();
                EntityWolf entityWolf = new EntityWolf(mcWorld);
                Location loc = event.getLocation();
                entityWolf.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
                mcWorld.addEntity(entityWolf, CreatureSpawnEvent.SpawnReason.SPAWNER_EGG);
            }
        }
    }

    private void SendDeathMessage(final EntityDeathEvent event)
    {
        MyWolf MPet = MyPetList.getMyPet(event.getEntity().getEntityId());
        String Killer = MyPetUtil.setColors(MyPetLanguage.getString("Unknow"));
        if (MPet != null)
        {
            if (event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent)
            {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event.getEntity().getLastDamageCause();

                if (e.getDamager().getType() == EntityType.PLAYER)
                {
                    if (e.getDamager() == MPet.getOwner())
                    {
                        Killer = MyPetUtil.setColors(MyPetLanguage.getString("You"));
                    }
                    else
                    {
                        Killer = MyPetUtil.setColors(MyPetLanguage.getString("Player")).replace("%player%", ((Player) e.getDamager()).getName());
                    }
                }
                else if (e.getDamager().getType() == EntityType.ZOMBIE)
                {
                    Killer = MyPetUtil.setColors(MyPetLanguage.getString("Zombie"));
                }
                else if (e.getDamager().getType() == EntityType.CREEPER)
                {
                    Killer = MyPetUtil.setColors(MyPetLanguage.getString("Creeper"));
                }
                else if (e.getDamager().getType() == EntityType.SPIDER)
                {
                    Killer = MyPetUtil.setColors(MyPetLanguage.getString("Spider"));
                }
                else if (e.getDamager().getType() == EntityType.SLIME)
                {
                    Killer = MyPetUtil.setColors(MyPetLanguage.getString("Slime"));
                }
                else if (e.getDamager().getType() == EntityType.GIANT)
                {
                    Killer = MyPetUtil.setColors(MyPetLanguage.getString("Giant"));
                }
                else if (e.getDamager().getType() == EntityType.SKELETON)
                {
                    Killer = MyPetUtil.setColors(MyPetLanguage.getString("Skeleton"));
                }
                else if (e.getDamager().getType() == EntityType.CAVE_SPIDER)
                {
                    Killer = MyPetUtil.setColors(MyPetLanguage.getString("CaveSpider"));
                }
                else if (e.getDamager().getType() == EntityType.ENDERMAN)
                {
                    Killer = MyPetUtil.setColors(MyPetLanguage.getString("Enderman"));
                }
                else if (e.getDamager().getType() == EntityType.PIG_ZOMBIE)
                {
                    Killer = MyPetUtil.setColors(MyPetLanguage.getString("PigZombie"));
                }
                else if (e.getDamager().getType() == EntityType.SILVERFISH)
                {
                    Killer = MyPetUtil.setColors(MyPetLanguage.getString("Silverfish"));
                }
                else if (e.getDamager().getType() == EntityType.SNOWMAN)
                {
                    Killer = MyPetUtil.setColors(MyPetLanguage.getString("Snowman"));
                }
                else if (e.getDamager().getType() == EntityType.ENDER_DRAGON)
                {
                    Killer = MyPetUtil.setColors(MyPetLanguage.getString("EnderDragon"));
                }
                else if (e.getDamager().getType() == EntityType.BLAZE)
                {
                    Killer = MyPetUtil.setColors(MyPetLanguage.getString("Blaze"));
                }
                else if (e.getDamager().getType() == EntityType.MAGMA_CUBE)
                {
                    Killer = MyPetUtil.setColors(MyPetLanguage.getString("MagmaCube"));
                }
                else if (e.getDamager().getType() == EntityType.WOLF)
                {
                    Wolf w = (Wolf) e.getDamager();
                    if (w.isTamed())
                    {
                        if (MyPetList.isMyPet(w.getEntityId()))
                        {
                            Killer = MyPetUtil.setColors(MyPetLanguage.getString("MyPet")).replace("%player%", MyPetList.getMyPet(w.getEntityId()).getOwner().getName()).replace("%wolfname%", MyPetList.getMyPet(w.getEntityId()).Name);
                        }
                        else
                        {
                            Killer = MyPetUtil.setColors(MyPetLanguage.getString("OwnedWolf")).replace("%player%", ((CraftWolf) w).getHandle().getOwnerName());
                        }
                    }
                    else
                    {
                        Killer = MyPetUtil.setColors(MyPetLanguage.getString("Wolf"));
                    }
                }
            }
            else if (event.getEntity().getLastDamageCause().getCause().equals(DamageCause.BLOCK_EXPLOSION))
            {
                Killer = MyPetUtil.setColors(MyPetLanguage.getString("Explosion"));
            }
            else if (event.getEntity().getLastDamageCause().getCause().equals(DamageCause.DROWNING))
            {
                Killer = MyPetUtil.setColors(MyPetLanguage.getString("Drowning"));
            }
            else if (event.getEntity().getLastDamageCause().getCause().equals(DamageCause.FALL))
            {
                Killer = MyPetUtil.setColors(MyPetLanguage.getString("Fall"));
            }
            else if (event.getEntity().getLastDamageCause().getCause().equals(DamageCause.FIRE))
            {
                Killer = MyPetUtil.setColors(MyPetLanguage.getString("Fire"));
            }
            else if (event.getEntity().getLastDamageCause().getCause().equals(DamageCause.LAVA))
            {
                Killer = MyPetUtil.setColors(MyPetLanguage.getString("Lava"));
            }
            else if (event.getEntity().getLastDamageCause().getCause().equals(DamageCause.LIGHTNING))
            {
                Killer = MyPetUtil.setColors(MyPetLanguage.getString("Lightning"));
            }
            else if (event.getEntity().getLastDamageCause().getCause().equals(DamageCause.VOID))
            {
                Killer = MyPetUtil.setColors(MyPetLanguage.getString("kvoid"));
            }

            MPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_DeathMessage")).replace("%wolfname%", MPet.Name) + Killer);
        }
    }
}