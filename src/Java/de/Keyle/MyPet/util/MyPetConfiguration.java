/*
 * Copyright (C) 2011-2013 Keyle
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

package de.Keyle.MyPet.util;

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.entity.MyPetInfo;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.LeashFlag;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.entity.types.chicken.EntityMyChicken;
import de.Keyle.MyPet.entity.types.cow.EntityMyCow;
import de.Keyle.MyPet.entity.types.mooshroom.EntityMyMooshroom;
import de.Keyle.MyPet.entity.types.ocelot.EntityMyOcelot;
import de.Keyle.MyPet.entity.types.pig.EntityMyPig;
import de.Keyle.MyPet.entity.types.sheep.EntityMySheep;
import de.Keyle.MyPet.entity.types.villager.EntityMyVillager;
import de.Keyle.MyPet.entity.types.wolf.EntityMyWolf;
import de.Keyle.MyPet.entity.types.zombie.EntityMyZombie;
import de.Keyle.MyPet.skill.MyPetExperience;
import de.Keyle.MyPet.skill.skills.*;
import de.Keyle.MyPet.util.logger.MyPetLogger;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;

public class MyPetConfiguration
{
    public static FileConfiguration config;

    public static Material LEASH_ITEM = Material.STRING;
    public static int PASSIVE_PERCENT_PER_MONSTER = 25;
    public static int RESPAWN_TIME_FACTOR = 5;
    public static int RESPAWN_TIME_FIXED = 0;
    public static int AUTOSAVE_TIME = 60;
    public static int HUNGER_SYSTEM_TIME = 60;
    public static int SKILLTREE_SWITCH_PENALTY_PERCENT = 5;
    public static double SKILLTREE_SWITCH_PENALTY_FIXED = 0.0;
    public static double RESPAWN_COSTS_FACTOR = 1.0;
    public static double RESPAWN_COSTS_FIXED = 0.0;
    public static boolean SKILLTREE_SWITCH_PENALTY_ADMIN = false;
    public static boolean AUTOMATIC_SKILLTREE_ASSIGNMENT = true;
    public static boolean CHOOSE_SKILLTREE_ONLY_ONCE = true;
    public static boolean OWNER_CAN_ATTACK_PET = false;
    public static boolean USE_LEVEL_SYSTEM = true;
    public static boolean USE_HUNGER_SYSTEM = true;
    public static boolean SEND_METRICS = true;
    public static boolean CHECK_FOR_UPDATES = false;
    public static boolean USE_SUPERPERMS = false;
    public static boolean USE_DEBUG_LOGGER = true;
    public static boolean INHERIT_ALREADY_INHERITED_SKILLS = false;

    public static void setDefault()
    {
        setProperty("MyPet.Respawn.Time.Factor", 5);
        setProperty("MyPet.Respawn.Time.Fixed", 0);
        setProperty("MyPet.Respawn.EconomyCost.Fixed", 0.0);
        setProperty("MyPet.Respawn.EconomyCost.Factor", 1.0);
        setProperty("MyPet.Permissions.SuperPerms", false);
        setProperty("MyPet.Permissions.UseExtendedPermissions", false);
        setProperty("MyPet.OwnerCanAttackPet", false);
        setProperty("MyPet.LevelSystem", true);
        setProperty("MyPet.HungerSystem.Active", true);
        setProperty("MyPet.HungerSystem.Time", 60);
        setProperty("MyPet.SendMetrics", true);
        setProperty("MyPet.CheckForUpdates", false);
        setProperty("MyPet.DebugLogger", true);
        setProperty("MyPet.AutoSaveTime", 60);
        setProperty("MyPet.Skilltree.AutomaticAssignment", true);
        setProperty("MyPet.Skilltree.InheritAlreadyInheritedSkills", true);
        setProperty("MyPet.Skilltree.ChooseOnce", true);
        setProperty("MyPet.Skilltree.SwitchPenaltyFixed", 0.0);
        setProperty("MyPet.Skilltree.SwitchPenaltyPercent", 5);
        setProperty("MyPet.Skilltree.SwitchPenaltyAdmin", false);
        setProperty("MyPet.Support.Towny", true);
        setProperty("MyPet.Support.Heroes", true);
        setProperty("MyPet.Support.Factions", true);
        setProperty("MyPet.Support.WorldGuard", true);
        setProperty("MyPet.Support.Citizens", true);
        setProperty("MyPet.Support.mcMMO", true);
        setProperty("MyPet.Support.Regios", true);
        setProperty("MyPet.Support.MobArena", true);
        setProperty("MyPet.Support.Residence", true);
        setProperty("MyPet.Support.AncientRPG", true);
        setProperty("MyPet.Support.Vault.Economy", true);
        setProperty("MyPet.Exp.Passive.PercentPerMonster", 25);
        setProperty("MyPet.Exp.loss.Percent", 0);
        setProperty("MyPet.Exp.loss.Fixed", 0.0);
        setProperty("MyPet.Leash.Item", LEASH_ITEM.getId());

        setProperty("MyPet.Skill.Control.Item", Control.ITEM.getId());
        setProperty("MyPet.Skill.Ride.Item", Ride.ITEM.getId());
        setProperty("MyPet.Skill.HPregeneration.Time", 60);
        setProperty("MyPet.Skill.Inventory.Creative", true);
        setProperty("MyPet.Skill.Behavior.Aggro", true);
        setProperty("MyPet.Skill.Behavior.Farm", true);
        setProperty("MyPet.Skill.Behavior.Friendly", true);
        setProperty("MyPet.Skill.Behavior.Raid", true);
        setProperty("MyPet.Skill.Beacon.HungerDecreaseTime", 100);

        setProperty("MyPet.Pets.Chicken.CanLayEggs", true);
        setProperty("MyPet.Pets.Cow.CanGiveMilk", true);
        setProperty("MyPet.Pets.Sheep.CanBeSheared", true);
        setProperty("MyPet.Pets.Chicken.GrowUpItem", Material.POTION.getId());
        setProperty("MyPet.Pets.Cow.GrowUpItem", Material.POTION.getId());
        setProperty("MyPet.Pets.Mooshroom.GrowUpItem", Material.POTION.getId());
        setProperty("MyPet.Pets.Ocelot.GrowUpItem", Material.POTION.getId());
        setProperty("MyPet.Pets.Pig.GrowUpItem", Material.POTION.getId());
        setProperty("MyPet.Pets.Sheep.GrowUpItem", Material.POTION.getId());
        setProperty("MyPet.Pets.Villager.GrowUpItem", Material.POTION.getId());
        setProperty("MyPet.Pets.Wolf.GrowUpItem", Material.POTION.getId());
        setProperty("MyPet.Pets.Zombie.GrowUpItem", Material.POTION.getId());

        for (MyPetType petType : MyPetType.values())
        {
            MyPetInfo pi = petType.getMyPetClass().getAnnotation(MyPetInfo.class);

            setProperty("MyPet.Pets." + petType.getTypeName() + ".Damage", pi.damage());
            setProperty("MyPet.Pets." + petType.getTypeName() + ".HP", pi.hp());
            setProperty("MyPet.Pets." + petType.getTypeName() + ".Speed", pi.walkSpeed());
            setProperty("MyPet.Pets." + petType.getTypeName() + ".Food", linkFood(pi.food()));
            setProperty("MyPet.Pets." + petType.getTypeName() + ".LeashFlags", linkLeashFlags(pi.leashFlags()));
        }

        for (EntityType entityType : MyPetExperience.mobExp.keySet())
        {
            setProperty("MyPet.Exp.Active." + entityType.getName() + ".Min", MyPetExperience.mobExp.get(entityType).getMin());
            setProperty("MyPet.Exp.Active." + entityType.getName() + ".Max", MyPetExperience.mobExp.get(entityType).getMax());
        }

        MyPetPlugin.getPlugin().saveConfig();
    }

    public static void loadConfiguration()
    {
        LEASH_ITEM = MyPetUtil.checkMaterial(config.getInt("MyPet.Leash.Item", 287), Material.STRING);
        Control.ITEM = MyPetUtil.checkMaterial(config.getInt("MyPet.Skill.Control.Item", 287), Material.STRING);
        Ride.ITEM = MyPetUtil.checkMaterial(config.getInt("MyPet.Skill.Ride.Item", 287), Material.STRING);
        Beacon.HUNGER_DECREASE_TIME = config.getInt("MyPet.Skill.Beacon.HungerDecreaseTime", 100);
        HPregeneration.START_REGENERATION_TIME = config.getInt("MyPet.Skill.HPregeneration.Time", 60);
        Inventory.OPEN_IN_CREATIVEMODE = config.getBoolean("MyPet.Skill.Inventory.Creative", true);
        Behavior.BehaviorState.Aggressive.setActive(config.getBoolean("MyPet.Skill.Behavior.Aggro", true));
        Behavior.BehaviorState.Farm.setActive(config.getBoolean("MyPet.Skill.Behavior.Farm", true));
        Behavior.BehaviorState.Friendly.setActive(config.getBoolean("MyPet.Skill.Behavior.Friendly", true));
        Behavior.BehaviorState.Raid.setActive(config.getBoolean("MyPet.Skill.Behavior.Raid", true));

        SKILLTREE_SWITCH_PENALTY_FIXED = config.getDouble("MyPet.Skilltree.SwitchPenaltyFixed", 0.0);
        SKILLTREE_SWITCH_PENALTY_PERCENT = config.getInt("MyPet.Skilltree.SwitchPenaltyPercent", 5);
        SKILLTREE_SWITCH_PENALTY_ADMIN = config.getBoolean("MyPet.Skilltree.SwitchPenaltyAdmin", false);
        INHERIT_ALREADY_INHERITED_SKILLS = config.getBoolean("MyPet.Skilltree.InheritAlreadyInheritedSkills", false);
        PASSIVE_PERCENT_PER_MONSTER = config.getInt("MyPet.exp.passive.PercentPerMonster", 25);
        RESPAWN_TIME_FACTOR = config.getInt("MyPet.Respawn.Time.Factor", 5);
        RESPAWN_TIME_FIXED = config.getInt("MyPet.Respawn.Time.Fixed", 0);
        RESPAWN_COSTS_FACTOR = config.getDouble("MyPet.Respawn.EconomyCost.Factor", 1.0);
        RESPAWN_COSTS_FIXED = config.getDouble("MyPet.Respawn.EconomyCost.Fixed", 0.0);
        AUTOMATIC_SKILLTREE_ASSIGNMENT = config.getBoolean("MyPet.Skilltree.AutomaticAssignment", true);
        CHOOSE_SKILLTREE_ONLY_ONCE = config.getBoolean("MyPet.Skilltree.ChooseOnce", true);
        USE_LEVEL_SYSTEM = config.getBoolean("MyPet.LevelSystem", true);
        OWNER_CAN_ATTACK_PET = config.getBoolean("MyPet.OwnerCanAttackPet", false);
        USE_HUNGER_SYSTEM = config.getBoolean("MyPet.HungerSystem.Active", true);
        HUNGER_SYSTEM_TIME = config.getInt("MyPet.HungerSystem.Time", 60);
        USE_SUPERPERMS = config.getBoolean("MyPet.Permissions.SuperPerms", false);
        SEND_METRICS = config.getBoolean("MyPet.SendMetrics", true);
        CHECK_FOR_UPDATES = config.getBoolean("MyPet.CheckForUpdates", false);
        USE_DEBUG_LOGGER = config.getBoolean("MyPet.DebugLogger", false);
        MyPetPermissions.USE_EXTENDET_PERMISSIONS = config.getBoolean("MyPet.Permissions.UseExtendedPermissions", false);
        MyPetEconomy.USE_ECONOMY = config.getBoolean("MyPet.Support.Vault.Economy", true);
        MyPetPvP.USE_Towny = config.getBoolean("MyPet.Support.Towny", true);
        MyPetPvP.USE_Factions = config.getBoolean("MyPet.Support.Factions", true);
        MyPetPvP.USE_WorldGuard = config.getBoolean("MyPet.Support.WorldGuard", true);
        MyPetPvP.USE_Citizens = config.getBoolean("MyPet.Support.Citizens", true);
        MyPetPvP.USE_Heroes = config.getBoolean("MyPet.Support.Heroes", true);
        MyPetPvP.USE_McMMO = config.getBoolean("MyPet.Support.mcMMO", true);
        MyPetPvP.USE_MobArena = config.getBoolean("MyPet.Support.MobArena", true);
        MyPetPvP.USE_Regios = config.getBoolean("MyPet.Support.Regios", true);
        MyPetPvP.USE_Residence = config.getBoolean("MyPet.Support.Residence", true);
        MyPetPvP.USE_AncientRPG = config.getBoolean("MyPet.Support.AncientRPG", true);

        MyPetExperience.LOSS_PERCENT = config.getInt("MyPet.Exp.loss.Percent");
        MyPetExperience.LOSS_FIXED = config.getDouble("MyPet.Exp.loss.Fixed");

        EntityMyChicken.CAN_LAY_EGGS = config.getBoolean("MyPet.Pets.Chicken.CanLayEggs", true);
        EntityMyCow.CAN_GIVE_MILK = config.getBoolean("MyPet.Pets.Cow.CanGiveMilk", true);
        EntityMySheep.CAN_BE_SHEARED = config.getBoolean("MyPet.Pets.Sheep.CanBeSheared", true);
        EntityMyChicken.GROW_UP_ITEM = MyPetUtil.checkMaterial(config.getInt("MyPet.Pets.Chicken.GrowUpItem", Material.POTION.getId()), Material.POTION);
        EntityMyCow.GROW_UP_ITEM = MyPetUtil.checkMaterial(config.getInt("MyPet.Pets.Cow.GrowUpItem", Material.POTION.getId()), Material.POTION);
        EntityMyMooshroom.GROW_UP_ITEM = MyPetUtil.checkMaterial(config.getInt("MyPet.Pets.Mooshroom.GrowUpItem", Material.POTION.getId()), Material.POTION);
        EntityMyOcelot.GROW_UP_ITEM = MyPetUtil.checkMaterial(config.getInt("MyPet.Pets.Ocelot.GrowUpItem", Material.POTION.getId()), Material.POTION);
        EntityMyPig.GROW_UP_ITEM = MyPetUtil.checkMaterial(config.getInt("MyPet.Pets.Pig.GrowUpItem", Material.POTION.getId()), Material.POTION);
        EntityMySheep.GROW_UP_ITEM = MyPetUtil.checkMaterial(config.getInt("MyPet.Pets.Sheep.GrowUpItem", Material.POTION.getId()), Material.POTION);
        EntityMyVillager.GROW_UP_ITEM = MyPetUtil.checkMaterial(config.getInt("MyPet.Pets.Villager.GrowUpItem", Material.POTION.getId()), Material.POTION);
        EntityMyWolf.GROW_UP_ITEM = MyPetUtil.checkMaterial(config.getInt("MyPet.Pets.Wolf.GrowUpItem", Material.POTION.getId()), Material.POTION);
        EntityMyZombie.GROW_UP_ITEM = MyPetUtil.checkMaterial(config.getInt("MyPet.Pets.Zombie.GrowUpItem", Material.POTION.getId()), Material.POTION);

        for (MyPetType petType : MyPetType.values())
        {
            MyPetInfo pi = petType.getMyPetClass().getAnnotation(MyPetInfo.class);

            MyPet.setStartHP(petType.getMyPetClass(), config.getInt("MyPet.Pets." + petType.getTypeName() + ".HP", pi.hp()));
            MyPet.setStartDamage(petType.getMyPetClass(), config.getInt("MyPet.Pets." + petType.getTypeName() + ".Damage", pi.damage()));
            MyPet.setStartSpeed(petType.getMyPetClass(), (float) config.getDouble("MyPet.Pets." + petType.getTypeName() + ".Speed", pi.walkSpeed()));
            seperateFood(petType.getMyPetClass(), config.getString("MyPet.Pets." + petType.getTypeName() + ".Food", linkFood(pi.food())));
            seperateLeashFlags(petType.getMyPetClass(), config.getString("MyPet.Pets." + petType.getTypeName() + ".LeashFlags", linkLeashFlags(pi.leashFlags())));
        }

        if (config.getStringList("MyPet.exp.active") != null)
        {
            double min;
            double max;
            for (EntityType entityType : MyPetExperience.mobExp.keySet())
            {
                min = 0;
                max = 0;
                if (config.contains("MyPet.Exp.Active." + entityType.getName() + ".Max"))
                {
                    max = config.getDouble("MyPet.Exp.Active." + entityType.getName() + ".Max", 0.);
                }
                if (config.contains("MyPet.Exp.Active." + entityType.getName() + ".Min"))
                {
                    min = config.getDouble("MyPet.Exp.Active." + entityType.getName() + ".Min", 0.);
                }
                if (min == max)
                {
                    MyPetExperience.mobExp.get(entityType).setExp(max);
                }
                else
                {
                    MyPetExperience.mobExp.get(entityType).setMin(min);
                    MyPetExperience.mobExp.get(entityType).setMax(max);
                }
            }
        }
    }

    public static void setProperty(String key, Object value)
    {
        if (!config.contains(key))
        {
            config.set(key, value);
        }
    }

    private static String linkFood(Material[] foodTypes)
    {
        String linkedFood = "";
        for (Material foodType : foodTypes)
        {
            if (!linkedFood.equalsIgnoreCase(""))
            {
                linkedFood += ",";
            }
            linkedFood += foodType.getId();
        }
        return linkedFood;
    }

    private static void seperateFood(Class<? extends MyPet> myPetClass, String foodString)
    {
        foodString = foodString.replaceAll("\\s", "");
        if (foodString.contains(","))
        {
            for (String foodIDString : foodString.split(","))
            {
                if (MyPetUtil.isInt(foodIDString))
                {
                    int itemID = Integer.parseInt(foodIDString);
                    if (MyPetUtil.isValidMaterial(itemID) && itemID != 0)
                    {
                        MyPet.setFood(myPetClass, Material.getMaterial(itemID));
                    }
                }
            }
        }
        else
        {
            if (MyPetUtil.isInt(foodString))
            {
                int itemID = Integer.parseInt(foodString);
                if (MyPetUtil.isValidMaterial(itemID) && itemID != 0)
                {
                    MyPet.setFood(myPetClass, Material.getMaterial(itemID));
                }
            }
        }
    }

    private static String linkLeashFlags(LeashFlag[] leashFlags)
    {
        String linkedLeashFlags = "";
        for (LeashFlag leashFlag : leashFlags)
        {
            if (!linkedLeashFlags.equalsIgnoreCase(""))
            {
                linkedLeashFlags += ",";
            }
            linkedLeashFlags += leashFlag.name();
        }
        return linkedLeashFlags;
    }

    private static void seperateLeashFlags(Class<? extends MyPet> myPetClass, String leashFlagString)
    {
        leashFlagString = leashFlagString.replaceAll("\\s", "");
        if (leashFlagString.contains(","))
        {
            for (String leashFlagSplit : leashFlagString.split(","))
            {
                if (LeashFlag.getLeashFlagByName(leashFlagSplit) != null)
                {
                    MyPet.setLeashFlags(myPetClass, LeashFlag.getLeashFlagByName(leashFlagSplit));
                }
                else
                {
                    MyPetLogger.write(ChatColor.RED + leashFlagString + " is not a valid LeashFlag!");
                    MyPetUtil.getDebugLogger().info(leashFlagString + " is not a valid LeashFlag!");
                }
            }
        }
        else
        {
            if (LeashFlag.getLeashFlagByName(leashFlagString) != null)
            {
                MyPet.setLeashFlags(myPetClass, LeashFlag.getLeashFlagByName(leashFlagString));
            }
            else
            {
                MyPetLogger.write(ChatColor.RED + leashFlagString + " is not a valid LeashFlag!");
                MyPetUtil.getDebugLogger().info(leashFlagString + " is not a valid LeashFlag!");
            }
        }
    }
}