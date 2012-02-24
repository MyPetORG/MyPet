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

import de.Keyle.MyWolf.Listeners.*;
import de.Keyle.MyWolf.MyWolf.WolfState;
import de.Keyle.MyWolf.Skill.MyWolfExperience;
import de.Keyle.MyWolf.Skill.MyWolfGenericSkill;
import de.Keyle.MyWolf.Skill.MyWolfSkillSystem;
import de.Keyle.MyWolf.Skill.Skills.*;
import de.Keyle.MyWolf.chatcommands.*;
import de.Keyle.MyWolf.util.*;
import de.Keyle.MyWolf.util.MyWolfPermissions.PermissionsType;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class MyWolfPlugin extends JavaPlugin
{
    public static MyWolfPlugin Plugin;
    public static MyWolfConfiguration MWWolvesConfig;
    public static MyWolfLanguage MWLanguage;


    public static final List<Player> WolfChestOpened = new ArrayList<Player>();

    public void onDisable()
    {
        SaveWolves(MWWolvesConfig);
        for (MyWolf MWolf : MyWolfList.getMyWolfList())
        {
            if (MWolf.Status == WolfState.Here)
            {
                MWolf.removeWolf();
            }
        }
        
        getServer().getScheduler().cancelTasks(this);
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

        MyWolfConfiguration MWSkillTreeConfig = new MyWolfConfiguration(this.getDataFolder().getPath() + File.separator + "skill.yml");
        MyWolfSkillTreeConfigLoader.setConfig(MWSkillTreeConfig);
        MyWolfSkillTreeConfigLoader.loadSkillTrees();


        MyWolfSkillSystem.registerSkill(Inventory.class);
        MyWolfSkillSystem.registerSkill(HPregeneration.class);
        MyWolfSkillSystem.registerSkill(Pickup.class);
        MyWolfSkillSystem.registerSkill(Behavior.class);
        MyWolfSkillSystem.registerSkill(Damage.class);
        MyWolfSkillSystem.registerSkill(Control.class);

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

        MWLanguage = new MyWolfLanguage(new MyWolfConfiguration(this.getDataFolder().getPath() + File.separator + "lang.yml"));
        MWLanguage.loadVariables();

        MWWolvesConfig = new MyWolfConfiguration(this.getDataFolder().getPath() + File.separator + "Wolves.yml");

        LoadWolves(MWWolvesConfig);

        if (MyWolfConfig.LevelSystem)
        {
            try
            {
                MyWolfExperience.JSreader = MyWolfUtil.readFileAsString(MyWolfPlugin.Plugin.getDataFolder().getPath() + File.separator + "exp.js");
            }
            catch (Exception e)
            {
                MyWolfExperience.JSreader = null;
                MyWolfUtil.getLogger().info("[MyWolf] EXP-Script not found (exp.js).");
            }
        }

        MyWolfUtil.getLogger().info("[MyWolf] version " + MyWolfPlugin.Plugin.getDescription().getVersion() + " ENABLED");
    }

    void LoadWolves(MyWolfConfiguration MWC)
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

                    MyWolf MWolf = new MyWolf(ownername);

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

    public void SaveWolves(MyWolfConfiguration MWC)
    {
        MWC.clearConfig();
        for (MyWolf MWolf : MyWolfList.getMyWolfList())
        {
            String owner = MWolf.getOwnerName();
            MWC.getConfig().set("Wolves." + owner + ".loc.X", MWolf.getLocation().getX());
            MWC.getConfig().set("Wolves." + owner + ".loc.Y", MWolf.getLocation().getY());
            MWC.getConfig().set("Wolves." + owner + ".loc.Z", MWolf.getLocation().getZ());
            MWC.getConfig().set("Wolves." + owner + ".loc.world", MWolf.getLocation().getWorld().getName());

            MWC.getConfig().set("Wolves." + owner + ".health.now", MWolf.getHealth());
            MWC.getConfig().set("Wolves." + owner + ".health.respawntime", MWolf.RespawnTime);
            MWC.getConfig().set("Wolves." + owner + ".name", MWolf.Name);
            MWC.getConfig().set("Wolves." + owner + ".sitting", MWolf.isSitting());
            MWC.getConfig().set("Wolves." + owner + ".exp", MWolf.Experience.getExp());

            Collection<MyWolfGenericSkill> Skills = MWolf.SkillSystem.getSkills();
            if(Skills.size() > 0)
            {
                for(MyWolfGenericSkill Skill : Skills)
                {
                    Skill.save(MWC);
                }
            }
        }
        MWC.saveConfig();
    }
}