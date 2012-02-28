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

package de.Keyle.MyWolf;

import de.Keyle.MyWolf.Entity.EntityMyWolf;
import de.Keyle.MyWolf.Listeners.*;
import de.Keyle.MyWolf.MyWolf.WolfState;
import de.Keyle.MyWolf.Skill.MyWolfExperience;
import de.Keyle.MyWolf.Skill.MyWolfGenericSkill;
import de.Keyle.MyWolf.Skill.MyWolfSkillSystem;
import de.Keyle.MyWolf.Skill.Skills.*;
import de.Keyle.MyWolf.chatcommands.*;
import de.Keyle.MyWolf.util.*;
import de.Keyle.MyWolf.util.MyWolfPermissions.PermissionsType;
import de.Keyle.MyWolf.util.configuration.MyWolfYamlConfiguration;
import de.Keyle.MyWolf.util.configuration.MyWolfNBTConfiguration;
import net.minecraft.server.*;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class MyWolfPlugin extends JavaPlugin
{
    private static MyWolfPlugin Plugin;
    public static MyWolfLanguage MWLanguage;

    private MyWolfTimer Timer = new MyWolfTimer();

    public static final List<Player> WolfChestOpened = new ArrayList<Player>();

    public static File NBTWolvesFile;

    public static MyWolfPlugin getPlugin()
    {
        return Plugin;
    }

    public void onDisable()
    {
        saveWolves(NBTWolvesFile);
        for (MyWolf MWolf : MyWolfList.getMyWolfList())
        {
            if (MWolf.Status == WolfState.Here)
            {
                MWolf.removeWolf();
            }
        }
        
        Timer.stopTimer();
        MyWolfList.clearList();
        WolfChestOpened.clear();
        
        MyWolfUtil.getLogger().info("[MyWolf] Disabled");
        
    }

    public void onEnable()
    {
        Plugin = this;

        MyWolfConfig.Config = this.getConfig();
        MyWolfConfig.setDefault();
        MyWolfConfig.loadConfiguration();

        MyWolfPlayerListener playerListener = new MyWolfPlayerListener();
        getServer().getPluginManager().registerEvents(playerListener, this);

        MyWolfVehicleListener vehicleListener = new MyWolfVehicleListener();
        getServer().getPluginManager().registerEvents(vehicleListener, this);

        MyWolfWorldListener worldListener = new MyWolfWorldListener();
        getServer().getPluginManager().registerEvents(worldListener, this);

        MyWolfEntityListener entityListener = new MyWolfEntityListener();
        getServer().getPluginManager().registerEvents(entityListener, this);

        MyWolfLevelUpListener levelupListener = new MyWolfLevelUpListener();
        getServer().getPluginManager().registerEvents(levelupListener, this);

        getCommand("wolfname").setExecutor(new CommandName());
        getCommand("wolfcall").setExecutor(new CommandCall());
        getCommand("wolfstop").setExecutor(new CommandStop());
        getCommand("wolfrelease").setExecutor(new CommandRelease());
        getCommand("mywolf").setExecutor(new CommandHelp());
        getCommand("wolfinventory").setExecutor(new CommandInventory());
        getCommand("wolfpickup").setExecutor(new CommandPickup());
        getCommand("wolfbehavior").setExecutor(new CommandBehavior());
        getCommand("wolfinfo").setExecutor(new CommandInfo());

        MyWolfYamlConfiguration MWSkillTreeConfig = new MyWolfYamlConfiguration(this.getDataFolder().getPath() + File.separator + "skill.yml");

        MyWolfSkillTreeConfigLoader.setConfig(MWSkillTreeConfig);
        MyWolfSkillTreeConfigLoader.loadSkillTrees();


        MyWolfSkillSystem.registerSkill(Inventory.class);
        MyWolfSkillSystem.registerSkill(HPregeneration.class);
        MyWolfSkillSystem.registerSkill(Pickup.class);
        MyWolfSkillSystem.registerSkill(Behavior.class);
        MyWolfSkillSystem.registerSkill(Damage.class);
        MyWolfSkillSystem.registerSkill(Control.class);
        MyWolfSkillSystem.registerSkill(HP.class);

        try
        {
            Class[] args = new Class[5];
            args[0] = Class.class;
            args[1] = String.class;
            args[2] = Integer.TYPE;
            args[3] = Integer.TYPE;
            args[4] = Integer.TYPE;

            Method a = EntityTypes.class.getDeclaredMethod("a", args);
            a.setAccessible(true);
            a.invoke(a, EntityMyWolf.class, "Wolf", 95, 14144467, 13545366);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            setEnabled(false);
            return;
        }

        // For future of the client mod
        //this.getServer().getMessenger().registerOutgoingPluginChannel(this,"MyWolfByKeyle");

        if (MyWolfConfig.PermissionsBukkit)
        {
            MyWolfPermissions.setup(PermissionsType.BukkitPermissions);
        }
        else
        {
            MyWolfPermissions.setup();
        }

        MWLanguage = new MyWolfLanguage(new MyWolfYamlConfiguration(this.getDataFolder().getPath() + File.separator + "lang.yml"));
        MWLanguage.loadVariables();

        

        if (MyWolfConfig.LevelSystem)
        {
            try
            {
                MyWolfExperience.JSreader = MyWolfUtil.readFileAsString(MyWolfPlugin.Plugin.getDataFolder().getPath() + File.separator + "exp.js");
            }
            catch (Exception e)
            {
                MyWolfExperience.JSreader = null;
                MyWolfUtil.getLogger().info("[MyWolf] No custom EXP-Script found (exp.js).");
            }
        }

        File MWWolvesConfigFile = new File(this.getDataFolder().getPath() + File.separator + "Wolves.yml");
        NBTWolvesFile = new File(this.getDataFolder().getPath() + File.separator + "Wolves.MyWolf");

        if(MWWolvesConfigFile.exists())
        {
            MyWolfYamlConfiguration MWWolvesConfig = new MyWolfYamlConfiguration(MWWolvesConfigFile);
            loadWolves(MWWolvesConfig);
            MWWolvesConfigFile.renameTo(new File(this.getDataFolder().getPath() + File.separator + "oldWolves.yml"));
        }
        else
        {
            loadWolves(NBTWolvesFile);
        }

        Timer.startTimer();

        MyWolfUtil.getLogger().info("[MyWolf] version " + MyWolfPlugin.Plugin.getDescription().getVersion() + " ENABLED");
    }

    void loadWolves(File f)
    {
        int anzahlWolves = 0;

        MyWolfNBTConfiguration nbtConfiguration = new MyWolfNBTConfiguration(f);
        nbtConfiguration.load();
        NBTTagList Wolves = nbtConfiguration.getNBTTagCompound().getList("Wolves");

        for (int i = 0; i < Wolves.size(); i++)
        {
            NBTTagCompound MWolfNBT = (NBTTagCompound) Wolves.get(i);
            NBTTagCompound Location = MWolfNBT.getCompound("Location");

            double WolfX = Location.getDouble("X");
            double WolfY = Location.getDouble("Y");
            double WolfZ = Location.getDouble("Z");
            String WolfWorld = Location.getString("World");
            double WolfEXP = MWolfNBT.getDouble("Exp");
            int WolfHealthNow = MWolfNBT.getInt("Health");
            int WolfRespawnTime = MWolfNBT.getInt("Respawntime");
            String WolfName = MWolfNBT.getString("Name");
            String Owner = MWolfNBT.getString("Owner");
            boolean WolfSitting = MWolfNBT.getBoolean("Sitting");

            if (getServer().getWorld(WolfWorld) == null)
            {
                MyWolfUtil.getLogger().info("[MyWolf] World \"" + WolfWorld + "\" for " + Owner + "'s wolf \"" + WolfName + "\" not found - skiped wolf");
                continue;
            }

            MyWolf MWolf = new MyWolf(Plugin.getServer().getOfflinePlayer(Owner));

            MyWolfList.addMyWolf(MWolf);

            MWolf.setLocation(new Location(this.getServer().getWorld(WolfWorld), WolfX, WolfY, WolfZ));

            MWolf.setHealth(WolfHealthNow);
            MWolf.RespawnTime = WolfRespawnTime;
            if (WolfRespawnTime > 0)
            {
                MWolf.Status = WolfState.Dead;
            }
            else
            {
                MWolf.Status = WolfState.Despawned;
            }
            MWolf.SetName(WolfName);
            MWolf.setSitting(WolfSitting);
            MWolf.Experience.setExp(WolfEXP);

            Collection<MyWolfGenericSkill> Skills = MWolf.SkillSystem.getSkills();
            if(Skills.size() > 0)
            {
                for(MyWolfGenericSkill Skill : Skills)
                {
                    Skill.load(MWolfNBT.getCompound("Skills").getCompound(Skill.getName()));
                }
            }
            anzahlWolves++;
        }
        MyWolfUtil.getLogger().info("[MyWolf] " + anzahlWolves + " wolf/wolves loaded");
    }

    void loadWolves(MyWolfYamlConfiguration MWC)
    {
        int anzahlWolves = 0;
        if(MWC.getConfig().contains("Wolves"))
        {
            Set<String> WolfList = MWC.getConfig().getConfigurationSection("Wolves").getKeys(false);
            if (WolfList.size() != 0)
            {
                for (String ownername : WolfList)
                {
                    double WolfX = MWC.getConfig().getDouble("Wolves." + ownername + ".loc.X", 0);
                    double WolfY = MWC.getConfig().getDouble("Wolves." + ownername + ".loc.Y", 0);
                    double WolfZ = MWC.getConfig().getDouble("Wolves." + ownername + ".loc.Z", 0);
                    double WolfEXP = MWC.getConfig().getDouble("Wolves." + ownername + ".exp", 0);
                    String WolfWorld = MWC.getConfig().getString("Wolves." + ownername + ".loc.world", getServer().getWorlds().get(0).getName());
                    int WolfHealthNow = MWC.getConfig().getInt("Wolves." + ownername + ".health.now", 6);
                    int WolfRespawnTime = MWC.getConfig().getInt("Wolves." + ownername + ".health.respawntime", 0);
                    String WolfName = MWC.getConfig().getString("Wolves." + ownername + ".name", "Wolf");
                    boolean WolfSitting = MWC.getConfig().getBoolean("Wolves." + ownername + ".sitting", false);

                    if (getServer().getWorld(WolfWorld) == null)
                    {
                        MyWolfUtil.getLogger().info("[MyWolf] World \"" + WolfWorld + "\" for " + ownername + "'s wolf \"" + WolfName + "\" not found - skiped wolf");
                        continue;
                    }

                    MyWolf MWolf = new MyWolf(Plugin.getServer().getOfflinePlayer(ownername));

                    MyWolfList.addMyWolf(MWolf);

                    MWolf.setLocation(new Location(this.getServer().getWorld(WolfWorld), WolfX, WolfY, WolfZ));

                    MWolf.setHealth(WolfHealthNow);
                    MWolf.RespawnTime = WolfRespawnTime;
                    if (WolfRespawnTime > 0)
                    {
                        MWolf.Status = WolfState.Dead;
                    }
                    else
                    {
                        MWolf.Status = WolfState.Despawned;
                    }
                    MWolf.SetName(WolfName);
                    MWolf.setSitting(WolfSitting);
                    MWolf.Experience.setExp(WolfEXP);

                    Collection<MyWolfGenericSkill> Skills = MWolf.SkillSystem.getSkills();
                    if(Skills.size() > 0)
                    {
                        for(MyWolfGenericSkill Skill : Skills)
                        {
                            Skill.load(MWC);
                        }
                    }
                    anzahlWolves++;
                }
            }
        }
        MyWolfUtil.getLogger().info("[MyWolf] " + anzahlWolves + " wolf/wolves loaded");
    }

    public void saveWolves(File f)
    {
        MyWolfNBTConfiguration nbtConfiguration = new MyWolfNBTConfiguration(f);
        NBTTagList Wolves = new NBTTagList();
        for (MyWolf MWolf : MyWolfList.getMyWolfList())
        {

            NBTTagCompound Wolf = new NBTTagCompound();

            NBTTagCompound Location = new NBTTagCompound("Location");
            Location.setDouble("X", MWolf.getLocation().getX());
            Location.setDouble("Y", MWolf.getLocation().getY());
            Location.setDouble("Z", MWolf.getLocation().getZ());
            Location.setString("World", MWolf.getLocation().getWorld().getName());

            Wolf.setString("Owner",  MWolf.getOwner().getName());
            Wolf.setCompound("Location", Location);
            Wolf.setInt("Health", MWolf.getHealth());
            Wolf.setInt("Respawntime", MWolf.RespawnTime);
            Wolf.setString("Name", MWolf.Name);
            Wolf.setBoolean("Sitting", MWolf.isSitting());
            Wolf.setDouble("Exp", MWolf.Experience.getExp());

            NBTTagCompound SkillsNBTTagCompound = new NBTTagCompound("Skills");
            Collection<MyWolfGenericSkill> Skills = MWolf.SkillSystem.getSkills();
            if(Skills.size() > 0)
            {
                for(MyWolfGenericSkill Skill : Skills)
                {
                    NBTTagCompound s = new NBTTagCompound(Skill.getName());
                    Skill.save(s);
                    SkillsNBTTagCompound.set(Skill.getName(), s);
                }
            }
            Wolf.set("Skills",SkillsNBTTagCompound);
            Wolves.add(Wolf);
        }
        nbtConfiguration.getNBTTagCompound().set("Wolves", Wolves);
        nbtConfiguration.save();
    }
}