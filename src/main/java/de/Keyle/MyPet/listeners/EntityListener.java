/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2014 Keyle
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

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.api.event.MyPetLeashEvent;
import de.Keyle.MyPet.commands.CommandInfo;
import de.Keyle.MyPet.commands.CommandInfo.PetInfoDisplay;
import de.Keyle.MyPet.entity.EquipmentSlot;
import de.Keyle.MyPet.entity.ai.target.BehaviorDuelTarget;
import de.Keyle.MyPet.entity.types.*;
import de.Keyle.MyPet.entity.types.MyPet.LeashFlag;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.entity.types.enderman.EntityMyEnderman;
import de.Keyle.MyPet.entity.types.rabbit.MyRabbit;
import de.Keyle.MyPet.skill.Experience;
import de.Keyle.MyPet.skill.MonsterExperience;
import de.Keyle.MyPet.skill.skills.implementation.*;
import de.Keyle.MyPet.skill.skills.implementation.Wither;
import de.Keyle.MyPet.skill.skills.implementation.inventory.CustomInventory;
import de.Keyle.MyPet.skill.skills.implementation.inventory.ItemStackNBTConverter;
import de.Keyle.MyPet.skill.skills.implementation.ranged.MyPetProjectile;
import de.Keyle.MyPet.skill.skills.info.BehaviorInfo.BehaviorState;
import de.Keyle.MyPet.util.*;
import de.Keyle.MyPet.util.locale.Locales;
import de.Keyle.MyPet.util.logger.DebugLogger;
import de.Keyle.MyPet.util.player.MyPetPlayer;
import de.Keyle.MyPet.util.support.Economy;
import de.Keyle.MyPet.util.support.Permissions;
import de.Keyle.MyPet.util.support.PluginSupportManager;
import de.Keyle.MyPet.util.support.PvPChecker;
import de.keyle.knbt.TagByte;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagInt;
import de.keyle.knbt.TagList;
import net.citizensnpcs.api.CitizensAPI;
import net.minecraft.server.v1_8_R1.*;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R1.entity.*;
import org.bukkit.craftbukkit.v1_8_R1.inventory.CraftItemStack;
import org.bukkit.entity.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Skeleton.SkeletonType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityTargetEvent.TargetReason;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.projectiles.ProjectileSource;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.bukkit.Bukkit.getPluginManager;

