/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2013 Keyle
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
import de.Keyle.MyPet.chatcommands.CommandInfo;
import de.Keyle.MyPet.chatcommands.CommandInfo.PetInfoDisplay;
import de.Keyle.MyPet.entity.EquipmentSlot;
import de.Keyle.MyPet.entity.ai.target.BehaviorDuelTarget;
import de.Keyle.MyPet.entity.types.*;
import de.Keyle.MyPet.entity.types.MyPet.LeashFlag;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.entity.types.enderman.EntityMyEnderman;
import de.Keyle.MyPet.skill.Experience;
import de.Keyle.MyPet.skill.MonsterExperience;
import de.Keyle.MyPet.skill.skills.implementation.*;
import de.Keyle.MyPet.skill.skills.implementation.Wither;
import de.Keyle.MyPet.skill.skills.implementation.inventory.ItemStackNBTConverter;
import de.Keyle.MyPet.skill.skills.info.BehaviorInfo.BehaviorState;
import de.Keyle.MyPet.util.*;
import de.Keyle.MyPet.util.locale.Locales;
import de.Keyle.MyPet.util.logger.DebugLogger;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.WordUtils;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_6_R2.entity.*;
import org.bukkit.craftbukkit.v1_6_R2.inventory.CraftItemStack;
import org.bukkit.entity.*;
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
import org.spout.nbt.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.bukkit.Bukkit.getPluginManager;

public class EntityListener implements Listener
{
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onMyPetEntitySpawn(final CreatureSpawnEvent event)
    {
        if (event.getEntity() instanceof CraftMyPet)
        {
            event.setCancelled(false);
        }
        if (Configuration.USE_LEVEL_SYSTEM && !Experience.GAIN_EXP_FROM_MONSTER_SPAWNER_MOBS && event.getSpawnReason() == SpawnReason.SPAWNER)
        {
            event.getEntity().setMetadata("MonsterSpawner", new FixedMetadataValue(MyPetPlugin.getPlugin(), true));
        }
    }

