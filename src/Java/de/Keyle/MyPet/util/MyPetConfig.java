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

package de.Keyle.MyPet.util;

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.LeashFlag;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.entity.types.bat.MyBat;
import de.Keyle.MyPet.entity.types.cavespider.MyCaveSpider;
import de.Keyle.MyPet.entity.types.chicken.MyChicken;
import de.Keyle.MyPet.entity.types.cow.MyCow;
import de.Keyle.MyPet.entity.types.creeper.MyCreeper;
import de.Keyle.MyPet.entity.types.enderman.MyEnderman;
import de.Keyle.MyPet.entity.types.irongolem.MyIronGolem;
import de.Keyle.MyPet.entity.types.magmacube.MyMagmaCube;
import de.Keyle.MyPet.entity.types.mooshroom.MyMooshroom;
import de.Keyle.MyPet.entity.types.ocelot.MyOcelot;
import de.Keyle.MyPet.entity.types.pig.MyPig;
import de.Keyle.MyPet.entity.types.pigzombie.MyPigZombie;
import de.Keyle.MyPet.entity.types.sheep.MySheep;
import de.Keyle.MyPet.entity.types.silverfish.MySilverfish;
import de.Keyle.MyPet.entity.types.skeleton.MySkeleton;
import de.Keyle.MyPet.entity.types.slime.MySlime;
import de.Keyle.MyPet.entity.types.spider.MySpider;
import de.Keyle.MyPet.entity.types.villager.MyVillager;
import de.Keyle.MyPet.entity.types.wolf.MyWolf;
import de.Keyle.MyPet.entity.types.zombie.MyZombie;
import de.Keyle.MyPet.skill.MyPetExperience;
import de.Keyle.MyPet.skill.skills.*;
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
    public static boolean automaticSkilltreeAssignment = true;
    public static boolean ownerCanAttackPet = false;
    public static boolean levelSystem = true;
    public static boolean hungerSystem = true;
    public static boolean sendMetrics = true;
    public static boolean checkForUpdates = false;
    public static boolean useTowny = true;
    public static boolean useFactions = true;
    public static boolean useWorldGuard = true;
    public static boolean useCitizens = true;
    public static boolean useHeroes = true;

    public static boolean superperms = false;
    public static boolean debugLogger = true;

    public static void setDefault()
    {
        setProperty("MyPet.RespawnTime.Factor", 5);
        setProperty("MyPet.RespawnTime.Fixed", 0);
        setProperty("MyPet.SuperPerms", false);
        setProperty("MyPet.OwnerCanAttackPet", false);
        setProperty("MyPet.LevelSystem", true);
        setProperty("MyPet.HungerSystem.Active", true);
        setProperty("MyPet.HungerSystem.Time", 60);
        setProperty("MyPet.SendMetrics", true);
        setProperty("MyPet.CheckForUpdates", false);
        setProperty("MyPet.DebugLogger", true);
        setProperty("MyPet.AutoSaveTime", 60);
        setProperty("MyPet.Skilltree.AutomaticAssignment", true);
        setProperty("MyPet.Support.Towny", true);
        setProperty("MyPet.Support.Heroes", true);
        setProperty("MyPet.Support.Factions", true);
        setProperty("MyPet.Support.WorldGuard", true);
        setProperty("MyPet.Support.Citizens", true);
        setProperty("MyPet.Exp.Passive.PercentPerMonster", 25);
        setProperty("MyPet.Exp.loss.Percent", 0);
        setProperty("MyPet.Exp.loss.Fixed", 0.0);

        setProperty("MyPet.Leash.Item", 287);
        setProperty("MyPet.Skill.Control.Item", 287);
        setProperty("MyPet.Skill.Ride.Item", 287);
        setProperty("MyPet.Skill.Ride.SpeedPerLevel", 0.2F);
        setProperty("MyPet.Skill.Pickup.RangePerLvl", 1);
        setProperty("MyPet.Skill.Poison.ChancePerLevel", 5);
        setProperty("MyPet.Skill.HPregeneration.Time", 60);
        setProperty("MyPet.Skill.Inventory.Creative", true);
        setProperty("MyPet.Skill.Behavior.Aggro", true);
        setProperty("MyPet.Skill.Behavior.Farm", true);
        setProperty("MyPet.Skill.Behavior.Friendly", true);
        setProperty("MyPet.Skill.Behavior.Raid", true);

        for (MyPetType petType : MyPetType.values())
        {
            setProperty("MyPet.Pets." + petType.getTypeName() + ".Damage", 2);
            setProperty("MyPet.Pets." + petType.getTypeName() + ".HP", 20);
        }

        setProperty("MyPet.Pets.Bat.Food", Material.SPIDER_EYE.getId());
        setProperty("MyPet.Pets.CaveSpider.Food", Material.ROTTEN_FLESH.getId());
        setProperty("MyPet.Pets.Chicken.Food", Material.SEEDS.getId());
        setProperty("MyPet.Pets.Cow.Food", Material.WHEAT.getId());
        setProperty("MyPet.Pets.Creeper.Food", Material.SULPHUR.getId());
        setProperty("MyPet.Pets.Enderman.Food", Material.SOUL_SAND.getId());
        setProperty("MyPet.Pets.IronGolem.Food", Material.IRON_INGOT.getId());
        setProperty("MyPet.Pets.MagmaCube.Food", Material.REDSTONE.getId());
        setProperty("MyPet.Pets.Mooshroom.Food", Material.WHEAT.getId());
        setProperty("MyPet.Pets.Ocelot.Food", Material.RAW_FISH.getId());
        setProperty("MyPet.Pets.Pig.Food", Material.CARROT_ITEM.getId());
        setProperty("MyPet.Pets.PigZombie.Food", Material.ROTTEN_FLESH.getId());
        setProperty("MyPet.Pets.Sheep.Food", Material.WHEAT.getId());
        setProperty("MyPet.Pets.Silverfish.Food", Material.SUGAR.getId());
        setProperty("MyPet.Pets.Skeleton.Food", Material.BONE.getId());
        setProperty("MyPet.Pets.Slime.Food", Material.SUGAR.getId());
        setProperty("MyPet.Pets.Spider.Food", Material.ROTTEN_FLESH.getId());
        setProperty("MyPet.Pets.Villager.Food", Material.APPLE.getId());
        setProperty("MyPet.Pets.Wolf.Food", Material.RAW_BEEF.getId() + "," + Material.RAW_CHICKEN.getId());
        setProperty("MyPet.Pets.Zombie.Food", Material.ROTTEN_FLESH.getId());

        setProperty("MyPet.Pets.Bat.LeashFlags", LeashFlag.LowHp.name());
        setProperty("MyPet.Pets.CaveSpider.LeashFlags", LeashFlag.LowHp.name());
        setProperty("MyPet.Pets.Chicken.LeashFlags", LeashFlag.Baby.name());
        setProperty("MyPet.Pets.Cow.LeashFlags", LeashFlag.Baby.name());
        setProperty("MyPet.Pets.Creeper.LeashFlags", LeashFlag.LowHp.name());
        setProperty("MyPet.Pets.Enderman.LeashFlags", LeashFlag.LowHp.name());
        setProperty("MyPet.Pets.IronGolem.LeashFlags", LeashFlag.UserCreated.name());
        setProperty("MyPet.Pets.MagmaCube.LeashFlags", LeashFlag.LowHp.name());
        setProperty("MyPet.Pets.Mooshroom.LeashFlags", LeashFlag.Baby.name());
        setProperty("MyPet.Pets.Ocelot.LeashFlags", LeashFlag.Tamed.name());
        setProperty("MyPet.Pets.Pig.LeashFlags", LeashFlag.Baby.name());
        setProperty("MyPet.Pets.PigZombie.LeashFlags", LeashFlag.LowHp.name());
        setProperty("MyPet.Pets.Sheep.LeashFlags", LeashFlag.Baby.name());
        setProperty("MyPet.Pets.Silverfish.LeashFlags", LeashFlag.LowHp.name());
        setProperty("MyPet.Pets.Skeleton.LeashFlags", LeashFlag.LowHp.name());
        setProperty("MyPet.Pets.Slime.LeashFlags", LeashFlag.LowHp.name());
        setProperty("MyPet.Pets.Spider.LeashFlags", LeashFlag.LowHp.name());
        setProperty("MyPet.Pets.Villager.LeashFlags", LeashFlag.Baby.name());
        setProperty("MyPet.Pets.Wolf.LeashFlags", LeashFlag.Tamed.name());
        setProperty("MyPet.Pets.Zombie.LeashFlags", LeashFlag.LowHp.name() + ", " + LeashFlag.Adult.name());

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
        Ride.speedPerLevel = (float) config.getDouble("MyPet.Skill.Ride.SpeedPerLevel", 0.2);
        Pickup.rangePerLevel = config.getDouble("MyPet.Skill.Pickup.RangePerLvl", 1.0);
        HPregeneration.healtregenTime = config.getInt("MyPet.Skill.HPregeneration.Time", 60);
        Poison.chancePerLevel = config.getInt("MyPet.Skill.Poison.ChancePerLevel", 5);
        Inventory.creative = config.getBoolean("MyPet.Skill.Inventory.Creative", true);
        Behavior.BehaviorState.Aggressive.setActive(config.getBoolean("MyPet.Skill.Behavior.Aggro", true));
        Behavior.BehaviorState.Farm.setActive(config.getBoolean("MyPet.Skill.Behavior.Farm", true));
        Behavior.BehaviorState.Friendly.setActive(config.getBoolean("MyPet.Skill.Behavior.Friendly", true));
        Behavior.BehaviorState.Raid.setActive(config.getBoolean("MyPet.Skill.Behavior.Raid", true));

        passivePercentPerMonster = config.getInt("MyPet.exp.passive.PercentPerMonster", 25);
        respawnTimeFactor = config.getInt("MyPet.RespawnTime.Factor", 5);
        respawnTimeFixed = config.getInt("MyPet.RespawnTime.Fixed", 0);
        automaticSkilltreeAssignment = config.getBoolean("MyPet.Skilltree.AutomaticAssignment", true);
        levelSystem = config.getBoolean("MyPet.LevelSystem", true);
        ownerCanAttackPet = config.getBoolean("MyPet.OwnerCanAttackPet", false);
        hungerSystem = config.getBoolean("MyPet.HungerSystem.Active", true);
        hungerSystemTime = config.getInt("MyPet.HungerSystem.Time", 60);
        superperms = config.getBoolean("MyPet.SuperPerms", false);
        sendMetrics = config.getBoolean("MyPet.SendMetrics", true);
        checkForUpdates = config.getBoolean("MyPet.CheckForUpdates", false);
        debugLogger = config.getBoolean("MyPet.DebugLogger", false);
        useTowny = config.getBoolean("MyPet.Support.Towny", true);
        useFactions = config.getBoolean("MyPet.Support.Factions", true);
        useWorldGuard = config.getBoolean("MyPet.Support.WorldGuard", true);
        useCitizens = config.getBoolean("MyPet.Support.Citizens", true);
        useHeroes = config.getBoolean("MyPet.Support.Heroes", true);

        MyPetExperience.lossPercent = config.getInt("MyPet.Exp.loss.Percent");
        MyPetExperience.lossFixed = config.getDouble("MyPet.Exp.loss.Fixed");

        for (MyPetType petType : MyPetType.values())
        {
            MyPet.setStartHP(petType.getMyPetClass(), config.getInt("MyPet.Pets." + petType.getTypeName() + ".HP", 20));
            MyPet.setStartDamage(petType.getMyPetClass(), config.getInt("MyPet.Pets." + petType.getTypeName() + ".Damage", 2));
        }

        seperateFood(MyBat.class, config.getString("MyPet.Pets.Bat.Food", "375"));
        seperateFood(MyCaveSpider.class, config.getString("MyPet.Pets.CaveSpider.Food", "367"));
        seperateFood(MyChicken.class, config.getString("MyPet.Pets.Chicken.Food", "295"));
        seperateFood(MyCow.class, config.getString("MyPet.Pets.Cow.Food", "296"));
        seperateFood(MyCreeper.class, config.getString("MyPet.Pets.Creeper.Food", "289"));
        seperateFood(MyEnderman.class, config.getString("MyPet.Pets.Enderman.Food", "88"));
        seperateFood(MyIronGolem.class, config.getString("MyPet.Pets.IronGolem.Food", "265"));
        seperateFood(MyMagmaCube.class, config.getString("MyPet.Pets.MagmaCube.Food", "331"));
        seperateFood(MyMooshroom.class, config.getString("MyPet.Pets.Mooshroom.Food", "296"));
        seperateFood(MyOcelot.class, config.getString("MyPet.Pets.Ocelot.Food", "249"));
        seperateFood(MyPig.class, config.getString("MyPet.Pets.Pig.Food", "391"));
        seperateFood(MyPigZombie.class, config.getString("MyPet.Pets.PigZombie.Food", "367"));
        seperateFood(MySheep.class, config.getString("MyPet.Pets.Sheep.Food", "296"));
        seperateFood(MySilverfish.class, config.getString("MyPet.Pets.Silverfish.Food", "353"));
        seperateFood(MySkeleton.class, config.getString("MyPet.Pets.Skeleton.Food", "352"));
        seperateFood(MySlime.class, config.getString("MyPet.Pets.Slime.Food", "353"));
        seperateFood(MySpider.class, config.getString("MyPet.Pets.Spider.Food", "367"));
        seperateFood(MyVillager.class, config.getString("MyPet.Pets.Villager.Food", "260"));
        seperateFood(MyWolf.class, config.getString("MyPet.Pets.Wolf.Food", "363,365"));
        seperateFood(MyZombie.class, config.getString("MyPet.Pets.Zombie.Food", "367"));

        seperateLeashFlags(MyBat.class, config.getString("MyPet.Pets.Bat.LeashFlags", LeashFlag.LowHp.name()));
        seperateLeashFlags(MyCaveSpider.class, config.getString("MyPet.Pets.CaveSpider.LeashFlags", LeashFlag.LowHp.name()));
        seperateLeashFlags(MyChicken.class, config.getString("MyPet.Pets.Chicken.LeashFlags", LeashFlag.Baby.name()));
        seperateLeashFlags(MyCow.class, config.getString("MyPet.Pets.Cow.LeashFlags", LeashFlag.Baby.name()));
        seperateLeashFlags(MyCreeper.class, config.getString("MyPet.Pets.Creeper.LeashFlags", LeashFlag.LowHp.name()));
        seperateLeashFlags(MyEnderman.class, config.getString("MyPet.Pets.Enderman.LeashFlags", LeashFlag.LowHp.name()));
        seperateLeashFlags(MyIronGolem.class, config.getString("MyPet.Pets.IronGolem.LeashFlags", LeashFlag.UserCreated.name()));
        seperateLeashFlags(MyMagmaCube.class, config.getString("MyPet.Pets.MagmaCube.LeashFlags", LeashFlag.LowHp.name()));
        seperateLeashFlags(MyMooshroom.class, config.getString("MyPet.Pets.Mooshroom.LeashFlags", LeashFlag.Baby.name()));
        seperateLeashFlags(MyOcelot.class, config.getString("MyPet.Pets.Ocelot.LeashFlags", LeashFlag.Tamed.name()));
        seperateLeashFlags(MyPig.class, config.getString("MyPet.Pets.Pig.LeashFlags", LeashFlag.Baby.name()));
        seperateLeashFlags(MyPigZombie.class, config.getString("MyPet.Pets.PigZombie.LeashFlags", LeashFlag.LowHp.name()));
        seperateLeashFlags(MySheep.class, config.getString("MyPet.Pets.Sheep.LeashFlags", LeashFlag.Baby.name()));
        seperateLeashFlags(MySilverfish.class, config.getString("MyPet.Pets.Silverfish.LeashFlags", LeashFlag.LowHp.name()));
        seperateLeashFlags(MySkeleton.class, config.getString("MyPet.Pets.Skeleton.LeashFlags", LeashFlag.LowHp.name()));
        seperateLeashFlags(MySlime.class, config.getString("MyPet.Pets.Slime.LeashFlags", LeashFlag.LowHp.name()));
        seperateLeashFlags(MySpider.class, config.getString("MyPet.Pets.Spider.LeashFlags", LeashFlag.LowHp.name()));
        seperateLeashFlags(MyVillager.class, config.getString("MyPet.Pets.Villager.LeashFlags", LeashFlag.Baby.name()));
        seperateLeashFlags(MyWolf.class, config.getString("MyPet.Pets.Wolf.LeashFlags", LeashFlag.Tamed.name()));
        seperateLeashFlags(MyZombie.class, config.getString("MyPet.Pets.Zombie.LeashFlags", LeashFlag.LowHp.name() + ", " + LeashFlag.Adult.name()));

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
                    MyPetUtil.getLogger().info(leashFlagString + " is not a valid LeashFlag!");
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
                MyPetUtil.getLogger().info(leashFlagString + " is not a valid LeashFlag!");
                MyPetUtil.getDebugLogger().info(leashFlagString + " is not a valid LeashFlag!");
            }
        }
    }
}