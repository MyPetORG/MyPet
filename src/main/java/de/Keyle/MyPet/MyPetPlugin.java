/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2013 Keyle
 * MyPet is licensed under the GNU Lesser General Public License.
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet;

import de.Keyle.MyPet.chatcommands.*;
import de.Keyle.MyPet.entity.types.*;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.entity.types.bat.EntityMyBat;
import de.Keyle.MyPet.entity.types.blaze.EntityMyBlaze;
import de.Keyle.MyPet.entity.types.cavespider.EntityMyCaveSpider;
import de.Keyle.MyPet.entity.types.chicken.EntityMyChicken;
import de.Keyle.MyPet.entity.types.cow.EntityMyCow;
import de.Keyle.MyPet.entity.types.creeper.EntityMyCreeper;
import de.Keyle.MyPet.entity.types.enderman.EntityMyEnderman;
import de.Keyle.MyPet.entity.types.ghast.EntityMyGhast;
import de.Keyle.MyPet.entity.types.giant.EntityMyGiant;
import de.Keyle.MyPet.entity.types.horse.EntityMyHorse;
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
import de.Keyle.MyPet.entity.types.squid.EntityMySquid;
import de.Keyle.MyPet.entity.types.villager.EntityMyVillager;
import de.Keyle.MyPet.entity.types.witch.EntityMyWitch;
import de.Keyle.MyPet.entity.types.wither.EntityMyWither;
import de.Keyle.MyPet.entity.types.wolf.EntityMyWolf;
import de.Keyle.MyPet.entity.types.zombie.EntityMyZombie;
import de.Keyle.MyPet.listeners.*;
import de.Keyle.MyPet.skill.MyPetSkillTreeMobType;
import de.Keyle.MyPet.skill.MyPetSkills;
import de.Keyle.MyPet.skill.MyPetSkillsInfo;
import de.Keyle.MyPet.skill.skills.implementation.*;
import de.Keyle.MyPet.skill.skills.info.*;
import de.Keyle.MyPet.skill.skilltreeloader.MyPetSkillTreeLoader;
import de.Keyle.MyPet.skill.skilltreeloader.MyPetSkillTreeLoaderJSON;
import de.Keyle.MyPet.skill.skilltreeloader.MyPetSkillTreeLoaderNBT;
import de.Keyle.MyPet.skill.skilltreeloader.MyPetSkillTreeLoaderYAML;
import de.Keyle.MyPet.util.*;
import de.Keyle.MyPet.util.configuration.NBT_Configuration;
import de.Keyle.MyPet.util.configuration.YAML_Configuration;
import de.Keyle.MyPet.util.locale.MyPetLocales;
import de.Keyle.MyPet.util.logger.DebugLogger;
import de.Keyle.MyPet.util.logger.MyPetLogger;
import de.Keyle.MyPet.util.support.*;
import net.minecraft.server.v1_6_R2.EntityTypes;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_6_R2.CraftServer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.mcstats.Metrics;
import org.mcstats.Metrics.Graph;
import org.mcstats.Metrics.Plotter;
import org.spout.nbt.ByteTag;
import org.spout.nbt.CompoundTag;
import org.spout.nbt.ListTag;
import org.spout.nbt.StringTag;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

public class MyPetPlugin extends JavaPlugin implements IScheduler
{
    private static MyPetPlugin plugin;
    private File NBTPetFile;
    private boolean isReady = false;
    private int autoSaveTimer = 0;

    public static MyPetPlugin getPlugin()
    {
        return plugin;
    }

    public void onDisable()
    {
        if (isReady)
        {
            int petCount = savePets(true);
            MyPetLogger.write("" + ChatColor.YELLOW + petCount + ChatColor.RESET + " pet(s) saved");
            for (MyPet myPet : MyPetList.getAllActiveMyPets())
            {
                myPet.removePet(myPet.wantToRespawn());
            }
            MyPetList.clearList();
        }
        MyPetTimer.reset();
        MyPetLogger.setConsole(null);
        Bukkit.getServer().getScheduler().cancelTasks(getPlugin());
        DebugLogger.info("MyPet disabled!");
    }

