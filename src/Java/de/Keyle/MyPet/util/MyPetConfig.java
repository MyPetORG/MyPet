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
import de.Keyle.MyPet.skill.MyPetExperience;
import de.Keyle.MyPet.skill.skills.*;
import de.Keyle.MyPet.util.logger.MyPetLogger;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;

public class MyPetConfig
{
    public static FileConfiguration config;

    public static Material leashItem = Material.STRING;
    public static int passivePercentPerMonster = 25;
    public static int respawnTimeFactor = 5;
    public static int respawnTimeFixed = 0;
    public static int autoSaveTime = 60;
    public static int hungerSystemTime = 60;
    public static int skilltreeSwitchPenaltyPercent = 5;
    public static double skilltreeSwitchPenaltyFixed = 0.0;
    public static double respawnCostFactor = 1.0;
    public static double respawnCostFixed = 0.0;
    public static boolean skilltreeSwitchPenaltyAdmin = false;
    public static boolean automaticSkilltreeAssignment = true;
    public static boolean chooseSkilltreeOnce = true;
    public static boolean ownerCanAttackPet = false;
    public static boolean levelSystem = true;
    public static boolean hungerSystem = true;
    public static boolean sendMetrics = true;
    public static boolean checkForUpdates = false;
    public static boolean superperms = false;
    public static boolean debugLogger = true;
    public static boolean inheritAlreadyInheritedSkills = false;

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

        setProperty("MyPet.Leash.Item", 287);
        setProperty("MyPet.Skill.Control.Item", 287);
        setProperty("MyPet.Skill.Ride.Item", 287);
        setProperty("MyPet.Skill.HPregeneration.Time", 60);
        setProperty("MyPet.Skill.Inventory.Creative", true);
        setProperty("MyPet.Skill.Behavior.Aggro", true);
        setProperty("MyPet.Skill.Behavior.Farm", true);
        setProperty("MyPet.Skill.Behavior.Friendly", true);
        setProperty("MyPet.Skill.Behavior.Raid", true);
        setProperty("MyPet.Skill.Beacon.HungerDecreaseTime", 100);

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
        leashItem = MyPetUtil.checkMaterial(config.getInt("MyPet.Leash.Item", 287), Material.STRING);
        Control.item = MyPetUtil.checkMaterial(config.getInt("MyPet.Skill.Control.Item", 287), Material.STRING);
        Ride.item = MyPetUtil.checkMaterial(config.getInt("MyPet.Skill.Ride.Item", 287), Material.STRING);
        Beacon.hungerDecreaseTime = config.getInt("MyPet.Skill.Beacon.HungerDecreaseTime", 100);
        HPregeneration.healtregenTime = config.getInt("MyPet.Skill.HPregeneration.Time", 60);
        Inventory.creative = config.getBoolean("MyPet.Skill.Inventory.Creative", true);
        Behavior.BehaviorState.Aggressive.setActive(config.getBoolean("MyPet.Skill.Behavior.Aggro", true));
        Behavior.BehaviorState.Farm.setActive(config.getBoolean("MyPet.Skill.Behavior.Farm", true));
        Behavior.BehaviorState.Friendly.setActive(config.getBoolean("MyPet.Skill.Behavior.Friendly", true));
        Behavior.BehaviorState.Raid.setActive(config.getBoolean("MyPet.Skill.Behavior.Raid", true));

        skilltreeSwitchPenaltyFixed = config.getDouble("MyPet.Skilltree.SwitchPenaltyFixed", 0.0);
        skilltreeSwitchPenaltyPercent = config.getInt("MyPet.Skilltree.SwitchPenaltyPercent", 5);
        skilltreeSwitchPenaltyAdmin = config.getBoolean("MyPet.Skilltree.SwitchPenaltyAdmin", false);
        inheritAlreadyInheritedSkills = config.getBoolean("MyPet.Skilltree.InheritAlreadyInheritedSkills", false);
        passivePercentPerMonster = config.getInt("MyPet.exp.passive.PercentPerMonster", 25);
        respawnTimeFactor = config.getInt("MyPet.Respawn.Time.Factor", 5);
        respawnTimeFixed = config.getInt("MyPet.Respawn.Time.Fixed", 0);
        respawnCostFactor = config.getDouble("MyPet.Respawn.EconomyCost.Factor", 1.0);
        respawnCostFixed = config.getDouble("MyPet.Respawn.EconomyCost.Fixed", 0.0);
        automaticSkilltreeAssignment = config.getBoolean("MyPet.Skilltree.AutomaticAssignment", true);
        chooseSkilltreeOnce = config.getBoolean("MyPet.Skilltree.ChooseOnce", true);
        levelSystem = config.getBoolean("MyPet.LevelSystem", true);
        ownerCanAttackPet = config.getBoolean("MyPet.OwnerCanAttackPet", false);
        hungerSystem = config.getBoolean("MyPet.HungerSystem.Active", true);
        hungerSystemTime = config.getInt("MyPet.HungerSystem.Time", 60);
        superperms = config.getBoolean("MyPet.Permissions.SuperPerms", false);
        MyPetPermissions.useExtendedPermissions = config.getBoolean("MyPet.Permissions.UseExtendedPermissions", false);
        sendMetrics = config.getBoolean("MyPet.SendMetrics", true);
        checkForUpdates = config.getBoolean("MyPet.CheckForUpdates", false);
        debugLogger = config.getBoolean("MyPet.DebugLogger", false);
        MyPetEconomy.useEconomy = config.getBoolean("MyPet.Support.Vault.Economy", true);
        MyPetPvP.useTowny = config.getBoolean("MyPet.Support.Towny", true);
        MyPetPvP.useFactions = config.getBoolean("MyPet.Support.Factions", true);
        MyPetPvP.useWorldGuard = config.getBoolean("MyPet.Support.WorldGuard", true);
        MyPetPvP.useCitizens = config.getBoolean("MyPet.Support.Citizens", true);
        MyPetPvP.useHeroes = config.getBoolean("MyPet.Support.Heroes", true);
        MyPetPvP.useMcMMO = config.getBoolean("MyPet.Support.mcMMO", true);
        MyPetPvP.useMobArena = config.getBoolean("MyPet.Support.MobArena", true);
        MyPetPvP.useRegios = config.getBoolean("MyPet.Support.Regios", true);
        MyPetPvP.useResidence = config.getBoolean("MyPet.Support.Residence", true);
        MyPetPvP.useAncientRPG = config.getBoolean("MyPet.Support.AncientRPG", true);

        MyPetExperience.lossPercent = config.getInt("MyPet.Exp.loss.Percent");
        MyPetExperience.lossFixed = config.getDouble("MyPet.Exp.loss.Fixed");

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
        for(Material foodType : foodTypes)
        {
            if(!linkedFood.equalsIgnoreCase(""))
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
        for(LeashFlag leashFlag : leashFlags)
        {
            if(!linkedLeashFlags.equalsIgnoreCase(""))
            {
                linkedLeashFlags += ",";
            }
            linkedLeashFlags+= leashFlag.name();
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