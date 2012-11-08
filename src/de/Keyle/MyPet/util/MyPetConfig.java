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
    public static int respawnTimeFactor = 5;
    public static int respawnTimeFixed = 0;
    public static int autoSaveTime = 60;
    public static boolean levelSystem = true;
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

        setProperty("MyPet.StartHP.CaveSpider", 20);
        setProperty("MyPet.StartHP.Chicken", 20);
        setProperty("MyPet.StartHP.Cow", 20);
        setProperty("MyPet.StartHP.IronGolem", 20);
        setProperty("MyPet.StartHP.Mooshroom", 20);
        setProperty("MyPet.StartHP.Ocelot", 20);
        setProperty("MyPet.StartHP.Pig", 20);
        setProperty("MyPet.StartHP.PigZombie", 20);
        setProperty("MyPet.StartHP.Sheep", 20);
        setProperty("MyPet.StartHP.Silverfish", 20);
        setProperty("MyPet.StartHP.Skeleton", 20);
        setProperty("MyPet.StartHP.Slime", 20);
        setProperty("MyPet.StartHP.Spider", 20);
        setProperty("MyPet.StartHP.Villager", 20);
        setProperty("MyPet.StartHP.Wolf", 20);
        setProperty("MyPet.StartHP.Zombie", 20);

        for (EntityType entityType : MyPetExperience.mobExp.keySet())
        {
            setProperty("MyPet.exp." + entityType.getName() + ".min", MyPetExperience.mobExp.get(entityType).getMin());
            setProperty("MyPet.exp." + entityType.getName() + ".max", MyPetExperience.mobExp.get(entityType).getMax());
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

        respawnTimeFactor = config.getInt("MyPet.RespawnTime.Factor", 5);
        respawnTimeFixed = config.getInt("MyPet.RespawnTime.Fixed", 0);
        levelSystem = config.getBoolean("MyPet.LevelSystem", true);
        superperms = config.getBoolean("MyPet.SuperPerms", false);
        sendMetrics = config.getBoolean("MyPet.SendMetrics", true);
        debugLogger = config.getBoolean("MyPet.DebugLogger", false);
        useTowny = config.getBoolean("MyPet.Support.Towny", true);
        useFactions = config.getBoolean("MyPet.Support.Factions", true);
        useWorldGuard = config.getBoolean("MyPet.Support.WorldGuard", true);
        useCitizens = config.getBoolean("MyPet.Support.Citizens", true);

        MyPet.setStartHP(MyCaveSpider.class, config.getInt("MyPet.StartHP.CaveSpider", 20));
        MyPet.setStartHP(MyChicken.class, config.getInt("MyPet.StartHP.Chicken", 20));
        MyPet.setStartHP(MyCow.class, config.getInt("MyPet.StartHP.Cow", 20));
        MyPet.setStartHP(MyIronGolem.class, config.getInt("MyPet.StartHP.IronGolem", 20));
        MyPet.setStartHP(MyMooshroom.class, config.getInt("MyPet.StartHP.Mooshroom", 20));
        MyPet.setStartHP(MyOcelot.class, config.getInt("MyPet.StartHP.Ocelot", 20));
        MyPet.setStartHP(MyPig.class, config.getInt("MyPet.StartHP.Pig", 20));
        MyPet.setStartHP(MyPigZombie.class, config.getInt("MyPet.StartHP.PigZombie", 20));
        MyPet.setStartHP(MySheep.class, config.getInt("MyPet.StartHP.Sheep", 20));
        MyPet.setStartHP(MySilverfish.class, config.getInt("MyPet.StartHP.Silverfish", 20));
        MyPet.setStartHP(MySkeleton.class, config.getInt("MyPet.StartHP.Skeleton", 20));
        MyPet.setStartHP(MySlime.class, config.getInt("MyPet.StartHP.Slime", 20));
        MyPet.setStartHP(MySpider.class, config.getInt("MyPet.StartHP.Spider", 20));
        MyPet.setStartHP(MyVillager.class, config.getInt("MyPet.StartHP.Villager", 20));
        MyPet.setStartHP(MyWolf.class, config.getInt("MyPet.StartHP.Wolf", 20));
        MyPet.setStartHP(MyZombie.class, config.getInt("MyPet.StartHP.Zombie", 20));

        if (config.getStringList("MyPet.exp") != null)
        {
            int min;
            int max;
            for (EntityType entityType : MyPetExperience.mobExp.keySet())
            {
                min = 0;
                max = 0;
                if (config.contains("MyPet.exp." + entityType.getName() + ".max"))
                {
                    max = config.getInt("MyPet.exp." + entityType.getName() + ".max", 0);
                }
                if (config.contains("MyPet.exp." + entityType.getName() + ".min"))
                {
                    min = config.getInt("MyPet.exp." + entityType.getName() + ".min", 0);
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