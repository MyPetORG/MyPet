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

import de.Keyle.MyWolf.MyWolf.WolfState;
import de.Keyle.MyWolf.chatcommands.*;
import de.Keyle.MyWolf.entity.EntityMyWolf;
import de.Keyle.MyWolf.listeners.*;
import de.Keyle.MyWolf.skill.MyWolfExperience;
import de.Keyle.MyWolf.skill.MyWolfGenericSkill;
import de.Keyle.MyWolf.skill.MyWolfSkillSystem;
import de.Keyle.MyWolf.skill.skills.*;
import de.Keyle.MyWolf.util.*;
import de.Keyle.MyWolf.util.configuration.MyWolfNBTConfiguration;
import de.Keyle.MyWolf.util.configuration.MyWolfYamlConfiguration;
import net.minecraft.server.EntityTypes;
import net.minecraft.server.EntityWolf;
import net.minecraft.server.NBTTagCompound;
import net.minecraft.server.NBTTagList;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MyWolfPlugin extends JavaPlugin
{
    private static MyWolfPlugin Plugin;
    public static MyWolfLanguage MWLanguage;
    private final MyWolfTimer Timer = new MyWolfTimer();
    public static final List<Player> WolfChestOpened = new ArrayList<Player>();
    public static File NBTWolvesFile;

    public static MyWolfPlugin getPlugin()
    {
        return Plugin;
    }

    public MyWolfTimer getTimer()
    {
        return Timer;
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

        MyWolfList.clearList();
        WolfChestOpened.clear();
        getPlugin().getServer().getScheduler().cancelTasks(getPlugin());

        MyWolfUtil.getLogger().info("Disabled");
    }

    public void onEnable()
    {
        Plugin = this;

        if (!checkVersion(getServer().getVersion(), getDescription().getVersion()))
        {
            String mwv = getDescription().getVersion();
            mwv = getDescription().getVersion().substring(mwv.indexOf('(') + 1, mwv.indexOf(')'));
            MyWolfUtil.getLogger().warning("---------------------------------------------------------");
            MyWolfUtil.getLogger().warning("This version of MyWolf should only work with Minecraft " + mwv);
            MyWolfUtil.getLogger().warning("Don't expect any support.");
            MyWolfUtil.getLogger().warning("---------------------------------------------------------");
        }

        MyWolfConfig.Config = this.getConfig();
        MyWolfConfig.setDefault();
        MyWolfConfig.loadConfiguration();

        MyWolfPlayerListener playerListener = new MyWolfPlayerListener();
        getServer().getPluginManager().registerEvents(playerListener, getPlugin());

        MyWolfVehicleListener vehicleListener = new MyWolfVehicleListener();
        getServer().getPluginManager().registerEvents(vehicleListener, getPlugin());

        MyWolfWorldListener worldListener = new MyWolfWorldListener();
        getServer().getPluginManager().registerEvents(worldListener, getPlugin());

        MyWolfEntityListener entityListener = new MyWolfEntityListener();
        getServer().getPluginManager().registerEvents(entityListener, getPlugin());

        MyWolfLevelUpListener levelupListener = new MyWolfLevelUpListener();
        getServer().getPluginManager().registerEvents(levelupListener, getPlugin());

        getCommand("wolfname").setExecutor(new CommandName());
        getCommand("wolfcall").setExecutor(new CommandCall());
        getCommand("wolfstop").setExecutor(new CommandStop());
        getCommand("wolfrelease").setExecutor(new CommandRelease());
        getCommand("mywolf").setExecutor(new CommandHelp());
        getCommand("wolfinventory").setExecutor(new CommandInventory());
        getCommand("wolfpickup").setExecutor(new CommandPickup());
        getCommand("wolfbehavior").setExecutor(new CommandBehavior());
        getCommand("wolfinfo").setExecutor(new CommandInfo());
        getCommand("wolfadmin").setExecutor(new CommandAdmin());
        getCommand("wolfskill").setExecutor(new CommandSkill());

        MyWolfYamlConfiguration MWSkillTreeConfig = new MyWolfYamlConfiguration(getPlugin().getDataFolder().getPath() + File.separator + "skill.yml");
        if (!MWSkillTreeConfig.ConfigFile.exists())
        {
            try
            {
                InputStream template = getPlugin().getResource("skill.yml");
                OutputStream out = new FileOutputStream(MWSkillTreeConfig.ConfigFile);

                byte[] buf = new byte[1024];
                int len;
                while ((len = template.read(buf)) > 0)
                {
                    out.write(buf, 0, len);
                }
                template.close();
                out.close();
                MyWolfUtil.getLogger().info("Default skill.yml file created. Please restart the server to load the skilltrees!");
            }
            catch (IOException ex)
            {
                MyWolfUtil.getLogger().info("Unable to create the default skill.yml file!");
            }
        }
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
            Class[] args = {Class.class, String.class, Integer.TYPE, Integer.TYPE, Integer.TYPE};
            Method a = EntityTypes.class.getDeclaredMethod("a", args);
            a.setAccessible(true);
            a.invoke(a, EntityMyWolf.class, "Wolf", 95, 14144467, 13545366);
            a.invoke(a, EntityWolf.class, "Wolf", 95, 14144467, 13545366);
        }
        catch (Exception e)
        {
            MyWolfUtil.getLogger().info("version " + MyWolfPlugin.Plugin.getDescription().getVersion() + " NOT ENABLED");
            e.printStackTrace();
            setEnabled(false);
            return;
        }

        // For future of the client mod
        //MyWolfUtil.getServer().getMessenger().registerOutgoingPluginChannel(getPlugin(),"MyWolfByKeyle");

        MyWolfPermissions.setup();

        MWLanguage = new MyWolfLanguage(new MyWolfYamlConfiguration(getPlugin().getDataFolder().getPath() + File.separator + "lang.yml"));
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
                MyWolfUtil.getLogger().info("No custom EXP-Script found (exp.js).");
            }
        }

        NBTWolvesFile = new File(getPlugin().getDataFolder().getPath() + File.separator + "Wolves.MyWolf");
        loadWolves(NBTWolvesFile);

        Timer.startTimer();

        if (MyWolfConfig.sendMetrics)
        {
            try
            {
                Metrics metrics = new Metrics();

                metrics.addCustomData(getPlugin(), new Metrics.Plotter("Total MyWolves")
                {
                    @Override
                    public int getValue()
                    {
                        return MyWolfList.getMyWolfCount();
                    }
                });

                metrics.beginMeasuringPlugin(getPlugin());
            }
            catch (IOException e)
            {
                MyWolfUtil.getLogger().info(e.getMessage());
            }
        }
        MyWolfUtil.getLogger().info("version " + MyWolfPlugin.Plugin.getDescription().getVersion() + " ENABLED");

        for (Player p : getServer().getOnlinePlayers())
        {
            if (MyWolfList.hasInactiveMyWolf(p))
            {
                MyWolfList.setMyWolfActive(p, true);
            }
            if (MyWolfList.hasMyWolf(p))
            {
                MyWolf MWolf = MyWolfList.getMyWolf(p);
                if (MWolf.Status == WolfState.Dead)
                {
                    p.sendMessage(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_RespawnIn").replace("%wolfname%", MWolf.Name).replace("%time%", "" + MWolf.RespawnTime)));
                }
                else if (MyWolfUtil.getDistance(MWolf.getLocation(), p.getLocation()) < 75)
                {
                    MWolf.ResetSitTimer();
                    MWolf.createWolf(MWolf.isSitting());
                }
                else
                {
                    MWolf.Status = WolfState.Despawned;
                }
            }
        }
    }

    int loadWolves(File f)
    {
        int anzahlWolves = 0;

        MyWolfNBTConfiguration nbtConfiguration = new MyWolfNBTConfiguration(f);
        nbtConfiguration.load();
        NBTTagList Wolves = nbtConfiguration.getNBTTagCompound().getList("Wolves");

        for (int i = 0 ; i < Wolves.size() ; i++)
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

            InactiveMyWolf IMWolf = new InactiveMyWolf(MyWolfUtil.getOfflinePlayer(Owner));

            IMWolf.setLocation(new Location(MyWolfUtil.getServer().getWorld(WolfWorld) != null ? MyWolfUtil.getServer().getWorld(WolfWorld) : MyWolfUtil.getServer().getWorlds().get(0), WolfX, WolfY, WolfZ));
            IMWolf.setHealth(WolfHealthNow);
            IMWolf.setRespawnTime(WolfRespawnTime);
            IMWolf.setName(WolfName);
            IMWolf.setSitting(WolfSitting);
            IMWolf.setExp(WolfEXP);
            IMWolf.setSkills(MWolfNBT.getCompound("Skills"));

            MyWolfList.addInactiveMyWolf(IMWolf);

            anzahlWolves++;
        }
        MyWolfUtil.getLogger().info(anzahlWolves + " wolf/wolves loaded");
        return anzahlWolves;
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

            Wolf.setString("Owner", MWolf.getOwner().getName());
            Wolf.setCompound("Location", Location);
            Wolf.setInt("Health", MWolf.getHealth());
            Wolf.setInt("Respawntime", MWolf.RespawnTime);
            Wolf.setString("Name", MWolf.Name);
            Wolf.setBoolean("Sitting", MWolf.isSitting());
            Wolf.setDouble("Exp", MWolf.Experience.getExp());

            NBTTagCompound SkillsNBTTagCompound = new NBTTagCompound("Skills");
            Collection<MyWolfGenericSkill> Skills = MWolf.SkillSystem.getSkills();
            if (Skills.size() > 0)
            {
                for (MyWolfGenericSkill Skill : Skills)
                {
                    NBTTagCompound s = Skill.save();
                    if (s != null)
                    {
                        SkillsNBTTagCompound.set(Skill.getName(), s);
                    }
                }
            }
            Wolf.set("Skills", SkillsNBTTagCompound);
            Wolves.add(Wolf);
        }
        for (InactiveMyWolf IMWolf : MyWolfList.getInactiveMyWolfList())
        {

            NBTTagCompound Wolf = new NBTTagCompound();

            NBTTagCompound Location = new NBTTagCompound("Location");
            Location.setDouble("X", IMWolf.getLocation().getX());
            Location.setDouble("Y", IMWolf.getLocation().getY());
            Location.setDouble("Z", IMWolf.getLocation().getZ());
            Location.setString("World", IMWolf.getLocation().getWorld().getName());

            Wolf.setString("Owner", IMWolf.getOwner().getName());
            Wolf.setCompound("Location", Location);
            Wolf.setInt("Health", IMWolf.getHealth());
            Wolf.setInt("Respawntime", IMWolf.getRespawnTime());
            Wolf.setString("Name", IMWolf.getName());
            Wolf.setBoolean("Sitting", IMWolf.isSitting());
            Wolf.setDouble("Exp", IMWolf.getExp());

            Wolf.set("Skills", IMWolf.getSkills());
            Wolves.add(Wolf);
        }
        String[] version = Plugin.getDescription().getVersion().split(" \\(");
        nbtConfiguration.getNBTTagCompound().setString("Version", version[0]);
        nbtConfiguration.getNBTTagCompound().set("Wolves", Wolves);
        nbtConfiguration.save();
    }

    private boolean checkVersion(String mc, String mw)
    {
        mw = mw.substring(mw.indexOf('(') + 1, mw.indexOf(')'));
        mc = mc.substring(mc.indexOf("(MC: ") + 5, mc.indexOf(')'));
        if (mw.contains("/"))
        {
            String[] temp = mw.split("/");
            for (String v : temp)
            {
                if (v.equals(mc))
                {
                    return true;
                }
            }
            return false;
        }
        else
        {
            return mw.equals(mc);
        }
    }
}