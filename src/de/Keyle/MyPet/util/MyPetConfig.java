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
import de.Keyle.MyPet.entity.types.cavespider.MyCaveSpider;
import de.Keyle.MyPet.entity.types.chicken.MyChicken;
import de.Keyle.MyPet.entity.types.cow.MyCow;
import de.Keyle.MyPet.entity.types.irongolem.MyIronGolem;
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
    public static boolean levelSystem = true;
    public static boolean hungerSystem = true;
    public static boolean sendMetrics = true;
    public static boolean useTowny = true;
    public static boolean useFactions = true;
    public static boolean useWorldGuard = true;
    public static boolean useCitizens = true;

    public static boolean superperms = false;
    public static boolean debugLogger = true;

    public static void setDefault()
    {
        setProperty("MyPet.RespawnTime.Factor", 5);
        setProperty("MyPet.RespawnTime.Fixed", 0);
        setProperty("MyPet.SuperPerms", false);
        setProperty("MyPet.LevelSystem", true);
        setProperty("MyPet.HungerSystem.Active", true);
        setProperty("MyPet.HungerSystem.Time", 60);
        setProperty("MyPet.SendMetrics", true);
        setProperty("MyPet.DebugLogger", true);
        setProperty("MyPet.AutoSaveTime", 60);
        setProperty("MyPet.Support.Towny", true);
        setProperty("MyPet.Support.Factions", true);
        setProperty("MyPet.Support.WorldGuard", true);
        setProperty("MyPet.Support.Citizens", true);

        setProperty("MyPet.Leash.Item", 287);
        setProperty("MyPet.Skill.Control.Item", 287);
        setProperty("MyPet.Skill.Pickup.RangePerLvl", 1);
        setProperty("MyPet.Skill.Poison.ChancePerLevel", 5);
        setProperty("MyPet.Skill.HPregeneration.Time", 60);
        setProperty("MyPet.Skill.Inventory.Creative", true);

        setProperty("MyPet.pets.CaveSpider.HP", 20);
        setProperty("MyPet.pets.Chicken.HP", 20);
        setProperty("MyPet.pets.Cow.HP", 20);
        setProperty("MyPet.pets.IronGolem.HP", 20);
        setProperty("MyPet.pets.Mooshroom.HP", 20);
        setProperty("MyPet.pets.Ocelot.HP", 20);
        setProperty("MyPet.pets.Pig.HP", 20);
        setProperty("MyPet.pets.PigZombie.HP", 20);
        setProperty("MyPet.pets.Sheep.HP", 20);
        setProperty("MyPet.pets.Silverfish.HP", 20);
        setProperty("MyPet.pets.Skeleton.HP", 20);
        setProperty("MyPet.pets.Slime.HP", 20);
        setProperty("MyPet.pets.Spider.HP", 20);
        setProperty("MyPet.pets.Villager.HP", 20);
        setProperty("MyPet.pets.Wolf.HP", 20);
        setProperty("MyPet.pets.Zombie.HP", 20);

        setProperty("MyPet.pets.CaveSpider.damage", 4);
        setProperty("MyPet.pets.Chicken.damage", 4);
        setProperty("MyPet.pets.Cow.damage", 4);
        setProperty("MyPet.pets.IronGolem.damage", 4);
        setProperty("MyPet.pets.Mooshroom.damage", 4);
        setProperty("MyPet.pets.Ocelot.damage", 4);
        setProperty("MyPet.pets.Pig.damage", 4);
        setProperty("MyPet.pets.PigZombie.damage", 4);
        setProperty("MyPet.pets.Sheep.damage", 4);
        setProperty("MyPet.pets.Silverfish.damage", 4);
        setProperty("MyPet.pets.Skeleton.damage", 4);
        setProperty("MyPet.pets.Slime.damage", 4);
        setProperty("MyPet.pets.Spider.damage", 4);
        setProperty("MyPet.pets.Villager.damage", 4);
        setProperty("MyPet.pets.Wolf.damage", 4);
        setProperty("MyPet.pets.Zombie.damage", 4);

        setProperty("MyPet.exp.passive.PercentPerMonster", 25);
        for (EntityType entityType : MyPetExperience.mobExp.keySet())
        {
            setProperty("MyPet.exp.active." + entityType.getName() + ".min", MyPetExperience.mobExp.get(entityType).getMin());
            setProperty("MyPet.exp.active." + entityType.getName() + ".max", MyPetExperience.mobExp.get(entityType).getMax());
        }

        MyPetPlugin.getPlugin().saveConfig();
    }

    public static void loadConfiguration()
    {
        leashItem = MyPetUtil.checkMaterial(config.getInt("MyPet.Leash.Item", 287), Material.STRING);
        Control.item = MyPetUtil.checkMaterial(config.getInt("MyPet.Skill.Control.Item", 287), Material.STRING);
        Pickup.rangePerLevel = config.getDouble("MyPet.Skill.Pickup.RangePerLvl", 1.0);
        HPregeneration.healtregenTime = config.getInt("MyPet.Skill.HPregeneration.Time", 60);
        Poison.chancePerLevel = config.getInt("MyPet.Skill.Poison.ChancePerLevel", 5);
        Inventory.creative = config.getBoolean("MyPet.Skill.Inventory.Creative", true);

        passivePercentPerMonster = config.getInt("MyPet.exp.passive.PercentPerMonster", 25);
        respawnTimeFactor = config.getInt("MyPet.RespawnTime.Factor", 5);
        respawnTimeFixed = config.getInt("MyPet.RespawnTime.Fixed", 0);
        levelSystem = config.getBoolean("MyPet.LevelSystem", true);
        hungerSystem = config.getBoolean("MyPet.HungerSystem.Active", true);
        hungerSystemTime = config.getInt("MyPet.HungerSystem.Time", 60);
        superperms = config.getBoolean("MyPet.SuperPerms", false);
        sendMetrics = config.getBoolean("MyPet.SendMetrics", true);
        debugLogger = config.getBoolean("MyPet.DebugLogger", false);
        useTowny = config.getBoolean("MyPet.Support.Towny", true);
        useFactions = config.getBoolean("MyPet.Support.Factions", true);
        useWorldGuard = config.getBoolean("MyPet.Support.WorldGuard", true);
        useCitizens = config.getBoolean("MyPet.Support.Citizens", true);

        MyPet.setStartHP(MyCaveSpider.class, config.getInt("MyPet.pets.CaveSpider.HP", 20));
        MyPet.setStartHP(MyChicken.class, config.getInt("MyPet.pets.Chicken.HP", 20));
        MyPet.setStartHP(MyCow.class, config.getInt("MyPet.pets.Cow.HP", 20));
        MyPet.setStartHP(MyIronGolem.class, config.getInt("MyPet.pets.IronGolem.HP", 20));
        MyPet.setStartHP(MyMooshroom.class, config.getInt("MyPet.pets.Mooshroom.HP", 20));
        MyPet.setStartHP(MyOcelot.class, config.getInt("MyPet.pets.Ocelot.HP", 20));
        MyPet.setStartHP(MyPig.class, config.getInt("MyPet.pets.Pig.HP", 20));
        MyPet.setStartHP(MyPigZombie.class, config.getInt("MyPet.pets.PigZombie.HP", 20));
        MyPet.setStartHP(MySheep.class, config.getInt("MyPet.pets.Sheep.HP", 20));
        MyPet.setStartHP(MySilverfish.class, config.getInt("MyPet.pets.Silverfish.HP", 20));
        MyPet.setStartHP(MySkeleton.class, config.getInt("MyPet.pets.Skeleton.HP", 20));
        MyPet.setStartHP(MySlime.class, config.getInt("MyPet.pets.Slime.HP", 20));
        MyPet.setStartHP(MySpider.class, config.getInt("MyPet.pets.Spider.HP", 20));
        MyPet.setStartHP(MyVillager.class, config.getInt("MyPet.pets.Villager.HP", 20));
        MyPet.setStartHP(MyWolf.class, config.getInt("MyPet.pets.Wolf.HP", 20));
        MyPet.setStartHP(MyZombie.class, config.getInt("MyPet.pets.Zombie.HP", 20));

        MyPet.setStartDamage(MyCaveSpider.class, config.getInt("MyPet.pets.CaveSpider.damage", 4));
        MyPet.setStartDamage(MyChicken.class, config.getInt("MyPet.pets.Chicken.damage", 4));
        MyPet.setStartDamage(MyCow.class, config.getInt("MyPet.pets.Cow.damage", 4));
        MyPet.setStartDamage(MyIronGolem.class, config.getInt("MyPet.pets.IronGolem.damage", 4));
        MyPet.setStartDamage(MyMooshroom.class, config.getInt("MyPet.pets.Mooshroom.damage", 4));
        MyPet.setStartDamage(MyOcelot.class, config.getInt("MyPet.pets.Ocelot.damage", 4));
        MyPet.setStartDamage(MyPig.class, config.getInt("MyPet.pets.Pig.damage", 4));
        MyPet.setStartDamage(MyPigZombie.class, config.getInt("MyPet.pets.PigZombie.damage", 4));
        MyPet.setStartDamage(MySheep.class, config.getInt("MyPet.pets.Sheep.damage", 4));
        MyPet.setStartDamage(MySilverfish.class, config.getInt("MyPet.pets.Silverfish.damage", 4));
        MyPet.setStartDamage(MySkeleton.class, config.getInt("MyPet.pets.Skeleton.damage", 4));
        MyPet.setStartDamage(MySlime.class, config.getInt("MyPet.pets.Slime.damage", 4));
        MyPet.setStartDamage(MySpider.class, config.getInt("MyPet.pets.Spider.damage", 4));
        MyPet.setStartDamage(MyVillager.class, config.getInt("MyPet.pets.Villager.damage", 4));
        MyPet.setStartDamage(MyWolf.class, config.getInt("MyPet.pets.Wolf.damage", 4));
        MyPet.setStartDamage(MyZombie.class, config.getInt("MyPet.pets.Zombie.damage", 4));

        if (config.getStringList("MyPet.exp.active") != null)
        {
            int min;
            int max;
            for (EntityType entityType : MyPetExperience.mobExp.keySet())
            {
                min = 0;
                max = 0;
                if (config.contains("MyPet.exp.active." + entityType.getName() + ".max"))
                {
                    max = config.getInt("MyPet.exp.active." + entityType.getName() + ".max", 0);
                }
                if (config.contains("MyPet.exp.active." + entityType.getName() + ".min"))
                {
                    min = config.getInt("MyPet.exp.active." + entityType.getName() + ".min", 0);
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
}