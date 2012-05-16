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

package de.Keyle.MyPet;

import de.Keyle.MyPet.chatcommands.*;
import de.Keyle.MyPet.entity.types.InactiveMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.entity.types.ocelot.EntityMyOcelot;
import de.Keyle.MyPet.entity.types.wolf.EntityMyWolf;
import de.Keyle.MyPet.listeners.*;
import de.Keyle.MyPet.skill.MyPetExperience;
import de.Keyle.MyPet.skill.MyPetGenericSkill;
import de.Keyle.MyPet.skill.MyPetJSexp;
import de.Keyle.MyPet.skill.MyPetSkillSystem;
import de.Keyle.MyPet.skill.skills.*;
import de.Keyle.MyPet.util.*;
import de.Keyle.MyPet.util.configuration.NBTConfiguration;
import de.Keyle.MyPet.util.configuration.YamlConfiguration;
import de.Keyle.MyPet.util.logger.DebugLogger;
import net.minecraft.server.*;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.Method;
import java.util.Collection;

public class MyPetPlugin extends JavaPlugin
{
    private static MyPetPlugin plugin;
    public static MyPetLanguage language;
    private final MyPetTimer timer = new MyPetTimer();
    public static File NBTPetFile;
    private DebugLogger debugLogger;

    public static MyPetPlugin getPlugin()
    {
        return plugin;
    }

    public MyPetTimer getTimer()
    {
        return timer;
    }

    public void onDisable()
    {
        debugLogger.info(savePets(NBTPetFile) + " pet/pets saved.");
        for (MyPet MPet : MyPetList.getMyPetList())
        {
            MPet.removePet();
        }
        getTimer().stopTimer();
        MyPetList.clearList();
        Inventory.PetChestOpened.clear();
        getPlugin().getServer().getScheduler().cancelTasks(getPlugin());
        debugLogger.info("MyPet disabled!");
        MyPetUtil.getLogger().info("Disabled");
    }