    @EventHandler
    public void onMyPetEntityPortal(EntityPortalEvent event)
    {
        if (event.getEntity() instanceof CraftMyPet)
        {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMyPetEntityDamageByEntity(final EntityDamageByEntityEvent event)
    {
        if (event.getEntity() instanceof CraftMyPet)
        {
            CraftMyPet craftMyPet = (CraftMyPet) event.getEntity();
            MyPet myPet = craftMyPet.getMyPet();
            if (event.getDamager() instanceof Player || (event.getDamager() instanceof Projectile && ((Projectile) event.getDamager()).getShooter() instanceof Player))
            {
                Player damager;
                if (event.getDamager() instanceof Projectile)
                {
                    damager = (Player) ((Projectile) event.getDamager()).getShooter();
                }
                else
                {
                    damager = (Player) event.getDamager();
                }
                if (damager.getItemInHand().getTypeId() == Configuration.LEASH_ITEM)
                {
                    MyPetPlayer myPetPlayer = MyPetPlayer.getMyPetPlayer(damager);

                    boolean infoShown = false;
                    if (CommandInfo.canSee(PetInfoDisplay.Name.adminOnly, myPetPlayer, myPet))
                    {
                        damager.sendMessage(ChatColor.AQUA + myPet.getPetName() + ChatColor.RESET + ":");
                        infoShown = true;
                    }
                    if (CommandInfo.canSee(PetInfoDisplay.Owner.adminOnly, myPetPlayer, myPet) && myPet.getOwner() != myPetPlayer)
                    {
                        damager.sendMessage("   " + Locales.getString("Name.Owner", damager) + ": " + myPetPlayer.getName());
                        infoShown = true;
                    }
                    if (CommandInfo.canSee(PetInfoDisplay.HP.adminOnly, myPetPlayer, myPet))
                    {
                        String msg;
                        if (myPet.getHealth() > myPet.getMaxHealth() / 3 * 2)
                        {
                            msg = "" + ChatColor.GREEN;
                        }
                        else if (myPet.getHealth() > myPet.getMaxHealth() / 3)
                        {
                            msg = "" + ChatColor.YELLOW;
                        }
                        else
                        {
                            msg = "" + ChatColor.RED;
                        }
                        msg += String.format("%1.2f", myPet.getHealth()) + ChatColor.WHITE + "/" + String.format("%1.2f", myPet.getMaxHealth());
                        damager.sendMessage("   " + Locales.getString("Name.HP", damager) + ": " + msg);
                        infoShown = true;
                    }
                    if (!myPet.isPassiv() && CommandInfo.canSee(PetInfoDisplay.Damage.adminOnly, myPetPlayer, myPet))
                    {
                        double damage = (myPet.getSkills().isSkillActive("Damage") ? ((Damage) myPet.getSkills().getSkill("Damage")).getDamage() : 0);
                        damager.sendMessage("   " + Locales.getString("Name.Damage", damager) + ": " + String.format("%1.2f", damage));
                        infoShown = true;
                    }
                    if (myPet.getRangedDamage() > 0 && CommandInfo.canSee(PetInfoDisplay.RangedDamage.adminOnly, myPetPlayer, myPet))
                    {
                        double damage = myPet.getRangedDamage();
                        damager.sendMessage("   " + Locales.getString("Name.RangedDamage", damager) + ": " + String.format("%1.2f", damage));
                        infoShown = true;
                    }
                    if (Configuration.USE_HUNGER_SYSTEM && CommandInfo.canSee(PetInfoDisplay.Hunger.adminOnly, myPetPlayer, myPet))
                    {
                        damager.sendMessage("   " + Locales.getString("Name.Hunger", damager) + ": " + myPet.getHungerValue());
                        infoShown = true;
                    }
                    if (CommandInfo.canSee(PetInfoDisplay.Skilltree.adminOnly, myPetPlayer, myPet) && myPet.getSkillTree() != null)
                    {
                        damager.sendMessage("   " + Locales.getString("Name.Skilltree", damager) + ": " + myPet.getSkillTree().getName());
                        infoShown = true;
                    }
                    if (Configuration.USE_LEVEL_SYSTEM)
                    {
                        if (CommandInfo.canSee(PetInfoDisplay.Level.adminOnly, myPetPlayer, myPet))
                        {
                            int lvl = myPet.getExperience().getLevel();
                            damager.sendMessage("   " + Locales.getString("Name.Level", damager) + ": " + lvl);
                            infoShown = true;
                        }
                        if (CommandInfo.canSee(PetInfoDisplay.Exp.adminOnly, myPetPlayer, myPet))
                        {
                            double exp = myPet.getExperience().getCurrentExp();
                            double reqEXP = myPet.getExperience().getRequiredExp();
                            damager.sendMessage("   " + Locales.getString("Name.Exp", damager) + ": " + String.format("%1.2f", exp) + "/" + String.format("%1.2f", reqEXP));
                            infoShown = true;
                        }
                    }

                    if (myPet.getOwner().isDonator())
                    {
                        infoShown = true;
                        damager.sendMessage("   " + myPet.getOwner().getDonationRank().getDisplayText());
                    }
                    if (!infoShown)
                    {
                        damager.sendMessage(Locales.getString("Message.NothingToSeeHere", myPet.getOwner().getLanguage()));
                    }

                    event.setCancelled(true);
                }
                else if (myPet.getOwner().equals(damager) && (!Configuration.OWNER_CAN_ATTACK_PET || !PvPChecker.canHurt(myPet.getOwner().getPlayer())))
                {
                    event.setCancelled(true);
                }
                else if (!myPet.getOwner().equals(damager) && !PvPChecker.canHurt(damager, myPet.getOwner().getPlayer()))
                {
                    event.setCancelled(true);
                }
            }
            if (!event.isCancelled() && event.getDamager() instanceof LivingEntity)
            {
                LivingEntity damager = (LivingEntity) event.getDamager();
                if (damager instanceof Player)
                {
                    if (!PvPChecker.canHurtAncientRPG(myPet.getOwner().getPlayer(), (Player) damager))
                    {
                        return;
                    }
                }
                if (myPet.getSkills().isSkillActive("Thorns"))
                {
                    Thorns thornsSkill = ((Thorns) myPet.getSkills().getSkill("Thorns"));
                    if (thornsSkill.activate())
                    {
                        isSkillActive = true;
                        thornsSkill.reflectDamage(damager, event.getDamage());
                        isSkillActive = false;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamageByPlayer(final EntityDamageByEntityEvent event)
    {
        if (!(event.getEntity() instanceof CraftMyPet) && event.getDamager() instanceof Player)
        {
            if (MyPetType.isLeashableEntityType(event.getEntity().getType()))
            {
                Player damager = (Player) event.getDamager();

                if (!MyPetList.hasMyPet(damager))
                {
                    LivingEntity leashTarget = (LivingEntity) event.getEntity();

                    Class<? extends MyPet> myPetClass = MyPetType.getMyPetTypeByEntityType(leashTarget.getType()).getMyPetClass();

                    if (damager.getItemInHand().getTypeId() != Configuration.LEASH_ITEM || !Permissions.has(damager, "MyPet.user.leash." + MyPetType.getMyPetTypeByEntityType(leashTarget.getType()).getTypeName()))
                    {
                        return;
                    }
                    if (Permissions.has(damager, "MyPet.user.capturehelper") && MyPetPlayer.isMyPetPlayer(damager) && MyPetPlayer.getMyPetPlayer(damager).isCaptureHelperActive())
                    {
                        CaptureHelper.checkTamable(leashTarget, event.getDamage(), damager);
                    }

                    boolean willBeLeashed = true;
                    List<LeashFlag> leashFlags = MyPet.getLeashFlags(myPetClass);

                    for (LeashFlag flag : leashFlags)
                    {
                        if (flag == LeashFlag.Adult)
                        {
                            if (leashTarget instanceof Ageable)
                            {
                                willBeLeashed = ((Ageable) leashTarget).isAdult();
                            }
                            else if (leashTarget instanceof Zombie)
                            {
                                willBeLeashed = !((Zombie) leashTarget).isBaby();
                            }
                        }
                        else if (flag == LeashFlag.Baby)
                        {
                            if (leashTarget instanceof Ageable)
                            {
                                willBeLeashed = !((Ageable) leashTarget).isAdult();
                            }
                            else if (leashTarget instanceof Zombie)
                            {
                                willBeLeashed = ((Zombie) leashTarget).isBaby();
                            }
                        }
                        else if (flag == LeashFlag.LowHp)
                        {
                            willBeLeashed = leashTarget.getHealth() <= 2;
                        }
                        else if (flag == LeashFlag.UserCreated)
                        {
                            if (leashTarget instanceof IronGolem)
                            {
                                //willBeLeashed = ((IronGolem) leashTarget).isPlayerCreated();

                                //temporary fix
                                willBeLeashed = ((CraftIronGolem) leashTarget).getHandle().bW();
                            }
                        }
                        else if (flag == LeashFlag.Wild)
                        {
                            if (leashTarget instanceof IronGolem)
                            {
                                //willBeLeashed = !((IronGolem) leashTarget).isPlayerCreated();

                                //temporary fix
                                willBeLeashed = !((CraftIronGolem) leashTarget).getHandle().bW();
                            }
                            else if (leashTarget instanceof Tameable)
                            {
                                willBeLeashed = !((Tameable) leashTarget).isTamed();
                            }
                            else if (leashTarget instanceof Horse)
                            {
                                willBeLeashed = !((CraftHorse) leashTarget).getHandle().isTame();
                            }
                        }
                        else if (flag == LeashFlag.Tamed)
                        {
                            if (leashTarget instanceof Tameable)
                            {
                                willBeLeashed = ((Tameable) leashTarget).isTamed();
                            }
                            if (leashTarget instanceof Horse)
                            {
                                willBeLeashed = ((CraftHorse) leashTarget).getHandle().isTame();
                            }
                        }
                        else if (flag == LeashFlag.CanBreed)
                        {
                            if (leashTarget instanceof Ageable)
                            {
                                willBeLeashed = ((Ageable) leashTarget).canBreed();
                            }
                        }
                        else if (flag == LeashFlag.Angry)
                        {
                            if (leashTarget instanceof Wolf)
                            {
                                willBeLeashed = ((Wolf) leashTarget).isAngry();
                            }
                        }
                        else if (flag == LeashFlag.Impossible)
                        {
                            willBeLeashed = false;
                            break;
                        }
                        else if (flag == LeashFlag.None)
                        {
                            willBeLeashed = true;
                            break;
                        }
                        if (!willBeLeashed)
                        {
                            break;
                        }
                    }

                    if (willBeLeashed)
                    {
                        event.setCancelled(true);
                        InactiveMyPet inactiveMyPet = new InactiveMyPet(MyPetPlayer.getMyPetPlayer(damager.getName()));
                        inactiveMyPet.setPetType(MyPetType.getMyPetTypeByEntityType(leashTarget.getType()));
                        inactiveMyPet.setPetName(Locales.getString("Name." + inactiveMyPet.getPetType().getTypeName(), inactiveMyPet.getOwner().getLanguage()));
                        /*
                        if(leashTarget.getCustomName() != null)
                        {
                            inactiveMyPet.setPetName(leashTarget.getCustomName());
                        }
                        */

                        CompoundTag extendedInfo = new CompoundTag("Info", new CompoundMap());
                        if (leashTarget instanceof Ocelot)
                        {
                            extendedInfo.getValue().put("CatType", new IntTag("CatType", ((Ocelot) leashTarget).getCatType().getId()));
                            extendedInfo.getValue().put("Sitting", new ByteTag("Sitting", ((Ocelot) leashTarget).isSitting()));
                        }
                        else if (leashTarget instanceof Wolf)
                        {
                            extendedInfo.getValue().put("Sitting", new ByteTag("Sitting", ((Wolf) leashTarget).isSitting()));
                            extendedInfo.getValue().put("Tamed", new ByteTag("Tamed", ((Wolf) leashTarget).isTamed()));
                            extendedInfo.getValue().put("CollarColor", new ByteTag("CollarColor", ((Wolf) leashTarget).getCollarColor().getDyeData()));
                        }
                        else if (leashTarget instanceof Sheep)
                        {
                            extendedInfo.getValue().put("Color", new IntTag("Color", ((Sheep) leashTarget).getColor().getDyeData()));
                            extendedInfo.getValue().put("Sheared", new ByteTag("Sheared", ((Sheep) leashTarget).isSheared()));
                        }
                        else if (leashTarget instanceof Villager)
                        {
                            extendedInfo.getValue().put("Profession", new IntTag("Profession", ((Villager) leashTarget).getProfession().getId()));
                        }
                        else if (leashTarget instanceof Pig)
                        {
                            extendedInfo.getValue().put("Saddle", new ByteTag("Saddle", ((Pig) leashTarget).hasSaddle()));
                        }
                        else if (leashTarget instanceof Slime)
                        {
                            extendedInfo.getValue().put("Size", new IntTag("Size", ((Slime) leashTarget).getSize()));
                        }
                        else if (leashTarget instanceof Creeper)
                        {
                            extendedInfo.getValue().put("Powered", new ByteTag("Powered", ((Creeper) leashTarget).isPowered()));
                        }
                        else if (leashTarget instanceof Horse)
                        {
                            extendedInfo.getValue().put("Type", new ByteTag("Type", (byte) ((CraftHorse) leashTarget).getHandle().getType()));
                            extendedInfo.getValue().put("Variant", new IntTag("Variant", ((CraftHorse) leashTarget).getHandle().getVariant()));
                            extendedInfo.getValue().put("Armor", new IntTag("Armor", ((CraftHorse) leashTarget).getHandle().cf()));
                            extendedInfo.getValue().put("Chest", new ByteTag("Chest", ((CraftHorse) leashTarget).getHandle().hasChest()));
                            extendedInfo.getValue().put("Saddle", new ByteTag("Saddle", ((CraftHorse) leashTarget).getHandle().co()));
                            extendedInfo.getValue().put("Age", new IntTag("Age", ((CraftHorse) leashTarget).getHandle().getAge()));
                        }
                        else if (leashTarget instanceof Zombie)
                        {
                            extendedInfo.getValue().put("Baby", new ByteTag("Baby", ((Zombie) leashTarget).isBaby()));
                            extendedInfo.getValue().put("Villager", new ByteTag("Villager", ((Zombie) leashTarget).isVillager()));

                            Random random = ((CraftLivingEntity) leashTarget).getHandle().aC();
                            List<CompoundTag> equipmentList = new ArrayList<CompoundTag>();
                            if (random.nextFloat() <= leashTarget.getEquipment().getChestplateDropChance())
                            {
                                ItemStack itemStack = leashTarget.getEquipment().getChestplate();
                                if (itemStack != null && itemStack.getType() != Material.AIR)
                                {
                                    net.minecraft.server.v1_6_R2.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(itemStack);
                                    CompoundTag item = ItemStackNBTConverter.ItemStackToCompund(nmsItemStack);
                                    item.getValue().put("Slot", new IntTag("Slot", EquipmentSlot.Chestplate.getSlotId()));
                                    equipmentList.add(item);
                                }
                            }
                            if (random.nextFloat() <= leashTarget.getEquipment().getHelmetDropChance())
                            {
                                ItemStack itemStack = leashTarget.getEquipment().getHelmet();
                                if (itemStack != null && itemStack.getType() != Material.AIR)
                                {
                                    net.minecraft.server.v1_6_R2.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(itemStack);
                                    CompoundTag item = ItemStackNBTConverter.ItemStackToCompund(nmsItemStack);
                                    item.getValue().put("Slot", new IntTag("Slot", EquipmentSlot.Helmet.getSlotId()));
                                    equipmentList.add(item);
                                }
                            }
                            if (random.nextFloat() <= leashTarget.getEquipment().getLeggingsDropChance())
                            {
                                ItemStack itemStack = leashTarget.getEquipment().getLeggings();
                                if (itemStack != null && itemStack.getType() != Material.AIR)
                                {
                                    net.minecraft.server.v1_6_R2.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(itemStack);
                                    CompoundTag item = ItemStackNBTConverter.ItemStackToCompund(nmsItemStack);
                                    item.getValue().put("Slot", new IntTag("Slot", EquipmentSlot.Leggins.getSlotId()));
                                    equipmentList.add(item);
                                }
                            }
                            if (random.nextFloat() <= leashTarget.getEquipment().getBootsDropChance())
                            {
                                ItemStack itemStack = leashTarget.getEquipment().getBoots();
                                if (itemStack != null && itemStack.getType() != Material.AIR)
                                {
                                    net.minecraft.server.v1_6_R2.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(itemStack);
                                    CompoundTag item = ItemStackNBTConverter.ItemStackToCompund(nmsItemStack);
                                    item.getValue().put("Slot", new IntTag("Slot", EquipmentSlot.Boots.getSlotId()));
                                    equipmentList.add(item);
                                }
                            }
                            extendedInfo.getValue().put("Equipment", new ListTag<CompoundTag>("Equipment", CompoundTag.class, equipmentList));
                        }
                        else if (leashTarget instanceof Enderman)
                        {
                            extendedInfo.getValue().put("BlockID", new ShortTag("BlockID", (short) ((CraftEnderman) leashTarget).getHandle().getCarriedId()));
                            extendedInfo.getValue().put("BlockData", new ShortTag("BlockData", (short) ((CraftEnderman) leashTarget).getHandle().getCarriedData()));
                        }
                        else if (leashTarget instanceof Skeleton)
                        {
                            extendedInfo.getValue().put("Wither", new ByteTag("Wither", ((CraftSkeleton) leashTarget).getSkeletonType() == SkeletonType.WITHER));

                            Random random = ((CraftLivingEntity) leashTarget).getHandle().aC();
                            List<CompoundTag> equipmentList = new ArrayList<CompoundTag>();
                            if (random.nextFloat() <= leashTarget.getEquipment().getChestplateDropChance())
                            {
                                ItemStack itemStack = leashTarget.getEquipment().getChestplate();
                                if (itemStack != null && itemStack.getType() != Material.AIR)
                                {
                                    net.minecraft.server.v1_6_R2.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(itemStack);
                                    CompoundTag item = ItemStackNBTConverter.ItemStackToCompund(nmsItemStack);
                                    item.getValue().put("Slot", new IntTag("Slot", EquipmentSlot.Chestplate.getSlotId()));
                                    equipmentList.add(item);
                                }
                            }
                            if (random.nextFloat() <= leashTarget.getEquipment().getHelmetDropChance())
                            {
                                ItemStack itemStack = leashTarget.getEquipment().getHelmet();
                                if (itemStack != null && itemStack.getType() != Material.AIR)
                                {
                                    net.minecraft.server.v1_6_R2.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(itemStack);
                                    CompoundTag item = ItemStackNBTConverter.ItemStackToCompund(nmsItemStack);
                                    item.getValue().put("Slot", new IntTag("Slot", EquipmentSlot.Helmet.getSlotId()));
                                    equipmentList.add(item);
                                }
                            }
                            if (random.nextFloat() <= leashTarget.getEquipment().getLeggingsDropChance())
                            {
                                ItemStack itemStack = leashTarget.getEquipment().getLeggings();
                                if (itemStack != null && itemStack.getType() != Material.AIR)
                                {
                                    net.minecraft.server.v1_6_R2.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(itemStack);
                                    CompoundTag item = ItemStackNBTConverter.ItemStackToCompund(nmsItemStack);
                                    item.getValue().put("Slot", new IntTag("Slot", EquipmentSlot.Leggins.getSlotId()));
                                    equipmentList.add(item);
                                }
                            }
                            if (random.nextFloat() <= leashTarget.getEquipment().getBootsDropChance())
                            {
                                ItemStack itemStack = leashTarget.getEquipment().getBoots();
                                if (itemStack != null && itemStack.getType() != Material.AIR)
                                {
                                    net.minecraft.server.v1_6_R2.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(itemStack);
                                    CompoundTag item = ItemStackNBTConverter.ItemStackToCompund(nmsItemStack);
                                    item.getValue().put("Slot", new IntTag("Slot", EquipmentSlot.Boots.getSlotId()));
                                    equipmentList.add(item);
                                }
                            }
                            extendedInfo.getValue().put("Equipment", new ListTag<CompoundTag>("Equipment", CompoundTag.class, equipmentList));
                        }
                        else if (leashTarget instanceof PigZombie)
                        {
                            Random random = ((CraftLivingEntity) leashTarget).getHandle().aC();
                            List<CompoundTag> equipmentList = new ArrayList<CompoundTag>();
                            if (random.nextFloat() <= leashTarget.getEquipment().getChestplateDropChance())
                            {
                                ItemStack itemStack = leashTarget.getEquipment().getChestplate();
                                if (itemStack != null && itemStack.getType() != Material.AIR)
                                {
                                    net.minecraft.server.v1_6_R2.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(itemStack);
                                    CompoundTag item = ItemStackNBTConverter.ItemStackToCompund(nmsItemStack);
                                    item.getValue().put("Slot", new IntTag("Slot", EquipmentSlot.Chestplate.getSlotId()));
                                    equipmentList.add(item);
                                }
                            }
                            if (random.nextFloat() <= leashTarget.getEquipment().getHelmetDropChance())
                            {
                                ItemStack itemStack = leashTarget.getEquipment().getHelmet();
                                if (itemStack != null && itemStack.getType() != Material.AIR)
                                {
                                    net.minecraft.server.v1_6_R2.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(itemStack);
                                    CompoundTag item = ItemStackNBTConverter.ItemStackToCompund(nmsItemStack);
                                    item.getValue().put("Slot", new IntTag("Slot", EquipmentSlot.Helmet.getSlotId()));
                                    equipmentList.add(item);
                                }
                            }
                            if (random.nextFloat() <= leashTarget.getEquipment().getLeggingsDropChance())
                            {
                                ItemStack itemStack = leashTarget.getEquipment().getLeggings();
                                if (itemStack != null && itemStack.getType() != Material.AIR)
                                {
                                    net.minecraft.server.v1_6_R2.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(itemStack);
                                    CompoundTag item = ItemStackNBTConverter.ItemStackToCompund(nmsItemStack);
                                    item.getValue().put("Slot", new IntTag("Slot", EquipmentSlot.Leggins.getSlotId()));
                                    equipmentList.add(item);
                                }
                            }
                            if (random.nextFloat() <= leashTarget.getEquipment().getBootsDropChance())
                            {
                                ItemStack itemStack = leashTarget.getEquipment().getBoots();
                                if (itemStack != null && itemStack.getType() != Material.AIR)
                                {
                                    net.minecraft.server.v1_6_R2.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(itemStack);
                                    CompoundTag item = ItemStackNBTConverter.ItemStackToCompund(nmsItemStack);
                                    item.getValue().put("Slot", new IntTag("Slot", EquipmentSlot.Boots.getSlotId()));
                                    equipmentList.add(item);
                                }
                            }
                            extendedInfo.getValue().put("Equipment", new ListTag<CompoundTag>("Equipment", CompoundTag.class, equipmentList));
                        }
                        if (leashTarget instanceof Ageable)
                        {
                            extendedInfo.getValue().put("Baby", new ByteTag("Baby", !((Ageable) leashTarget).isAdult()));
                        }
                        inactiveMyPet.setInfo(extendedInfo);

                        event.getEntity().remove();

                        MyPet myPet = MyPetList.setMyPetActive(inactiveMyPet);
                        myPet.createPet();

                        if (Configuration.CONSUME_LEASH_ITEM && damager.getGameMode() != GameMode.CREATIVE && damager.getItemInHand() != null)
                        {
                            if (damager.getItemInHand().getAmount() > 1)
                            {
                                damager.getItemInHand().setAmount(damager.getItemInHand().getAmount() - 1);
                            }
                            else
                            {
                                damager.setItemInHand(null);
                            }
                        }

                        WorldGroup worldGroup = WorldGroup.getGroup(damager.getWorld().getName());
                        myPet.setWorldGroup(worldGroup.getName());
                        myPet.getOwner().setMyPetForWorldGroup(worldGroup.getName(), myPet.getUUID());

                        if (Configuration.ENABLE_EVENTS)
                        {
                            getPluginManager().callEvent(new MyPetLeashEvent(myPet));
                        }
                        DebugLogger.info("New Pet leashed:");
                        DebugLogger.info("   " + myPet.toString());
                        if (Configuration.STORE_PETS_ON_PET_LEASH)
                        {
                            DebugLogger.info(MyPetPlugin.getPlugin().savePets(false) + " pet(s) saved.");
                        }
                        damager.sendMessage(Locales.getString("Message.AddLeash", myPet.getOwner().getLanguage()));
                    }
                }
            }
        }
    }

    @EventHandler
    public void onMyPetEntityDamage(final EntityDamageEvent event)
    {
        if (event.getEntity() instanceof CraftMyPet)
        {
            CraftMyPet craftMyPet = (CraftMyPet) event.getEntity();

            if (event.getCause() == DamageCause.FALL)
            {
                if (craftMyPet.getPetType() == MyPetType.Chicken || craftMyPet.getPetType() == MyPetType.Bat || craftMyPet.getPetType() == MyPetType.IronGolem)
                {
                    event.setCancelled(true);
                }
            }
            else if (event.getCause() == DamageCause.DROWNING)
            {
                if (craftMyPet.getPetType() == MyPetType.IronGolem)
                {
                    event.setCancelled(true);
                }
            }
            else if (event.getCause() == DamageCause.SUFFOCATION)
            {
                MyPet myPet = craftMyPet.getMyPet();

                myPet.removePet(true);

                switch (myPet.createPet())
                {
                    case Success:
                        myPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Call", myPet.getOwner().getLanguage()), myPet.getPetName()));
                        break;
                    case Canceled:
                        myPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.SpawnPrevent", myPet.getOwner().getLanguage()), myPet.getPetName()));
                        break;
                    case NoSpace:
                        myPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.SpawnNoSpace", myPet.getOwner().getLanguage()), myPet.getPetName()));
                        break;
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDamageByEntityDamageMonitor(final EntityDamageByEntityEvent event)
    {
        if (!event.isCancelled() && Experience.DAMAGE_WEIGHTED_EXPERIENCE_DISTRIBUTION && event.getEntity() instanceof LivingEntity && !(event.getEntity() instanceof Player) && !(event.getEntity() instanceof CraftMyPet))
        {
            LivingEntity damager = null;
            if (event.getDamager() instanceof Projectile)
            {
                Projectile projectile = (Projectile) event.getDamager();
                damager = projectile.getShooter();
            }
            else if (event.getDamager() instanceof LivingEntity)
            {
                damager = (LivingEntity) event.getDamager();
            }
            if (damager != null)
            {
                Experience.addDamageToEntity(damager, (LivingEntity) event.getEntity(), event.getDamage());
            }
        }
    }

    boolean isSkillActive = false;

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDamageByEntityResult(final EntityDamageByEntityEvent event)
    {
        Entity damagedEntity = event.getEntity();
        // --  fix unwanted screaming of Endermen --
        if (damagedEntity instanceof CraftMyPet && ((CraftMyPet) damagedEntity).getPetType() == MyPetType.Enderman)
        {
            ((EntityMyEnderman) ((CraftMyPet) damagedEntity).getHandle()).setScreaming(true);
            ((EntityMyEnderman) ((CraftMyPet) damagedEntity).getHandle()).setScreaming(false);
        }

        if (damagedEntity instanceof LivingEntity)
        {
            if (event.getDamager() instanceof Player)
            {
                Player damager = (Player) event.getDamager();
                if (damager.getItemInHand().getTypeId() == Configuration.LEASH_ITEM && damagedEntity instanceof CraftMyPet)
                {
                    return;
                }
                if (MyPetList.hasMyPet(damager))
                {
                    MyPet myPet = MyPetList.getMyPet(damager);
                    if (myPet.getStatus() == PetState.Here && damagedEntity != myPet.getCraftPet())
                    {
                        myPet.getCraftPet().getHandle().goalTarget = ((CraftLivingEntity) damagedEntity).getHandle();
                    }
                }
            }
            else if (event.getDamager() instanceof CraftMyPet && !isSkillActive)
            {
                MyPet myPet = ((CraftMyPet) event.getDamager()).getMyPet();

                // fix influence of other plugins
                event.setDamage(myPet.getDamage());

                //  --  Skills  --
                boolean skillUsed = false;
                if (myPet.getSkills().hasSkill("Poison"))
                {
                    Poison poisonSkill = (Poison) myPet.getSkills().getSkill("Poison");
                    if (poisonSkill.activate())
                    {
                        poisonSkill.poisonTarget((LivingEntity) damagedEntity);
                        skillUsed = true;
                    }
                }
                if (!skillUsed && myPet.getSkills().hasSkill("Wither"))
                {
                    Wither witherSkill = (Wither) myPet.getSkills().getSkill("Wither");
                    if (witherSkill.activate())
                    {
                        witherSkill.witherTarget((LivingEntity) damagedEntity);
                        skillUsed = true;
                    }
                }
                if (!skillUsed && myPet.getSkills().hasSkill("Fire"))
                {
                    Fire fireSkill = (Fire) myPet.getSkills().getSkill("Fire");
                    if (fireSkill.activate())
                    {
                        fireSkill.igniteTarget((LivingEntity) damagedEntity);
                        skillUsed = true;
                    }
                }
                if (!skillUsed && myPet.getSkills().hasSkill("Slow"))
                {
                    Slow slowSkill = (Slow) myPet.getSkills().getSkill("Slow");
                    if (slowSkill.activate())
                    {
                        slowSkill.slowTarget((LivingEntity) damagedEntity);
                        skillUsed = true;
                    }
                }
                if (!skillUsed && myPet.getSkills().hasSkill("Knockback"))
                {
                    Knockback knockbackSkill = (Knockback) myPet.getSkills().getSkill("Knockback");
                    if (knockbackSkill.activate())
                    {
                        knockbackSkill.knockbackTarget((LivingEntity) damagedEntity);
                        skillUsed = true;
                    }
                }
                if (!skillUsed && myPet.getSkills().hasSkill("Lightning"))
                {
                    Lightning lightningSkill = (Lightning) myPet.getSkills().getSkill("Lightning");
                    if (lightningSkill.activate())
                    {
                        isSkillActive = true;
                        lightningSkill.strikeLightning(damagedEntity.getLocation());
                        isSkillActive = false;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onMyPetEntityDeath(final EntityDeathEvent event)
    {
        LivingEntity deadEntity = event.getEntity();
        if (deadEntity instanceof CraftMyPet)
        {
            MyPet myPet = ((CraftMyPet) deadEntity).getMyPet();
            if (myPet == null || myPet.getHealth() > 0) // check health for death events where the pet isn't really dead (/killall)
            {
                return;
            }

            myPet.setRespawnTime((Configuration.RESPAWN_TIME_FIXED + MyPet.getCustomRespawnTimeFixed(myPet.getClass())) + (myPet.getExperience().getLevel() * (Configuration.RESPAWN_TIME_FACTOR + MyPet.getCustomRespawnTimeFactor(myPet.getClass()))));
            myPet.setStatus(PetState.Dead);

            if (deadEntity.getLastDamageCause() instanceof EntityDamageByEntityEvent)
            {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) deadEntity.getLastDamageCause();

                if (e.getDamager() instanceof Player)
                {
                    myPet.setRespawnTime((Configuration.RESPAWN_TIME_PLAYER_FIXED + MyPet.getCustomRespawnTimeFixed(myPet.getClass())) + (myPet.getExperience().getLevel() * (Configuration.RESPAWN_TIME_PLAYER_FACTOR + MyPet.getCustomRespawnTimeFactor(myPet.getClass()))));
                }
                else if (e.getDamager() instanceof CraftMyPet)
                {
                    MyPet killerMyPet = ((CraftMyPet) e.getDamager()).getMyPet();
                    if (myPet.getSkills().isSkillActive("Behavior") && killerMyPet.getSkills().isSkillActive("Behavior"))
                    {
                        Behavior killerBehaviorSkill = (Behavior) killerMyPet.getSkills().getSkill("Behavior");
                        Behavior deadBehaviorSkill = (Behavior) myPet.getSkills().getSkill("Behavior");
                        if (deadBehaviorSkill.getBehavior() == BehaviorState.Duel && killerBehaviorSkill.getBehavior() == BehaviorState.Duel)
                        {
                            EntityMyPet myPetEntity = ((CraftMyPet) deadEntity).getHandle();
                            EntityMyPet duelKiller = ((CraftMyPet) e.getDamager()).getHandle();
                            if (myPetEntity.petTargetSelector.hasGoal("DuelTarget"))
                            {
                                BehaviorDuelTarget duelTarget = (BehaviorDuelTarget) myPetEntity.petTargetSelector.getGoal("DuelTarget");
                                if (duelTarget.getDuelOpponent() == duelKiller)
                                {
                                    myPet.setRespawnTime(10);
                                }
                            }
                        }
                    }
                }
            }
            event.setDroppedExp(0);

            if (Configuration.USE_LEVEL_SYSTEM && (Experience.LOSS_FIXED > 0 || Experience.LOSS_PERCENT > 0))
            {
                double lostExpirience = Experience.LOSS_FIXED;
                lostExpirience += myPet.getExperience().getRequiredExp() * Experience.LOSS_PERCENT / 100;
                if (lostExpirience > myPet.getExperience().getCurrentExp())
                {
                    lostExpirience = myPet.getExperience().getCurrentExp();
                }
                if (Experience.DROP_LOST_EXP)
                {
                    event.setDroppedExp((int) (lostExpirience + 0.5));
                }
                myPet.getExperience().removeCurrentExp(lostExpirience);
            }
            if (myPet.getSkills().isSkillActive("Inventory"))
            {
                Inventory inventorySkill = (Inventory) myPet.getSkills().getSkill("Inventory");
                inventorySkill.closeInventory();
                if (inventorySkill.dropOnDeath() && !myPet.getOwner().isMyPetAdmin())
                {
                    inventorySkill.inv.dropContentAt(myPet.getLocation());
                }
            }
            sendDeathMessage(event);
            myPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.RespawnIn", myPet.getOwner().getPlayer()), myPet.getPetName(), myPet.getRespawnTime()));

            if (Economy.canUseEconomy() && myPet.getOwner().hasAutoRespawnEnabled() && myPet.getRespawnTime() >= myPet.getOwner().getAutoRespawnMin() && Permissions.has(myPet.getOwner().getPlayer(), "MyPet.user.respawn"))
            {
                double costs = myPet.getRespawnTime() * Configuration.RESPAWN_COSTS_FACTOR + Configuration.RESPAWN_COSTS_FIXED;
                if (Economy.canPay(myPet.getOwner(), costs))
                {
                    Economy.pay(myPet.getOwner(), costs);
                    myPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.RespawnPaid", myPet.getOwner().getPlayer()), myPet.getPetName(), costs + " " + Economy.getEconomy().currencyNameSingular()));
                    myPet.setRespawnTime(1);
                }
                else
                {
                    myPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.RespawnNoMoney", myPet.getOwner().getPlayer()), myPet.getPetName(), costs + " " + Economy.getEconomy().currencyNameSingular()));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    @EventHandler
    public void onEntityDeath(final EntityDeathEvent event)
    {
        if (Configuration.USE_LEVEL_SYSTEM)
        {
            LivingEntity deadEntity = event.getEntity();
            if (deadEntity instanceof CraftMyPet)
            {
                return;
            }
            if (!Experience.GAIN_EXP_FROM_MONSTER_SPAWNER_MOBS && event.getEntity().hasMetadata("MonsterSpawner"))
            {
                for (MetadataValue value : event.getEntity().getMetadata("MonsterSpawner"))
                {
                    if (value.getOwningPlugin().getName().equals(MyPetPlugin.getPlugin().getName()))
                    {
                        if (value.asBoolean())
                        {
                            return;
                        }
                        break;
                    }
                }
            }
            if (Experience.DAMAGE_WEIGHTED_EXPERIENCE_DISTRIBUTION)
            {
                Map<Entity, Double> damagePercentMap = Experience.getDamageToEntityPercent(deadEntity);
                for (Entity entity : damagePercentMap.keySet())
                {
                    if (entity instanceof CraftMyPet)
                    {
                        MyPet myPet = ((CraftMyPet) entity).getMyPet();
                        if (Configuration.PREVENT_LEVELLING_WITHOUT_SKILLTREE && myPet.getSkillTree() == null)
                        {
                            continue;
                        }
                        double randomExp = MonsterExperience.getMonsterExperience(deadEntity.getType()).getRandomExp();
                        myPet.getExperience().addExp(damagePercentMap.get(entity) * randomExp);
                    }
                    else if (entity instanceof Player)
                    {
                        Player owner = (Player) entity;
                        if (MyPetList.hasMyPet(owner))
                        {
                            MyPet myPet = MyPetList.getMyPet(owner);
                            if (Configuration.PREVENT_LEVELLING_WITHOUT_SKILLTREE && myPet.getSkillTree() == null)
                            {
                                return;
                            }
                            if (myPet.isPassiv())
                            {
                                if (myPet.getStatus() == PetState.Here)
                                {
                                    double randomExp = MonsterExperience.getMonsterExperience(deadEntity.getType()).getRandomExp();
                                    myPet.getExperience().addExp(damagePercentMap.get(entity) * randomExp);
                                }
                            }
                        }
                    }
                }
            }
            else if (deadEntity.getLastDamageCause() instanceof EntityDamageByEntityEvent)
            {
                EntityDamageByEntityEvent edbee = (EntityDamageByEntityEvent) deadEntity.getLastDamageCause();
                if (edbee.getDamager() instanceof CraftMyPet)
                {
                    MyPet myPet = ((CraftMyPet) edbee.getDamager()).getMyPet();
                    if (myPet.getSkillTree() == null && Configuration.PREVENT_LEVELLING_WITHOUT_SKILLTREE)
                    {
                        return;
                    }
                    myPet.getExperience().addExp(edbee.getEntity().getType());
                }
                else if (edbee.getDamager() instanceof Player)
                {
                    Player owner = (Player) edbee.getDamager();
                    if (MyPetList.hasMyPet(owner))
                    {
                        MyPet myPet = MyPetList.getMyPet(owner);
                        if (Configuration.PREVENT_LEVELLING_WITHOUT_SKILLTREE && myPet.getSkillTree() == null)
                        {
                            return;
                        }
                        if (myPet.isPassiv())
                        {
                            if (myPet.getStatus() == PetState.Here)
                            {
                                myPet.getExperience().addExp(deadEntity.getType(), Configuration.PASSIVE_PERCENT_PER_MONSTER);
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityTarget(final EntityTargetEvent event)
    {
        if (event.getEntity() instanceof CraftMyPet)
        {
            MyPet myPet = ((CraftMyPet) event.getEntity()).getMyPet();
            if (myPet.getSkills().isSkillActive("Behavior"))
            {
                Behavior behaviorSkill = (Behavior) myPet.getSkills().getSkill("Behavior");
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
                    if (event.getTarget() instanceof Player)
                    {
                        event.setCancelled(true);
                    }
                    else if (event.getTarget() instanceof Tameable && ((Tameable) event.getTarget()).isTamed())
                    {
                        event.setCancelled(true);
                    }
                    else if (event.getTarget() instanceof CraftMyPet)
                    {
                        event.setCancelled(true);
                    }
                }
            }
        }
        else if (event.getEntity() instanceof Tameable)
        {
            if (event.getTarget() instanceof CraftMyPet)
            {
                Tameable tameable = ((Tameable) event.getEntity());
                MyPet myPet = ((CraftMyPet) event.getTarget()).getMyPet();
                if (myPet.getOwner().equals(tameable.getOwner()))
                {
                    event.setCancelled(true);
                }
            }
        }
        else if (event.getEntity() instanceof IronGolem)
        {
            if (event.getTarget() instanceof CraftMyPet)
            {
                if (event.getReason() == TargetReason.RANDOM_TARGET)
                {
                    event.setCancelled(true);
                }
            }
        }
    }

    private void sendDeathMessage(final EntityDeathEvent event)
    {

        if (event.getEntity() instanceof CraftMyPet)
        {
            MyPet myPet = ((CraftMyPet) event.getEntity()).getMyPet();
            String killer;
            if (event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent)
            {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event.getEntity().getLastDamageCause();

                if (e.getDamager().getType() == EntityType.PLAYER)
                {
                    if (e.getDamager() == myPet.getOwner().getPlayer())
                    {
                        killer = Locales.getString("Name.You", myPet.getOwner().getLanguage());
                    }
                    else
                    {
                        killer = ((Player) e.getDamager()).getName();
                    }
                }
                else if (e.getDamager().getType() == EntityType.WOLF)
                {
                    Wolf w = (Wolf) e.getDamager();
                    killer = Locales.getString("Name.Wolf", myPet.getOwner().getLanguage());
                    if (w.isTamed())
                    {
                        killer += " (" + w.getOwner().getName() + ')';
                    }
                }
                else if (e.getDamager() instanceof CraftMyPet)
                {
                    CraftMyPet craftMyPet = (CraftMyPet) e.getDamager();
                    killer = craftMyPet.getMyPet().getPetName() + " (" + craftMyPet.getOwner().getName() + ')';
                }
                else if (e.getDamager() instanceof Projectile)
                {
                    Projectile projectile = (Projectile) e.getDamager();
                    killer = Locales.getString("Name." + capitalizeName(projectile.getType().name()), myPet.getOwner().getLanguage()) + " (";
                    if (projectile.getShooter() instanceof Player)
                    {
                        if (projectile.getShooter() == myPet.getOwner().getPlayer())
                        {
                            killer += Locales.getString("Name.You", myPet.getOwner().getLanguage());
                        }
                        else
                        {
                            killer += ((Player) projectile.getShooter()).getName();
                        }
                    }
                    else
                    {
                        if (MyPetType.isLeashableEntityType(e.getDamager().getType()))
                        {
                            killer = Locales.getString("Name." + capitalizeName(MyPetType.getMyPetTypeByEntityType(e.getDamager().getType()).getTypeName()), myPet.getOwner().getLanguage());
                        }
                        else
                        {
                            killer = Locales.getString("Name." + capitalizeName(e.getDamager().getType().getName()), myPet.getOwner().getLanguage());
                        }
                    }
                    killer += ")";
                }
                else
                {
                    if (MyPetType.isLeashableEntityType(e.getDamager().getType()))
                    {
                        killer = Locales.getString("Name." + capitalizeName(MyPetType.getMyPetTypeByEntityType(e.getDamager().getType()).getTypeName()), myPet.getOwner().getLanguage());
                    }
                    else
                    {
                        killer = Locales.getString("Name." + capitalizeName(e.getDamager().getType().getName()), myPet.getOwner().getLanguage());
                    }
                }
            }
            else
            {
                if (event.getEntity().getLastDamageCause() != null)
                {
                    killer = Locales.getString("Name." + capitalizeName(event.getEntity().getLastDamageCause().getCause().name()), myPet.getOwner().getLanguage());
                }
                else
                {
                    killer = Locales.getString("Name.Unknow", myPet.getOwner().getLanguage());
                }
            }
            myPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.DeathMessage", myPet.getOwner().getLanguage()), myPet.getPetName(), killer));
        }
    }

    private static String capitalizeName(String name)
    {
        Validate.notNull(name, "Name can't be null");

        name = name.replace("_", " ");
        name = WordUtils.capitalizeFully(name);
        name = name.replace(" ", "");
        return name;
    }
}