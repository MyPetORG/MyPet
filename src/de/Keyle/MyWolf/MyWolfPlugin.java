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

package de.Keyle.MyWolf;

import de.Keyle.MyWolf.Listeners.*;
import de.Keyle.MyWolf.MyWolf.BehaviorState;
import de.Keyle.MyWolf.MyWolf.WolfState;
import de.Keyle.MyWolf.Skill.MyWolfExperience;
import de.Keyle.MyWolf.Skill.MyWolfSkill;
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

public class MyWolfPlugin extends JavaPlugin
{
    public static MyWolfPlugin Plugin;
    public static final List<Player> OpenMyWolfChests = new ArrayList<Player>();

    public void onDisable()
    {
        SaveWolves(ConfigBuffer.WolvesConfig);
        for (String owner : ConfigBuffer.mWolves.keySet())
        {
            if (ConfigBuffer.mWolves.get(owner).Status == WolfState.Here)
            {
                ConfigBuffer.mWolves.get(owner).removeWolf();
            }
        }
        
        getServer().getScheduler().cancelTasks(this);
        ConfigBuffer.mWolves.clear();
        ConfigBuffer.WolfChestOpened.clear();
        
        MyWolfUtil.Log.info("[MyWolf] Disabled");
        
    }

    public void onEnable()
    {
        Plugin = this;

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

        MyWolfInventoryListener inventoryListener = new MyWolfInventoryListener();
        getServer().getPluginManager().registerEvents(inventoryListener, this);



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

        new MyWolfSkill("Control").registerSkill();

        MyWolfConfig.Config = this.getConfig();
        MyWolfConfig.setStandart();
        MyWolfConfig.loadVariables();

        if (MyWolfConfig.PermissionsBukkit)
        {
            MyWolfPermissions.setup(PermissionsType.BukkitPermissions);
        }
        else
        {
            MyWolfPermissions.setup();
        }

        ConfigBuffer.lv = new MyWolfLanguage(new MyWolfConfiguration(this.getDataFolder().getPath() + File.separator + "lang.yml"));
        ConfigBuffer.lv.setDefault();
        ConfigBuffer.lv.loadVariables();

        ConfigBuffer.WolvesConfig = new MyWolfConfiguration(this.getDataFolder().getPath() + File.separator + "Wolves.yml");

        LoadWolves(ConfigBuffer.WolvesConfig);

        if (MyWolfConfig.LevelSystem)
        {
            try
            {
                MyWolfExperience.JSreader = MyWolfUtil.readFileAsString(MyWolfPlugin.Plugin.getDataFolder().getPath() + File.separator + "exp.js");
            }
            catch (Exception e)
            {
                MyWolfExperience.JSreader = null;
                MyWolfUtil.Log.info("[MyWolf] EXP-Script not found (exp.js). Working with factor.");
            }
        }

        for (Player p : this.getServer().getOnlinePlayers())
        {
            if (ConfigBuffer.mWolves.containsKey(p.getName()) && p.isOnline())
            {
                ConfigBuffer.mWolves.get(p.getName()).createWolf(ConfigBuffer.mWolves.get(p.getName()).isSitting());
            }
        }
        MyWolfUtil.Log.info("[" + ConfigBuffer.pdfFile.getName() + "] version " + ConfigBuffer.pdfFile.getVersion() + " ENABLED");
    }

