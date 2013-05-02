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
import de.Keyle.MyPet.chatcommands.CommandInfo;
import de.Keyle.MyPet.chatcommands.CommandInfo.PetInfoDisplay;
import de.Keyle.MyPet.entity.EquipmentSlot;
import de.Keyle.MyPet.entity.ai.movement.MyPetAIRide;
import de.Keyle.MyPet.entity.ai.target.MyPetAIDuelTarget;
import de.Keyle.MyPet.entity.types.*;
import de.Keyle.MyPet.entity.types.MyPet.LeashFlag;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.entity.types.enderman.EntityMyEnderman;
import de.Keyle.MyPet.event.MyPetLeashEvent;
import de.Keyle.MyPet.skill.MyPetExperience;
import de.Keyle.MyPet.skill.MyPetMonsterExperience;
import de.Keyle.MyPet.skill.skills.implementation.*;
import de.Keyle.MyPet.skill.skills.implementation.Behavior.BehaviorState;
import de.Keyle.MyPet.skill.skills.implementation.Wither;
import de.Keyle.MyPet.skill.skills.implementation.inventory.ItemStackNBTConverter;
import de.Keyle.MyPet.util.*;
import de.Keyle.MyPet.util.logger.DebugLogger;
import net.minecraft.server.v1_5_R2.MathHelper;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_5_R2.entity.CraftEnderman;
import org.bukkit.craftbukkit.v1_5_R2.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_5_R2.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_5_R2.entity.CraftSkeleton;
import org.bukkit.craftbukkit.v1_5_R2.inventory.CraftItemStack;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.spout.nbt.*;

import java.util.*;

import static org.bukkit.Bukkit.getPluginManager;