    public void onEnable()
    {
        plugin = this;

        if (!checkVersion(getServer().getVersion(), getDescription().getVersion()))
        {
            String mwv = getDescription().getVersion();
            mwv = getDescription().getVersion().substring(mwv.indexOf('(') + 1, mwv.indexOf(')'));
            MyPetUtil.getLogger().warning("---------------------------------------------------------");
            MyPetUtil.getLogger().warning("This version of MyPet should only work with Minecraft " + mwv);
            MyPetUtil.getLogger().warning("Expect bugs and errors!");
            MyPetUtil.getLogger().warning("---------------------------------------------------------");
        }

        MyPetConfig.Config = this.getConfig();
        MyPetConfig.setDefault();
        MyPetConfig.loadConfiguration();

        debugLogger = new DebugLogger(MyPetConfig.DebugLogger);
        debugLogger.info("----------- loading MyPet ... -----------");
        debugLogger.info("MyPet " + getDescription().getVersion());
        debugLogger.info("Bukkit " + getServer().getVersion());

        MyPetUtil.getDebugLogger().info("MobEXP table: -------------------------");
        for (EntityType ET : MyPetExperience.MobEXP.keySet())
        {
            debugLogger.info("   " + MyPetExperience.MobEXP.get(ET).toString());
        }
        MyPetUtil.getDebugLogger().info("MobEXP table end ----------------------");

        MyPetPlayerListener playerListener = new MyPetPlayerListener();
        getServer().getPluginManager().registerEvents(playerListener, getPlugin());

        MyPetVehicleListener vehicleListener = new MyPetVehicleListener();
        getServer().getPluginManager().registerEvents(vehicleListener, getPlugin());

        MyPetWorldListener worldListener = new MyPetWorldListener();
        getServer().getPluginManager().registerEvents(worldListener, getPlugin());

        MyPetEntityListener entityListener = new MyPetEntityListener();
        getServer().getPluginManager().registerEvents(entityListener, getPlugin());

        MyPetLevelUpListener levelupListener = new MyPetLevelUpListener();
        getServer().getPluginManager().registerEvents(levelupListener, getPlugin());

        getCommand("petname").setExecutor(new CommandName());
        getCommand("petcall").setExecutor(new CommandCall());
        getCommand("petstop").setExecutor(new CommandStop());
        getCommand("petrelease").setExecutor(new CommandRelease());
        getCommand("mypet").setExecutor(new CommandHelp());
        getCommand("petinventory").setExecutor(new CommandInventory());
        getCommand("petpickup").setExecutor(new CommandPickup());
        getCommand("petbehavior").setExecutor(new CommandBehavior());
        getCommand("petinfo").setExecutor(new CommandInfo());
        getCommand("petadmin").setExecutor(new CommandAdmin());
        getCommand("petskill").setExecutor(new CommandSkill());

        YamlConfiguration MWSkillTreeConfig = new YamlConfiguration(getPlugin().getDataFolder().getPath() + File.separator + "skill.yml");
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
                MyPetUtil.getLogger().info("Default skill.yml file created. Please restart the server to load the skilltrees!");
                debugLogger.info("created default skill.yml.");
            }
            catch (IOException ex)
            {
                MyPetUtil.getLogger().info("Unable to create the default skill.yml file!");
                debugLogger.info("unable to create skill.yml.");
            }
        }
        MyPetSkillTreeConfigLoader.setConfig(MWSkillTreeConfig);
        MyPetSkillTreeConfigLoader.loadSkillTrees();


        MyPetSkillSystem.registerSkill(Inventory.class);
        MyPetSkillSystem.registerSkill(HPregeneration.class);
        MyPetSkillSystem.registerSkill(Pickup.class);
        MyPetSkillSystem.registerSkill(Behavior.class);
        MyPetSkillSystem.registerSkill(Damage.class);
        MyPetSkillSystem.registerSkill(Control.class);
        MyPetSkillSystem.registerSkill(HP.class);
        MyPetSkillSystem.registerSkill(Poison.class);

        try
        {
            Class[] args = {Class.class, String.class, Integer.TYPE};
            Method a = EntityTypes.class.getDeclaredMethod("a", args);
            a.setAccessible(true);
            a.invoke(a, EntityMyWolf.class, "Wolf", 95);
            a.invoke(a, EntityWolf.class, "Wolf", 95);
            a.invoke(a, EntityMyOcelot.class, "Ozelot", 98);
            a.invoke(a, EntityOcelot.class, "Ozelot", 98);
            debugLogger.info("registered MyPet entities.");
        }
        catch (Exception e)
        {
            MyPetUtil.getLogger().info("version " + MyPetPlugin.plugin.getDescription().getVersion() + " NOT ENABLED");
            e.printStackTrace();
            debugLogger.severe("error while registering MyPet entity.");
            debugLogger.severe(e.getMessage());
            setEnabled(false);
            return;
        }

        // For future of the client mod
        //MyPetUtil.getServer().getMessenger().registerOutgoingPluginChannel(getPlugin(),"MyPetByKeyle");

        MyPetPermissions.setup();

        language = new MyPetLanguage(new YamlConfiguration(getPlugin().getDataFolder().getPath() + File.separator + "lang.yml"));
        language.loadVariables();


        if (MyPetConfig.LevelSystem)
        {
            if (MyPetJSexp.setScriptPath(MyPetPlugin.plugin.getDataFolder().getPath() + File.separator + "exp.js"))
            {
                MyPetUtil.getLogger().info("Custom EXP-Script loaded!");
                MyPetUtil.getDebugLogger().info("loaded exp.js.");
            }
            else
            {
                MyPetUtil.getLogger().info("No custom EXP-Script found (exp.js).");
                MyPetUtil.getDebugLogger().info("exp.js not loaded.");
            }
        }

        NBTPetFile = new File(getPlugin().getDataFolder().getPath() + File.separator + "Wolves.MyPet");
        if (NBTPetFile.exists())
        {
            NBTPetFile.renameTo(new File(getPlugin().getDataFolder().getPath() + File.separator + "My.Pets"));
        }
        NBTPetFile = new File(getPlugin().getDataFolder().getPath() + File.separator + "My.Pets");
        loadPets(NBTPetFile);

        timer.startTimer();

        if (MyPetConfig.sendMetrics)
        {
            debugLogger.info("Metrics is activivated");
            try
            {
                Metrics metrics = new Metrics(MyPetPlugin.getPlugin());

                metrics.addCustomData(new Metrics.Plotter("Total MyWolves")
                {
                    @Override
                    public int getValue()
                    {
                        return MyPetList.getMyPetCount();
                    }
                });

                metrics.start();
            }
            catch (IOException e)
            {
                MyPetUtil.getLogger().info(e.getMessage());
            }
        }

        debugLogger.info("version " + MyPetPlugin.plugin.getDescription().getVersion() + " ENABLED");
        MyPetUtil.getLogger().info("version " + MyPetPlugin.plugin.getDescription().getVersion() + " ENABLED");

        for (Player p : getServer().getOnlinePlayers())
        {
            if (MyPetPermissions.has(p, "MyPet.user.leash"))
            {
                if (MyPetList.hasInactiveMyPet(p))
                {
                    MyPetList.setMyPetActive(p, true);

                    MyPet MPet = MyPetList.getMyPet(p);
                    if (MPet.Status == PetState.Dead)
                    {
                        p.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_RespawnIn").replace("%petname%", MPet.Name).replace("%time%", "" + MPet.RespawnTime)));
                    }
                    else if (MyPetUtil.getDistance(MPet.getLocation(), p.getLocation()) < 75)
                    {
                        MPet.ResetSitTimer();
                        MPet.createPet();
                    }
                    else
                    {
                        MPet.Status = PetState.Despawned;
                    }
                }
            }
        }
        debugLogger.info("----------- MyPet ready -----------");
    }

    int loadPets(File f)
    {
        int petCount = 0;

        NBTConfiguration nbtConfiguration = new NBTConfiguration(f);
        nbtConfiguration.load();
        NBTTagList Wolves = nbtConfiguration.getNBTTagCompound().getList("Pets");
        debugLogger.info("loading Pets: -----------------------------");
        for (int i = 0 ; i < Wolves.size() ; i++)
        {
            NBTTagCompound MPetNBT = (NBTTagCompound) Wolves.get(i);
            NBTTagCompound Location = MPetNBT.getCompound("Location");

            double PetX = Location.getDouble("X");
            double PetY = Location.getDouble("Y");
            double PetZ = Location.getDouble("Z");
            String PetWorld = Location.getString("World");
            double PetExp = MPetNBT.getDouble("Exp");
            int PetHealthNow = MPetNBT.getInt("Health");
            int PetRespawnTime = MPetNBT.getInt("Respawntime");
            String PetName = MPetNBT.getString("Name");
            String Owner = MPetNBT.getString("Owner");
            String PetType;
            if (MPetNBT.hasKey("Type"))
            {
                PetType = MPetNBT.getString("Type");
            }
            else
            {
                PetType = "Wolf";
            }
            boolean PetSitting = MPetNBT.getBoolean("Sitting");

            InactiveMyPet IMPet = new InactiveMyPet(MyPetUtil.getOfflinePlayer(Owner));

            IMPet.setLocation(new Location(MyPetUtil.getServer().getWorld(PetWorld) != null ? MyPetUtil.getServer().getWorld(PetWorld) : MyPetUtil.getServer().getWorlds().get(0), PetX, PetY, PetZ));
            IMPet.setHealth(PetHealthNow);
            IMPet.setRespawnTime(PetRespawnTime);
            IMPet.setName(PetName);
            IMPet.setSitting(PetSitting);
            IMPet.setExp(PetExp);
            IMPet.setSkills(MPetNBT.getCompound("Skills"));
            IMPet.setType(MyPetType.valueOf(PetType));

            MyPetList.addInactiveMyPet(IMPet);

            debugLogger.info("   " + IMPet.toString());

            petCount++;
        }
        debugLogger.info(petCount + " pet/pets loaded -------------------------");
        MyPetUtil.getLogger().info(petCount + " pet/pets loaded");
        return petCount;
    }

    public int savePets(File f)
    {
        int petCount = 0;
        NBTConfiguration nbtConfiguration = new NBTConfiguration(f);
        NBTTagList Pets = new NBTTagList();
        for (MyPet MPet : MyPetList.getMyPetList())
        {

            NBTTagCompound Pet = new NBTTagCompound();

            NBTTagCompound Location = new NBTTagCompound("Location");
            Location.setDouble("X", MPet.getLocation().getX());
            Location.setDouble("Y", MPet.getLocation().getY());
            Location.setDouble("Z", MPet.getLocation().getZ());
            Location.setString("World", MPet.getLocation().getWorld().getName());

            Pet.setString("Type", MPet.getPetType().getTypeName());
            Pet.setString("Owner", MPet.getOwner().getName());
            Pet.setCompound("Location", Location);
            Pet.setInt("Health", MPet.getHealth());
            Pet.setInt("Respawntime", MPet.RespawnTime);
            Pet.setString("Name", MPet.Name);
            Pet.setBoolean("Sitting", MPet.isSitting());
            Pet.setDouble("Exp", MPet.getExperience().getExp());

            NBTTagCompound SkillsNBTTagCompound = new NBTTagCompound("Skills");
            Collection<MyPetGenericSkill> skills = MPet.getSkillSystem().getSkills();
            if (skills.size() > 0)
            {
                for (MyPetGenericSkill skill : skills)
                {
                    NBTTagCompound s = skill.save();
                    if (s != null)
                    {
                        SkillsNBTTagCompound.set(skill.getName(), s);
                    }
                }
            }
            Pet.set("Skills", SkillsNBTTagCompound);
            Pets.add(Pet);
            petCount++;
        }
        for (InactiveMyPet IMPet : MyPetList.getInactiveMyPetList())
        {

            NBTTagCompound Pet = new NBTTagCompound();

            NBTTagCompound Location = new NBTTagCompound("Location");
            Location.setDouble("X", IMPet.getLocation().getX());
            Location.setDouble("Y", IMPet.getLocation().getY());
            Location.setDouble("Z", IMPet.getLocation().getZ());
            Location.setString("World", IMPet.getLocation().getWorld().getName());

            Pet.setString("Owner", IMPet.getOwner().getName());
            Pet.setCompound("Location", Location);
            Pet.setInt("Health", IMPet.getHealth());
            Pet.setInt("Respawntime", IMPet.getRespawnTime());
            Pet.setString("Name", IMPet.getName());
            Pet.setBoolean("Sitting", IMPet.isSitting());
            Pet.setDouble("Exp", IMPet.getExp());

            Pet.set("Skills", IMPet.getSkills());
            Pets.add(Pet);
            petCount++;
        }
        String[] version = plugin.getDescription().getVersion().split(" \\(");
        nbtConfiguration.getNBTTagCompound().setString("Version", version[0]);
        nbtConfiguration.getNBTTagCompound().set("Pets", Pets);
        nbtConfiguration.save();
        return petCount;
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

    public DebugLogger getDebugLogger()
    {
        return debugLogger;
    }
}