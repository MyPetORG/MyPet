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
import de.Keyle.MyPet.entity.types.chicken.MyChicken;
import de.Keyle.MyPet.entity.types.cow.MyCow;
import de.Keyle.MyPet.entity.types.irongolem.MyIronGolem;
import de.Keyle.MyPet.entity.types.mooshroom.MyMooshroom;
import de.Keyle.MyPet.entity.types.ocelot.MyOcelot;
import de.Keyle.MyPet.entity.types.silverfish.MySilverfish;
import de.Keyle.MyPet.entity.types.wolf.MyWolf;
import de.Keyle.MyPet.skill.MyPetExperience;
import de.Keyle.MyPet.skill.skills.Control;
import de.Keyle.MyPet.skill.skills.HPregeneration;
import de.Keyle.MyPet.skill.skills.Pickup;
import de.Keyle.MyPet.skill.skills.Poison;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;

public class MyPetConfig
{
    public static FileConfiguration Config;

    public static Material LeashItem = Material.STRING;
    public static int RespawnTimeFactor = 5;
    public static int RespawnTimeFixed = 0;
    public static int SitdownTime = 15;
    public static int AutoSaveTime = 60;
    public static boolean LevelSystem = true;
    //public static boolean HeroesSkill = true;
    //public static Heroes HeroesPlugin = null;
    public static boolean sendMetrics = true;
    public static boolean useTowny = true;
    public static boolean useFactions = true;
    public static boolean useWorldGuard = true;

    public static boolean Superperms = false;
    public static boolean DebugLogger = false;

    public static void setDefault()
    {
        setProperty("MyPet.Leash.Item", 287);
        setProperty("MyPet.Skill.Control.Item", 287);
        setProperty("MyPet.Skill.Pickup.RangePerLvl", 1);
        setProperty("MyPet.Skill.Poison.ChancePerLevel", 5);
        setProperty("MyPet.Skill.HPregeneration.Time", 60);
        setProperty("MyPet.RespawnTime.Factor", 5);
        setProperty("MyPet.RespawnTime.Fixed", 0);
        setProperty("MyPet.SitdownTime", 60);
        setProperty("MyPet.StartHP.Wolf", 20);
        setProperty("MyPet.StartHP.Chicken", 4);
        setProperty("MyPet.StartHP.Cow", 10);
        setProperty("MyPet.StartHP.Mooshroom", 10);
        setProperty("MyPet.StartHP.Ocelot", 15);
        setProperty("MyPet.StartHP.IronGolem", 15);
        setProperty("MyPet.StartHP.Silverfish", 8);
        setProperty("MyPet.SuperPerms", false);
        setProperty("MyPet.LevelSystem", true);
        setProperty("MyPet.SendMetrics", true);
        setProperty("MyPet.DebugLogger", false);
        setProperty("MyPet.AutoSaveTime", 60);
        setProperty("MyPet.Support.Towny", true);
        setProperty("MyPet.Support.Factions", true);
        setProperty("MyPet.Support.WorldGuard", true);

        for (EntityType entityType : MyPetExperience.MobEXP.keySet())
        {
            setProperty("MyPet.exp." + entityType.getName() + ".min", MyPetExperience.MobEXP.get(entityType).getMin());
            setProperty("MyPet.exp." + entityType.getName() + ".max", MyPetExperience.MobEXP.get(entityType).getMax());
        }

        MyPetPlugin.getPlugin().saveConfig();
    }

    public static void loadConfiguration()
    {
        LeashItem = MyPetUtil.checkMaterial(Config.getInt("MyPet.Leash.Item", 287), Material.STRING);
        Control.Item = MyPetUtil.checkMaterial(Config.getInt("MyPet.Skill.Control.Item", 287), Material.STRING);
        Pickup.RangePerLevel = Config.getDouble("MyPet.Skill.Pickup.RangePerLvl", 1.0);
        HPregeneration.HealtregenTime = Config.getInt("MyPet.Skill.HPregeneration.Time", 60);
        Poison.ChancePerLevel = Config.getInt("MyPet.Skill.Poison.ChancePerLevel", 5);
        RespawnTimeFactor = Config.getInt("MyPet.RespawnTime.Factor", 5);
        RespawnTimeFixed = Config.getInt("MyPet.RespawnTime.Fixed", 0);
        LevelSystem = Config.getBoolean("MyPet.LevelSystem", true);
        //HeroesSkill = Config.getBoolean("MyPet.HeroesSkill", false);
        SitdownTime = Config.getInt("MyPet.SitdownTime", 60);
        Superperms = Config.getBoolean("MyPet.SuperPerms", false);
        sendMetrics = Config.getBoolean("MyPet.SendMetrics", true);
        DebugLogger = Config.getBoolean("MyPet.DebugLogger", false);
        useTowny = Config.getBoolean("MyPet.Support.Towny", true);
        useFactions = Config.getBoolean("MyPet.Support.Factions", true);
        useWorldGuard = Config.getBoolean("MyPet.Support.WorldGuard", true);

        MyWolf.startHP = Config.getInt("MyPet.StartHP.Wolf", 20);
        MySilverfish.startHP = Config.getInt("MyPet.StartHP.Silverfish", 8);
        MyOcelot.startHP = Config.getInt("MyPet.StartHP.Ocelot", 15);
        MyIronGolem.startHP = Config.getInt("MyPet.StartHP.IronGolem", 20);
        MyChicken.startHP = Config.getInt("MyPet.StartHP.Chicken", 4);
        MyCow.startHP = Config.getInt("MyPet.StartHP.Cow", 10);
        MyMooshroom.startHP = Config.getInt("MyPet.StartHP.Mooshroom", 10);

        if (Config.getStringList("MyPet.exp") != null)
        {
            int min;
            int max;
            for (EntityType entityType : MyPetExperience.MobEXP.keySet())
            {
                min = 0;
                max = 0;
                if (Config.contains("MyPet.exp." + entityType.getName() + ".max"))
                {
                    max = Config.getInt("MyPet.exp." + entityType.getName() + ".max", 0);
                }
                if (Config.contains("MyPet.exp." + entityType.getName() + ".min"))
                {
                    min = Config.getInt("MyPet.exp." + entityType.getName() + ".min", 0);
                }
                if (min == max)
                {
                    MyPetExperience.MobEXP.get(entityType).setExp(max);
                }
                else
                {
                    MyPetExperience.MobEXP.get(entityType).setMin(min);
                    MyPetExperience.MobEXP.get(entityType).setMax(max);
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