    public void onEnable()
    {
        plugin = this;
        this.isReady = false;
        new File(getPlugin().getDataFolder().getAbsolutePath() + File.separator + "skilltrees" + File.separator).mkdirs();
        new File(getPlugin().getDataFolder().getAbsolutePath() + File.separator + "backups" + File.separator).mkdirs();
        new File(getPlugin().getDataFolder().getAbsolutePath() + File.separator + "locale" + File.separator).mkdirs();
        new File(getPlugin().getDataFolder().getAbsolutePath() + File.separator + "logs" + File.separator).mkdirs();
        NBTPetFile = new File(getPlugin().getDataFolder().getPath() + File.separator + "My.Pets");

        MyPetVersion.reset();
        MyPetLogger.setConsole(getServer().getConsoleSender());
        MyPetPvP.reset();
        MyPetEconomy.reset();
        MyPetConfiguration.config = this.getConfig();
        MyPetConfiguration.setDefault();
        MyPetConfiguration.loadConfiguration();
        DebugLogger.setup();

        String minecraftVersion = ((CraftServer) getServer()).getHandle().getServer().getVersion();
        if (!MyPetVersion.getMinecraftVersion().equalsIgnoreCase(minecraftVersion))
        {
            MyPetLogger.write(ChatColor.RED + "---------------------------------------------------------");
            MyPetLogger.write(ChatColor.RED + "This version of MyPet only works with:");
            MyPetLogger.write(ChatColor.RED + "   Minecraft " + MyPetVersion.getMinecraftVersion());
            MyPetLogger.write(ChatColor.RED + "MyPet disabled!");
            MyPetLogger.write(ChatColor.RED + "---------------------------------------------------------");
            this.setEnabled(false);
            return;
        }

        DebugLogger.info("----------- loading MyPet ... -----------");
        DebugLogger.info("MyPet " + MyPetVersion.getMyPetVersion() + " build: " + MyPetVersion.getMyPetBuild());
        DebugLogger.info("Bukkit " + getServer().getVersion());

        DebugLogger.info("Plugins: " + Arrays.toString(getServer().getPluginManager().getPlugins()));

        MyPetPlayerListener playerListener = new MyPetPlayerListener();
        getServer().getPluginManager().registerEvents(playerListener, this);

        MyPetVehicleListener vehicleListener = new MyPetVehicleListener();
        getServer().getPluginManager().registerEvents(vehicleListener, this);

        MyPetEntityListener entityListener = new MyPetEntityListener();
        getServer().getPluginManager().registerEvents(entityListener, this);

        MyPetLevelUpListener levelupListener = new MyPetLevelUpListener();
        getServer().getPluginManager().registerEvents(levelupListener, this);

        MyPetWorldListener worldListener = new MyPetWorldListener();
        getServer().getPluginManager().registerEvents(worldListener, this);

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
        getCommand("pettype").setExecutor(new CommandPetType());
        getCommand("petcapturehelper").setExecutor(new CommandCaptureHelper());

        registerSkillsInfo();
        registerSkills();

        File defaultSkillConfigNBT = new File(getPlugin().getDataFolder().getPath() + File.separator + "skilltrees" + File.separator + "default.st");
        File defaultSkillConfigYAML = new File(getPlugin().getDataFolder().getPath() + File.separator + "skilltrees" + File.separator + "default.yml");
        File defaultSkillConfigJSON = new File(getPlugin().getDataFolder().getPath() + File.separator + "skilltrees" + File.separator + "default.json");

        if (!defaultSkillConfigNBT.exists() && !defaultSkillConfigYAML.exists() && !defaultSkillConfigJSON.exists())
        {
            try
            {
                InputStream template = getPlugin().getResource("skilltrees/default.st");
                OutputStream out = new FileOutputStream(defaultSkillConfigNBT);

                byte[] buf = new byte[1024];
                int len;
                while ((len = template.read(buf)) > 0)
                {
                    out.write(buf, 0, len);
                }
                template.close();
                out.close();
                MyPetLogger.write("Default skilltree configfile created.");
                DebugLogger.info("created default.st");
            }
            catch (IOException ex)
            {
                MyPetLogger.write(ChatColor.RED + "Unable" + ChatColor.RESET + " to create the default.st!");
                DebugLogger.info("unable to create default.st");
            }
        }

        String[] petTypes = new String[MyPetType.values().length];
        for (int i = 0 ; i < MyPetType.values().length ; i++)
        {
            petTypes[i] = MyPetType.values()[i].getTypeName();
        }

        MyPetSkillTreeMobType.clearMobTypes();
        MyPetSkillTreeLoaderNBT.getSkilltreeLoader().loadSkillTrees(getPlugin().getDataFolder().getPath() + File.separator + "skilltrees", petTypes);
        MyPetSkillTreeLoaderYAML.getSkilltreeLoader().loadSkillTrees(getPlugin().getDataFolder().getPath() + File.separator + "skilltrees", petTypes);
        MyPetSkillTreeLoaderJSON.getSkilltreeLoader().loadSkillTrees(getPlugin().getDataFolder().getPath() + File.separator + "skilltrees", petTypes);

        for (MyPetType mobType : MyPetType.values())
        {
            MyPetSkillTreeMobType skillTreeMobType = MyPetSkillTreeMobType.getMobTypeByName(mobType.getTypeName());
            MyPetSkillTreeLoader.addDefault(skillTreeMobType);
            MyPetSkillTreeLoader.manageInheritance(skillTreeMobType);
        }

        registerMyPetEntity(EntityMyCreeper.class, "Creeper", 50);
        registerMyPetEntity(EntityMySkeleton.class, "Skeleton", 51);
        registerMyPetEntity(EntityMySpider.class, "Spider", 52);
        registerMyPetEntity(EntityMyGiant.class, "Giant", 53);
        registerMyPetEntity(EntityMyZombie.class, "Zombie", 54);
        registerMyPetEntity(EntityMySlime.class, "Slime", 55);
        registerMyPetEntity(EntityMyGhast.class, "Ghast", 56);
        registerMyPetEntity(EntityMyPigZombie.class, "PigZombie", 57);
        registerMyPetEntity(EntityMyEnderman.class, "Enderman", 58);
        registerMyPetEntity(EntityMyCaveSpider.class, "CaveSpider", 59);
        registerMyPetEntity(EntityMySilverfish.class, "Silverfish", 60);
        registerMyPetEntity(EntityMyBlaze.class, "Blaze", 61);
        registerMyPetEntity(EntityMyMagmaCube.class, "LavaSlime", 62);
        registerMyPetEntity(EntityMyWither.class, "WitherBoss", 64);
        registerMyPetEntity(EntityMyBat.class, "Bat", 65);
        registerMyPetEntity(EntityMyWitch.class, "Witch", 66);
        registerMyPetEntity(EntityMyPig.class, "Pig", 90);
        registerMyPetEntity(EntityMySheep.class, "Sheep", 91);
        registerMyPetEntity(EntityMyCow.class, "Cow", 92);
        registerMyPetEntity(EntityMyChicken.class, "Chicken", 93);
        registerMyPetEntity(EntityMySquid.class, "Squid", 94);
        registerMyPetEntity(EntityMyWolf.class, "Wolf", 95);
        registerMyPetEntity(EntityMyMooshroom.class, "MushroomCow", 96);
        registerMyPetEntity(EntityMySnowman.class, "SnowMan", 97);
        registerMyPetEntity(EntityMyOcelot.class, "Ozelot", 98);
        registerMyPetEntity(EntityMyIronGolem.class, "VillagerGolem", 99);
        registerMyPetEntity(EntityMyHorse.class, "EntityHorse", 100);
        registerMyPetEntity(EntityMyVillager.class, "Villager", 120);

        DebugLogger.info("Pet type: ----------");
        for (MyPetType myPetType : MyPetType.values())
        {
            DebugLogger.info("  " + myPetType.getTypeName() + " { " +
                    "startHP:" + MyPet.getStartHP(myPetType.getMyPetClass()) + ", " +
                    "speed:" + MyPet.getStartSpeed(myPetType.getMyPetClass()) + ", " +
                    "food:" + MyPet.getFood(myPetType.getMyPetClass()) + ", " +
                    "leashFlags:" + MyPet.getLeashFlags(myPetType.getMyPetClass()) + " }");
        }

        new MyPetLocales();

        File groupsFile = new File(getPlugin().getDataFolder().getPath() + File.separator + "worldgroups.yml");

        if (MyPetBackup.MAKE_BACKUPS)
        {
            new MyPetBackup(NBTPetFile, new File(getPlugin().getDataFolder().getPath() + File.separator + "backups" + File.separator));
        }
        loadGroups(groupsFile);
        loadPets(NBTPetFile);

        MyPetTimer.startTimer();

        MobArena.findPlugin();
        Minigames.findPlugin();
        PvPArena.findPlugin();
        BattleArena.findPlugin();
        SurvivalGames.findPlugin();
        MyHungerGames.findPlugin();

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
            plotter = new Metrics.Plotter("Active MyPets")
            {
                @Override
                public int getValue()
                {
                    return MyPetList.countActiveMyPets();
                }
            };
            graphTotalCount.addPlotter(plotter);

            boolean metricsActive = metrics.start();
            DebugLogger.info("Metrics " + (metricsActive ? "" : "not ") + "activated");
        }
        catch (IOException e)
        {
            MyPetLogger.write(e.getMessage());
        }