public class EntityListener implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onMyPetEntitySpawn(final CreatureSpawnEvent event) {
        if (event.getEntity() instanceof CraftMyPet) {
            event.setCancelled(false);
        }
        if (!Experience.GAIN_EXP_FROM_MONSTER_SPAWNER_MOBS && event.getSpawnReason() == SpawnReason.SPAWNER) {
            event.getEntity().setMetadata("MonsterSpawner", new FixedMetadataValue(MyPetPlugin.getPlugin(), true));
        }
        if (Configuration.ADD_ZOMBIE_TARGET_GOAL && event.getEntity() instanceof CraftZombie && !event.isCancelled()) {
            EntityZombie ez = ((CraftZombie) event.getEntity()).getHandle();
            try {
                Field goalSelector = EntityInsentient.class.getDeclaredField("goalSelector");
                goalSelector.setAccessible(true);
                PathfinderGoalSelector pgs = (PathfinderGoalSelector) goalSelector.get(ez);
                pgs.a(3, new PathfinderGoalMeleeAttack(ez, EntityMyPet.class, 1.0D, true));
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
                DebugLogger.printThrowable(e);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                DebugLogger.printThrowable(e);
            }
        }
    }

    @EventHandler
    public void onMyPetEntityPortal(EntityPortalEvent event) {
        if (event.getEntity() instanceof CraftMyPet) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMyPetEntityInteract(EntityInteractEvent event) {
        if (event.getEntity() instanceof CraftMyPet) {
            if (event.getBlock().getType() == Material.SOIL) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onMyPetEntityDamageByEntity(final EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof CraftMyPet) {
            CraftMyPet craftMyPet = (CraftMyPet) event.getEntity();
            MyPet myPet = craftMyPet.getMyPet();
            if (event.getDamager() instanceof Player || (event.getDamager() instanceof Projectile && ((Projectile) event.getDamager()).getShooter() instanceof Player)) {
                Player damager;
                if (event.getDamager() instanceof Projectile) {
                    damager = (Player) ((Projectile) event.getDamager()).getShooter();
                } else {
                    damager = (Player) event.getDamager();
                }
                if (MyPet.getLeashItem(myPet.getPetType().getMyPetClass()).compare(damager.getItemInHand())) {
                    boolean infoShown = false;
                    if (CommandInfo.canSee(PetInfoDisplay.Name.adminOnly, damager, myPet)) {
                        damager.sendMessage(ChatColor.AQUA + myPet.getPetName() + ChatColor.RESET + ":");
                        infoShown = true;
                    }
                    if (CommandInfo.canSee(PetInfoDisplay.Owner.adminOnly, damager, myPet) && myPet.getOwner().getPlayer() != damager) {
                        damager.sendMessage("   " + Locales.getString("Name.Owner", damager) + ": " + myPet.getOwner().getName());
                        infoShown = true;
                    }
                    if (CommandInfo.canSee(PetInfoDisplay.HP.adminOnly, damager, myPet)) {
                        String msg;
                        if (myPet.getHealth() > myPet.getMaxHealth() / 3 * 2) {
                            msg = "" + ChatColor.GREEN;
                        } else if (myPet.getHealth() > myPet.getMaxHealth() / 3) {
                            msg = "" + ChatColor.YELLOW;
                        } else {
                            msg = "" + ChatColor.RED;
                        }
                        msg += String.format("%1.2f", myPet.getHealth()) + ChatColor.WHITE + "/" + String.format("%1.2f", myPet.getMaxHealth());
                        damager.sendMessage("   " + Locales.getString("Name.HP", damager) + ": " + msg);
                        infoShown = true;
                    }
                    if (myPet.getStatus() == PetState.Dead && CommandInfo.canSee(PetInfoDisplay.RespawnTime.adminOnly, damager, myPet)) {
                        damager.sendMessage("   " + Locales.getString("Name.Respawntime", damager) + ": " + myPet.getRespawnTime());
                        infoShown = true;
                    }
                    if (!myPet.isPassiv() && CommandInfo.canSee(PetInfoDisplay.Damage.adminOnly, damager, myPet)) {
                        double damage = (myPet.getSkills().isSkillActive(Damage.class) ? myPet.getSkills().getSkill(Damage.class).getDamage() : 0);
                        damager.sendMessage("   " + Locales.getString("Name.Damage", damager) + ": " + String.format("%1.2f", damage));
                        infoShown = true;
                    }
                    if (myPet.getRangedDamage() > 0 && CommandInfo.canSee(PetInfoDisplay.RangedDamage.adminOnly, damager, myPet)) {
                        double damage = myPet.getRangedDamage();
                        damager.sendMessage("   " + Locales.getString("Name.RangedDamage", damager) + ": " + String.format("%1.2f", damage));
                        infoShown = true;
                    }
                    if (myPet.getSkills().hasSkill(Behavior.class) && CommandInfo.canSee(PetInfoDisplay.Behavior.adminOnly, damager, myPet)) {
                        Behavior behavior = myPet.getSkills().getSkill(Behavior.class);
                        damager.sendMessage("   Behavior: " + Locales.getString("Name." + behavior.getBehavior().name(), damager));
                        infoShown = true;
                    }
                    if (Configuration.USE_HUNGER_SYSTEM && CommandInfo.canSee(PetInfoDisplay.Hunger.adminOnly, damager, myPet)) {
                        damager.sendMessage("   " + Locales.getString("Name.Hunger", damager) + ": " + myPet.getHungerValue());
                        infoShown = true;
                    }
                    if (CommandInfo.canSee(PetInfoDisplay.Skilltree.adminOnly, damager, myPet) && myPet.getSkillTree() != null) {
                        damager.sendMessage("   " + Locales.getString("Name.Skilltree", damager) + ": " + myPet.getSkillTree().getName());
                        infoShown = true;
                    }
                    if (CommandInfo.canSee(PetInfoDisplay.Level.adminOnly, damager, myPet)) {
                        int lvl = myPet.getExperience().getLevel();
                        damager.sendMessage("   " + Locales.getString("Name.Level", damager) + ": " + lvl);
                        infoShown = true;
                    }
                    int maxLevel = myPet.getSkillTree() != null ? myPet.getSkillTree().getMaxLevel() : 0;
                    if (CommandInfo.canSee(PetInfoDisplay.Exp.adminOnly, damager, myPet) && (maxLevel == 0 || myPet.getExperience().getLevel() < maxLevel)) {
                        double exp = myPet.getExperience().getCurrentExp();
                        double reqEXP = myPet.getExperience().getRequiredExp();
                        damager.sendMessage("   " + Locales.getString("Name.Exp", damager) + ": " + String.format("%1.2f", exp) + "/" + String.format("%1.2f", reqEXP));
                        infoShown = true;
                    }

                    if (!infoShown) {
                        damager.sendMessage(Locales.getString("Message.No.NothingToSeeHere", myPet.getOwner().getLanguage()));
                    }

                    event.setCancelled(true);
                } else if (myPet.getOwner().equals(damager) && (!Configuration.OWNER_CAN_ATTACK_PET || !PvPChecker.canHurt(myPet.getOwner().getPlayer()))) {
                    event.setCancelled(true);
                } else if (!myPet.getOwner().equals(damager) && !PvPChecker.canHurt(damager, myPet.getOwner().getPlayer())) {
                    event.setCancelled(true);
                }
            }
            if (!event.isCancelled() && event.getDamager() instanceof LivingEntity) {
                LivingEntity damager = (LivingEntity) event.getDamager();
                if (damager instanceof Player) {
                    if (!PvPChecker.canHurt(myPet.getOwner().getPlayer(), (Player) damager)) {
                        return;
                    }
                }
                if (myPet.getSkills().isSkillActive(Thorns.class)) {
                    Thorns thornsSkill = myPet.getSkills().getSkill(Thorns.class);
                    if (thornsSkill.activate()) {
                        isSkillActive = true;
                        thornsSkill.reflectDamage(damager, event.getDamage());
                        isSkillActive = false;
                    }
                }
            }
            if (((CraftEntity) event.getDamager()).getHandle() instanceof MyPetProjectile) {
                MyPetProjectile projectile = (MyPetProjectile) ((CraftEntity) event.getDamager()).getHandle();

                if (myPet == projectile.getShooter().getMyPet()) {
                    event.setCancelled(true);
                }
                if (!PvPChecker.canHurt(projectile.getShooter().getOwner().getPlayer(), myPet.getOwner().getPlayer())) {
                    event.setCancelled(true);
                }
            }
        }
    }

    boolean selfThrownEventRunning = false;

    @EventHandler
    public void onEntityDamageByMyPet(final EntityDamageByEntityEvent event) {
        if (PvPChecker.USE_PlayerDamageEntityEvent) {
            Entity damager = event.getDamager();
            if (damager instanceof Projectile) {
                if (((Projectile) damager).getShooter() instanceof Entity) {
                    damager = (Entity) ((Projectile) damager).getShooter();
                } else {
                    return;
                }
            }
            if (damager instanceof CraftMyPet && event.getEntity() instanceof LivingEntity) {
                MyPet myPet = ((CraftMyPet) damager).getMyPet();

                selfThrownEventRunning = true;
                if (!PvPChecker.canHurtEvent(myPet.getOwner().getPlayer(), (LivingEntity) event.getEntity())) {
                    event.setCancelled(true);
                }
                selfThrownEventRunning = false;
            }
        }
    }

    @EventHandler
    public void onEntityDamageByPlayer(final EntityDamageByEntityEvent event) {
        if (!selfThrownEventRunning && !(event.getEntity() instanceof CraftMyPet) && event.getDamager() instanceof Player) {
            if (MyPetType.isLeashableEntityType(event.getEntity().getType())) {
                Player damager = (Player) event.getDamager();

                if (!MyPetList.hasMyPet(damager)) {
                    LivingEntity leashTarget = (LivingEntity) event.getEntity();

                    Class<? extends MyPet> myPetClass = MyPetType.getMyPetTypeByEntityType(leashTarget.getType()).getMyPetClass();
                    ConfigItem leashItem = MyPet.getLeashItem(myPetClass);

                    if (!leashItem.compare(damager.getItemInHand()) || !Permissions.has(damager, "MyPet.user.leash." + MyPetType.getMyPetTypeByEntityType(leashTarget.getType()).getTypeName())) {
                        return;
                    }
                    if (Permissions.has(damager, "MyPet.user.capturehelper") && MyPetPlayer.isMyPetPlayer(damager) && MyPetPlayer.getOrCreateMyPetPlayer(damager).isCaptureHelperActive()) {
                        CaptureHelper.checkTamable(leashTarget, event.getDamage(), damager);
                    }
                    if (PluginSupportManager.isPluginUsable("Citizens")) {
                        try {
                            if (CitizensAPI.getNPCRegistry().isNPC(leashTarget)) {
                                return;
                            }
                        } catch (Error ignored) {
                        } catch (Exception ignored) {
                        }
                    }

                    boolean willBeLeashed = true;

                    flagLoop:
                    for (LeashFlag flag : MyPet.getLeashFlags(myPetClass)) {
                        switch (flag) {
                            case Adult:
                                if (leashTarget instanceof Ageable) {
                                    willBeLeashed = ((Ageable) leashTarget).isAdult();
                                } else if (leashTarget instanceof Zombie) {
                                    willBeLeashed = !((Zombie) leashTarget).isBaby();
                                }
                                break;
                            case Baby:
                                if (leashTarget instanceof Ageable) {
                                    willBeLeashed = !((Ageable) leashTarget).isAdult();
                                } else if (leashTarget instanceof Zombie) {
                                    willBeLeashed = ((Zombie) leashTarget).isBaby();
                                }
                                break;
                            case LowHp:
                                willBeLeashed = ((leashTarget.getHealth() - event.getDamage()) * 100) / leashTarget.getMaxHealth() <= 10;
                                break;
                            case UserCreated:
                                if (leashTarget instanceof IronGolem) {
                                    willBeLeashed = ((IronGolem) leashTarget).isPlayerCreated();
                                }
                                break;
                            case Wild:
                                if (leashTarget instanceof IronGolem) {
                                    willBeLeashed = !((IronGolem) leashTarget).isPlayerCreated();
                                } else if (leashTarget instanceof Tameable) {
                                    willBeLeashed = !((Tameable) leashTarget).isTamed();
                                } else if (leashTarget instanceof Horse) {
                                    willBeLeashed = !((CraftHorse) leashTarget).getHandle().isTame();
                                }
                                break;
                            case Tamed:
                                if (leashTarget instanceof Tameable) {
                                    willBeLeashed = ((Tameable) leashTarget).isTamed();
                                }
                                if (leashTarget instanceof Horse) {
                                    willBeLeashed = ((CraftHorse) leashTarget).getHandle().isTame();
                                }
                                break;
                            case CanBreed:
                                if (leashTarget instanceof Ageable) {
                                    willBeLeashed = ((Ageable) leashTarget).canBreed();
                                }
                                break;
                            case Angry:
                                if (leashTarget instanceof Wolf) {
                                    willBeLeashed = ((Wolf) leashTarget).isAngry();
                                }
                                break;
                            case Impossible:
                                willBeLeashed = false;
                                break flagLoop;
                            case None:
                                willBeLeashed = true;
                                break flagLoop;
                        }
                        if (!willBeLeashed) {
                            break;
                        }
                    }

                    if (willBeLeashed) {
                        event.setCancelled(true);
                        InactiveMyPet inactiveMyPet = new InactiveMyPet(MyPetPlayer.getOrCreateMyPetPlayer(damager));
                        inactiveMyPet.setPetType(MyPetType.getMyPetTypeByEntityType(leashTarget.getType()));
                        inactiveMyPet.setPetName(Locales.getString("Name." + inactiveMyPet.getPetType().getTypeName(), inactiveMyPet.getOwner().getLanguage()));

                        WorldGroup worldGroup = WorldGroup.getGroupByWorld(damager.getWorld().getName());
                        inactiveMyPet.setWorldGroup(worldGroup.getName());
                        inactiveMyPet.getOwner().setMyPetForWorldGroup(worldGroup.getName(), inactiveMyPet.getUUID());

                        /*
                        if(leashTarget.getCustomName() != null)
                        {
                            inactiveMyPet.setPetName(leashTarget.getCustomName());
                        }
                        */

                        TagCompound extendedInfo = new TagCompound();
                        if (leashTarget instanceof Ocelot) {
                            extendedInfo.getCompoundData().put("CatType", new TagInt(((Ocelot) leashTarget).getCatType().getId()));
                            extendedInfo.getCompoundData().put("Sitting", new TagByte(((Ocelot) leashTarget).isSitting()));
                        } else if (leashTarget instanceof Wolf) {
                            extendedInfo.getCompoundData().put("Sitting", new TagByte(((Wolf) leashTarget).isSitting()));
                            extendedInfo.getCompoundData().put("Tamed", new TagByte(((Wolf) leashTarget).isTamed()));
                            extendedInfo.getCompoundData().put("CollarColor", new TagByte(((Wolf) leashTarget).getCollarColor().getDyeData()));
                        } else if (leashTarget instanceof Sheep) {
                            extendedInfo.getCompoundData().put("Color", new TagInt(((Sheep) leashTarget).getColor().getDyeData()));
                            extendedInfo.getCompoundData().put("Sheared", new TagByte(((Sheep) leashTarget).isSheared()));
                        } else if (leashTarget instanceof Villager) {
                            extendedInfo.getCompoundData().put("Profession", new TagInt(((Villager) leashTarget).getProfession().getId()));
                        } else if (leashTarget instanceof Pig) {
                            extendedInfo.getCompoundData().put("Saddle", new TagByte(((Pig) leashTarget).hasSaddle()));
                        } else if (leashTarget instanceof Slime) {
                            extendedInfo.getCompoundData().put("Size", new TagInt(((Slime) leashTarget).getSize()));
                        } else if (leashTarget instanceof Creeper) {
                            extendedInfo.getCompoundData().put("Powered", new TagByte(((Creeper) leashTarget).isPowered()));
                        } else if (leashTarget instanceof Horse) {
                            Horse horse = (Horse) leashTarget;

                            extendedInfo.getCompoundData().put("Type", new TagByte((byte) ((CraftHorse) leashTarget).getHandle().getType()));
                            extendedInfo.getCompoundData().put("Variant", new TagInt(((CraftHorse) leashTarget).getHandle().getVariant()));
                            extendedInfo.getCompoundData().put("Armor", new TagInt(((CraftHorse) leashTarget).getHandle().cv()));
                            extendedInfo.getCompoundData().put("Chest", new TagByte(horse.isCarryingChest()));
                            extendedInfo.getCompoundData().put("Saddle", new TagByte(((CraftHorse) leashTarget).getHandle().cE()));
                            extendedInfo.getCompoundData().put("Age", new TagInt(((CraftHorse) leashTarget).getHandle().getAge()));
                        } else if (leashTarget instanceof Zombie) {
                            extendedInfo.getCompoundData().put("Baby", new TagByte(((Zombie) leashTarget).isBaby()));
                            extendedInfo.getCompoundData().put("Villager", new TagByte(((Zombie) leashTarget).isVillager()));
                        } else if (leashTarget instanceof Enderman) {
                            CraftEnderman enderman = (CraftEnderman) leashTarget;
                            if (enderman.getHandle().getCarried() != Blocks.AIR) {
                                net.minecraft.server.v1_8_R1.ItemStack block = new net.minecraft.server.v1_8_R1.ItemStack(enderman.getHandle().getCarried().getBlock(), 1, enderman.getHandle().getCarried().getBlock().getDropData(enderman.getHandle().getCarried()));
                                extendedInfo.getCompoundData().put("Block", ItemStackNBTConverter.itemStackToCompund(block));
                            }
                        } else if (leashTarget instanceof Skeleton) {
                            extendedInfo.getCompoundData().put("Wither", new TagByte(((CraftSkeleton) leashTarget).getSkeletonType() == SkeletonType.WITHER));
                        } else if (leashTarget instanceof Guardian) {
                            extendedInfo.getCompoundData().put("Elder", new TagByte(((Guardian) leashTarget).isElder()));
                        } else if (leashTarget instanceof Rabbit) {
                            extendedInfo.getCompoundData().put("Variant", new TagByte(MyRabbit.RabbitType.getTypeByBukkitEnum(((Rabbit) leashTarget).getRabbitType()).getId()));
                        }
                        if (leashTarget instanceof Ageable) {
                            extendedInfo.getCompoundData().put("Baby", new TagByte(!((Ageable) leashTarget).isAdult()));
                        }
                        if (leashTarget.getWorld().getGameRuleValue("doMobLoot").equalsIgnoreCase("true") && Configuration.RETAIN_EQUIPMENT_ON_TAME && (leashTarget instanceof Zombie || leashTarget instanceof PigZombie || leashTarget instanceof Skeleton)) {
                            Random random = ((CraftLivingEntity) leashTarget).getHandle().bb();
                            List<TagCompound> equipmentList = new ArrayList<TagCompound>();
                            if (random.nextFloat() <= leashTarget.getEquipment().getChestplateDropChance()) {
                                ItemStack itemStack = leashTarget.getEquipment().getChestplate();
                                if (itemStack != null && itemStack.getType() != Material.AIR) {
                                    net.minecraft.server.v1_8_R1.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(itemStack);
                                    TagCompound item = ItemStackNBTConverter.itemStackToCompund(nmsItemStack);
                                    item.getCompoundData().put("Slot", new TagInt(EquipmentSlot.Chestplate.getSlotId()));
                                    equipmentList.add(item);
                                }
                            }
                            if (random.nextFloat() <= leashTarget.getEquipment().getHelmetDropChance()) {
                                ItemStack itemStack = leashTarget.getEquipment().getHelmet();
                                if (itemStack != null && itemStack.getType() != Material.AIR) {
                                    net.minecraft.server.v1_8_R1.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(itemStack);
                                    TagCompound item = ItemStackNBTConverter.itemStackToCompund(nmsItemStack);
                                    item.getCompoundData().put("Slot", new TagInt(EquipmentSlot.Helmet.getSlotId()));
                                    equipmentList.add(item);
                                }
                            }
                            if (random.nextFloat() <= leashTarget.getEquipment().getLeggingsDropChance()) {
                                ItemStack itemStack = leashTarget.getEquipment().getLeggings();
                                if (itemStack != null && itemStack.getType() != Material.AIR) {
                                    net.minecraft.server.v1_8_R1.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(itemStack);
                                    TagCompound item = ItemStackNBTConverter.itemStackToCompund(nmsItemStack);
                                    item.getCompoundData().put("Slot", new TagInt(EquipmentSlot.Leggins.getSlotId()));
                                    equipmentList.add(item);
                                }
                            }
                            if (random.nextFloat() <= leashTarget.getEquipment().getBootsDropChance()) {
                                ItemStack itemStack = leashTarget.getEquipment().getBoots();
                                if (itemStack != null && itemStack.getType() != Material.AIR) {
                                    net.minecraft.server.v1_8_R1.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(itemStack);
                                    TagCompound item = ItemStackNBTConverter.itemStackToCompund(nmsItemStack);
                                    item.getCompoundData().put("Slot", new TagInt(EquipmentSlot.Boots.getSlotId()));
                                    equipmentList.add(item);
                                }
                            }
                            extendedInfo.getCompoundData().put("Equipment", new TagList(equipmentList));
                        }
                        inactiveMyPet.setInfo(extendedInfo);

                        event.getEntity().remove();

                        if (Configuration.CONSUME_LEASH_ITEM && damager.getGameMode() != GameMode.CREATIVE && damager.getItemInHand() != null) {
                            if (damager.getItemInHand().getAmount() > 1) {
                                damager.getItemInHand().setAmount(damager.getItemInHand().getAmount() - 1);
                            } else {
                                damager.setItemInHand(null);
                            }
                        }

                        MyPet myPet = MyPetList.setMyPetActive(inactiveMyPet);
                        if (myPet != null) {
                            myPet.createPet();

                            getPluginManager().callEvent(new MyPetLeashEvent(myPet));
                            DebugLogger.info("New Pet leashed:");
                            DebugLogger.info("   " + myPet.toString());
                            if (Configuration.STORE_PETS_ON_PET_LEASH) {
                                MyPetPlugin.getPlugin().saveData(false);
                            }
                            damager.sendMessage(Locales.getString("Message.Leash.Add", myPet.getOwner().getLanguage()));

                            if (myPet.getOwner().isCaptureHelperActive()) {
                                myPet.getOwner().setCaptureHelperActive(false);
                                myPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Command.CaptureHelper.Mode", myPet.getOwner()), Locales.getString("Name.Disabled", myPet.getOwner())));
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onMyPetEntityDamage(final EntityDamageEvent event) {
        if (event.getEntity() instanceof CraftMyPet) {
            CraftMyPet craftMyPet = (CraftMyPet) event.getEntity();

            if (event.getCause() == DamageCause.SUFFOCATION) {
                final MyPet myPet = craftMyPet.getMyPet();
                final MyPetPlayer myPetPlayer = myPet.getOwner();

                myPet.removePet(true);

                MyPetPlugin.getPlugin().getServer().getScheduler().runTaskLater(MyPetPlugin.getPlugin(), new Runnable() {
                    public void run() {
                        if (myPetPlayer.hasMyPet()) {
                            MyPet runMyPet = myPetPlayer.getMyPet();
                            switch (runMyPet.createPet()) {
                                case Canceled:
                                    runMyPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Spawn.Prevent", myPet.getOwner()), runMyPet.getPetName()));
                                    break;
                                case NoSpace:
                                    runMyPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Spawn.NoSpace", myPet.getOwner()), runMyPet.getPetName()));
                                    break;
                                case NotAllowed:
                                    runMyPet.sendMessageToOwner(Locales.getString("Message.No.AllowedHere", myPet.getOwner()).replace("%petname%", myPet.getPetName()));
                                    break;
                                case Flying:
                                    runMyPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Spawn.Flying", myPet.getOwner()), myPet.getPetName()));
                                    break;
                                case Success:
                                    if (runMyPet != myPet) {
                                        runMyPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Command.Call.Success", myPet.getOwner().getLanguage()), runMyPet.getPetName()));
                                    }
                                    break;
                            }
                        }
                    }
                }, 10L);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDamageByEntityDamageMonitor(final EntityDamageByEntityEvent event) {
        if (!event.isCancelled() && Experience.DAMAGE_WEIGHTED_EXPERIENCE_DISTRIBUTION && event.getEntity() instanceof LivingEntity && !(event.getEntity() instanceof Player) && !(event.getEntity() instanceof CraftMyPet)) {
            LivingEntity damager = null;
            if (event.getDamager() instanceof Projectile) {
                Projectile projectile = (Projectile) event.getDamager();
                if (projectile.getShooter() instanceof LivingEntity) {
                    damager = (LivingEntity) projectile.getShooter();
                }
            } else if (event.getDamager() instanceof LivingEntity) {
                damager = (LivingEntity) event.getDamager();
            }
            if (damager != null) {
                Experience.addDamageToEntity(damager, (LivingEntity) event.getEntity(), event.getDamage());
            }
        }
    }

    boolean isSkillActive = false;

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDamageByEntityResult(final EntityDamageByEntityEvent event) {
        Entity damagedEntity = event.getEntity();
        // --  fix unwanted screaming of Endermen --
        if (damagedEntity instanceof CraftMyPet && ((CraftMyPet) damagedEntity).getPetType() == MyPetType.Enderman) {
            ((EntityMyEnderman) ((CraftMyPet) damagedEntity).getHandle()).setScreaming(true);
            ((EntityMyEnderman) ((CraftMyPet) damagedEntity).getHandle()).setScreaming(false);
        }

        if (damagedEntity instanceof LivingEntity) {
            Entity damager = event.getDamager();

            if (damager instanceof Projectile) {
                ProjectileSource source = ((Projectile) damager).getShooter();
                if (source instanceof Entity) {
                    damager = (Entity) source;
                }
            }

            if (damager instanceof Player) {
                Player player = (Player) damager;
                if (event.getDamage() == 0) {
                    return;
                } else if (damagedEntity instanceof CraftMyPet) {
                    if (MyPet.getLeashItem(((CraftMyPet) damagedEntity).getPetType().getMyPetClass()).compare(player.getItemInHand())) {
                        return;
                    }
                }
                if (MyPetList.hasMyPet(player)) {
                    MyPet myPet = MyPetList.getMyPet(player);
                    if (myPet.getStatus() == PetState.Here && damagedEntity != myPet.getCraftPet()) {
                        myPet.getCraftPet().getHandle().goalTarget = ((CraftLivingEntity) damagedEntity).getHandle();
                    }
                }
            } else if (damager instanceof CraftMyPet) {
                MyPet myPet = ((CraftMyPet) damager).getMyPet();

                // fix influence of other plugins
                if (event.getDamager() instanceof Projectile) {
                    event.setDamage(myPet.getRangedDamage());
                } else {
                    event.setDamage(myPet.getDamage());
                }

                if (damagedEntity instanceof Player && event.isCancelled()) {
                    return;
                }

                if (!isSkillActive) {
                    //  --  Skills  --
                    boolean skillUsed = false;
                    if (myPet.getSkills().hasSkill(Poison.class)) {
                        Poison poisonSkill = myPet.getSkills().getSkill(Poison.class);
                        if (poisonSkill.activate()) {
                            poisonSkill.poisonTarget((LivingEntity) damagedEntity);
                            skillUsed = true;
                        }
                    }
                    if (!skillUsed && myPet.getSkills().hasSkill(Wither.class)) {
                        Wither witherSkill = myPet.getSkills().getSkill(Wither.class);
                        if (witherSkill.activate()) {
                            witherSkill.witherTarget((LivingEntity) damagedEntity);
                            skillUsed = true;
                        }
                    }
                    if (!skillUsed && myPet.getSkills().hasSkill(Fire.class)) {
                        Fire fireSkill = myPet.getSkills().getSkill(Fire.class);
                        if (fireSkill.activate()) {
                            fireSkill.igniteTarget((LivingEntity) damagedEntity);
                            skillUsed = true;
                        }
                    }
                    if (!skillUsed && myPet.getSkills().hasSkill(Slow.class)) {
                        Slow slowSkill = myPet.getSkills().getSkill(Slow.class);
                        if (slowSkill.activate()) {
                            slowSkill.slowTarget((LivingEntity) damagedEntity);
                            skillUsed = true;
                        }
                    }
                    if (!skillUsed && myPet.getSkills().hasSkill(Knockback.class)) {
                        Knockback knockbackSkill = myPet.getSkills().getSkill(Knockback.class);
                        if (knockbackSkill.activate()) {
                            knockbackSkill.knockbackTarget((LivingEntity) damagedEntity);
                            skillUsed = true;
                        }
                    }
                    if (!skillUsed && myPet.getSkills().hasSkill(Lightning.class)) {
                        Lightning lightningSkill = myPet.getSkills().getSkill(Lightning.class);
                        if (lightningSkill.activate()) {
                            isSkillActive = true;
                            lightningSkill.strikeLightning(damagedEntity.getLocation());
                            isSkillActive = false;
                        }
                    }
                    if (!skillUsed && myPet.getSkills().hasSkill(Stomp.class)) {
                        Stomp stompSkill = myPet.getSkills().getSkill(Stomp.class);
                        if (stompSkill.activate()) {
                            isSkillActive = true;
                            stompSkill.stomp(myPet.getLocation());
                            isSkillActive = false;
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onMyPetEntityDeath(final EntityDeathEvent event) {
        LivingEntity deadEntity = event.getEntity();
        if (deadEntity instanceof CraftMyPet) {
            MyPet myPet = ((CraftMyPet) deadEntity).getMyPet();
            if (myPet == null || myPet.getHealth() > 0) // check health for death events where the pet isn't really dead (/killall)
            {
                return;
            }

            if (Configuration.RELEASE_PETS_ON_DEATH && !myPet.getOwner().isMyPetAdmin()) {
                if (myPet.getSkills().isSkillActive(Inventory.class)) {
                    CustomInventory inv = myPet.getSkills().getSkill(Inventory.class).inv;
                    inv.dropContentAt(myPet.getLocation());
                }
                if (myPet instanceof IMyPetEquipment) {
                    World world = myPet.getCraftPet().getHandle().world;
                    Location petLocation = myPet.getLocation();
                    for (net.minecraft.server.v1_8_R1.ItemStack is : ((IMyPetEquipment) myPet).getEquipment()) {
                        if (is != null) {
                            EntityItem itemEntity = new EntityItem(world, petLocation.getX(), petLocation.getY(), petLocation.getZ(), is);
                            itemEntity.pickupDelay = 10;
                            world.addEntity(itemEntity);
                        }
                    }
                }

                myPet.removePet();
                myPet.getOwner().setMyPetForWorldGroup(WorldGroup.getGroupByWorld(myPet.getOwner().getPlayer().getWorld().getName()).getName(), null);

                myPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Command.Release.Dead", myPet.getOwner()), myPet.getPetName()));
                MyPetList.removeInactiveMyPet(MyPetList.setMyPetInactive(myPet.getOwner()));
                DebugLogger.info(myPet.getOwner().getName() + " released pet (dead).");
                if (Configuration.STORE_PETS_ON_PET_RELEASE) {
                    MyPetPlugin.getPlugin().saveData(false);
                }
                return;
            }

            myPet.setRespawnTime((Configuration.RESPAWN_TIME_FIXED + MyPet.getCustomRespawnTimeFixed(myPet.getClass())) + (myPet.getExperience().getLevel() * (Configuration.RESPAWN_TIME_FACTOR + MyPet.getCustomRespawnTimeFactor(myPet.getClass()))));
            myPet.setStatus(PetState.Dead);

            if (deadEntity.getLastDamageCause() instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) deadEntity.getLastDamageCause();

                if (e.getDamager() instanceof Player) {
                    myPet.setRespawnTime((Configuration.RESPAWN_TIME_PLAYER_FIXED + MyPet.getCustomRespawnTimeFixed(myPet.getClass())) + (myPet.getExperience().getLevel() * (Configuration.RESPAWN_TIME_PLAYER_FACTOR + MyPet.getCustomRespawnTimeFactor(myPet.getClass()))));
                } else if (e.getDamager() instanceof CraftMyPet) {
                    MyPet killerMyPet = ((CraftMyPet) e.getDamager()).getMyPet();
                    if (myPet.getSkills().isSkillActive(Behavior.class) && killerMyPet.getSkills().isSkillActive(Behavior.class)) {
                        Behavior killerBehaviorSkill = killerMyPet.getSkills().getSkill(Behavior.class);
                        Behavior deadBehaviorSkill = myPet.getSkills().getSkill(Behavior.class);
                        if (deadBehaviorSkill.getBehavior() == BehaviorState.Duel && killerBehaviorSkill.getBehavior() == BehaviorState.Duel) {
                            EntityMyPet myPetEntity = ((CraftMyPet) deadEntity).getHandle();
                            EntityMyPet duelKiller = ((CraftMyPet) e.getDamager()).getHandle();
                            if (myPetEntity.petTargetSelector.hasGoal("DuelTarget")) {
                                BehaviorDuelTarget duelTarget = (BehaviorDuelTarget) myPetEntity.petTargetSelector.getGoal("DuelTarget");
                                if (duelTarget.getDuelOpponent() == duelKiller) {
                                    myPet.setRespawnTime(10);
                                }
                            }
                        }
                    }
                }
            }
            event.setDroppedExp(0);

            if (Experience.LOSS_FIXED > 0 || Experience.LOSS_PERCENT > 0) {
                double lostExpirience = Experience.LOSS_FIXED;
                lostExpirience += myPet.getExperience().getRequiredExp() * Experience.LOSS_PERCENT / 100;
                if (lostExpirience > myPet.getExperience().getCurrentExp()) {
                    lostExpirience = myPet.getExperience().getCurrentExp();
                }
                if (myPet.getSkillTree() != null) {
                    int requiredLevel = myPet.getSkillTree().getRequiredLevel();
                    if (requiredLevel > 1) {
                        double minExp = myPet.getExperience().getExpByLevel(requiredLevel);
                        lostExpirience = myPet.getExp() - lostExpirience < minExp ? myPet.getExp() - minExp : lostExpirience;
                    }
                }
                if (Experience.DROP_LOST_EXP) {
                    event.setDroppedExp((int) (lostExpirience + 0.5));
                }
                myPet.getExperience().removeCurrentExp(lostExpirience);
            }
            if (myPet.getSkills().isSkillActive(Inventory.class)) {
                Inventory inventorySkill = myPet.getSkills().getSkill(Inventory.class);
                inventorySkill.closeInventory();
                if (inventorySkill.dropOnDeath() && !myPet.getOwner().isMyPetAdmin()) {
                    inventorySkill.inv.dropContentAt(myPet.getLocation());
                }
            }
            sendDeathMessage(event);
            myPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Spawn.Respawn.In", myPet.getOwner().getPlayer()), myPet.getPetName(), myPet.getRespawnTime()));

            if (Economy.canUseEconomy() && myPet.getOwner().hasAutoRespawnEnabled() && myPet.getRespawnTime() >= myPet.getOwner().getAutoRespawnMin() && Permissions.has(myPet.getOwner().getPlayer(), "MyPet.user.respawn")) {
                double costs = myPet.getRespawnTime() * Configuration.RESPAWN_COSTS_FACTOR + Configuration.RESPAWN_COSTS_FIXED;
                if (Economy.canPay(myPet.getOwner(), costs)) {
                    Economy.pay(myPet.getOwner(), costs);
                    myPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Command.Respawn.Paid", myPet.getOwner().getPlayer()), myPet.getPetName(), costs + " " + Economy.getEconomy().currencyNameSingular()));
                    myPet.setRespawnTime(1);
                } else {
                    myPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Command.Respawn.NoMoney", myPet.getOwner().getPlayer()), myPet.getPetName(), costs + " " + Economy.getEconomy().currencyNameSingular()));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    @EventHandler
    public void onEntityDeath(final EntityDeathEvent event) {
        LivingEntity deadEntity = event.getEntity();
        if (deadEntity instanceof CraftMyPet) {
            return;
        }
        if (!Experience.GAIN_EXP_FROM_MONSTER_SPAWNER_MOBS && event.getEntity().hasMetadata("MonsterSpawner")) {
            for (MetadataValue value : event.getEntity().getMetadata("MonsterSpawner")) {
                if (value.getOwningPlugin().getName().equals(MyPetPlugin.getPlugin().getName())) {
                    if (value.asBoolean()) {
                        return;
                    }
                    break;
                }
            }
        }
        if (Experience.DAMAGE_WEIGHTED_EXPERIENCE_DISTRIBUTION) {
            Map<Entity, Double> damagePercentMap = Experience.getDamageToEntityPercent(deadEntity);
            for (Entity entity : damagePercentMap.keySet()) {
                if (entity instanceof CraftMyPet) {
                    MyPet myPet = ((CraftMyPet) entity).getMyPet();
                    if (Configuration.PREVENT_LEVELLING_WITHOUT_SKILLTREE && myPet.getSkillTree() == null) {
                        if (!myPet.autoAssignSkilltree()) {
                            continue;
                        }
                    }
                    if (myPet.getSkillTree() == null || myPet.getSkillTree().getMaxLevel() <= 1 || myPet.getExperience().getLevel() < myPet.getSkillTree().getMaxLevel()) {
                        double randomExp = MonsterExperience.getMonsterExperience(deadEntity.getType()).getRandomExp();
                        myPet.getExperience().addExp(damagePercentMap.get(entity) * randomExp);
                    }
                } else if (entity instanceof Player) {
                    Player owner = (Player) entity;
                    if (MyPetList.hasMyPet(owner)) {
                        MyPet myPet = MyPetList.getMyPet(owner);
                        if (Configuration.PREVENT_LEVELLING_WITHOUT_SKILLTREE && myPet.getSkillTree() == null) {
                            if (!myPet.autoAssignSkilltree()) {
                                continue;
                            }
                        }
                        if (myPet.isPassiv() || Experience.ALWAYS_GRANT_PASSIVE_XP) {
                            if (myPet.getStatus() == PetState.Here) {
                                if (myPet.getSkillTree() == null || myPet.getSkillTree().getMaxLevel() <= 1 || myPet.getExperience().getLevel() < myPet.getSkillTree().getMaxLevel()) {
                                    myPet.getExperience().addExp(deadEntity.getType(), Experience.PASSIVE_PERCENT_PER_MONSTER);
                                }
                            }
                        }
                    }
                }
            }
        } else if (deadEntity.getLastDamageCause() instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent edbee = (EntityDamageByEntityEvent) deadEntity.getLastDamageCause();

            Entity damager = edbee.getDamager();
            if (damager instanceof Projectile) {
                damager = (Entity) ((Projectile) damager).getShooter();
            }
            if (damager instanceof CraftMyPet) {
                MyPet myPet = ((CraftMyPet) damager).getMyPet();
                if (myPet.getSkillTree() == null && Configuration.PREVENT_LEVELLING_WITHOUT_SKILLTREE) {
                    if (!myPet.autoAssignSkilltree()) {
                        return;
                    }
                }
                myPet.getExperience().addExp(edbee.getEntity().getType());
            } else if (damager instanceof Player) {
                Player owner = (Player) damager;
                if (MyPetList.hasMyPet(owner)) {
                    MyPet myPet = MyPetList.getMyPet(owner);
                    if (Configuration.PREVENT_LEVELLING_WITHOUT_SKILLTREE && myPet.getSkillTree() == null) {
                        if (!myPet.autoAssignSkilltree()) {
                            return;
                        }
                    }
                    if (myPet.isPassiv() || Experience.ALWAYS_GRANT_PASSIVE_XP) {
                        if (myPet.getStatus() == PetState.Here) {
                            if (myPet.getSkillTree() == null || myPet.getSkillTree().getMaxLevel() <= 1 || myPet.getExperience().getLevel() < myPet.getSkillTree().getMaxLevel()) {
                                myPet.getExperience().addExp(deadEntity.getType(), Experience.PASSIVE_PERCENT_PER_MONSTER);
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityTarget(final EntityTargetEvent event) {
        if (event.getEntity() instanceof CraftMyPet) {
            MyPet myPet = ((CraftMyPet) event.getEntity()).getMyPet();
            if (myPet.getSkills().isSkillActive(Behavior.class)) {
                Behavior behaviorSkill = myPet.getSkills().getSkill(Behavior.class);
                if (behaviorSkill.getBehavior() == Behavior.BehaviorState.Friendly) {
                    event.setCancelled(true);
                } else if (event.getTarget() instanceof Player && ((Player) event.getTarget()).getName().equals(myPet.getOwner().getName())) {
                    event.setCancelled(true);
                } else if (behaviorSkill.getBehavior() == Behavior.BehaviorState.Raid) {
                    if (event.getTarget() instanceof Player) {
                        event.setCancelled(true);
                    } else if (event.getTarget() instanceof Tameable && ((Tameable) event.getTarget()).isTamed()) {
                        event.setCancelled(true);
                    } else if (event.getTarget() instanceof CraftMyPet) {
                        event.setCancelled(true);
                    }
                }
            }
        } else if (event.getEntity() instanceof Tameable) {
            if (event.getTarget() instanceof CraftMyPet) {
                Tameable tameable = ((Tameable) event.getEntity());
                MyPet myPet = ((CraftMyPet) event.getTarget()).getMyPet();
                if (myPet.getOwner().equals(tameable.getOwner())) {
                    event.setCancelled(true);
                }
            }
        } else if (event.getEntity() instanceof IronGolem) {
            if (event.getTarget() instanceof CraftMyPet) {
                if (event.getReason() == TargetReason.RANDOM_TARGET) {
                    event.setCancelled(true);
                }
            }
        }
    }

    private void sendDeathMessage(final EntityDeathEvent event) {
        if (event.getEntity() instanceof CraftMyPet) {
            MyPet myPet = ((CraftMyPet) event.getEntity()).getMyPet();
            String killer;
            if (event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event.getEntity().getLastDamageCause();

                if (e.getDamager().getType() == EntityType.PLAYER) {
                    if (e.getDamager() == myPet.getOwner().getPlayer()) {
                        killer = Locales.getString("Name.You", myPet.getOwner().getLanguage());
                    } else {
                        killer = ((Player) e.getDamager()).getName();
                    }
                } else if (e.getDamager().getType() == EntityType.WOLF) {
                    Wolf w = (Wolf) e.getDamager();
                    killer = Locales.getString("Name.Wolf", myPet.getOwner().getLanguage());
                    if (w.isTamed()) {
                        killer += " (" + w.getOwner().getName() + ')';
                    }
                } else if (e.getDamager() instanceof CraftMyPet) {
                    CraftMyPet craftMyPet = (CraftMyPet) e.getDamager();
                    killer = craftMyPet.getMyPet().getPetName() + " (" + craftMyPet.getOwner().getName() + ')';
                } else if (e.getDamager() instanceof Projectile) {
                    Projectile projectile = (Projectile) e.getDamager();
                    killer = Locales.getString("Name." + Util.capitalizeName(projectile.getType().name()), myPet.getOwner().getLanguage()) + " (";
                    if (projectile.getShooter() instanceof Player) {
                        if (projectile.getShooter() == myPet.getOwner().getPlayer()) {
                            killer += Locales.getString("Name.You", myPet.getOwner().getLanguage());
                        } else {
                            killer += ((Player) projectile.getShooter()).getName();
                        }
                    } else {
                        if (MyPetType.isLeashableEntityType(e.getDamager().getType())) {
                            killer += Locales.getString("Name." + Util.capitalizeName(MyPetType.getMyPetTypeByEntityType(e.getDamager().getType()).getTypeName()), myPet.getOwner().getLanguage());
                        } else if (e.getDamager().getType().getName() != null) {
                            killer += Locales.getString("Name." + Util.capitalizeName(e.getDamager().getType().getName()), myPet.getOwner().getLanguage());
                        } else {
                            killer += Locales.getString("Name.Unknow", myPet.getOwner().getLanguage());
                        }
                    }
                    killer += ")";
                } else {
                    if (MyPetType.isLeashableEntityType(e.getDamager().getType())) {
                        killer = Locales.getString("Name." + Util.capitalizeName(MyPetType.getMyPetTypeByEntityType(e.getDamager().getType()).getTypeName()), myPet.getOwner().getLanguage());
                    } else {
                        killer = Locales.getString("Name." + Util.capitalizeName(e.getDamager().getType().getName()), myPet.getOwner().getLanguage());
                    }
                }
            } else {
                if (event.getEntity().getLastDamageCause() != null) {
                    killer = Locales.getString("Name." + Util.capitalizeName(event.getEntity().getLastDamageCause().getCause().name()), myPet.getOwner().getLanguage());
                } else {
                    killer = Locales.getString("Name.Unknow", myPet.getOwner().getLanguage());
                }
            }
            myPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.DeathMessage", myPet.getOwner().getLanguage()), myPet.getPetName(), killer));
        }
    }
}