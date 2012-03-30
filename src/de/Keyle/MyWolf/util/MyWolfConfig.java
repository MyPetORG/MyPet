/*
 * Copyright (C) 2011-2012 Keyle
 *
 * This file is part of MyWolf
 *
 * MyWolf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyWolf is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyWolf. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyWolf.util;

import de.Keyle.MyWolf.MyWolfPlugin;
import de.Keyle.MyWolf.skill.MyWolfExperience;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;

public class MyWolfConfig
{
    public static FileConfiguration Config;

    public static Material LeashItem = Material.STRING;
    public static Material ControlItem = Material.STRING;
    public static double PickupRangePerLevel = 1;
    public static int RespawnTimeFactor = 5;
    public static int RespawnTimeFixed = 0;
    public static int SitdownTime = 15;
    public static int StartHP = 20;
    public static int AutoSaveTime = 60;
    public static boolean LevelSystem = true;
    public static boolean sendMetrics = true;

    public static boolean PermissionsBukkit = false;


    public static void setDefault()
    {
        setProperty("MyWolf.leash.item", 287);
        setProperty("MyWolf.control.item", 287);
        setProperty("MyWolf.pickuprangeperlvl", 1);
        setProperty("MyWolf.respawntime.factor", 5);
        setProperty("MyWolf.respawntime.fixed", 0);
        setProperty("MyWolf.sitdowntime", 15);
        setProperty("MyWolf.hpregendefault", 60);
        setProperty("MyWolf.starthp", 20);
        setProperty("MyWolf.bukkitpermissions", false);
        setProperty("MyWolf.levelsystem", true);
        setProperty("MyWolf.SendMetrics", true);
        setProperty("MyWolf.AutoSaveTime", 60);

        for (EntityType entityType : MyWolfExperience.MobEXP.keySet())
        {
            setProperty("MyWolf.exp." + entityType.getName() + ".min", MyWolfExperience.MobEXP.get(entityType).getMin());
            setProperty("MyWolf.exp." + entityType.getName() + ".max", MyWolfExperience.MobEXP.get(entityType).getMax());
        }

        MyWolfPlugin.getPlugin().saveConfig();
    }

    public static void loadConfiguration()
    {
        LeashItem = MyWolfUtil.checkMaterial(Config.getInt("MyWolf.leash.item", 287), Material.STRING);
        ControlItem = MyWolfUtil.checkMaterial(Config.getInt("MyWolf.control.item", 287), Material.STRING);
        PickupRangePerLevel = Config.getDouble("MyWolf.pickuprangeperlvl", 1.0);
        RespawnTimeFactor = Config.getInt("MyWolf.respawntime.factor", 5);
        RespawnTimeFixed = Config.getInt("MyWolf.respawntime.fixed", 0);
        LevelSystem = Config.getBoolean("MyWolf.levelsystem", true);
        SitdownTime = Config.getInt("MyWolf.sitdowntime", 15);
        StartHP = Config.getInt("MyWolf.starthp", 15);
        PermissionsBukkit = Config.getBoolean("MyWolf.bukkitpermissions", false);
        sendMetrics = Config.getBoolean("MyWolf.SendMetrics", true);

        if (Config.getStringList("MyWolf.exp") != null)
        {
            int min;
            int max;
            for (EntityType entityType : MyWolfExperience.MobEXP.keySet())
            {
                min = 0;
                max = 0;
                if (Config.contains("MyWolf.exp." + entityType.getName() + ".max"))
                {
                    max = Config.getInt("MyWolf.exp." + entityType.getName() + ".max", 0);
                }
                if (Config.contains("MyWolf.exp." + entityType.getName() + ".min"))
                {
                    min = Config.getInt("MyWolf.exp." + entityType.getName() + ".min", 0);
                }
                if (min == max)
                {
                    MyWolfExperience.MobEXP.get(entityType).setExp(max);
                    /*
                    MyWolfUtil.getLogger().info(entityType.getName() + ":");
                    MyWolfUtil.getLogger().info("min: " + MyWolfExperience.MobEXP.get(entityType).getMin() + " | max: " + MyWolfExperience.MobEXP.get(entityType).getMax());
                    MyWolfUtil.getLogger().info("exp-test: " + MyWolfExperience.MobEXP.get(entityType).getRandomExp());
                    MyWolfUtil.getLogger().info("exp-test: " + MyWolfExperience.MobEXP.get(entityType).getRandomExp());
                    MyWolfUtil.getLogger().info("exp-test: " + MyWolfExperience.MobEXP.get(entityType).getRandomExp());
                    MyWolfUtil.getLogger().info("-----");
                    */
                }
                else
                {
                    MyWolfExperience.MobEXP.get(entityType).setMin(min);
                    MyWolfExperience.MobEXP.get(entityType).setMax(max);
                    /*
                    MyWolfUtil.getLogger().info(entityType.getName() + ":");
                    MyWolfUtil.getLogger().info("min: " + MyWolfExperience.MobEXP.get(entityType).getMin() + " | max: " + MyWolfExperience.MobEXP.get(entityType).getMax());
                    MyWolfUtil.getLogger().info("exp-test: " + MyWolfExperience.MobEXP.get(entityType).getRandomExp());
                    MyWolfUtil.getLogger().info("exp-test: " + MyWolfExperience.MobEXP.get(entityType).getRandomExp());
                    MyWolfUtil.getLogger().info("exp-test: " + MyWolfExperience.MobEXP.get(entityType).getRandomExp());
                    MyWolfUtil.getLogger().info("-----");
                    */
                }
            }
        }
    }

    public static void setProperty(String key, Object value)
    {
        if (!Config.contains(key))
        {
            Config.set(key, value);
        }
    }
}
