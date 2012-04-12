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
import de.Keyle.MyWolf.skill.skills.Control;
import de.Keyle.MyWolf.skill.skills.HPregeneration;
import de.Keyle.MyWolf.skill.skills.Pickup;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;

public class MyWolfConfig
{
    public static FileConfiguration Config;

    public static Material LeashItem = Material.STRING;
    public static int RespawnTimeFactor = 5;
    public static int RespawnTimeFixed = 0;
    public static int SitdownTime = 15;
    public static int StartHP = 20;
    public static int AutoSaveTime = 60;
    public static boolean LevelSystem = true;
    //public static boolean HeroesSkill = true;
    //public static Heroes HeroesPlugin = null;
    public static boolean sendMetrics = true;

    public static boolean Superperms = false;
    public static boolean DebugLogger = false;

    public static void setDefault()
    {
        setProperty("MyWolf.Leash.Item", 287);
        setProperty("MyWolf.Skill.Control.Item", 287);
        setProperty("MyWolf.Skill.Pickup.RangePerLvl", 1);
        setProperty("MyWolf.Skill.HPregeneration.Time", 60);
        setProperty("MyWolf.RespawnTime.Factor", 5);
        setProperty("MyWolf.RespawnTime.Fixed", 0);
        setProperty("MyWolf.SitdownTime", 60);
        setProperty("MyWolf.StartHP", 15);
        setProperty("MyWolf.SuperPerms", false);
        setProperty("MyWolf.LevelSystem", true);
        setProperty("MyWolf.SendMetrics", true);
        setProperty("MyWolf.DebugLogger", false);
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
        LeashItem = MyWolfUtil.checkMaterial(Config.getInt("MyWolf.Leash.Item", 287), Material.STRING);
        Control.Item = MyWolfUtil.checkMaterial(Config.getInt("MyWolf.Skill.Control.Item", 287), Material.STRING);
        Pickup.RangePerLevel = Config.getDouble("MyWolf.Skill.Pickup.RangePerLvl", 1.0);
        HPregeneration.HealtregenTime = Config.getInt("MyWolf.Skill.HPregeneration.Time", 60);
        RespawnTimeFactor = Config.getInt("MyWolf.RespawnTime.Factor", 5);
        RespawnTimeFixed = Config.getInt("MyWolf.RespawnTime.Fixed", 0);
        LevelSystem = Config.getBoolean("MyWolf.LevelSystem", true);
        //HeroesSkill = Config.getBoolean("MyWolf.HeroesSkill", false);
        SitdownTime = Config.getInt("MyWolf.SitdownTime", 60);
        StartHP = Config.getInt("MyWolf.StartHP", 15);
        Superperms = Config.getBoolean("MyWolf.SuperPerms", false);
        sendMetrics = Config.getBoolean("MyWolf.SendMetrics", true);
        DebugLogger = Config.getBoolean("MyWolf.DebugLogger", false);

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
                }
                else
                {
                    MyWolfExperience.MobEXP.get(entityType).setMin(min);
                    MyWolfExperience.MobEXP.get(entityType).setMax(max);
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