    public void LoadWolves(MyWolfConfiguration MWC)
    {
        int anzahlWolves = 0;

        List<String> WolfList = MWC.Config.getStringList("Wolves");
        if (WolfList != null)
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

                MyWolf Wolf = new MyWolf(ownername);

                ConfigBuffer.mWolves.put(ownername, Wolf);



                Wolf.setLocation(new Location(this.getServer().getWorld(WolfWorld), WolfX, WolfY, WolfZ));

                Wolf.setHealth(WolfHealthNow);
                Wolf.RespawnTime = WolfRespawnTime;
                if (WolfRespawnTime > 0)
                {
                    Wolf.Status = WolfState.Dead;
                }
                else
                {
                    Wolf.Status = WolfState.Despawned;
                }
                Wolf.SetName(WolfName);
                Wolf.setSitting(WolfSitting);
                Wolf.Experience.setExp(WolfEXP);
                Wolf.setTameSkin(WolfSkin);
                Wolf.Behavior = WolfBehavior;
                Wolf.isPickup = WolfPickup;

                String inv = MWC.Config.getString("Wolves." + ownername + ".inventory", ",,");
                String[] invSplit = inv.split(";");
                for (int i = 0 ; i < invSplit.length ; i++)
                {
                    if (i < Wolf.inv.getSize())
                    {
                        String[] itemvalues = invSplit[i].split(",");
                        if (itemvalues.length == 3 && MyWolfUtil.isInt(itemvalues[0]) && MyWolfUtil.isInt(itemvalues[1]) && MyWolfUtil.isInt(itemvalues[2]))
                        {
                            if (Material.getMaterial(Integer.parseInt(itemvalues[0])) != null)
                            {
                                if (Integer.parseInt(itemvalues[1]) <= 64)
                                {
                                    Wolf.inv.setItem(i, new ItemStack(Integer.parseInt(itemvalues[0]), Integer.parseInt(itemvalues[1]), Integer.parseInt(itemvalues[2])));
                                }
                            }
                        }
                    }
                }
                anzahlWolves++;
            }
        }
        MyWolfUtil.Log.info("[" + this.getDescription().getName() + "] " + anzahlWolves + " wolf/wolves loaded");
    }

    public void SaveWolves(MyWolfConfiguration MWC)
    {
        MWC.Config.set("Wolves",null);
        for (String owner : ConfigBuffer.mWolves.keySet())
        {
            MyWolf wolf = ConfigBuffer.mWolves.get(owner);
            String Items = "";
            for (int i = 0 ; i < wolf.inv.getSize() ; i++)
            {
                ItemStack Item = wolf.inv.getItem(i);
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
            MWC.Config.set("Wolves." + owner + ".loc.X", wolf.getLocation().getX());
            MWC.Config.set("Wolves." + owner + ".loc.Y", wolf.getLocation().getY());
            MWC.Config.set("Wolves." + owner + ".loc.Z", wolf.getLocation().getZ());
            MWC.Config.set("Wolves." + owner + ".loc.world", wolf.getLocation().getWorld().getName());

            MWC.Config.set("Wolves." + owner + ".health.now", wolf.getHealth());
            MWC.Config.set("Wolves." + owner + ".health.respawntime", wolf.RespawnTime);
            MWC.Config.set("Wolves." + owner + ".name", wolf.Name);
            MWC.Config.set("Wolves." + owner + ".sitting", wolf.isSitting());
            MWC.Config.set("Wolves." + owner + ".exp", wolf.Experience.getExp());
            MWC.Config.set("Wolves." + owner + ".skin", wolf.SkinURL);
            MWC.Config.set("Wolves." + owner + ".behavior", wolf.Behavior.name());
            MWC.Config.set("Wolves." + owner + ".pickup", wolf.isPickup);
        }
        MWC.saveConfig();
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static boolean isMyWolf(Wolf wolf)
    {
        for (MyWolf w : ConfigBuffer.mWolves.values())
        {
            if (w.getID() == wolf.getEntityId())
            {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static MyWolf getMyWolf(Wolf wolf)
    {
        for (MyWolf w : ConfigBuffer.mWolves.values())
        {
            if (w.getID() == wolf.getEntityId())
            {
                return w;
            }
        }
        return null;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static MyWolf getMyWolf(Player player)
    {
        return ConfigBuffer.mWolves.containsKey(player.getName()) ? ConfigBuffer.mWolves.get(player.getName()) : null;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static boolean isMyWolfInventoryOpen(Player player)
    {
        return OpenMyWolfChests.contains(player);

    }
}