/*
 * Copyright (C) 2011-2013 Keyle
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
import de.Keyle.MyPet.chatcommands.CommandHelp;
import de.Keyle.MyPet.chatcommands.CommandStop;
import de.Keyle.MyPet.entity.types.InactiveMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.entity.types.bat.EntityMyBat;
import de.Keyle.MyPet.entity.types.blaze.EntityMyBlaze;
import de.Keyle.MyPet.entity.types.cavespider.EntityMyCaveSpider;
import de.Keyle.MyPet.entity.types.chicken.EntityMyChicken;
import de.Keyle.MyPet.entity.types.cow.EntityMyCow;
import de.Keyle.MyPet.entity.types.creeper.EntityMyCreeper;
import de.Keyle.MyPet.entity.types.enderman.EntityMyEnderman;
import de.Keyle.MyPet.entity.types.giant.EntityMyGiant;
import de.Keyle.MyPet.entity.types.irongolem.EntityMyIronGolem;
import de.Keyle.MyPet.entity.types.magmacube.EntityMyMagmaCube;
import de.Keyle.MyPet.entity.types.mooshroom.EntityMyMooshroom;
import de.Keyle.MyPet.entity.types.ocelot.EntityMyOcelot;
import de.Keyle.MyPet.entity.types.pig.EntityMyPig;
import de.Keyle.MyPet.entity.types.pigzombie.EntityMyPigZombie;
import de.Keyle.MyPet.entity.types.sheep.EntityMySheep;
import de.Keyle.MyPet.entity.types.silverfish.EntityMySilverfish;
import de.Keyle.MyPet.entity.types.skeleton.EntityMySkeleton;
import de.Keyle.MyPet.entity.types.slime.EntityMySlime;
import de.Keyle.MyPet.entity.types.snowman.EntityMySnowman;
import de.Keyle.MyPet.entity.types.spider.EntityMySpider;
import de.Keyle.MyPet.entity.types.villager.EntityMyVillager;
import de.Keyle.MyPet.entity.types.witch.EntityMyWitch;
import de.Keyle.MyPet.entity.types.wolf.EntityMyWolf;
import de.Keyle.MyPet.entity.types.zombie.EntityMyZombie;
import de.Keyle.MyPet.listeners.*;
import de.Keyle.MyPet.skill.*;
import de.Keyle.MyPet.skill.skills.*;
import de.Keyle.MyPet.skill.skilltreeloader.MyPetSkillTreeLoaderJSON;
import de.Keyle.MyPet.skill.skilltreeloader.MyPetSkillTreeLoaderNBT;
import de.Keyle.MyPet.skill.skilltreeloader.MyPetSkillTreeLoaderYAML;
import de.Keyle.MyPet.util.*;
import de.Keyle.MyPet.util.Metrics.Graph;
import de.Keyle.MyPet.util.Metrics.Plotter;
import de.Keyle.MyPet.util.configuration.NBT_Configuration;
import de.Keyle.MyPet.util.configuration.YAML_Configuration;
import de.Keyle.MyPet.util.logger.DebugLogger;
import de.Keyle.MyPet.util.logger.MyPetLogger;
import net.minecraft.server.v1_4_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_4_R1.CraftServer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;

public class MyPetPlugin extends JavaPlugin
{
    private static MyPetPlugin plugin;
    public static MyPetLanguage language;
    private final MyPetTimer timer = new MyPetTimer();
    private File NBTPetFile;
    private DebugLogger debugLogger = null;
    private boolean isReady = false;

    public static final String MyPetVersion = "{@MYPET_VERSION@}";
    public static final String MyPetBuild = "{@BUILD_NUMBER@}";
    public static final String CompatibleMinecraftVersion = "{@MINECRAFT_VERSION@}";

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
        if (isReady)
        {
            debugLogger.info(savePets(true) + " pet/pets saved.");
            for (MyPet myPet : MyPetList.getAllMyPets())
            {
                myPet.removePet();
            }
        }
        getTimer().stopTimer();
        MyPetList.clearList();
        MyPetLogger.setConsole(null);
        Bukkit.getServer().getScheduler().cancelTasks(getPlugin());
        debugLogger.info("MyPet disabled!");
    }

    public void onEnable()
    {
        plugin = this;
        MyPetLogger.setConsole(getServer().getConsoleSender());

        new File(getPlugin().getDataFolder().getAbsolutePath() + File.separator + "skilltrees" + File.separator).mkdirs();
        File delCraftBukkit = new File(getPlugin().getDataFolder().getPath() + File.separator + "craftbukkit.jar");
        if (delCraftBukkit.exists())
        {
            delCraftBukkit.delete();
        }
        MyPetPvP.reset();
        MyPetEconomy.reset();
        MyPetConfiguration.config = this.getConfig();
        MyPetConfiguration.setDefault();
        MyPetConfiguration.loadConfiguration();

        if (debugLogger == null)
        {
            debugLogger = new DebugLogger(MyPetConfiguration.USE_DEBUG_LOGGER);
        }

        String minecraftVersion = ((CraftServer) getServer()).getHandle().getServer().getVersion();

        if (!CompatibleMinecraftVersion.equalsIgnoreCase(minecraftVersion))
        {
            MyPetLogger.write(ChatColor.RED + "---------------------------------------------------------");
            MyPetLogger.write(ChatColor.RED + "This version of MyPet only work with:");
            MyPetLogger.write(ChatColor.RED + "   Minecraft " + CompatibleMinecraftVersion);
            MyPetLogger.write(ChatColor.RED + "MyPet disabled!");
            MyPetLogger.write(ChatColor.RED + "---------------------------------------------------------");
            MyPetUtil.getDebugLogger().warning("---------------------------------------------------------");
            MyPetUtil.getDebugLogger().warning("This version of MyPet only work with:");
            MyPetUtil.getDebugLogger().warning("   Minecraft " + CompatibleMinecraftVersion);
            MyPetUtil.getDebugLogger().warning("MyPet disabled!");
            MyPetUtil.getDebugLogger().warning("---------------------------------------------------------");
            checkForUpdates(minecraftVersion);
            this.setEnabled(false);
            return;
        }

        debugLogger.info("----------- loading MyPet ... -----------");
        debugLogger.info("MyPet " + MyPetVersion + " build: " + MyPetBuild);
        debugLogger.info("Bukkit " + getServer().getVersion());

        debugLogger.info("Plugins: " + Arrays.toString(getServer().getPluginManager().getPlugins()));

        checkForUpdates(CompatibleMinecraftVersion);

        MyPetUtil.getDebugLogger().info("MobEXP table: -------------------------");
        for (EntityType ET : MyPetExperience.mobExp.keySet())
        {
            debugLogger.info("   " + MyPetExperience.mobExp.get(ET).toString());
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
        getCommand("petsendaway").setExecutor(new CommandSendAway());
        getCommand("petstop").setExecutor(new CommandStop());
        getCommand("petrelease").setExecutor(new CommandRelease());
        getCommand("mypet").setExecutor(new CommandHelp());
        getCommand("petinventory").setExecutor(new CommandInventory());
        getCommand("petpickup").setExecutor(new CommandPickup());
        getCommand("petbehavior").setExecutor(new CommandBehavior());
        getCommand("petinfo").setExecutor(new CommandInfo());
        getCommand("petadmin").setExecutor(new CommandAdmin());
        getCommand("petskill").setExecutor(new CommandSkill());
        getCommand("petskilltree").setExecutor(new CommandShowSkillTree());
        getCommand("petchooseskilltree").setExecutor(new CommandChooseSkilltree());
        getCommand("petbeacon").setExecutor(new CommandBeacon());
        getCommand("petrespawn").setExecutor(new CommandRespawn());

        registerSkills();

        File defaultSkillConfig = new File(getPlugin().getDataFolder().getPath() + File.separator + "skilltrees" + File.separator + "default.st");

        if (!defaultSkillConfig.exists())
        {
            try
            {
                InputStream template = getPlugin().getResource("skilltrees/default.st");
                OutputStream out = new FileOutputStream(defaultSkillConfig);

                byte[] buf = new byte[1024];
                int len;
                while ((len = template.read(buf)) > 0)
                {
                    out.write(buf, 0, len);
                }
                template.close();
                out.close();
                MyPetLogger.write("Default skilltree configfile created.");
                debugLogger.info("created default.st");
            }
            catch (IOException ex)
            {
                MyPetLogger.write(ChatColor.RED + "Unable" + ChatColor.RESET + " to create the default.yml!");
                debugLogger.info("unable to create default.yml");
            }
        }

        MyPetSkillTreeLoaderYAML.getSkilltreeLoader().loadSkillTrees(getPlugin().getDataFolder().getPath() + File.separator + "skilltrees");
        MyPetSkillTreeLoaderNBT.getSkilltreeLoader().saveSkillTrees(getPlugin().getDataFolder().getPath() + File.separator + "skilltrees");

        MyPetSkillTreeMobType.clearMobTypes();
        MyPetSkillTreeLoaderNBT.getSkilltreeLoader().loadSkillTrees(getPlugin().getDataFolder().getPath() + File.separator + "skilltrees");
        MyPetSkillTreeLoaderJSON.getSkilltreeLoader().loadSkillTrees(getPlugin().getDataFolder().getPath() + File.separator + "skilltrees");

        try
        {
            Method a = EntityTypes.class.getDeclaredMethod("a", Class.class, String.class, Integer.TYPE);
            a.setAccessible(true);

            // https://github.com/Bukkit/mc-dev/blob/master/net/minecraft/server/EntityTypes.java
            a.invoke(a, EntityMyCreeper.class, "Creeper", 50);
            a.invoke(a, EntityCreeper.class, "Creeper", 50);
            a.invoke(a, EntityMySkeleton.class, "Skeleton", 51);
            a.invoke(a, EntitySkeleton.class, "Skeleton", 51);
            a.invoke(a, EntityMySpider.class, "Spider", 52);
            a.invoke(a, EntitySpider.class, "Spider", 52);
            a.invoke(a, EntityMyGiant.class, "Giant", 53);
            a.invoke(a, EntityGiantZombie.class, "Giant", 53);
            a.invoke(a, EntityMyZombie.class, "Zombie", 54);
            a.invoke(a, EntityZombie.class, "Zombie", 54);
            a.invoke(a, EntityMySlime.class, "Slime", 55);
            a.invoke(a, EntitySlime.class, "Slime", 55);
            a.invoke(a, EntityMyPigZombie.class, "PigZombie", 57);
            a.invoke(a, EntityPigZombie.class, "PigZombie", 57);
            a.invoke(a, EntityMyEnderman.class, "Enderman", 58);
            a.invoke(a, EntityEnderman.class, "Enderman", 58);
            a.invoke(a, EntityMyCaveSpider.class, "CaveSpider", 59);
            a.invoke(a, EntityCaveSpider.class, "CaveSpider", 59);
            a.invoke(a, EntityMySilverfish.class, "Silverfish", 60);
            a.invoke(a, EntitySilverfish.class, "Silverfish", 60);
            a.invoke(a, EntityMyBlaze.class, "Blaze", 61);
            a.invoke(a, EntityBlaze.class, "Blaze", 61);
            a.invoke(a, EntityMyMagmaCube.class, "LavaSlime", 62);
            a.invoke(a, EntityMagmaCube.class, "LavaSlime", 62);
            a.invoke(a, EntityMyBat.class, "Bat", 65);
            a.invoke(a, EntityBat.class, "Bat", 65);
            a.invoke(a, EntityMyWitch.class, "Witch", 66);
            a.invoke(a, EntityWitch.class, "Witch", 66);
            a.invoke(a, EntityMyPig.class, "Pig", 90);
            a.invoke(a, EntityPig.class, "Pig", 90);
            a.invoke(a, EntityMySheep.class, "Sheep", 91);
            a.invoke(a, EntitySheep.class, "Sheep", 91);
            a.invoke(a, EntityMyCow.class, "Cow", 92);
            a.invoke(a, EntityCow.class, "Cow", 92);
            a.invoke(a, EntityMyChicken.class, "Chicken", 93);
            a.invoke(a, EntityChicken.class, "Chicken", 93);
            a.invoke(a, EntityMyWolf.class, "Wolf", 95);
            a.invoke(a, EntityWolf.class, "Wolf", 95);
            a.invoke(a, EntityMyMooshroom.class, "MushroomCow", 96);
            a.invoke(a, EntityMushroomCow.class, "MushroomCow", 96);
            a.invoke(a, EntityMySnowman.class, "SnowMan", 97);
            a.invoke(a, EntitySnowman.class, "SnowMan", 97);
            a.invoke(a, EntityMyOcelot.class, "Ozelot", 98);
            a.invoke(a, EntityOcelot.class, "Ozelot", 98);
            a.invoke(a, EntityMyIronGolem.class, "VillagerGolem", 99);
            a.invoke(a, EntityIronGolem.class, "VillagerGolem", 99);
            a.invoke(a, EntityMyVillager.class, "Villager", 120);
            a.invoke(a, EntityVillager.class, "Villager", 120);

            debugLogger.info("registered MyPet entities.");
        }
        catch (Exception e)
        {
            MyPetLogger.write("version " + MyPetPlugin.plugin.getDescription().getVersion() + ChatColor.RED + " NOT ENABLED");
            e.printStackTrace();
            debugLogger.severe("error while registering MyPet entity.");
            debugLogger.severe(e.getMessage());
            setEnabled(false);
            return;
        }

        debugLogger.info("Pet type: ----------");
        for (MyPetType myPetType : MyPetType.values())
        {
            debugLogger.info("  " + myPetType.getTypeName() + ":");
            debugLogger.info("    damage:     " + MyPet.getStartDamage(myPetType.getMyPetClass()));
            debugLogger.info("    startHP:    " + MyPet.getStartHP(myPetType.getMyPetClass()));
            debugLogger.info("    food:       " + MyPet.getFood(myPetType.getMyPetClass()));
            debugLogger.info("    leashFlags: " + MyPet.getLeashFlags(myPetType.getMyPetClass()));
            debugLogger.info("    speed:      " + MyPet.getStartSpeed(myPetType.getMyPetClass()));
        }

        MyPetPermissions.setup();

        language = new MyPetLanguage(new YAML_Configuration(getPlugin().getDataFolder().getPath() + File.separator + "lang.yml"));
        language.load();


        if (MyPetConfiguration.USE_LEVEL_SYSTEM)
        {
            if (MyPetJSexp.setScriptPath(MyPetPlugin.plugin.getDataFolder().getPath() + File.separator + "exp.js"))
            {
                MyPetLogger.write("Custom EXP-Script loaded!");
                MyPetUtil.getDebugLogger().info("loaded exp.js.");
            }
            else
            {
                MyPetLogger.write("No custom EXP-Script found (exp.js).");
                MyPetUtil.getDebugLogger().info("exp.js not loaded.");
            }
        }

        NBTPetFile = new File(getPlugin().getDataFolder().getPath() + File.separator + "Wolves.MyWolf");
        if (NBTPetFile.exists())
        {
            NBTPetFile.renameTo(new File(getPlugin().getDataFolder().getPath() + File.separator + "Wolves.MyWolf.old"));
            NBTPetFile = new File(getPlugin().getDataFolder().getPath() + File.separator + "Wolves.MyWolf.old");
            loadMyWolfWolves(NBTPetFile);
        }
        NBTPetFile = new File(getPlugin().getDataFolder().getPath() + File.separator + "My.Pets");
        loadPets(NBTPetFile);

        timer.startTimer();

        debugLogger.info("MyPetPlayer: ---------------");
        for (MyPetPlayer myPetPlayer : MyPetPlayer.getPlayerList())
        {
            debugLogger.info("   " + myPetPlayer.toString());
        }
        debugLogger.info("----------------------------");

        if (MyPetConfiguration.SEND_METRICS)
        {
            debugLogger.info("Metrics is activated");
            try
            {
                Metrics metrics = new Metrics(this);

                Graph graphPercent = metrics.createGraph("Percentage of every MyPet type");
                Graph graphCount = metrics.createGraph("Counted MyPets per type");
                Graph graphTotalCount = metrics.createGraph("Total MyPets");

                for (final MyPetType petType : MyPetType.values())
                {
                    Plotter plotter = new Metrics.Plotter(petType.getTypeName())
                    {
                        final MyPetType type = petType;

                        @Override
                        public int getValue()
                        {
                            return MyPetList.countMyPets(type);
                        }
                    };
                    graphPercent.addPlotter(plotter);
                    graphCount.addPlotter(plotter);
                }

                Plotter plotter = new Metrics.Plotter("Total MyPets")
                {
                    @Override
                    public int getValue()
                    {
                        return MyPetList.countMyPets();
                    }
                };
                graphTotalCount.addPlotter(plotter);

                metrics.start();
            }
            catch (IOException e)
            {
                MyPetLogger.write(e.getMessage());
            }
        }
        else
        {
            debugLogger.info("Metrics not activated");
        }

        HeroesDamageFix.reset();
        AncientRpgDamageFix.findAncientRpgPlugin();

        debugLogger.info("version " + MyPetPlugin.plugin.getDescription().getVersion() + " ENABLED");
        MyPetLogger.write("version " + MyPetPlugin.plugin.getDescription().getVersion() + ChatColor.GREEN + " ENABLED");

        for (Player player : getServer().getOnlinePlayers())
        {
            if (MyPetList.hasInactiveMyPets(player))
            {
                for (InactiveMyPet inactiveMyPet : MyPetList.getInactiveMyPets(player))
                {
                    if (MyPetPermissions.has(player, "MyPet.user.leash." + inactiveMyPet.getPetType().getTypeName()))
                    {
                        MyPetList.setMyPetActive(inactiveMyPet);

                        MyPet myPet = MyPetList.getMyPet(player);
                        if (myPet.status == PetState.Dead)
                        {
                            player.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_RespawnIn").replace("%petname%", myPet.petName).replace("%time%", "" + myPet.respawnTime)));
                        }
                        else if (myPet.getLocation().distance(player.getLocation()) < 75)
                        {
                            myPet.createPet();
                        }
                        else
                        {
                            myPet.status = PetState.Despawned;
                        }
                        break;
                    }
                }
            }
        }
        this.isReady = true;
        debugLogger.info("----------- MyPet ready -----------");
    }

    public static void registerSkills()
    {
        MyPetSkills.registerSkill(Inventory.class);
        MyPetSkills.registerSkill(HPregeneration.class);
        MyPetSkills.registerSkill(Pickup.class);
        MyPetSkills.registerSkill(Behavior.class);
        MyPetSkills.registerSkill(Damage.class);
        MyPetSkills.registerSkill(Control.class);
        MyPetSkills.registerSkill(HP.class);
        MyPetSkills.registerSkill(Poison.class);
        MyPetSkills.registerSkill(Ride.class);
        MyPetSkills.registerSkill(Thorns.class);
        MyPetSkills.registerSkill(Fire.class);
        MyPetSkills.registerSkill(Beacon.class);
        MyPetSkills.registerSkill(Wither.class);
    }

    public static boolean checkForUpdates(String compatibleMinecraftVersion)
    {
        UpdateCheck updateCheck = new UpdateCheck();
        if (MyPetConfiguration.CHECK_FOR_UPDATES)
        {
            if (updateCheck.isUpdateAvailable(compatibleMinecraftVersion, MyPetVersion))
            {
                MyPetLogger.write(ChatColor.RED + "Update available!: " + ChatColor.RESET + updateCheck.getLastAvailableUpdate().getTitle());
                MyPetUtil.getDebugLogger().info("Update available!: " + updateCheck.getLastAvailableUpdate().getTitle());
                return true;
            }
            else
            {
                MyPetLogger.write(ChatColor.GREEN + "No" + ChatColor.RESET + " Update available.");
                MyPetUtil.getDebugLogger().info("No Update available");
                return false;
            }
        }
        else
        {
            MyPetLogger.write("Update-Check " + ChatColor.YELLOW + "disabled" + ChatColor.RESET + ".");
            MyPetUtil.getDebugLogger().info("Updates not activated");
            return false;
        }
    }

    int loadPets(File f)
    {
        int petCount = 0;

        NBT_Configuration nbtConfiguration = new NBT_Configuration(f);
        nbtConfiguration.load();
        NBTTagList petList = nbtConfiguration.getNBTTagCompound().getList("Pets");
        if (nbtConfiguration.getNBTTagCompound().hasKey("CleanShutdown"))
        {
            debugLogger.info("Clean shutdown: " + nbtConfiguration.getNBTTagCompound().getBoolean("CleanShutdown"));
        }

        debugLogger.info("Loading players -------------------------");
        if (nbtConfiguration.getNBTTagCompound().hasKey("Players"))
        {
            loadPlayers(nbtConfiguration);
        }
        debugLogger.info("Players loaded -------------------------");

        debugLogger.info("loading Pets: -----------------------------");
        for (int i = 0 ; i < petList.size() ; i++)
        {
            NBTTagCompound myPetNBT = (NBTTagCompound) petList.get(i);
            NBTTagCompound locationNBT = myPetNBT.getCompound("Location");

            double petX = locationNBT.getDouble("X");
            double petY = locationNBT.getDouble("Y");
            double petZ = locationNBT.getDouble("Z");
            float petYaw = 1F;
            if (locationNBT.hasKey("Yaw"))
            {
                petYaw = locationNBT.getFloat("Yaw");
            }
            float petPitch = 1F;
            if (locationNBT.hasKey("Pitch"))
            {
                petPitch = locationNBT.getFloat("Pitch");
            }
            String petWorld = locationNBT.getString("World");
            double petExp = myPetNBT.getDouble("Exp");
            int petHealthNow = myPetNBT.getInt("Health");
            int petRespawnTime = myPetNBT.getInt("Respawntime");
            String petName = myPetNBT.getString("Name");
            String petOwner = myPetNBT.getString("Owner");
            String skillTree = null;
            if (myPetNBT.hasKey("Skilltree"))
            {
                skillTree = myPetNBT.getString("Skilltree");
            }
            int petHunger = 100;
            if (myPetNBT.hasKey("Hunger"))
            {
                petHunger = myPetNBT.getInt("Hunger");
            }
            String petType = "Wolf";
            if (myPetNBT.hasKey("Type"))
            {
                petType = myPetNBT.getString("Type");
            }

            InactiveMyPet inactiveMyPet = new InactiveMyPet(MyPetPlayer.getMyPetPlayer(petOwner));

            inactiveMyPet.setLocation(new Location(Bukkit.getServer().getWorld(petWorld) != null ? MyPetUtil.getServer().getWorld(petWorld) : MyPetUtil.getServer().getWorlds().get(0), petX, petY, petZ, petYaw, petPitch));
            inactiveMyPet.setHealth(petHealthNow);
            inactiveMyPet.setHungerValue(petHunger);
            inactiveMyPet.setRespawnTime(petRespawnTime);
            inactiveMyPet.setPetName(petName);
            inactiveMyPet.setExp(petExp);
            inactiveMyPet.setSkills(myPetNBT.getCompound("Skills"));
            inactiveMyPet.setPetType(MyPetType.valueOf(petType));
            inactiveMyPet.setInfo(myPetNBT.getCompound("Info"));
            if (skillTree != null)
            {
                if (MyPetSkillTreeMobType.getMobTypeByPetType(inactiveMyPet.getPetType()) != null)
                {
                    MyPetSkillTreeMobType mobType = MyPetSkillTreeMobType.getMobTypeByPetType(inactiveMyPet.getPetType());

                    if (mobType.hasSkillTree(skillTree))
                    {
                        inactiveMyPet.setSkillTree(mobType.getSkillTree(skillTree));
                    }
                }
            }

            MyPetList.addInactiveMyPet(inactiveMyPet);

            debugLogger.info("   " + inactiveMyPet.toString());

            petCount++;
        }
        debugLogger.info(petCount + " pet/pets loaded -------------------------");
        MyPetLogger.write("" + ChatColor.YELLOW + petCount + ChatColor.RESET + " pet/pets loaded");
        return petCount;
    }

    int loadMyWolfWolves(File f)
    {
        int wolfCount = 0;

        NBT_Configuration nbtConfiguration = new NBT_Configuration(f);
        nbtConfiguration.load();
        NBTTagList wolfList = nbtConfiguration.getNBTTagCompound().getList("Wolves");

        debugLogger.info("loading Wolves: -----------------------------");
        for (int i = 0 ; i < wolfList.size() ; i++)
        {
            NBTTagCompound myMolfNBT = (NBTTagCompound) wolfList.get(i);

            NBTTagCompound locationNBT = myMolfNBT.getCompound("Location");
            double wolfX = locationNBT.getDouble("X");
            double wolfY = locationNBT.getDouble("Y");
            double wolfZ = locationNBT.getDouble("Z");
            String wolfWorld = locationNBT.getString("World");

            double wolfExp = myMolfNBT.getDouble("Exp");
            int wolfHealthNow = myMolfNBT.getInt("Health");
            String wolfName = myMolfNBT.getString("Name");
            String wolfOwner = myMolfNBT.getString("Owner");

            InactiveMyPet inactiveMyPet = new InactiveMyPet(MyPetPlayer.getMyPetPlayer(wolfOwner));

            inactiveMyPet.setLocation(new Location(Bukkit.getServer().getWorld(wolfWorld) != null ? MyPetUtil.getServer().getWorld(wolfWorld) : MyPetUtil.getServer().getWorlds().get(0), wolfX, wolfY, wolfZ));
            inactiveMyPet.setHealth(wolfHealthNow);
            inactiveMyPet.setPetName(wolfName);
            inactiveMyPet.setExp(wolfExp);
            inactiveMyPet.setPetType(MyPetType.valueOf("Wolf"));

            MyPetList.addInactiveMyPet(inactiveMyPet);

            debugLogger.info("   " + inactiveMyPet.toString());

            wolfCount++;
        }
        debugLogger.info(wolfCount + " wolf/wolves converted -------------------------");
        MyPetLogger.write("" + ChatColor.YELLOW + wolfCount + ChatColor.RESET + " wolf/wolves converted");
        return wolfCount;
    }

    public int savePets(boolean shutdown)
    {
        int petCount = 0;
        NBT_Configuration nbtConfiguration = new NBT_Configuration(NBTPetFile);
        NBTTagList petNBTlist = new NBTTagList();

        for (MyPet myPet : MyPetList.getAllMyPets())
        {
            NBTTagCompound petNBT = new NBTTagCompound();
            NBTTagCompound locationNBT = new NBTTagCompound("Location");

            locationNBT.setDouble("X", myPet.getLocation().getX());
            locationNBT.setDouble("Y", myPet.getLocation().getY());
            locationNBT.setDouble("Z", myPet.getLocation().getZ());
            locationNBT.setFloat("Yaw", myPet.getLocation().getYaw());
            locationNBT.setFloat("Pitch", myPet.getLocation().getPitch());
            locationNBT.setString("World", myPet.getLocation().getWorld().getName());

            petNBT.setString("Type", myPet.getPetType().getTypeName());
            petNBT.setString("Owner", myPet.getOwner().getName());
            petNBT.setCompound("Location", locationNBT);
            petNBT.setInt("Health", myPet.getHealth());
            petNBT.setInt("Respawntime", myPet.respawnTime);
            petNBT.setInt("Hunger", myPet.getHungerValue());
            petNBT.setString("Name", myPet.petName);
            petNBT.setDouble("Exp", myPet.getExperience().getExp());
            petNBT.setCompound("Info", myPet.getExtendedInfo());
            if (myPet.getSkillTree() != null)
            {
                petNBT.setString("Skilltree", myPet.getSkillTree().getName());
            }

            NBTTagCompound skillsNBT = new NBTTagCompound("Skills");
            Collection<MyPetGenericSkill> skillList = myPet.getSkills().getSkills();
            if (skillList.size() > 0)
            {
                for (MyPetGenericSkill skill : skillList)
                {
                    NBTTagCompound s = skill.save();
                    if (s != null)
                    {
                        skillsNBT.set(skill.getName(), s);
                    }
                }
            }
            petNBT.set("Skills", skillsNBT);
            petNBTlist.add(petNBT);
            petCount++;
        }
        for (InactiveMyPet inactiveMyPet : MyPetList.getAllInactiveMyPets())
        {
            NBTTagCompound petNBT = new NBTTagCompound();
            NBTTagCompound locationNBT = new NBTTagCompound("Location");

            locationNBT.setDouble("X", inactiveMyPet.getLocation().getX());
            locationNBT.setDouble("Y", inactiveMyPet.getLocation().getY());
            locationNBT.setDouble("Z", inactiveMyPet.getLocation().getZ());
            locationNBT.setFloat("Yaw", inactiveMyPet.getLocation().getYaw());
            locationNBT.setFloat("Pitch", inactiveMyPet.getLocation().getPitch());
            locationNBT.setString("World", inactiveMyPet.getLocation().getWorld().getName());

            petNBT.setString("Type", inactiveMyPet.getPetType().getTypeName());
            petNBT.setString("Owner", inactiveMyPet.getPetOwner().getName());
            petNBT.setCompound("Location", locationNBT);
            petNBT.setInt("Health", inactiveMyPet.getHealth());
            petNBT.setInt("Hunger", inactiveMyPet.getHungerValue());
            petNBT.setInt("Respawntime", inactiveMyPet.getRespawnTime());
            petNBT.setString("Name", inactiveMyPet.getPetName());
            petNBT.setDouble("Exp", inactiveMyPet.getExp());
            petNBT.setCompound("Info", inactiveMyPet.getInfo());
            if (inactiveMyPet.getSkillTree() != null)
            {
                petNBT.setString("Skilltree", inactiveMyPet.getSkillTree().getName());
            }

            petNBT.set("Skills", inactiveMyPet.getSkills());

            petNBTlist.add(petNBT);
            petCount++;
        }
        String[] version = this.getDescription().getVersion().split(" \\(");
        nbtConfiguration.getNBTTagCompound().setString("Version", version[0]);
        nbtConfiguration.getNBTTagCompound().setBoolean("CleanShutdown", shutdown);
        nbtConfiguration.getNBTTagCompound().set("Pets", petNBTlist);
        nbtConfiguration.getNBTTagCompound().set("Players", savePlayers());
        nbtConfiguration.save();
        return petCount;
    }

    private NBTTagList savePlayers()
    {
        NBTTagList playerNBTlist = new NBTTagList();

        for (MyPetPlayer myPetPlayer : MyPetPlayer.getPlayerList())
        {
            if (myPetPlayer.hasCustomData())
            {
                NBTTagCompound playerNBT = new NBTTagCompound(myPetPlayer.getName());

                playerNBT.setString("Name", myPetPlayer.getName());
                playerNBT.setBoolean("AutoRespawn", myPetPlayer.hasAutoRespawnEnabled());
                playerNBT.setInt("AutoRespawnMin", myPetPlayer.getAutoRespawnMin());

                playerNBTlist.add(playerNBT);
            }
        }

        return playerNBTlist;
    }

    private void loadPlayers(NBT_Configuration nbtConfiguration)
    {
        nbtConfiguration.load();
        NBTTagList playerList = nbtConfiguration.getNBTTagCompound().getList("Players");

        for (int i = 0 ; i < playerList.size() ; i++)
        {
            NBTTagCompound myplayerNBT = (NBTTagCompound) playerList.get(i);

            MyPetPlayer petPlayer = MyPetPlayer.getMyPetPlayer(myplayerNBT.getString("Name"));

            if (myplayerNBT.hasKey("AutoRespawn"))
            {
                petPlayer.setAutoRespawnEnabled(myplayerNBT.getBoolean("AutoRespawn"));
            }
            if (myplayerNBT.hasKey("AutoRespawnMin"))
            {
                petPlayer.setAutoRespawnMin(myplayerNBT.getInt("AutoRespawnMin"));
            }
        }
    }

    public DebugLogger getDebugLogger()
    {
        return debugLogger;
    }
}