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
import org.bukkit.entity.CreatureType;

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
        setProperty("MyWolf.exp_default", true);
        setProperty("MyWolf.AutoSaveTime", 60);

        setProperty("MyWolf.exp.SKELETON", 1.1);
        setProperty("MyWolf.exp.ZOMBIE", 1.1);
        setProperty("MyWolf.exp.SPIDER", 1.05);
        setProperty("MyWolf.exp.WOLF", 0.5);
        setProperty("MyWolf.exp.CREEPER", 1.55);
        setProperty("MyWolf.exp.GHAST", 0.85);
        setProperty("MyWolf.exp.PIG_ZOMBIE", 1.1);
        setProperty("MyWolf.exp.GIANT", 10.75);
        setProperty("MyWolf.exp.COW", 0.25);
        setProperty("MyWolf.exp.PIG", 0.25);
        setProperty("MyWolf.exp.CHICKEN", 0.25);
        setProperty("MyWolf.exp.SQUID", 0.25);
        setProperty("MyWolf.exp.SHEEP", 0.25);
        setProperty("MyWolf.exp.SLIME", 1.0);
        setProperty("MyWolf.exp.CAVE_SPIDER", 1.0);
        setProperty("MyWolf.exp.BLAZE", 1.1);
        setProperty("MyWolf.exp.ENDER_DRAGON", 20.0);
        setProperty("MyWolf.exp.ENDERMAN", 1.4);
        setProperty("MyWolf.exp.MAGMA_CUBE", 1.0);
        setProperty("MyWolf.exp.MUSHROOM_COW", 0.3);
        setProperty("MyWolf.exp.SILVERFISH", 0.6);
        setProperty("MyWolf.exp.SNOWMAN", 0.5);
        setProperty("MyWolf.exp.VILLAGER", 0.01);

        /*
        List<String> list = new LinkedList<String>();
        list.add("Inventory");
        setProperty("MyWolf.skills.2", list);

        list = new LinkedList<String>();
        list.add("Inventory");
        list.add("Inventory");
        list.add("Damage");
        list.add("HPregeneration");
        list.add("HPregeneration");
        setProperty("MyWolf.skills.3", list);

        list = new LinkedList<String>();
        list.add("Pickup");
        list.add("Control");
        list.add("HPregeneration");
        list.add("HPregeneration");
        setProperty("MyWolf.skills.5", list);

        list = new LinkedList<String>();
        list.add("Behavior");
        list.add("HP");
        list.add("HPregeneration");
        list.add("Damage");
        setProperty("MyWolf.skills.6", list);
        */
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
        MyWolfExperience.defaultEXPvalues = Config.getBoolean("MyWolf.exp_default", true);

        if (Config.getStringList("MyWolf.exp") != null)
        {
            for (String key : Config.getConfigurationSection("MyWolf.exp").getKeys(false))
            {
                double expval = Config.getDouble("MyWolf.exp." + key, -1.0);
                if (expval > -1)
                {
                    MyWolfExperience.MobEXP.put(CreatureType.valueOf(key), expval);
                }
            }
        }
    }

    public static void setProperty(String key, Object value)
    {
        if (Config.get(key) == null)
        {
            Config.set(key, value);
        }
    }
}