        MyPetLogger.write("version " + MyPetVersion.getMyPetVersion() + "-b" + MyPetVersion.getMyPetBuild() + ChatColor.GREEN + " ENABLED");

        for (Player player : getServer().getOnlinePlayers())
        {
            if (MyPetPlayer.isMyPetPlayer(player))
            {
                MyPetPlayer myPetPlayer = MyPetPlayer.getMyPetPlayer(player);
                MyPetWorldGroup joinGroup = MyPetWorldGroup.getGroup(player.getWorld().getName());
                if (joinGroup != null && !myPetPlayer.hasMyPet() && myPetPlayer.hasMyPetInWorldGroup(joinGroup.getName()))
                {
                    UUID groupMyPetUUID = myPetPlayer.getMyPetForWorldGroup(joinGroup.getName());
                    for (InactiveMyPet inactiveMyPet : myPetPlayer.getInactiveMyPets())
                    {
                        if (inactiveMyPet.getUUID().equals(groupMyPetUUID))
                        {
                            MyPetList.setMyPetActive(inactiveMyPet);
                            break;
                        }
                    }
                    if (!myPetPlayer.hasMyPet())
                    {
                        myPetPlayer.setMyPetForWorldGroup(joinGroup.getName(), null);
                    }
                }
                if (myPetPlayer.hasMyPet())
                {
                    MyPet myPet = MyPetList.getMyPet(player);
                    if (myPet.getStatus() == PetState.Dead)
                    {
                        player.sendMessage(MyPetUtil.formatText(MyPetLocales.getString("Message.RespawnIn", MyPetBukkitUtil.getPlayerLanguage(player)), myPet.getPetName(), myPet.getRespawnTime()));
                    }
                    else if (myPet.wantToRespawn() && myPet.getLocation().getWorld() == player.getLocation().getWorld() && myPet.getLocation().distance(player.getLocation()) < 75)
                    {
                        myPet.createPet();
                    }
                    else
                    {
                        myPet.setStatus(PetState.Despawned);
                    }
                }
            }
        }
        this.isReady = true;
        savePets(false);
        MyPetTimer.addTask(this);
        DebugLogger.info("----------- MyPet ready -----------");
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
        MyPetSkills.registerSkill(Lightning.class);
        MyPetSkills.registerSkill(Slow.class);
        MyPetSkills.registerSkill(Knockback.class);
        MyPetSkills.registerSkill(Ranged.class);
        MyPetSkills.registerSkill(Sprint.class);
    }

    public static void registerSkillsInfo()
    {
        MyPetSkillsInfo.registerSkill(InventoryInfo.class);
        MyPetSkillsInfo.registerSkill(HPregenerationInfo.class);
        MyPetSkillsInfo.registerSkill(PickupInfo.class);
        MyPetSkillsInfo.registerSkill(BehaviorInfo.class);
        MyPetSkillsInfo.registerSkill(DamageInfo.class);
        MyPetSkillsInfo.registerSkill(ControlInfo.class);
        MyPetSkillsInfo.registerSkill(HPInfo.class);
        MyPetSkillsInfo.registerSkill(PoisonInfo.class);
        MyPetSkillsInfo.registerSkill(RideInfo.class);
        MyPetSkillsInfo.registerSkill(ThornsInfo.class);
        MyPetSkillsInfo.registerSkill(FireInfo.class);
        MyPetSkillsInfo.registerSkill(BeaconInfo.class);
        MyPetSkillsInfo.registerSkill(WitherInfo.class);
        MyPetSkillsInfo.registerSkill(LightningInfo.class);
        MyPetSkillsInfo.registerSkill(SlowInfo.class);
        MyPetSkillsInfo.registerSkill(KnockbackInfo.class);
        MyPetSkillsInfo.registerSkill(RangedInfo.class);
        MyPetSkillsInfo.registerSkill(SprintInfo.class);
    }

    @SuppressWarnings("unchecked")
    public static boolean registerMyPetEntity(Class<? extends EntityMyPet> myPetEntityClass, String entityTypeName, int entityTypeId)
    {
        try
        {
            Field EntityTypes_c = EntityTypes.class.getDeclaredField("c");
            Field EntityTypes_e = EntityTypes.class.getDeclaredField("e");
            EntityTypes_c.setAccessible(true);
            EntityTypes_e.setAccessible(true);

            Map<Class, String> c = (Map) EntityTypes_c.get(EntityTypes_c);
            Map<Class, Integer> e = (Map) EntityTypes_e.get(EntityTypes_e);

            Iterator cIterator = c.keySet().iterator();
            while (cIterator.hasNext())
            {
                Class clazz = (Class) cIterator.next();
                if (clazz.getCanonicalName().equals(myPetEntityClass.getCanonicalName()))
                {
                    cIterator.remove();
                }
            }

            Iterator eIterator = e.keySet().iterator();
            while (eIterator.hasNext())
            {
                Class clazz = (Class) eIterator.next();
                if (clazz.getCanonicalName().equals(myPetEntityClass.getCanonicalName()))
                {
                    eIterator.remove();
                }
            }

            c.put(myPetEntityClass, entityTypeName);
            e.put(myPetEntityClass, entityTypeId);

            return true;
        }
        catch (Exception e)
        {
            DebugLogger.severe("error while registering " + myPetEntityClass.getCanonicalName());
            DebugLogger.severe(e.getMessage());
            return false;
        }
    }

    int loadPets(File f)
    {
        if (!f.exists())
        {
            MyPetLogger.write(ChatColor.YELLOW + "0" + ChatColor.RESET + " pet(s) loaded");
            return 0;
        }
        int petCount = 0;

        NBT_Configuration nbtConfiguration = new NBT_Configuration(f);
        if (!nbtConfiguration.load())
        {
            return 0;
        }
        ListTag petList = (ListTag) nbtConfiguration.getNBTCompound().getValue().get("Pets");
        if (nbtConfiguration.getNBTCompound().getValue().containsKey("CleanShutdown"))
        {
            DebugLogger.info("Clean shutdown: " + ((ByteTag) nbtConfiguration.getNBTCompound().getValue().get("CleanShutdown")).getBooleanValue());
        }

        DebugLogger.info("Loading players -------------------------");
        if (nbtConfiguration.getNBTCompound().getValue().containsKey("Players"))
        {
            DebugLogger.info(loadPlayers(nbtConfiguration) + " PetPlayer(s) loaded");
        }
        DebugLogger.info("-----------------------------------------");

        DebugLogger.info("loading Pets: -----------------------------");
        for (int i = 0 ; i < petList.getValue().size() ; i++)
        {
            CompoundTag myPetNBT = (CompoundTag) petList.getValue().get(i);
            String petOwner = ((StringTag) myPetNBT.getValue().get("Owner")).getValue();
            InactiveMyPet inactiveMyPet = new InactiveMyPet(MyPetPlayer.getMyPetPlayer(petOwner));
            inactiveMyPet.load(myPetNBT);

            MyPetList.addInactiveMyPet(inactiveMyPet);

            DebugLogger.info("   " + inactiveMyPet.toString());

            petCount++;
        }
        MyPetLogger.write("" + ChatColor.YELLOW + petCount + ChatColor.RESET + " pet(s) loaded");
        return petCount;
    }

    public int savePets(boolean shutdown)
    {
        autoSaveTimer = MyPetConfiguration.AUTOSAVE_TIME;
        int petCount = 0;
        NBT_Configuration nbtConfiguration = new NBT_Configuration(NBTPetFile);
        List<CompoundTag> petList = new ArrayList<CompoundTag>();

        for (MyPet myPet : MyPetList.getAllActiveMyPets())
        {
            CompoundTag petNBT = myPet.save();
            petList.add(petNBT);
            petCount++;
        }
        for (InactiveMyPet inactiveMyPet : MyPetList.getAllInactiveMyPets())
        {
            CompoundTag petNBT = inactiveMyPet.save();
            petList.add(petNBT);
            petCount++;
        }
        nbtConfiguration.getNBTCompound().getValue().put("Version", new StringTag("Version", MyPetVersion.getMyPetVersion()));
        nbtConfiguration.getNBTCompound().getValue().put("Build", new StringTag("Build", MyPetVersion.getMyPetBuild()));
        nbtConfiguration.getNBTCompound().getValue().put("CleanShutdown", new ByteTag("CleanShutdown", shutdown));
        nbtConfiguration.getNBTCompound().getValue().put("Pets", new ListTag<CompoundTag>("Pets", CompoundTag.class, petList));
        nbtConfiguration.getNBTCompound().getValue().put("Players", savePlayers());
        nbtConfiguration.save();
        return petCount;
    }

    private ListTag savePlayers()
    {
        List<CompoundTag> playerList = new ArrayList<CompoundTag>();
        for (MyPetPlayer myPetPlayer : MyPetPlayer.getMyPetPlayers())
        {
            if (myPetPlayer.hasCustomData())
            {
                playerList.add(myPetPlayer.save());
            }
        }
        return new ListTag<CompoundTag>("Players", CompoundTag.class, playerList);
    }

    private int loadPlayers(NBT_Configuration nbtConfiguration)
    {
        int playerCount = 0;
        ListTag playerList = (ListTag) nbtConfiguration.getNBTCompound().getValue().get("Players");

        for (int i = 0 ; i < playerList.getValue().size() ; i++)
        {
            CompoundTag myplayerNBT = (CompoundTag) playerList.getValue().get(i);
            MyPetPlayer petPlayer = MyPetPlayer.getMyPetPlayer(((StringTag) myplayerNBT.getValue().get("Name")).getValue());
            petPlayer.load(myplayerNBT);

            playerCount++;
            DebugLogger.info("   " + petPlayer);
        }
        return playerCount;
    }

    private int loadGroups(File f)
    {
        YAML_Configuration yamlConfiguration = new YAML_Configuration(f);
        FileConfiguration config = yamlConfiguration.getConfig();

        if (config == null)
        {
            return 0;
        }

        MyPetWorldGroup.clearGroups();

        Set<String> nodes;
        try
        {
            nodes = config.getConfigurationSection("Groups").getKeys(false);
        }
        catch (NullPointerException e)
        {
            nodes = new HashSet<String>();
            MyPetLogger.write("No groups found. Everything will be in 'default' group.");
        }

        DebugLogger.info("--- Load WorldGroups ---------------------------");
        if (nodes.size() == 0)
        {
            List<String> worldNames = new ArrayList<String>();
            MyPetWorldGroup defaultGroup = new MyPetWorldGroup("default");
            defaultGroup.registerGroup();
            for (org.bukkit.World world : this.getServer().getWorlds())
            {
                MyPetLogger.write("added " + ChatColor.GOLD + world.getName() + ChatColor.RESET + " to 'default' group.");
                worldNames.add(world.getName());
                DebugLogger.info("   added " + world.getName() + " to " + defaultGroup.getName());
                defaultGroup.addWorld(world.getName());
            }
            config.set("Groups.default", worldNames);
            yamlConfiguration.saveConfig();
        }
        else
        {
            for (String node : nodes)
            {
                List<String> worlds = config.getStringList("Groups." + node);
                if (worlds.size() > 0)
                {
                    MyPetWorldGroup newGroup = new MyPetWorldGroup(node);
                    for (String world : worlds)
                    {
                        if (getServer().getWorld(world) != null)
                        {
                            DebugLogger.info("   added '" + world + "' to '" + newGroup.getName() + "'");
                            newGroup.addWorld(world);
                        }
                    }
                    if (newGroup.getWorlds().size() > 0)
                    {
                        DebugLogger.info("   registered '" + newGroup.getName() + "' group");
                        newGroup.registerGroup();
                    }
                }
            }

            MyPetWorldGroup defaultGroup = null;
            for (MyPetWorldGroup group : MyPetWorldGroup.getGroups())
            {
                if (group.getName().equalsIgnoreCase("default"))
                {
                    defaultGroup = group;
                    break;
                }
            }
            if (defaultGroup == null)
            {
                defaultGroup = new MyPetWorldGroup("default");
                defaultGroup.registerGroup();
                DebugLogger.info("   registered 'default' group");
            }

            boolean saveConfig = false;
            for (org.bukkit.World world : getServer().getWorlds())
            {
                if (MyPetWorldGroup.getGroup(world.getName()) == null)
                {
                    MyPetLogger.write("added " + ChatColor.GOLD + world.getName() + ChatColor.RESET + " to 'default' group.");
                    defaultGroup.addWorld(world.getName());
                    saveConfig = true;
                }
            }
            if (saveConfig)
            {
                config.set("Groups.default", defaultGroup.getWorlds());
                yamlConfiguration.saveConfig();
            }
        }
        DebugLogger.info("-------------------------------------------------");
        return 0;
    }

    @Override
    public void schedule()
    {
        if (MyPetConfiguration.AUTOSAVE_TIME > 0 && autoSaveTimer-- <= 0)
        {
            MyPetPlugin.getPlugin().savePets(false);
            autoSaveTimer = MyPetConfiguration.AUTOSAVE_TIME;
        }
    }

    public File getFile()
    {
        return super.getFile();
    }
}