public class MyPetEntityListener implements Listener
{
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onMyPetEntitySpawn(final CreatureSpawnEvent event)
    {
        if (event.getEntity() instanceof CraftMyPet)
        {
            event.setCancelled(false);
        }
        if (MyPetConfiguration.USE_LEVEL_SYSTEM && event.getSpawnReason() == SpawnReason.SPAWNER)
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
    public void onEntityDamageByLightning(final EntityDamageByEntityEvent event)
    {
        if (event.getCause() == DamageCause.LIGHTNING)
        {
            LightningStrike bolt = (LightningStrike) event.getDamager();
            if (Lightning.isSkillLightning(bolt))
            {
                MyPet boltMyPet = Lightning.lightningList.get(bolt);
                if (event.getEntity() instanceof CraftMyPet)
                {
                    MyPet myPet = ((CraftMyPet) event.getEntity()).getMyPet();
                    if (boltMyPet == myPet)
                    {
                        event.setCancelled(true);
                    }
                    else if (!MyPetPvP.canHurt(boltMyPet.getOwner().getPlayer(), myPet.getOwner().getPlayer()))
                    {
                        event.setCancelled(true);
                    }
                }
                else if (event.getEntity() instanceof Player)
                {
                    Player victim = (Player) event.getEntity();
                    if (boltMyPet.getOwner().getPlayer() == victim)
                    {
                        event.setCancelled(true);
                    }
                    else if (!MyPetPvP.canHurt(boltMyPet.getOwner().getPlayer(), victim))
                    {
                        event.setCancelled(true);
                    }
                }
                else if (event.getEntity() instanceof Tameable)
                {
                    Tameable tameable = (Tameable) event.getEntity();
                    if (boltMyPet.getOwner().equals(tameable.getOwner()))
                    {
                        event.setCancelled(true);
                    }
                }
                else if (event.getEntity() instanceof LivingEntity)
                {
                    LivingEntity entity = (LivingEntity) event.getEntity();
                    event.setCancelled(true);
                    entity.damage(event.getDamage(), boltMyPet.getCraftPet());
                }
            }
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
                if (craftMyPet.getHandle().hasRider() && myPet.getOwner().equals(damager))
                {
                    event.setCancelled(true);
                    if (myPet.getSkills().hasSkill("Ride"))
                    {
                        if (craftMyPet.getHandle().petPathfinderSelector.hasGoal("Ride"))
                        {
                            ((MyPetAIRide) craftMyPet.getHandle().petPathfinderSelector.getGoal("Ride")).toggleRiding();
                        }
                    }
                }
                else if (damager.getItemInHand().getType() == MyPetConfiguration.LEASH_ITEM)
                {
                    MyPetPlayer myPetPlayer = MyPetPlayer.getMyPetPlayer(damager);

                    boolean infoShown = false;
                    if (CommandInfo.canSee(PetInfoDisplay.Name.adminOnly, myPetPlayer, myPet))
                    {
                        damager.sendMessage(MyPetBukkitUtil.setColors("%aqua%%petname%%white%:").replace("%petname%", myPet.petName));
                        infoShown = true;
                    }
                    if (CommandInfo.canSee(PetInfoDisplay.Owner.adminOnly, myPetPlayer, myPet) && myPet.getOwner() != myPetPlayer)
                    {
                        damager.sendMessage(MyPetBukkitUtil.setColors("   %N_Owner%: %owner%").replace("%owner%", myPet.getOwner().getName()).replace("%N_Owner%", MyPetLanguage.getString("Name_Owner")));
                        infoShown = true;
                    }
                    if (CommandInfo.canSee(PetInfoDisplay.HP.adminOnly, myPetPlayer, myPet))
                    {
                        String msg;
                        if (myPet.getHealth() > myPet.getMaxHealth() / 3 * 2)
                        {
                            msg = "" + ChatColor.GREEN + myPet.getHealth() + ChatColor.WHITE + "/" + myPet.getMaxHealth();
                        }
                        else if (myPet.getHealth() > myPet.getMaxHealth() / 3)
                        {
                            msg = "" + ChatColor.YELLOW + myPet.getHealth() + ChatColor.WHITE + "/" + myPet.getMaxHealth();
                        }
                        else
                        {
                            msg = "" + ChatColor.RED + myPet.getHealth() + ChatColor.WHITE + "/" + myPet.getMaxHealth();
                        }
                        damager.sendMessage(MyPetBukkitUtil.setColors("   %N_HP%: %hp%").replace("%petname%", myPet.petName).replace("%hp%", msg).replace("%N_HP%", MyPetLanguage.getString("Name_HP")));
                        infoShown = true;
                    }
                    if (!myPet.isPassiv() && CommandInfo.canSee(PetInfoDisplay.Damage.adminOnly, myPetPlayer, myPet))
                    {
                        int damage = (myPet.getSkills().isSkillActive("Damage") ? ((Damage) myPet.getSkills().getSkill("Damage")).getDamageIncrease() : 0);
                        damager.sendMessage(MyPetBukkitUtil.setColors("   %N_Damage%: %dmg%").replace("%petname%", myPet.petName).replace("%dmg%", "" + damage).replace("%N_Damage%", MyPetLanguage.getString("Name_Damage")));
                        infoShown = true;
                    }
                    if (myPet.getRangedDamage() > 0 && CommandInfo.canSee(PetInfoDisplay.RangedDamage.adminOnly, myPetPlayer, myPet))
                    {
                        int damage = myPet.getDamage();
                        damager.sendMessage(MyPetBukkitUtil.setColors("   %N_RangedDamage%: %dmg%").replace("%petname%", myPet.petName).replace("%dmg%", "" + damage).replace("%N_RangedDamage%", MyPetLanguage.getString("Name_RangedDamage")));
                        infoShown = true;
                    }
                    if (MyPetConfiguration.USE_HUNGER_SYSTEM && CommandInfo.canSee(PetInfoDisplay.Hunger.adminOnly, myPetPlayer, myPet))
                    {
                        damager.sendMessage(MyPetBukkitUtil.setColors("   %N_Hunger%: %hunger%").replace("%hunger%", "" + myPet.getHungerValue()).replace("%N_Hunger%", MyPetLanguage.getString("Name_Hunger")));
                        infoShown = true;
                    }
                    if (CommandInfo.canSee(PetInfoDisplay.Skilltree.adminOnly, myPetPlayer, myPet) && myPet.getSkillTree() != null)
                    {
                        damager.sendMessage(MyPetBukkitUtil.setColors("   %N_Skilltree%: %name%").replace("%name%", "" + myPet.getSkillTree().getDisplayName()).replace("%N_Skilltree%", MyPetLanguage.getString("Name_Skilltree")));
                        infoShown = true;
                    }
                    if (MyPetConfiguration.USE_LEVEL_SYSTEM)
                    {
                        if (CommandInfo.canSee(PetInfoDisplay.Level.adminOnly, myPetPlayer, myPet))
                        {
                            int lvl = myPet.getExperience().getLevel();
                            damager.sendMessage(MyPetBukkitUtil.setColors("   %N_Level%: %lvl%").replace("%lvl%", "" + lvl).replace("%N_Level%", MyPetLanguage.getString("Name_Level")));
                            infoShown = true;
                        }
                        if (CommandInfo.canSee(PetInfoDisplay.Exp.adminOnly, myPetPlayer, myPet))
                        {
                            double exp = myPet.getExperience().getCurrentExp();
                            double reqEXP = myPet.getExperience().getRequiredExp();
                            damager.sendMessage(MyPetBukkitUtil.setColors("   %N_Exp%: %exp%/%reqexp%").replace("%exp%", String.format("%1.2f", exp)).replace("%reqexp%", String.format("%1.2f", reqEXP)).replace("%N_Exp%", MyPetLanguage.getString("Name_Exp")));
                            infoShown = true;
                        }
                    }
                    if (!infoShown)
                    {
                        damager.sendMessage(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_NothingToSeeHere")));
                    }

                    event.setCancelled(true);
                }
                else if (myPet.getOwner().equals(damager) && (!MyPetConfiguration.OWNER_CAN_ATTACK_PET || !MyPetPvP.canHurt(myPet.getOwner().getPlayer())))
                {
                    event.setCancelled(true);
                }
                else if (!myPet.getOwner().equals(damager) && !MyPetPvP.canHurt(damager, myPet.getOwner().getPlayer()))
                {
                    event.setCancelled(true);
                }
            }
            if (event.getDamager() instanceof LivingEntity)
            {
                if (!event.isCancelled() && myPet.getSkills().isSkillActive("Thorns"))
                {
                    Thorns thornsSkill = ((Thorns) myPet.getSkills().getSkill("Thorns"));
                    if (thornsSkill.activate())
                    {
                        ((LivingEntity) event.getDamager()).damage((int) (event.getDamage() / 2 + 0.5), event.getEntity());
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
                    if (damager.getItemInHand().getType() != MyPetConfiguration.LEASH_ITEM || !MyPetPermissions.has(damager, "MyPet.user.leash." + MyPetType.getMyPetTypeByEntityType(leashTarget.getType()).getTypeName()))
                    {
                        return;
                    }

                    boolean willBeLeashed = true;
                    List<LeashFlag> leashFlags = MyPet.getLeashFlags(MyPetType.getMyPetTypeByEntityType(leashTarget.getType()).getMyPetClass());

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
                                willBeLeashed = ((IronGolem) leashTarget).isPlayerCreated();
                            }
                        }
                        else if (flag == LeashFlag.Wild)
                        {
                            if (leashTarget instanceof IronGolem)
                            {
                                willBeLeashed = !((IronGolem) leashTarget).isPlayerCreated();
                            }
                            else if (leashTarget instanceof Tameable)
                            {
                                willBeLeashed = !((Tameable) leashTarget).isTamed();
                            }
                        }
                        else if (flag == LeashFlag.Tamed)
                        {
                            if (leashTarget instanceof Tameable)
                            {
                                willBeLeashed = ((Tameable) leashTarget).isTamed();
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
                        inactiveMyPet.setPetName(MyPetType.getMyPetTypeByEntityType(leashTarget.getType()).getTypeName());
                        inactiveMyPet.setLocation(leashTarget.getLocation());

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
                        else if (leashTarget instanceof Zombie)
                        {
                            extendedInfo.getValue().put("Baby", new ByteTag("Baby", ((Zombie) leashTarget).isBaby()));
                            extendedInfo.getValue().put("Villager", new ByteTag("Villager", ((Zombie) leashTarget).isVillager()));

                            Random random = ((CraftLivingEntity) leashTarget).getHandle().aE();
                            List<CompoundTag> equipmentList = new ArrayList<CompoundTag>();
                            if (random.nextFloat() <= leashTarget.getEquipment().getChestplateDropChance())
                            {
                                ItemStack itemStack = leashTarget.getEquipment().getChestplate();
                                if (itemStack != null && itemStack.getType() != Material.AIR)
                                {
                                    net.minecraft.server.v1_5_R2.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(itemStack);
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
                                    net.minecraft.server.v1_5_R2.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(itemStack);
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
                                    net.minecraft.server.v1_5_R2.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(itemStack);
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
                                    net.minecraft.server.v1_5_R2.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(itemStack);
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

                            Random random = ((CraftLivingEntity) leashTarget).getHandle().aE();
                            List<CompoundTag> equipmentList = new ArrayList<CompoundTag>();
                            if (random.nextFloat() <= leashTarget.getEquipment().getChestplateDropChance())
                            {
                                ItemStack itemStack = leashTarget.getEquipment().getChestplate();
                                if (itemStack != null && itemStack.getType() != Material.AIR)
                                {
                                    net.minecraft.server.v1_5_R2.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(itemStack);
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
                                    net.minecraft.server.v1_5_R2.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(itemStack);
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
                                    net.minecraft.server.v1_5_R2.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(itemStack);
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
                                    net.minecraft.server.v1_5_R2.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(itemStack);
                                    CompoundTag item = ItemStackNBTConverter.ItemStackToCompund(nmsItemStack);
                                    item.getValue().put("Slot", new IntTag("Slot", EquipmentSlot.Boots.getSlotId()));
                                    equipmentList.add(item);
                                }
                            }
                            extendedInfo.getValue().put("Equipment", new ListTag<CompoundTag>("Equipment", CompoundTag.class, equipmentList));
                        }
                        else if (leashTarget instanceof PigZombie)
                        {
                            Random random = ((CraftLivingEntity) leashTarget).getHandle().aE();
                            List<CompoundTag> equipmentList = new ArrayList<CompoundTag>();
                            if (random.nextFloat() <= leashTarget.getEquipment().getChestplateDropChance())
                            {
                                ItemStack itemStack = leashTarget.getEquipment().getChestplate();
                                if (itemStack != null && itemStack.getType() != Material.AIR)
                                {
                                    net.minecraft.server.v1_5_R2.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(itemStack);
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
                                    net.minecraft.server.v1_5_R2.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(itemStack);
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
                                    net.minecraft.server.v1_5_R2.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(itemStack);
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
                                    net.minecraft.server.v1_5_R2.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(itemStack);
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

                        if (MyPetConfiguration.ENABLE_EVENTS)
                        {
                            getPluginManager().callEvent(new MyPetLeashEvent(myPet));
                        }
                        DebugLogger.info("New Pet leashed:");
                        DebugLogger.info("   " + myPet.toString());
                        if (MyPetConfiguration.STORE_PETS_ON_PET_LEASH)
                        {
                            DebugLogger.info(MyPetPlugin.getPlugin().savePets(false) + " pet(s) saved.");
                        }
                        damager.sendMessage(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_AddLeash")));
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

                myPet.removePet();
                myPet.setLocation(myPet.getOwner().getPlayer().getLocation());

                switch (myPet.createPet())
                {
                    case Success:
                        myPet.sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_Call")).replace("%petname%", myPet.petName));
                        break;
                    case Canceled:
                        myPet.sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_SpawnPrevent")).replace("%petname%", myPet.petName));
                        break;
                    case NoSpace:
                        myPet.sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_SpawnNoSpace")).replace("%petname%", myPet.petName));
                        break;
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDamageByEntityDamageMonitor(final EntityDamageByEntityEvent event)
    {
        if (MyPetExperience.DAMAGE_WEIGHTED_EXPERIENCE_DISTRIBUTION && event.getEntity() instanceof LivingEntity && !(event.getEntity() instanceof Player))
        {
            Map<Entity, Integer> damageMap;
            if (event.getEntity().hasMetadata("DamageCount"))
            {
                for (MetadataValue value : event.getEntity().getMetadata("DamageCount"))
                {
                    if (value.getOwningPlugin() == MyPetPlugin.getPlugin())
                    {
                        damageMap = (Map<Entity, Integer>) value.value();
                        if (damageMap.containsKey(event.getDamager()))
                        {
                            int oldDamage = damageMap.get(event.getDamager());
                            damageMap.put(event.getDamager(), ((LivingEntity) event.getEntity()).getHealth() < event.getDamage() ? ((LivingEntity) event.getEntity()).getHealth() + oldDamage : event.getDamage() + oldDamage);
                        }
                        else
                        {
                            damageMap.put(event.getDamager(), ((LivingEntity) event.getEntity()).getHealth() < event.getDamage() ? ((LivingEntity) event.getEntity()).getHealth() : event.getDamage());
                        }
                        break;
                    }
                }
            }
            else
            {
                damageMap = new HashMap<Entity, Integer>();
                damageMap.put(event.getDamager(), ((LivingEntity) event.getEntity()).getHealth() < event.getDamage() ? ((LivingEntity) event.getEntity()).getHealth() : event.getDamage());
                event.getEntity().setMetadata("DamageCount", new FixedMetadataValue(MyPetPlugin.getPlugin(), damageMap));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDamageByEntityResult(final EntityDamageByEntityEvent event)
    {
        // --  fix unwanted screaming of Endermen --
        if (event.getEntity() instanceof CraftMyPet && ((CraftMyPet) event.getEntity()).getPetType() == MyPetType.Enderman)
        {
            ((EntityMyEnderman) ((CraftMyPet) event.getEntity()).getHandle()).setScreaming(true);
            ((EntityMyEnderman) ((CraftMyPet) event.getEntity()).getHandle()).setScreaming(false);
        }

        if (event.getEntity() instanceof LivingEntity)
        {
            if (event.getDamager() instanceof Player)
            {
                Player damager = (Player) event.getDamager();
                if (damager.getItemInHand().getType() == MyPetConfiguration.LEASH_ITEM && event.getEntity() instanceof CraftMyPet)
                {
                    return;
                }
                if (MyPetList.hasMyPet(damager))
                {
                    MyPet myPet = MyPetList.getMyPet(damager);
                    if (myPet.getStatus() == PetState.Here && event.getEntity() != myPet.getCraftPet())
                    {
                        myPet.getCraftPet().getHandle().goalTarget = ((CraftLivingEntity) event.getEntity()).getHandle();
                    }
                }
            }
            else if (event.getDamager() instanceof CraftMyPet)
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
                        PotionEffect effect = new PotionEffect(PotionEffectType.POISON, poisonSkill.getDuration() * 20, 1);
                        ((LivingEntity) event.getEntity()).addPotionEffect(effect);
                        skillUsed = true;
                    }
                }
                if (!skillUsed && myPet.getSkills().hasSkill("Wither"))
                {
                    Wither witherSkill = (Wither) myPet.getSkills().getSkill("Wither");
                    if (witherSkill.activate())
                    {
                        PotionEffect effect = new PotionEffect(PotionEffectType.WITHER, witherSkill.getDuration() * 20, 1);
                        ((LivingEntity) event.getEntity()).addPotionEffect(effect);
                        skillUsed = true;
                    }
                }
                if (!skillUsed && myPet.getSkills().hasSkill("Fire"))
                {
                    Fire fireSkill = (Fire) myPet.getSkills().getSkill("Fire");
                    if (fireSkill.activate())
                    {
                        event.getEntity().setFireTicks(fireSkill.getDuration() * 20);
                        skillUsed = true;
                    }
                }
                if (!skillUsed && myPet.getSkills().hasSkill("Slow"))
                {
                    Slow slowSkill = (Slow) myPet.getSkills().getSkill("Slow");
                    if (slowSkill.activate())
                    {
                        PotionEffect effect = new PotionEffect(PotionEffectType.SLOW, slowSkill.getDuration() * 20, 1);
                        ((LivingEntity) event.getEntity()).addPotionEffect(effect);
                        skillUsed = true;
                    }
                }
                if (!skillUsed && myPet.getSkills().hasSkill("Knockback"))
                {
                    Knockback knockbackSkill = (Knockback) myPet.getSkills().getSkill("Knockback");
                    if (knockbackSkill.activate())
                    {
                        ((CraftEntity) event.getEntity()).getHandle().g(-MathHelper.sin(myPet.getLocation().getYaw() * 3.141593F / 180.0F) * 2 * 0.5F, 0.1D, MathHelper.cos(myPet.getLocation().getYaw() * 3.141593F / 180.0F) * 2 * 0.5F);
                        skillUsed = true;
                    }
                }
                if (!skillUsed && myPet.getSkills().hasSkill("Lightning"))
                {
                    Lightning lightningSkill = (Lightning) myPet.getSkills().getSkill("Lightning");
                    if (lightningSkill.activate())
                    {
                        Lightning.isStriking = true;
                        LightningStrike bolt = event.getEntity().getLocation().getWorld().strikeLightning(event.getEntity().getLocation());
                        Lightning.lightningList.put(bolt, myPet);
                        Lightning.isStriking = false;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onMyPetEntityDeath(final EntityDeathEvent event)
    {
        if (event.getEntity() instanceof CraftMyPet)
        {
            MyPet myPet = ((CraftMyPet) event.getEntity()).getMyPet();
            if (myPet == null || myPet.getHealth() > 0) // check health for death events where the pet isn't really dead (/killall)
            {
                return;
            }
            myPet.status = PetState.Dead;

            myPet.respawnTime = (MyPetConfiguration.RESPAWN_TIME_FIXED + MyPet.getCustomRespawnTimeFixed(myPet.getClass())) + (myPet.getExperience().getLevel() * (MyPetConfiguration.RESPAWN_TIME_FACTOR + MyPet.getCustomRespawnTimeFactor(myPet.getClass())));

            if (event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent)
            {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event.getEntity().getLastDamageCause();

                if (e.getDamager() instanceof Player)
                {
                    myPet.respawnTime = (MyPetConfiguration.RESPAWN_TIME_PLAYER_FIXED + MyPet.getCustomRespawnTimeFixed(myPet.getClass())) + (myPet.getExperience().getLevel() * (MyPetConfiguration.RESPAWN_TIME_PLAYER_FACTOR + MyPet.getCustomRespawnTimeFactor(myPet.getClass())));
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
                            EntityMyPet myPetEntity = ((CraftMyPet) event.getEntity()).getHandle();
                            EntityMyPet duelKiller = ((CraftMyPet) e.getDamager()).getHandle();
                            if (myPetEntity.petTargetSelector.hasGoal("DuelTarget"))
                            {
                                MyPetAIDuelTarget duelTarget = (MyPetAIDuelTarget) myPetEntity.petTargetSelector.getGoal("DuelTarget");
                                if (duelTarget.getDuelOpponent() == duelKiller)
                                {
                                    myPet.respawnTime = 10;
                                }
                            }
                        }
                    }
                }
            }
            event.setDroppedExp(0);

            if (MyPetConfiguration.USE_LEVEL_SYSTEM && MyPetExperience.LOSS_FIXED > 0 || MyPetExperience.LOSS_PERCENT > 0)
            {
                double lostExpirience = MyPetExperience.LOSS_FIXED;
                lostExpirience += myPet.getExperience().getRequiredExp() * MyPetExperience.LOSS_PERCENT / 100;
                if (lostExpirience > myPet.getExperience().getCurrentExp())
                {
                    lostExpirience = myPet.getExperience().getCurrentExp();
                }
                if (MyPetExperience.DROP_LOST_EXP)
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
            SendDeathMessage(event);
            myPet.sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_RespawnIn").replace("%petname%", myPet.petName).replace("%time%", "" + myPet.respawnTime)));

            if (MyPetEconomy.canUseEconomy() && myPet.getOwner().hasAutoRespawnEnabled() && myPet.respawnTime >= myPet.getOwner().getAutoRespawnMin() && MyPetPermissions.has(myPet.getOwner().getPlayer(), "MyPet.user.respawn"))
            {
                double costs = myPet.respawnTime * MyPetConfiguration.RESPAWN_COSTS_FACTOR + MyPetConfiguration.RESPAWN_COSTS_FIXED;
                if (MyPetEconomy.canPay(myPet.getOwner(), costs))
                {
                    MyPetEconomy.pay(myPet.getOwner(), costs);
                    myPet.sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_RespawnPaid").replace("%costs%", costs + " " + MyPetEconomy.getEconomy().currencyNameSingular()).replace("%petname%", myPet.petName)));
                    myPet.respawnTime = 1;
                }
                else
                {
                    myPet.sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_RespawnNoMoney").replace("%costs%", costs + " " + MyPetEconomy.getEconomy().currencyNameSingular()).replace("%petname%", myPet.petName)));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    @EventHandler
    public void onEntityDeath(final EntityDeathEvent event)
    {
        if (MyPetConfiguration.USE_LEVEL_SYSTEM)
        {
            if (!MyPetExperience.GAIN_EXP_FROM_MONSTER_SPAWNER_MOBS && event.getEntity().hasMetadata("MonsterSpawner"))
            {
                for (MetadataValue value : event.getEntity().getMetadata("MonsterSpawner"))
                {
                    if (value.getOwningPlugin() == MyPetPlugin.getPlugin())
                    {
                        if (value.asBoolean())
                        {
                            return;
                        }
                        break;
                    }
                }
            }
            if (MyPetExperience.DAMAGE_WEIGHTED_EXPERIENCE_DISTRIBUTION)
            {
                if (event.getEntity().hasMetadata("DamageCount"))
                {
                    for (MetadataValue value : event.getEntity().getMetadata("DamageCount"))
                    {
                        if (value.getOwningPlugin() == MyPetPlugin.getPlugin())
                        {
                            Map<Entity, Integer> damageMap = (Map<Entity, Integer>) value.value();
                            for (Entity entity : damageMap.keySet())
                            {
                                if (entity instanceof CraftMyPet)
                                {
                                    MyPet myPet = ((CraftMyPet) entity).getMyPet();
                                    if (MyPetConfiguration.PREVENT_LEVELLING_WITHOUT_SKILLTREE && myPet.getSkillTree() == null)
                                    {
                                        continue;
                                    }
                                    double damage = damageMap.get(entity);
                                    double allDamage = 0;
                                    double randomExp = MyPetMonsterExperience.getMonsterExperience(event.getEntity().getType()).getRandomExp();
                                    for (Integer d : damageMap.values())
                                    {
                                        allDamage += d;
                                    }
                                    //MyPetLogger.write("Exp: " + (damage / allDamage * randomExp) + "/" + randomExp + " (" + (damage / allDamage * 100) + ")");
                                    myPet.getExperience().addExp(damage / allDamage * randomExp);
                                }
                                else if (entity instanceof Player)
                                {
                                    Player owner = (Player) entity;
                                    if (MyPetList.hasMyPet(owner))
                                    {
                                        MyPet myPet = MyPetList.getMyPet(owner);
                                        if (MyPetConfiguration.PREVENT_LEVELLING_WITHOUT_SKILLTREE && myPet.getSkillTree() == null)
                                        {
                                            return;
                                        }
                                        if (myPet.isPassiv())
                                        {
                                            if (myPet.getStatus() == PetState.Here)
                                            {
                                                double damage = damageMap.get(entity);
                                                double allDamage = 0;
                                                double randomExp = MyPetMonsterExperience.getMonsterExperience(event.getEntity().getType()).getRandomExp();
                                                for (Integer d : damageMap.values())
                                                {
                                                    allDamage += d;
                                                }
                                                //MyPetLogger.write("Exp: " + (damage / allDamage * randomExp) + "/" + randomExp + " (" + (damage / allDamage *100) + ")");
                                                myPet.getExperience().addExp(damage / allDamage * randomExp);
                                            }
                                        }
                                    }
                                }
                            }
                            return;
                        }
                    }
                }
            }
            if (event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent)
            {
                EntityDamageByEntityEvent edbee = (EntityDamageByEntityEvent) event.getEntity().getLastDamageCause();
                if (edbee.getDamager() instanceof CraftMyPet)
                {
                    MyPet myPet = ((CraftMyPet) edbee.getDamager()).getMyPet();
                    if (myPet.getSkillTree() == null && MyPetConfiguration.PREVENT_LEVELLING_WITHOUT_SKILLTREE)
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
                        if (MyPetConfiguration.PREVENT_LEVELLING_WITHOUT_SKILLTREE && myPet.getSkillTree() == null)
                        {
                            return;
                        }
                        if (myPet.isPassiv())
                        {
                            if (myPet.getStatus() == PetState.Here)
                            {
                                myPet.getExperience().addExp(event.getEntity().getType(), MyPetConfiguration.PASSIVE_PERCENT_PER_MONSTER);
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

    private void SendDeathMessage(final EntityDeathEvent event)
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
                        killer = MyPetLanguage.getString("Name_You");
                    }
                    else
                    {
                        killer = ((Player) e.getDamager()).getName();
                    }
                }
                else if (e.getDamager().getType() == EntityType.WOLF)
                {
                    Wolf w = (Wolf) e.getDamager();
                    killer = MyPetLanguage.getString("Name_Wolf");
                    if (w.isTamed())
                    {
                        killer += " (" + w.getOwner().getName() + ')';
                    }
                }
                else if (e.getDamager() instanceof CraftMyPet)
                {
                    CraftMyPet craftMyPet = (CraftMyPet) e.getDamager();
                    killer = craftMyPet.getMyPet().petName + " (" + craftMyPet.getOwner().getName() + ')';
                }
                else if (e.getDamager() instanceof Projectile)
                {
                    Projectile projectile = (Projectile) e.getDamager();
                    killer = MyPetLanguage.getString("Name_" + projectile.getType().name()) + " (";
                    if (projectile.getShooter() instanceof Player)
                    {
                        if (projectile.getShooter() == myPet.getOwner().getPlayer())
                        {
                            killer += MyPetLanguage.getString("Name_You");
                        }
                        else
                        {
                            killer += ((Player) projectile.getShooter()).getName();
                        }
                    }
                    else
                    {
                        killer += MyPetLanguage.getString("Name_" + e.getDamager().getType().name());
                    }
                    killer += ")";
                }
                else
                {
                    killer = MyPetLanguage.getString("Name_" + e.getDamager().getType().getName());
                }
            }
            else
            {
                if (event.getEntity().getLastDamageCause() != null)
                {
                    killer = MyPetLanguage.getString("Name_" + event.getEntity().getLastDamageCause().getCause().name());
                }
                else
                {
                    killer = MyPetLanguage.getString("Name_Unknow");
                }
            }
            myPet.sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_DeathMessage")).replace("%petname%", myPet.petName) + killer);
        }
    }
}