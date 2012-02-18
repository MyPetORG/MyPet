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
import de.Keyle.MyWolf.MyWolf.BehaviorState;
import de.Keyle.MyWolf.MyWolf.WolfState;
import de.Keyle.MyWolf.Skill.MyWolfExperience;
import de.Keyle.MyWolf.Skill.Skills.*;
import de.Keyle.MyWolf.chatcommands.*;
import de.Keyle.MyWolf.util.*;
import de.Keyle.MyWolf.util.MyWolfPermissions.PermissionsType;
import net.minecraft.server.ItemStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MyWolfPlugin extends JavaPlugin
{
    public static MyWolfPlugin Plugin;
    public static MyWolfConfiguration MWWolvesConfig;
    public static MyWolfLanguage MWLanguage;

    public static final List<Player> WolfChestOpened = new ArrayList<Player>();
    public static final List<Player> OpenMyWolfChests = new ArrayList<Player>();

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
        
        MyWolfUtil.Log.info("[MyWolf] Disabled");
        
    }

    public void onEnable()
    {
        Plugin = this;

        MyWolfConfig.Config = this.getConfig();
        MyWolfConfig.setDefault();
        MyWolfConfig.loadConfiguration();

        if(Plugin.getServer().getPluginManager().getPlugin("Spout") == null)
        {
            MyWolfConfig.UseSpout = false;
        }

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

        if (MyWolfConfig.UseSpout)
        {
            MyWolfInventoryListener inventoryListener = new MyWolfInventoryListener();
            getServer().getPluginManager().registerEvents(inventoryListener, this);
        }

        getCommand("wolfname").setExecutor(new MyWolfName());
        getCommand("wolfcall").setExecutor(new MyWolfCall());
        getCommand("wolfstop").setExecutor(new MyWolfStop());
        getCommand("wolfrelease").setExecutor(new MyWolfRelease());
        getCommand("wolf").setExecutor(new MyWolfHelp());
        getCommand("wolfinventory").setExecutor(new MyWolfInventory());
        getCommand("wolfpickup").setExecutor(new MyWolfPickup());
        getCommand("wolfbehavior").setExecutor(new MyWolfBehavior());
        getCommand("wolfcompass").setExecutor(new MyWolfCompass());
        getCommand("wolfinfo").setExecutor(new MyWolfInfo());
        getCommand("wolfskin").setExecutor(new MyWolfSkin());

        if (MyWolfConfig.LevelSystem)
        {
            getCommand("wolfexp").setExecutor(new MyWolfEXP());
        }

        new Inventory();
        new HP();
        new HPregeneration();
        new Pickup();
        new Behavior();
        new Damage();
        new Control();

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
                MyWolfUtil.Log.info("[MyWolf] EXP-Script not found (exp.js).");
            }
        }

        MyWolfUtil.Log.info("[" + MyWolfPlugin.Plugin.getDescription().getName() + "] version " + MyWolfPlugin.Plugin.getDescription().getVersion() + " ENABLED");
    }

    public void LoadWolves(MyWolfConfiguration MWC)
    {
        int anzahlWolves = 0;
        if(MWC.Config.contains("Wolves"))
        {
            Set<String> WolfList = MWC.Config.getConfigurationSection("Wolves").getKeys(false);
            if (WolfList.size() != 0)
            {
                for (String ownername : WolfList)
                {
                    double WolfX = MWC.Config.getDouble("Wolves." + ownername + ".loc.X", 0);
                    double WolfY = MWC.Config.getDouble("Wolves." + ownername + ".loc.Y", 0);
                    double WolfZ = MWC.Config.getDouble("Wolves." + ownername + ".loc.Z", 0);
                    double WolfEXP = MWC.Config.getDouble("Wolves." + ownername + ".exp", 0);
                    String WolfWorld = MWC.Config.getString("Wolves." + ownername + ".loc.world", getServer().getWorlds().get(0).getName());
                    int WolfHealthNow = MWC.Config.getInt("Wolves." + ownername + ".health.now", 6);
                    int WolfRespawnTime = MWC.Config.getInt("Wolves." + ownername + ".health.respawntime", 0);
                    String WolfName = MWC.Config.getString("Wolves." + ownername + ".name", "Wolf");
                    String WolfSkin = MWC.Config.getString("Wolves." + ownername + ".skin", "");
                    boolean WolfSitting = MWC.Config.getBoolean("Wolves." + ownername + ".sitting", false);
                    BehaviorState WolfBehavior = BehaviorState.valueOf(MWC.Config.getString("Wolves." + ownername + ".behavior", "Normal"));
                    boolean WolfPickup = MWC.Config.getBoolean("Wolves." + ownername + ".pickup", false);

                    if (getServer().getWorld(WolfWorld) == null)
                    {
                        MyWolfUtil.Log.info("[MyWolf] World \"" + WolfWorld + "\" for " + ownername + "'s wolf \"" + WolfName + "\" not found - skiped wolf");
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
                    MWolf.setTameSkin(WolfSkin);
                    MWolf.Behavior = WolfBehavior;
                    MWolf.isPickup = WolfPickup;

                    String inv = MWC.Config.getString("Wolves." + ownername + ".inventory", ",,");
                    String[] invSplit = inv.split(";");
                    for (int i = 0 ; i < invSplit.length ; i++)
                    {
                        if (i < MWolf.inv.getSize())
                        {
                            String[] itemvalues = invSplit[i].split(",");
                            if (itemvalues.length == 3 && MyWolfUtil.isInt(itemvalues[0]) && MyWolfUtil.isInt(itemvalues[1]) && MyWolfUtil.isInt(itemvalues[2]))
                            {
                                if (Material.getMaterial(Integer.parseInt(itemvalues[0])) != null)
                                {
                                    if (Integer.parseInt(itemvalues[1]) <= 64)
                                    {
                                        MWolf.inv.setItem(i, new ItemStack(Integer.parseInt(itemvalues[0]), Integer.parseInt(itemvalues[1]), Integer.parseInt(itemvalues[2])));
                                    }
                                }
                            }
                        }
                    }
                    anzahlWolves++;
                }
            }
        }
        MyWolfUtil.Log.info("[" + this.getDescription().getName() + "] " + anzahlWolves + " wolf/wolves loaded");
    }

    public void SaveWolves(MyWolfConfiguration MWC)
    {
        MWC.Config.set("Wolves",null);
        for (MyWolf MWolf : MyWolfList.getMyWolfList())
        {
            String Items = "";
            String owner = MWolf.getOwnerName();
            for (int i = 0 ; i < MWolf.inv.getSize() ; i++)
            {
                ItemStack Item = MWolf.inv.getItem(i);
                if (Item != null)
                {
                    Items += Item.id + "," + Item.count + "," + Item.getData() + ";";
                }
                else
                {
                    Items += ",,;";
                }
            }
            Items = !Items.equals("") ? Items.substring(0, Items.length() - 1) : Items;
            MWC.Config.set("Wolves." + owner + ".inventory", Items);
            MWC.Config.set("Wolves." + owner + ".loc.X", MWolf.getLocation().getX());
            MWC.Config.set("Wolves." + owner + ".loc.Y", MWolf.getLocation().getY());
            MWC.Config.set("Wolves." + owner + ".loc.Z", MWolf.getLocation().getZ());
            MWC.Config.set("Wolves." + owner + ".loc.world", MWolf.getLocation().getWorld().getName());

            MWC.Config.set("Wolves." + owner + ".health.now", MWolf.getHealth());
            MWC.Config.set("Wolves." + owner + ".health.respawntime", MWolf.RespawnTime);
            MWC.Config.set("Wolves." + owner + ".name", MWolf.Name);
            MWC.Config.set("Wolves." + owner + ".sitting", MWolf.isSitting());
            MWC.Config.set("Wolves." + owner + ".exp", MWolf.Experience.getExp());
            MWC.Config.set("Wolves." + owner + ".skin", MWolf.SkinURL);
            MWC.Config.set("Wolves." + owner + ".behavior", MWolf.Behavior.name());
            MWC.Config.set("Wolves." + owner + ".pickup", MWolf.isPickup);
        }
        MWC.saveConfig();
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static boolean isMyWolf(Wolf wolf)
    {
        return MyWolfList.getMyWolf(wolf.getEntityId()) != null;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static MyWolf getMyWolf(Wolf wolf)
    {
        return MyWolfList.getMyWolf(wolf.getEntityId());
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static MyWolf getMyWolf(Player player)
    {
        return MyWolfList.getMyWolf(player);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static boolean isMyWolfInventoryOpen(Player player)
    {
        return OpenMyWolfChests.contains(player);

    }
}