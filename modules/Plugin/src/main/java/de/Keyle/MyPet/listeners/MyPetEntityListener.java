/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
 * MyPet is licensed under the GNU Lesser General Public License.
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.listeners;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.*;
import de.Keyle.MyPet.api.entity.MyPet.PetState;
import de.Keyle.MyPet.api.entity.skill.ranged.CraftMyPetProjectile;
import de.Keyle.MyPet.api.entity.skill.ranged.EntityMyPetProjectile;
import de.Keyle.MyPet.api.event.MyPetDamageEvent;
import de.Keyle.MyPet.api.event.MyPetOnHitSkillEvent;
import de.Keyle.MyPet.api.event.MyPetRemoveEvent;
import de.Keyle.MyPet.api.player.DonateCheck;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.skill.MyPetExperience;
import de.Keyle.MyPet.api.skill.OnDamageByEntitySkill;
import de.Keyle.MyPet.api.skill.OnHitSkill;
import de.Keyle.MyPet.api.skill.skills.Backpack;
import de.Keyle.MyPet.api.skill.skills.Behavior;
import de.Keyle.MyPet.api.skill.skills.Behavior.BehaviorMode;
import de.Keyle.MyPet.api.skill.skilltree.Skill;
import de.Keyle.MyPet.api.util.Colorizer;
import de.Keyle.MyPet.api.util.ConfigItem;
import de.Keyle.MyPet.api.util.EnumSelector;
import de.Keyle.MyPet.api.util.chat.FancyMessage;
import de.Keyle.MyPet.api.util.chat.parts.ItemTooltip;
import de.Keyle.MyPet.api.util.inventory.CustomInventory;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.commands.CommandInfo;
import de.Keyle.MyPet.commands.CommandInfo.PetInfoDisplay;
import de.Keyle.MyPet.skill.skills.BackpackImpl;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;

public class MyPetEntityListener implements Listener {

