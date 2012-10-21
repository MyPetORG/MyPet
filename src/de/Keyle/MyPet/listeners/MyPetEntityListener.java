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
import de.Keyle.MyPet.entity.types.CraftMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.entity.types.chicken.CraftMyChicken;
import de.Keyle.MyPet.entity.types.ocelot.MyOcelot;
import de.Keyle.MyPet.entity.types.pig.MyPig;
import de.Keyle.MyPet.entity.types.sheep.MySheep;
import de.Keyle.MyPet.entity.types.villager.MyVillager;
import de.Keyle.MyPet.event.MyPetLeashEvent;
import de.Keyle.MyPet.skill.skills.Behavior;
import de.Keyle.MyPet.skill.skills.Poison;
import de.Keyle.MyPet.util.*;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.entity.CraftOcelot;
import org.bukkit.craftbukkit.entity.CraftWolf;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class MyPetEntityListener implements Listener
{
    @EventHandler
    public void onEntityDamage(final EntityDamageEvent event)
    {
        if (event.isCancelled())
        {
            return;
        }
        if (event instanceof EntityDamageByEntityEvent)
        {
            EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;

            if (event.getEntity() instanceof CraftMyPet)
            {
                if (e.getDamager() instanceof Player)
                {
                    Player damager = (Player) e.getDamager();
                    MyPet myPet = MyPetList.getMyPet(event.getEntity().getEntityId());
                    if (damager.getItemInHand().getType() == MyPetConfig.leashItem)
                    {
                        String msg;
                        if (myPet.getHealth() > myPet.getMaxHealth() / 3 * 2)
                        {
                            msg = "" + ChatColor.GREEN + myPet.getHealth() + ChatColor.WHITE + "/" + ChatColor.YELLOW + myPet.getMaxHealth() + ChatColor.WHITE;
                        }
                        else if (myPet.getHealth() > myPet.getMaxHealth() / 3)
                        {
                            msg = "" + ChatColor.YELLOW + myPet.getHealth() + ChatColor.WHITE + "/" + ChatColor.YELLOW + myPet.getMaxHealth() + ChatColor.WHITE;
                        }
                        else
                        {
                            msg = "" + ChatColor.RED + myPet.getHealth() + ChatColor.WHITE + "/" + ChatColor.YELLOW + myPet.getMaxHealth() + ChatColor.WHITE;
                        }
                        damager.sendMessage(MyPetUtil.setColors("%aqua%%petname%%white% HP: %hp%").replace("%petname%", myPet.petName).replace("%hp%", msg));
                        if (MyPetConfig.levelSystem)
                        {
                            int lvl = myPet.getExperience().getLevel();
                            double exp = myPet.getExperience().getCurrentExp();
                            double reqEXP = myPet.getExperience().getRequiredExp();
                            damager.sendMessage(MyPetUtil.setColors("%aqua%%petname%%white% (Lv%lvl%) (%proz%%) EXP:%exp%/%reqexp%").replace("%petname%", myPet.petName).replace("%exp%", String.format("%1.2f", exp)).replace("%lvl%", "" + lvl).replace("%reqexp%", String.format("%1.2f", reqEXP)).replace("%proz%", String.format("%1.2f", exp * 100 / reqEXP)));
                        }

                        if (myPet.getPet().isSitting())
                        {
                            myPet.getPet().setSitting(true);
                        }
                        event.setCancelled(true);
                    }
                    if (!MyPetUtil.canHurt(damager, myPet.getOwner().getPlayer()))
                    {
                        event.setCancelled(true);
                    }
                }
            }
            else if (MyPetType.isLeashableEntityType(event.getEntity().getType()))
            {
                if (e.getDamager() instanceof Player)
                {
                    Player damager = (Player) e.getDamager();

                    if (!MyPetList.hasMyPet(damager))
                    {
                        if (!MyPetPermissions.has(damager, "MyPet.user.leash") || damager.getItemInHand().getType() != MyPetConfig.leashItem)
                        {
                            return;
                        }
                        Entity leashTarget = event.getEntity();
                        boolean willBeLeashed = false;
                        boolean sitting = false;

                        if (leashTarget instanceof Wolf)
                        {
                            Wolf targetWolf = (Wolf) event.getEntity();

                            String wolfOwner = ((CraftWolf) targetWolf).getHandle().getOwnerName();
                            Player attacker = (Player) e.getDamager();

                            boolean isTarmed = targetWolf.isTamed();
                            sitting = ((Wolf) event.getEntity()).isSitting();

                            if (isTarmed && wolfOwner.equals(attacker.getName()))
                            {
                                willBeLeashed = true;
                            }
                        }
                        else if (leashTarget instanceof Ocelot)
                        {
                            Ocelot targetOcelot = (Ocelot) event.getEntity();

                            String ocelotOwner = ((CraftOcelot) targetOcelot).getHandle().getOwnerName();
                            Player attacker = (Player) e.getDamager();

                            boolean isTarmed = targetOcelot.isTamed();
                            sitting = ((Ocelot) event.getEntity()).isSitting();

                            if (isTarmed && ocelotOwner.equals(attacker.getName()))
                            {
                                willBeLeashed = true;
                            }
                        }
                        else if (leashTarget instanceof IronGolem)
                        {
                            IronGolem targetIronGolem = (IronGolem) event.getEntity();

                            willBeLeashed = targetIronGolem.isPlayerCreated();
                        }
                        else if (leashTarget instanceof Silverfish || leashTarget instanceof Zombie || leashTarget instanceof PigZombie)// || leashTarget instanceof Slime || leashTarget instanceof CaveSpider)
                        {
                            willBeLeashed = ((LivingEntity) leashTarget).getHealth() <= 2;
                        }
                        else if (leashTarget instanceof Chicken || leashTarget instanceof MushroomCow || leashTarget instanceof Cow || leashTarget instanceof Pig || leashTarget instanceof Sheep)// || leashTarget instanceof Villager)
                        {
                            willBeLeashed = ((Ageable) leashTarget).isAdult();
                        }

                        if (willBeLeashed)
                        {
                            MyPetUtil.getLogger().info("" + MyPetType.getMyPetTypeByEntityType(leashTarget.getType()));
                            event.setCancelled(true);
                            MyPet myPet = MyPetType.getMyPetTypeByEntityType(leashTarget.getType()).getNewMyPetInstance(MyPetPlayer.getMyPetPlayer(damager.getName()));
                            MyPetUtil.getServer().getPluginManager().callEvent(new MyPetLeashEvent(myPet));
                            MyPetList.addMyPet(myPet);
                            myPet.createPet(leashTarget.getLocation());
                            myPet.setSitting(sitting);
                            if (leashTarget instanceof Ocelot)
                            {
                                ((MyOcelot) myPet).setCatType(((Ocelot) leashTarget).getCatType().getId());
                            }
                            else if (leashTarget instanceof Sheep)
                            {
                                ((MySheep) myPet).setColor(((Sheep) leashTarget).getColor().getData());
                                ((MySheep) myPet).setSheared(((Sheep) leashTarget).isSheared());
                            }
                            else if (leashTarget instanceof Villager)
                            {
                                ((MyVillager) myPet).setProfession(((Villager) leashTarget).getProfession().getId());
                            }
                            else if (leashTarget instanceof Pig)
                            {
                                ((MyPig) myPet).setSaddle(((Pig) leashTarget).hasSaddle());
                            }
                            event.getEntity().remove();
                            MyPetUtil.getDebugLogger().info("New Pet leashed:");
                            MyPetUtil.getDebugLogger().info("   " + myPet.toString());
                            MyPetUtil.getDebugLogger().info(MyPetPlugin.getPlugin().savePets(false) + " pet/pets saved.");
                            damager.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_AddLeash")));
                        }
                    }

                }
            }
        }
        else if (event.getCause() == DamageCause.FALL)
        {
            if (event.getEntity() instanceof CraftMyChicken)
            {
                event.setCancelled(true);
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
                    MyPet myPet = MyPetList.getMyPet(damager);
                    if (myPet.status == PetState.Here && event.getEntity() != myPet.getPet())
                    {
                        MyPetList.getMyPet(damager).getPet().getHandle().goalTarget = ((CraftLivingEntity) event.getEntity()).getHandle();
                    }

                }
                else if (e.getDamager() instanceof CraftMyPet)
                {
                    MyPet myPet = ((CraftMyPet) e.getDamager()).getHandle().getMyPet();
                    if (myPet.getSkillSystem().hasSkill("Poison"))
                    {
                        Poison poisonSkill = (Poison) myPet.getSkillSystem().getSkill("Poison");
                        if (poisonSkill.getPoison())
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
        if (event.getEntity() instanceof CraftMyPet)
        {
            if (MyPetList.isMyPet(event.getEntity().getEntityId()))
            {
                MyPet myPet = MyPetList.getMyPet(event.getEntity().getEntityId());
                myPet.status = PetState.Dead;
                myPet.respawnTime = MyPetConfig.respawnTimeFixed + (myPet.getExperience().getLevel() * MyPetConfig.respawnTimeFactor);
                if (event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent)
                {
                    EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event.getEntity().getLastDamageCause();
                    if (!(e.getDamager() instanceof Player && myPet.getOwner() != e.getDamager()))
                    {
                        event.setDroppedExp(0);
                    }
                }
                SendDeathMessage(event);
                myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_RespawnIn").replace("%petname%", myPet.petName).replace("%time%", "" + myPet.respawnTime)));
            }
        }
        if (MyPetConfig.levelSystem && event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent)
        {
            if (((EntityDamageByEntityEvent) event.getEntity().getLastDamageCause()).getDamager() instanceof CraftMyPet)
            {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event.getEntity().getLastDamageCause();
                if (MyPetList.isMyPet(e.getDamager().getEntityId()))
                {
                    MyPet myPet = MyPetList.getMyPet(e.getDamager().getEntityId());
                    event.setDroppedExp(myPet.getExperience().addExp(e.getEntity().getType()));
                }
            }
        }
    }

    @EventHandler
    public void onEntityTarget(final EntityTargetEvent event)
    {
        if (!event.isCancelled())
        {
            if (event.getEntity() instanceof CraftMyPet)
            {
                if (MyPetList.isMyPet(event.getEntity().getEntityId()))
                {
                    MyPet myPet = MyPetList.getMyPet(event.getEntity().getEntityId());
                    if (myPet.getSkillSystem().hasSkill("Behavior"))
                    {
                        Behavior behaviorSkill = (Behavior) myPet.getSkillSystem().getSkill("Behavior");
                        if (behaviorSkill.getLevel() > 0)
                        {
                            if (behaviorSkill.getBehavior() == Behavior.BehaviorState.Friendly)
                            {
                                event.setCancelled(true);
                            }
                            else if (event.getTarget() instanceof Player && ((Player) event.getTarget()).getName().equals(myPet.getOwner().getName()))
                            {
                                event.setCancelled(true);
                            }
                            else if (behaviorSkill.getBehavior() == Behavior.BehaviorState.Raid)
                            {
                                if (event.getTarget() instanceof Player || (event.getTarget() instanceof Tameable && ((Wolf) event.getTarget()).isTamed()))
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

    private void SendDeathMessage(final EntityDeathEvent event)
    {
        MyPet myPet = MyPetList.getMyPet(event.getEntity().getEntityId());
        String killer = MyPetUtil.setColors(MyPetLanguage.getString("Unknow"));
        if (myPet != null)
        {
            if (event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent)
            {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event.getEntity().getLastDamageCause();

                if (e.getDamager().getType() == EntityType.PLAYER)
                {
                    if (e.getDamager() == myPet.getOwner())
                    {
                        killer = MyPetUtil.setColors(MyPetLanguage.getString("You"));
                    }
                    else
                    {
                        killer = MyPetUtil.setColors(MyPetLanguage.getString("Player")).replace("%player%", ((Player) e.getDamager()).getName());
                    }
                }
                else if (e.getDamager().getType() == EntityType.ZOMBIE)
                {
                    killer = MyPetUtil.setColors(MyPetLanguage.getString("Zombie"));
                }
                else if (e.getDamager().getType() == EntityType.CREEPER)
                {
                    killer = MyPetUtil.setColors(MyPetLanguage.getString("Creeper"));
                }
                else if (e.getDamager().getType() == EntityType.SPIDER)
                {
                    killer = MyPetUtil.setColors(MyPetLanguage.getString("Spider"));
                }
                else if (e.getDamager().getType() == EntityType.SLIME)
                {
                    killer = MyPetUtil.setColors(MyPetLanguage.getString("Slime"));
                }
                else if (e.getDamager().getType() == EntityType.GIANT)
                {
                    killer = MyPetUtil.setColors(MyPetLanguage.getString("Giant"));
                }
                else if (e.getDamager().getType() == EntityType.SKELETON)
                {
                    killer = MyPetUtil.setColors(MyPetLanguage.getString("Skeleton"));
                }
                else if (e.getDamager().getType() == EntityType.CAVE_SPIDER)
                {
                    killer = MyPetUtil.setColors(MyPetLanguage.getString("CaveSpider"));
                }
                else if (e.getDamager().getType() == EntityType.ENDERMAN)
                {
                    killer = MyPetUtil.setColors(MyPetLanguage.getString("Enderman"));
                }
                else if (e.getDamager().getType() == EntityType.PIG_ZOMBIE)
                {
                    killer = MyPetUtil.setColors(MyPetLanguage.getString("PigZombie"));
                }
                else if (e.getDamager().getType() == EntityType.SILVERFISH)
                {
                    killer = MyPetUtil.setColors(MyPetLanguage.getString("Silverfish"));
                }
                else if (e.getDamager().getType() == EntityType.SNOWMAN)
                {
                    killer = MyPetUtil.setColors(MyPetLanguage.getString("Snowman"));
                }
                else if (e.getDamager().getType() == EntityType.ENDER_DRAGON)
                {
                    killer = MyPetUtil.setColors(MyPetLanguage.getString("EnderDragon"));
                }
                else if (e.getDamager().getType() == EntityType.BLAZE)
                {
                    killer = MyPetUtil.setColors(MyPetLanguage.getString("Blaze"));
                }
                else if (e.getDamager().getType() == EntityType.MAGMA_CUBE)
                {
                    killer = MyPetUtil.setColors(MyPetLanguage.getString("MagmaCube"));
                }
                else if (e.getDamager().getType() == EntityType.WOLF)
                {
                    Wolf w = (Wolf) e.getDamager();
                    if (w.isTamed())
                    {
                        if (MyPetList.isMyPet(w.getEntityId()))
                        {
                            killer = MyPetUtil.setColors(MyPetLanguage.getString("MyPet")).replace("%player%", MyPetList.getMyPet(w.getEntityId()).getOwner().getName()).replace("%petname%", MyPetList.getMyPet(w.getEntityId()).petName);
                        }
                        else
                        {
                            killer = MyPetUtil.setColors(MyPetLanguage.getString("PlayerWolf")).replace("%player%", ((CraftWolf) w).getHandle().getOwnerName());
                        }
                    }
                    else
                    {
                        killer = MyPetUtil.setColors(MyPetLanguage.getString("Wolf"));
                    }
                }
            }
            else if (event.getEntity().getLastDamageCause().getCause().equals(DamageCause.BLOCK_EXPLOSION))
            {
                killer = MyPetUtil.setColors(MyPetLanguage.getString("Explosion"));
            }
            else if (event.getEntity().getLastDamageCause().getCause().equals(DamageCause.DROWNING))
            {
                killer = MyPetUtil.setColors(MyPetLanguage.getString("Drowning"));
            }
            else if (event.getEntity().getLastDamageCause().getCause().equals(DamageCause.FALL))
            {
                killer = MyPetUtil.setColors(MyPetLanguage.getString("Fall"));
            }
            else if (event.getEntity().getLastDamageCause().getCause().equals(DamageCause.FIRE))
            {
                killer = MyPetUtil.setColors(MyPetLanguage.getString("Fire"));
            }
            else if (event.getEntity().getLastDamageCause().getCause().equals(DamageCause.LAVA))
            {
                killer = MyPetUtil.setColors(MyPetLanguage.getString("Lava"));
            }
            else if (event.getEntity().getLastDamageCause().getCause().equals(DamageCause.LIGHTNING))
            {
                killer = MyPetUtil.setColors(MyPetLanguage.getString("Lightning"));
            }
            else if (event.getEntity().getLastDamageCause().getCause().equals(DamageCause.VOID))
            {
                killer = MyPetUtil.setColors(MyPetLanguage.getString("kvoid"));
            }

            myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_DeathMessage")).replace("%petname%", myPet.petName) + killer);
        }
    }
}