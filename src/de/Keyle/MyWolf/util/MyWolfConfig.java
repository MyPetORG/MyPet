/*
* Copyright (C) 2011 Keyle
*
* This file is part of MyWolf.
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

import de.Keyle.MyWolf.ConfigBuffer;
import de.Keyle.MyWolf.Skill.MyWolfExperience;
import org.bukkit.Material;
import org.bukkit.entity.CreatureType;
import org.bukkit.util.config.Configuration;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class MyWolfConfig
{
    public static Configuration Config;

    public static Material LeashItem = Material.STRING;
    public static Material ControlItem = Material.STRING;
    public static int PickupRange = 2;
    public static int RespawnTimeFactor = 5;
    public static int RespawnTimeFixed = 0;
    public static int SitdownTime = 15;
    public static int ExpFactor = 2;
    public static int NameColor = -1;
    public static boolean LevelSystem = true;
    public static boolean SpoutSounds = true;
    public static boolean PermissionsBukkit = false;
    public static String SpoutSoundCall;
    public static String SpoutSoundLevelup;

    public static void setStandart()
    {
        setProperty("MyWolf.leash.item", 287);
        setProperty("MyWolf.control.item", 287);
        setProperty("MyWolf.pickup.range", 2);
        setProperty("MyWolf.respawntime.factor", 5);
        setProperty("MyWolf.respawntime.fixed", 0);
        setProperty("MyWolf.sitdowntime", 15);
        setProperty("MyWolf.expfactor", 2);
        setProperty("MyWolf.namecolor", -1);
        setProperty("MyWolf.hpregendefault", 60);
        setProperty("MyWolf.bukkitpermissions", false);
        setProperty("MyWolf.levelsystem", true);
        setProperty("MyWolf.spoutsounds.enabled", true);
        setProperty("MyWolf.spoutsounds.call", "http://dl.dropbox.com/u/23957620/MinecraftPlugins/util/call.ogg");
        setProperty("MyWolf.spoutsounds.levelup", "http://dl.dropbox.com/u/23957620/MinecraftPlugins/util/levelup.ogg");

        if (!Config.getKeys("MyWolf").contains("exp"))
        {
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
        }

        if (!Config.getKeys("MyWolf").contains("skills"))
        {
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
        }

        Config.save();
    }

    public static void loadVariables()
    {
        LeashItem = MyWolfUtil.checkMaterial(Config.getInt("MyWolf.leash.item", 287), Material.STRING);
        ControlItem = MyWolfUtil.checkMaterial(Config.getInt("MyWolf.control.item", 287), Material.STRING);
        PickupRange = Config.getInt("MyWolf.pickup.range", 2);
        RespawnTimeFactor = Config.getInt("MyWolf.respawntime.factor", 5);
        RespawnTimeFixed = Config.getInt("MyWolf.respawntime.fixed", 0);
        ExpFactor = Config.getInt("MyWolf.expfactor", 2);
        NameColor = Config.getInt("MyWolf.namecolor", -1);
        NameColor = NameColor <= 0xf ? NameColor : -1;
        LevelSystem = Config.getBoolean("MyWolf.levelsystem", true);
        SpoutSounds = Config.getBoolean("MyWolf.spoutsounds.enabled", true);
        SpoutSoundCall = Config.getString("MyWolf.spoutsounds.call", "http://dl.dropbox.com/u/23957620/MinecraftPlugins/util/call.ogg");
        SpoutSoundLevelup = Config.getString("MyWolf.spoutsounds.levelup", "http://dl.dropbox.com/u/23957620/MinecraftPlugins/util/levelup.ogg");
        SitdownTime = Config.getInt("MyWolf.sitdowntime", 15);
        PermissionsBukkit = Config.getBoolean("MyWolf.bukkitpermissions", false);

        if (Config.getKeys("MyWolf.exp") != null)
        {
            for (String key : Config.getKeys("MyWolf.exp"))
            {
                double expval = Config.getDouble("MyWolf.exp." + key, -1.0);
                if (expval > -1)
                {
                    MyWolfExperience.MobEXP.put(CreatureType.valueOf(key), expval);
                }
            }
        }
        if (Config.getKeys("MyWolf.skills") != null)
        {
            for (String lvl : Config.getKeys("MyWolf.skills"))
            {
                List<String> Skills = Arrays.asList(Config.getString("MyWolf.skills." + lvl).replace("[", "").replace("]", "").split(", "));
                ConfigBuffer.SkillPerLevel.put(Integer.parseInt(lvl), Skills);
            }
        }
    }

    public static void setProperty(String key, Object value)
    {
        if (Config.getProperty(key) == null)
        {
            Config.setProperty(key, value);
        }
    }
}