    boolean isSkillActive = false;

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMyPet(CreatureSpawnEvent event) {
        //noinspection ConstantConditions
        if (event.getEntity() == null) {
            // catch invalid events (i.e. EnchantmentAPI)
            return;
        }
        if (event.getEntity() instanceof MyPetBukkitEntity) {
            event.setCancelled(false);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMyPet(EntityPortalEvent event) {
        //noinspection ConstantConditions
        if (event.getEntity() == null) {
            // catch invalid events (i.e. EnchantmentAPI)
            return;
        }
        if (event.getEntity() instanceof MyPetBukkitEntity) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMyPet(EntityInteractEvent event) {
        //noinspection ConstantConditions
        if (event.getEntity() == null) {
            // catch invalid events (i.e. EnchantmentAPI)
            return;
        }
        if (event.getEntity() instanceof MyPetBukkitEntity) {
            if (event.getBlock().getType() == EnumSelector.find(Material.class, "SOIL", "FARMLAND")) {
                event.setCancelled(true);
            } else if ("TURTLE_EGG".equals(event.getBlock().getType().name())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onMyPet(EntityCombustByEntityEvent event) {
        //noinspection ConstantConditions
        if (event.getEntity() == null) {
            // catch invalid events (i.e. EnchantmentAPI)
            return;
        }
        if (WorldGroup.getGroupByWorld(event.getEntity().getWorld()).isDisabled()) {
            return;
        }
        if (event.getEntity() instanceof MyPetBukkitEntity) {
            if (event.getCombuster() instanceof Player || (event.getCombuster() instanceof Projectile && ((Projectile) event.getCombuster()).getShooter() instanceof Player)) {
                Player damager;
                if (event.getCombuster() instanceof Projectile) {
                    damager = (Player) ((Projectile) event.getCombuster()).getShooter();
                } else {
                    damager = (Player) event.getCombuster();
                }

                MyPet myPet = ((MyPetBukkitEntity) event.getEntity()).getMyPet();

                if (myPet.getOwner().equals(damager) && !Configuration.Misc.OWNER_CAN_ATTACK_PET) {
                    event.setCancelled(true);
                } else if (!myPet.getOwner().equals(damager) && !MyPetApi.getHookHelper().canHurt(damager, myPet.getOwner().getPlayer(), true)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onMyPet(final EntityDamageByEntityEvent event) {
        //noinspection ConstantConditions
        if (event.getEntity() == null) {
            // catch invalid events (i.e. EnchantmentAPI)
            return;
        }
        if (WorldGroup.getGroupByWorld(event.getEntity().getWorld()).isDisabled()) {
            return;
        }
        if (event.getEntity() instanceof MyPetBukkitEntity) {
            MyPetBukkitEntity craftMyPet = (MyPetBukkitEntity) event.getEntity();
            MyPet myPet = craftMyPet.getMyPet();
            if (event.getDamager() instanceof Player || (event.getDamager() instanceof Projectile && ((Projectile) event.getDamager()).getShooter() instanceof Player)) {
                Player damager;
                if (event.getDamager() instanceof Projectile) {
                    damager = (Player) ((Projectile) event.getDamager()).getShooter();
                } else {
                    damager = (Player) event.getDamager();
                }
                ItemStack leashItem;
                if (MyPetApi.getCompatUtil().compareWithMinecraftVersion("1.9") >= 0) {
                    leashItem = damager.getEquipment().getItemInMainHand();
                } else {
                    leashItem = damager.getItemInHand();
                }
                if (MyPetApi.getMyPetInfo().getLeashItem(myPet.getPetType()).compare(leashItem)) {
                    boolean infoShown = false;
                    if (CommandInfo.canSee(PetInfoDisplay.Name.adminOnly, damager, myPet)) {
                        damager.sendMessage(ChatColor.AQUA + myPet.getPetName() + ChatColor.RESET + ":");
                        infoShown = true;
                    }
                    if (CommandInfo.canSee(PetInfoDisplay.Owner.adminOnly, damager, myPet) && myPet.getOwner().getPlayer() != damager) {
                        damager.sendMessage("   " + Translation.getString("Name.Owner", damager) + ": " + myPet.getOwner().getName());
                        infoShown = true;
                    }
                    if (CommandInfo.canSee(PetInfoDisplay.HP.adminOnly, damager, myPet)) {
                        String msg;
                        double health = myPet.getHealth();
                        double maxHealth = myPet.getMaxHealth();
                        if (health > maxHealth / 3 * 2) {
                            msg = "" + ChatColor.GREEN;
                        } else if (health > maxHealth / 3) {
                            msg = "" + ChatColor.YELLOW;
                        } else {
                            msg = "" + ChatColor.RED;
                        }
                        msg += String.format("%1.2f", health) + ChatColor.WHITE + "/" + String.format("%1.2f", maxHealth);
                        damager.sendMessage("   " + Translation.getString("Name.HP", damager) + ": " + msg);
                        infoShown = true;
                    }
                    if (myPet.getStatus() == PetState.Dead && CommandInfo.canSee(PetInfoDisplay.RespawnTime.adminOnly, damager, myPet)) {
                        damager.sendMessage("   " + Translation.getString("Name.Respawntime", damager) + ": " + myPet.getRespawnTime());
                        infoShown = true;
                    }
                    if (myPet.getDamage() > 0 && CommandInfo.canSee(PetInfoDisplay.Damage.adminOnly, damager, myPet)) {
                        damager.sendMessage("   " + Translation.getString("Name.Damage", damager) + ": " + String.format("%1.2f", myPet.getDamage()));
                        infoShown = true;
                    }
                    if (myPet.getRangedDamage() > 0 && CommandInfo.canSee(PetInfoDisplay.RangedDamage.adminOnly, damager, myPet)) {
                        damager.sendMessage("   " + Translation.getString("Name.RangedDamage", damager) + ": " + String.format("%1.2f", myPet.getRangedDamage()));
                        infoShown = true;
                    }
                    if (myPet.getSkills().has(Behavior.class) && CommandInfo.canSee(PetInfoDisplay.Behavior.adminOnly, damager, myPet)) {
                        Behavior behavior = myPet.getSkills().get(Behavior.class);
                        damager.sendMessage("   " + Translation.getString("Name.Skill.Behavior", damager) + ": " + Translation.getString("Name." + behavior.getBehavior().name(), damager));
                        infoShown = true;
                    }
                    if (Configuration.HungerSystem.USE_HUNGER_SYSTEM && CommandInfo.canSee(PetInfoDisplay.Hunger.adminOnly, damager, myPet)) {
                        damager.sendMessage("   " + Translation.getString("Name.Hunger", damager) + ": " + Math.round(myPet.getSaturation()));

                        FancyMessage m = new FancyMessage("   " + Translation.getString("Name.Food", damager) + ": ");
                        boolean comma = false;
                        for (ConfigItem material : MyPetApi.getMyPetInfo().getFood(myPet.getPetType())) {
                            ItemStack is = material.getItem();
                            if (is == null) {
                                continue;
                            }
                            if (comma) {
                                m.then(", ");
                            }
                            if (is.hasItemMeta() && is.getItemMeta().hasDisplayName()) {
                                m.then(is.getItemMeta().getDisplayName());
                            } else {
                                try {
                                    m.thenTranslate(MyPetApi.getPlatformHelper().getVanillaName(is));
                                } catch (Exception e) {
                                    MyPetApi.getLogger().warning("A food item for \"" + myPet.getPetType().name() + "\" caused an error. If you think this is a bug please report it to the MyPet developer.");
                                    MyPetApi.getLogger().warning(is.toString());
                                    e.printStackTrace();
                                    continue;
                                }
                            }
                            m.color(ChatColor.GOLD);
                            ItemTooltip it = new ItemTooltip();
                            it.setMaterial(is.getType());
                            if (is.hasItemMeta()) {
                                if (is.getItemMeta().hasDisplayName()) {
                                    it.setTitle(is.getItemMeta().getDisplayName());
                                }
                                if (is.getItemMeta().hasLore()) {
                                    it.setLore(is.getItemMeta().getLore().toArray(new String[0]));
                                }
                            }
                            m.itemTooltip(it);
                            comma = true;
                        }
                        MyPetApi.getPlatformHelper().sendMessageRaw(damager, m.toJSONString());

                        infoShown = true;
                    }
                    if (CommandInfo.canSee(PetInfoDisplay.Skilltree.adminOnly, damager, myPet) && myPet.getSkilltree() != null) {
                        damager.sendMessage("   " + Translation.getString("Name.Skilltree", damager) + ": " + Colorizer.setColors(myPet.getSkilltree().getDisplayName()));
                        infoShown = true;
                    }
                    if (CommandInfo.canSee(PetInfoDisplay.Level.adminOnly, damager, myPet)) {
                        int lvl = myPet.getExperience().getLevel();
                        damager.sendMessage("   " + Translation.getString("Name.Level", damager) + ": " + lvl);
                        infoShown = true;
                    }
                    int maxLevel = myPet.getSkilltree() != null ? myPet.getSkilltree().getMaxLevel() : Configuration.LevelSystem.Experience.LEVEL_CAP;
                    if (CommandInfo.canSee(PetInfoDisplay.Exp.adminOnly, damager, myPet) && myPet.getExperience().getLevel() < maxLevel) {
                        double exp = myPet.getExperience().getCurrentExp();
                        double reqEXP = myPet.getExperience().getRequiredExp();
                        damager.sendMessage("   " + Translation.getString("Name.Exp", damager) + ": " + String.format("%1.2f", exp) + "/" + String.format("%1.2f", reqEXP));
                        infoShown = true;
                    }
                    if (myPet.getOwner().getDonationRank() != DonateCheck.DonationRank.None) {
                        infoShown = true;
                        String donationMessage = "" + ChatColor.GOLD;
                        donationMessage += myPet.getOwner().getDonationRank().getDefaultIcon();
                        donationMessage += " " + Translation.getString("Name.Title." + myPet.getOwner().getDonationRank().name(), damager) + " ";
                        donationMessage += myPet.getOwner().getDonationRank().getDefaultIcon();
                        damager.sendMessage("   " + donationMessage);
                    }

                    if (!infoShown) {
                        damager.sendMessage(Translation.getString("Message.No.NothingToSeeHere", myPet.getOwner()));
                    }

                    event.setCancelled(true);
                } else if (myPet.getOwner().equals(damager) && (!Configuration.Misc.OWNER_CAN_ATTACK_PET)) {
                    event.setCancelled(true);
                } else if (!myPet.getOwner().equals(damager) && !MyPetApi.getHookHelper().canHurt(damager, myPet.getOwner().getPlayer(), true)) {
                    event.setCancelled(true);
                }
            }
            if (event.getDamager() instanceof CraftMyPetProjectile) {
                EntityMyPetProjectile projectile = ((CraftMyPetProjectile) event.getDamager()).getMyPetProjectile();

                if (projectile != null && projectile.getShooter() != null) {
                    if (myPet == projectile.getShooter().getMyPet()) {
                        event.setCancelled(true);
                    }
                    if (!MyPetApi.getHookHelper().canHurt(projectile.getShooter().getOwner().getPlayer(), myPet.getOwner().getPlayer(), true)) {
                        event.setCancelled(true);
                    }
                }
            }
            if (!event.isCancelled() && event.getDamager() instanceof LivingEntity) {
                LivingEntity damager = (LivingEntity) event.getDamager();
                if (damager instanceof Player) {
                    if (!MyPetApi.getHookHelper().canHurt(myPet.getOwner().getPlayer(), (Player) damager, true)) {
                        return;
                    }
                }

                if (!isSkillActive) {
                    for (Skill skill : myPet.getSkills().all()) {
                        if (skill instanceof OnDamageByEntitySkill) {
                            OnDamageByEntitySkill damageByEntitySkill = (OnDamageByEntitySkill) skill;
                            if (damageByEntitySkill.trigger()) {
                                isSkillActive = true;
                                damageByEntitySkill.apply(damager, event);
                                isSkillActive = false;
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)

    public void onMyPetDamageMonitor(final EntityDamageByEntityEvent event) {
        if (event.getEntity() == null) {
            // catch invalid events (i.e. EnchantmentAPI)
            return;
        }
        Entity target = event.getEntity();
        if (WorldGroup.getGroupByWorld(target.getWorld()).isDisabled()) {
            return;
        }

        if (target instanceof LivingEntity) {
            Entity source = event.getDamager();

            if (Configuration.LevelSystem.Experience.DAMAGE_WEIGHTED_EXPERIENCE_DISTRIBUTION && !(target instanceof Player) && !(target instanceof MyPetBukkitEntity)) {
                LivingEntity livingSource = null;
                if (source instanceof Projectile) {
                    Projectile projectile = (Projectile) source;
                    if (projectile.getShooter() instanceof LivingEntity) {
                        livingSource = (LivingEntity) projectile.getShooter();
                    }
                } else if (source instanceof LivingEntity) {
                    livingSource = (LivingEntity) source;
                }
                if (livingSource != null) {
                    MyPetExperience.addDamageToEntity(livingSource, (LivingEntity) target, event.getDamage());
                }
            }

            if (source instanceof Projectile) {
                ProjectileSource projectileSource = ((Projectile) source).getShooter();
                if (projectileSource instanceof Entity) {
                    source = (Entity) projectileSource;
                }
            }

            if (source instanceof MyPetBukkitEntity) {
                MyPet myPet = ((MyPetBukkitEntity) source).getMyPet();

                if (myPet.getStatus() != PetState.Here) {
                    return;
                }

                // fix influence of other plugins for this event and throw damage event
                MyPetDamageEvent petDamageEvent = new MyPetDamageEvent(myPet, target, event.getOriginalDamage(EntityDamageEvent.DamageModifier.BASE));
                Bukkit.getPluginManager().callEvent(petDamageEvent);
                if (petDamageEvent.isCancelled()) {
                    event.setCancelled(true);
                    return;
                } else {
                    event.setDamage(petDamageEvent.getDamage());
                }

                if (!isSkillActive) {
                    for (Skill skill : myPet.getSkills().all()) {
                        if (skill instanceof OnHitSkill) {
                            OnHitSkill onHitSkill = (OnHitSkill) skill;
                            if (onHitSkill.trigger()) {
                                MyPetOnHitSkillEvent skillEvent = new MyPetOnHitSkillEvent(myPet, onHitSkill, (LivingEntity) target);
                                Bukkit.getPluginManager().callEvent(skillEvent);
                                if (!skillEvent.isCancelled()) {
                                    isSkillActive = true;
                                    onHitSkill.apply((LivingEntity) target);
                                    isSkillActive = false;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onMyPet(final EntityDamageEvent event) {
        //noinspection ConstantConditions
        if (event.getEntity() == null) {
            // catch invalid events (i.e. EnchantmentAPI)
            return;
        }
        if (event.getEntity() instanceof MyPetBukkitEntity) {
            if (WorldGroup.getGroupByWorld(event.getEntity().getWorld()).isDisabled()) {
                return;
            }

            MyPetBukkitEntity bukkitEntity = (MyPetBukkitEntity) event.getEntity();

            if (event.getCause() == DamageCause.SUFFOCATION) {
                if (bukkitEntity.getHandle().hasRider()) {
                    event.setCancelled(true);
                    return;
                }
                final MyPet myPet = bukkitEntity.getMyPet();
                final MyPetPlayer myPetPlayer = myPet.getOwner();

                myPet.removePet();
                myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Spawn.Despawn", myPetPlayer.getLanguage()), myPet.getPetName()));

                MyPetApi.getPlugin().getServer().getScheduler().runTaskLater(MyPetApi.getPlugin(), () -> {
                    if (myPetPlayer.hasMyPet()) {
                        MyPet runMyPet = myPetPlayer.getMyPet();
                        switch (runMyPet.createEntity()) {
                            case Canceled:
                                runMyPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Spawn.Prevent", myPet.getOwner()), runMyPet.getPetName()));
                                break;
                            case NoSpace:
                                runMyPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Spawn.NoSpace", myPet.getOwner()), runMyPet.getPetName()));
                                break;
                            case NotAllowed:
                                runMyPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.No.AllowedHere", myPet.getOwner()), myPet.getPetName()));
                                break;
                            case Flying:
                                runMyPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Spawn.Flying", myPet.getOwner()), myPet.getPetName()));
                                break;
                            case Success:
                                if (runMyPet != myPet) {
                                    runMyPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Command.Call.Success", myPet.getOwner()), runMyPet.getPetName()));
                                }
                                break;
                        }
                    }
                }, 10L);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMyPet(final EntityDeathEvent event) {
        //noinspection ConstantConditions
        if (event.getEntity() == null) {
            // catch invalid events (i.e. EnchantmentAPI)
            return;
        }
        LivingEntity deadEntity = event.getEntity();
        if (WorldGroup.getGroupByWorld(deadEntity.getWorld()).isDisabled()) {
            return;
        }
        if (deadEntity instanceof MyPetBukkitEntity) {
            MyPet myPet = ((MyPetBukkitEntity) deadEntity).getMyPet();
            // check health for death events where the pet isn't really dead (/killall)
            if (myPet == null || myPet.getHealth() > 0) {
                return;
            }

            final MyPetPlayer owner = myPet.getOwner();

            if (MyPetApi.getMyPetInfo().getReleaseOnDeath(myPet.getPetType()) && !owner.isMyPetAdmin()) {
                MyPetRemoveEvent removeEvent = new MyPetRemoveEvent(myPet, MyPetRemoveEvent.Source.Death);
                Bukkit.getServer().getPluginManager().callEvent(removeEvent);

                if (myPet.getSkills().isActive(Backpack.class)) {
                    CustomInventory inv = myPet.getSkills().get(Backpack.class).getInventory();
                    inv.dropContentAt(myPet.getLocation().get());
                }
                if (myPet instanceof MyPetEquipment) {
                    ((MyPetEquipment) myPet).dropEquipment();
                }


                myPet.removePet();
                owner.setMyPetForWorldGroup(WorldGroup.getGroupByWorld(owner.getPlayer().getWorld().getName()), null);

                myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Command.Release.Dead", owner), myPet.getPetName()));

                MyPetApi.getMyPetManager().deactivateMyPet(owner, false);
                MyPetApi.getRepository().removeMyPet(myPet.getUUID(), null);

                return;
            }

            myPet.setRespawnTime((Configuration.Respawn.TIME_FIXED + MyPetApi.getMyPetInfo().getCustomRespawnTimeFixed(myPet.getPetType())) + (myPet.getExperience().getLevel() * (Configuration.Respawn.TIME_FACTOR + MyPetApi.getMyPetInfo().getCustomRespawnTimeFactor(myPet.getPetType()))));
            myPet.setStatus(PetState.Dead);

            if (deadEntity.getLastDamageCause() instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) deadEntity.getLastDamageCause();

                if (e.getDamager() instanceof Player) {
                    myPet.setRespawnTime((Configuration.Respawn.TIME_PLAYER_FIXED + MyPetApi.getMyPetInfo().getCustomRespawnTimeFixed(myPet.getPetType())) + (myPet.getExperience().getLevel() * (Configuration.Respawn.TIME_PLAYER_FACTOR + MyPetApi.getMyPetInfo().getCustomRespawnTimeFactor(myPet.getPetType()))));
                } else if (e.getDamager() instanceof MyPetBukkitEntity) {
                    MyPet killerMyPet = ((MyPetBukkitEntity) e.getDamager()).getMyPet();
                    if (myPet.getSkills().isActive(Behavior.class) && killerMyPet.getSkills().isActive(Behavior.class)) {
                        Behavior killerBehaviorSkill = killerMyPet.getSkills().get(Behavior.class);
                        Behavior deadBehaviorSkill = myPet.getSkills().get(Behavior.class);
                        if (deadBehaviorSkill.getBehavior() == BehaviorMode.Duel && killerBehaviorSkill.getBehavior() == BehaviorMode.Duel) {
                            MyPetMinecraftEntity myPetEntity = ((MyPetBukkitEntity) deadEntity).getHandle();

                            if (e.getDamager().equals(myPetEntity.getTarget())) {
                                myPet.setRespawnTime(10);
                                killerMyPet.setHealth(Double.MAX_VALUE);
                            }
                        }
                    }
                }
            }
            event.setDroppedExp(0);
            event.getDrops().clear();

            if (Configuration.LevelSystem.Experience.LOSS_FIXED > 0 || Configuration.LevelSystem.Experience.LOSS_PERCENT > 0) {
                double lostExpirience = Configuration.LevelSystem.Experience.LOSS_FIXED;
                lostExpirience += myPet.getExperience().getRequiredExp() * Configuration.LevelSystem.Experience.LOSS_PERCENT / 100;
                if (lostExpirience > myPet.getExp()) {
                    lostExpirience = myPet.getExp();
                }
                if (myPet.getSkilltree() != null) {
                    int requiredLevel = myPet.getSkilltree().getRequiredLevel();
                    if (requiredLevel > 1) {
                        double minExp = myPet.getExperience().getExpByLevel(requiredLevel);
                        lostExpirience = myPet.getExp() - lostExpirience < minExp ? myPet.getExp() - minExp : lostExpirience;
                    }
                }
                if (Configuration.LevelSystem.Experience.ALLOW_LEVEL_DOWNGRADE) {
                    lostExpirience = myPet.getExperience().removeExp(lostExpirience);
                } else {
                    lostExpirience = myPet.getExperience().removeCurrentExp(lostExpirience);
                }
                if (Configuration.LevelSystem.Experience.DROP_LOST_EXP && lostExpirience < 0) {
                    event.setDroppedExp((int) (Math.abs(lostExpirience)));
                }
            }
            if (myPet.getSkills().isActive(Backpack.class)) {
                BackpackImpl inventorySkill = myPet.getSkills().get(BackpackImpl.class);
                inventorySkill.closeInventory();
                if (inventorySkill.getDropOnDeath().getValue() && !owner.isMyPetAdmin()) {
                    inventorySkill.getInventory().dropContentAt(myPet.getLocation().get());
                }
            }
            sendDeathMessage(event);
            myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Spawn.Respawn.In", owner.getPlayer()), myPet.getPetName(), myPet.getRespawnTime()));

            if (MyPetApi.getHookHelper().isEconomyEnabled() && owner.hasAutoRespawnEnabled() && myPet.getRespawnTime() >= owner.getAutoRespawnMin() && Permissions.has(owner.getPlayer(), "MyPet.command.respawn")) {
                double costs = myPet.getRespawnTime() * Configuration.Respawn.COSTS_FACTOR + Configuration.Respawn.COSTS_FIXED;
                if (MyPetApi.getHookHelper().getEconomy().canPay(owner, costs)) {
                    MyPetApi.getHookHelper().getEconomy().pay(owner, costs);
                    myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Command.Respawn.Paid", owner.getPlayer()), myPet.getPetName(), costs + " " + MyPetApi.getHookHelper().getEconomy().currencyNameSingular()));
                    myPet.setRespawnTime(1);
                } else {
                    myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Command.Respawn.NoMoney", owner.getPlayer()), myPet.getPetName(), costs + " " + MyPetApi.getHookHelper().getEconomy().currencyNameSingular()));
                }
            }
        }
    }

    @SuppressWarnings("RedundantCast")
    private void sendDeathMessage(final EntityDeathEvent event) {
        if (event.getEntity() instanceof MyPetBukkitEntity) {
            MyPet myPet = ((MyPetBukkitEntity) event.getEntity()).getMyPet();
            String killer;
            if (event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event.getEntity().getLastDamageCause();

                if (e.getDamager().getType() == EntityType.PLAYER) {
                    if (e.getDamager() == myPet.getOwner().getPlayer()) {
                        killer = Translation.getString("Name.You", myPet.getOwner());
                    } else {
                        killer = ((Player) e.getDamager()).getName();
                    }
                } else if (e.getDamager().getType() == EntityType.WOLF) {
                    Wolf w = (Wolf) e.getDamager();
                    killer = Translation.getString("Name.Wolf", myPet.getOwner());
                    if (w.isTamed()) {
                        killer += " (" + w.getOwner().getName() + ')';
                    }
                } else if (e.getDamager() instanceof MyPetBukkitEntity) {
                    MyPetBukkitEntity craftMyPet = (MyPetBukkitEntity) e.getDamager();
                    killer = ChatColor.AQUA + craftMyPet.getMyPet().getPetName() + ChatColor.RESET + " (" + craftMyPet.getOwner().getName() + ')';
                } else if (e.getDamager() instanceof Projectile) {
                    Projectile projectile = (Projectile) e.getDamager();
                    killer = Translation.getString("Name." + Util.capitalizeName(projectile.getType().name()), myPet.getOwner()) + " (";
                    if (projectile.getShooter() instanceof Player) {
                        if (projectile.getShooter() == myPet.getOwner().getPlayer()) {
                            killer += Translation.getString("Name.You", myPet.getOwner());
                        } else {
                            killer += ((Player) projectile.getShooter()).getName();
                        }
                    } else {
                        if (MyPetApi.getMyPetInfo().isLeashableEntityType(e.getDamager().getType())) {
                            killer += Translation.getString("Name." + Util.capitalizeName(MyPetType.byEntityTypeName(e.getDamager().getType().name()).name()), myPet.getOwner());
                        } else if (e.getDamager().getType().getName() != null) {
                            killer += Translation.getString("Name." + Util.capitalizeName(e.getDamager().getType().getName()), myPet.getOwner());
                        } else {
                            killer += Translation.getString("Name.Unknow", myPet.getOwner());
                        }
                    }
                    killer += ")";
                } else {
                    if (MyPetApi.getMyPetInfo().isLeashableEntityType(e.getDamager().getType())) {
                        killer = Translation.getString("Name." + Util.capitalizeName(MyPetType.byEntityTypeName(e.getDamager().getType().name()).name()), myPet.getOwner());
                    } else {
                        if (e.getDamager().getType().getName() != null) {
                            killer = Translation.getString("Name." + Util.capitalizeName(e.getDamager().getType().getName()), myPet.getOwner());
                        } else {
                            killer = Translation.getString("Name.Unknow", myPet.getOwner());
                        }
                    }
                }
            } else {
                if (event.getEntity().getLastDamageCause() != null) {
                    killer = Translation.getString("Name." + Util.capitalizeName(event.getEntity().getLastDamageCause().getCause().name()), myPet.getOwner());
                } else {
                    killer = Translation.getString("Name.Unknow", myPet.getOwner());
                }
            }

            String deathMessageKey = MyPetApi.getPlatformHelper().getLastDamageSource(event.getEntity());
            if (deathMessageKey == null) {
                myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.DeathMessage", myPet.getOwner()), myPet.getPetName(), killer));
                return;
            }

            FancyMessage message = new FancyMessage();
            if (event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent) {
                message.translateUsing(deathMessageKey, ChatColor.AQUA + myPet.getPetName() + ChatColor.RESET, killer);
            } else {
                message.translateUsing(deathMessageKey, ChatColor.AQUA + myPet.getPetName() + ChatColor.RESET);
            }

            MyPetApi.getPlatformHelper().sendMessageRaw(myPet.getOwner().getPlayer(), message.toJSONString());
        }
    }
}