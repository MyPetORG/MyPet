/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2014 Keyle
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

import de.Keyle.MyPet.api.util.IScheduler;
import de.Keyle.MyPet.commands.*;
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
import de.Keyle.MyPet.skill.experience.JavaScript;
import de.Keyle.MyPet.skill.skills.Skills;
import de.Keyle.MyPet.skill.skills.SkillsInfo;
import de.Keyle.MyPet.skill.skills.implementation.*;
import de.Keyle.MyPet.skill.skills.info.*;
import de.Keyle.MyPet.skill.skilltree.SkillTreeMobType;
import de.Keyle.MyPet.skill.skilltreeloader.SkillTreeLoader;
import de.Keyle.MyPet.skill.skilltreeloader.SkillTreeLoaderJSON;
import de.Keyle.MyPet.skill.skilltreeloader.SkillTreeLoaderNBT;
import de.Keyle.MyPet.skill.skilltreeloader.SkillTreeLoaderYAML;
import de.Keyle.MyPet.util.*;
import de.Keyle.MyPet.util.Timer;
import de.Keyle.MyPet.util.configuration.ConfigurationNBT;
import de.Keyle.MyPet.util.configuration.ConfigurationYAML;
import de.Keyle.MyPet.util.locale.Locales;
import de.Keyle.MyPet.util.logger.DebugLogger;
import de.Keyle.MyPet.util.logger.MyPetLogger;
import de.Keyle.MyPet.util.player.MyPetPlayer;
import de.Keyle.MyPet.util.player.UUIDFetcher;
import de.Keyle.MyPet.util.support.Economy;
import de.Keyle.MyPet.util.support.PluginSupportManager;
import de.Keyle.MyPet.util.support.PvPChecker;
import de.Keyle.MyPet.util.support.arenas.*;
import de.keyle.knbt.*;
import net.minecraft.server.v1_7_R3.EntityTypes;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;
import org.mcstats.Metrics;
import org.mcstats.Metrics.Graph;
import org.mcstats.Metrics.Plotter;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

public class MyPetPlugin extends JavaPlugin implements IScheduler {
    private static MyPetPlugin plugin;
    private File NBTPetFile;
    private boolean isReady = false;
    private int autoSaveTimer = 0;
    private Backup backupManager;

    public static MyPetPlugin getPlugin() {
        return plugin;
    }

    public void onDisable() {
        if (isReady) {
            int petCount = savePets(true);
            MyPetLogger.write("" + ChatColor.YELLOW + petCount + ChatColor.RESET + " pet(s) saved");
            for (MyPet myPet : MyPetList.getAllActiveMyPets()) {
                myPet.removePet(myPet.wantToRespawn());
            }
            MyPetList.clearList();
        }
        Timer.reset();
        MyPetPlayer.onlinePlayerUUIDList.clear();
        MyPetLogger.setConsole(null);
        Bukkit.getServer().getScheduler().cancelTasks(getPlugin());
        DebugLogger.info("MyPet disabled!");
    }

    public void onEnable() {
        plugin = this;
        this.isReady = false;
        new File(getPlugin().getDataFolder().getAbsolutePath() + File.separator + "skilltrees" + File.separator).mkdirs();
        new File(getPlugin().getDataFolder().getAbsolutePath() + File.separator + "backups" + File.separator).mkdirs();
        new File(getPlugin().getDataFolder().getAbsolutePath() + File.separator + "locale" + File.separator).mkdirs();
        new File(getPlugin().getDataFolder().getAbsolutePath() + File.separator + "logs" + File.separator).mkdirs();
        NBTPetFile = new File(getPlugin().getDataFolder().getPath() + File.separator + "My.Pets");

        MyPetVersion.reset();
        MyPetLogger.setConsole(getServer().getConsoleSender());
        PvPChecker.reset();
        Economy.reset();
        JavaScript.reset();
        Configuration.config = this.getConfig();
        Configuration.setDefault();
        Configuration.loadConfiguration();
        DebugLogger.setup();

        DebugLogger.info("----------- loading MyPet ... -----------");
        DebugLogger.info("MyPet " + MyPetVersion.getVersion() + " build: " + MyPetVersion.getBuild());
        DebugLogger.info("Bukkit " + getServer().getVersion());
        DebugLogger.info("OnlineMode: " + getServer().getOnlineMode());
        DebugLogger.info("Java: " + System.getProperty("java.version") + " (VM: " + System.getProperty("java.vm.version") + ") by " + System.getProperty("java.vendor"));
        DebugLogger.info("Plugins: " + Arrays.toString(getServer().getPluginManager().getPlugins()));

        PlayerListener playerListener = new PlayerListener();
        getServer().getPluginManager().registerEvents(playerListener, this);

        VehicleListener vehicleListener = new VehicleListener();
        getServer().getPluginManager().registerEvents(vehicleListener, this);

        EntityListener entityListener = new EntityListener();
        getServer().getPluginManager().registerEvents(entityListener, this);

        LevelUpListener levelupListener = new LevelUpListener();
        getServer().getPluginManager().registerEvents(levelupListener, this);

        WorldListener worldListener = new WorldListener();
        getServer().getPluginManager().registerEvents(worldListener, this);

        PluginSupportManager pluginSupportListener = new PluginSupportManager();
        getServer().getPluginManager().registerEvents(pluginSupportListener, this);

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
        getCommand("mypetadmin").setExecutor(new CommandAdmin());
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

        if (!defaultSkillConfigNBT.exists() && !defaultSkillConfigYAML.exists() && !defaultSkillConfigJSON.exists()) {
            try {
                InputStream template = getPlugin().getResource("skilltrees/default.st");
                OutputStream out = new FileOutputStream(defaultSkillConfigNBT);

                byte[] buf = new byte[1024];
                int len;
                while ((len = template.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                template.close();
                out.close();
                MyPetLogger.write("Default skilltree configfile created.");
                DebugLogger.info("created default.st");
            } catch (IOException ex) {
                MyPetLogger.write(ChatColor.RED + "Unable" + ChatColor.RESET + " to create the default.st!");
                DebugLogger.info("unable to create default.st");
            }
        }

        String[] petTypes = new String[MyPetType.values().length + 1];
        petTypes[0] = "default";
        for (int i = 1; i <= MyPetType.values().length; i++) {
            petTypes[i] = MyPetType.values()[i - 1].getTypeName();
        }

        SkillTreeMobType.clearMobTypes();
        SkillTreeLoaderNBT.getSkilltreeLoader().loadSkillTrees(getPlugin().getDataFolder().getPath() + File.separator + "skilltrees", petTypes);
        SkillTreeLoaderYAML.getSkilltreeLoader().loadSkillTrees(getPlugin().getDataFolder().getPath() + File.separator + "skilltrees", petTypes);
        SkillTreeLoaderJSON.getSkilltreeLoader().loadSkillTrees(getPlugin().getDataFolder().getPath() + File.separator + "skilltrees", petTypes);

        Set<String> skilltreeNames = new LinkedHashSet<String>();
        for (MyPetType mobType : MyPetType.values()) {
            SkillTreeMobType skillTreeMobType = SkillTreeMobType.getMobTypeByName(mobType.getTypeName());
            SkillTreeLoader.addDefault(skillTreeMobType);
            SkillTreeLoader.manageInheritance(skillTreeMobType);
            skilltreeNames.addAll(skillTreeMobType.getSkillTreeNames());
        }
        for (String skilltreeName : skilltreeNames) {
            try {
                Bukkit.getPluginManager().addPermission(new Permission("MyPet.custom.skilltree." + skilltreeName));
            } catch (Exception ignored) {
                DebugLogger.warning("Permission \"" + "MyPet.custom.skilltree." + skilltreeName + "\" is already registered.");
            }
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
        for (MyPetType myPetType : MyPetType.values()) {
            DebugLogger.info("  " + myPetType.getTypeName() + " { " +
                    "startHP:" + MyPet.getStartHP(myPetType.getMyPetClass()) + ", " +
                    "speed:" + MyPet.getStartSpeed(myPetType.getMyPetClass()) + ", " +
                    "food:" + MyPet.getFood(myPetType.getMyPetClass()) + ", " +
                    "leashFlags:" + MyPet.getLeashFlags(myPetType.getMyPetClass()) + " }");
        }

        new Locales();

        File groupsFile = new File(getPlugin().getDataFolder().getPath() + File.separator + "worldgroups.yml");

        if (Backup.MAKE_BACKUPS) {
            backupManager = new Backup(NBTPetFile, new File(getPlugin().getDataFolder().getPath() + File.separator + "backups" + File.separator));
        }
        loadGroups(groupsFile);
        loadPets(NBTPetFile);

        Timer.startTimer();

        MobArena.findPlugin();
        Minigames.findPlugin();
        PvPArena.findPlugin();
        BattleArena.findPlugin();
        SurvivalGames.findPlugin();
        MyHungerGames.findPlugin();

        try {
            Metrics metrics = new Metrics(this);
            boolean metricsActive = false;
            if (!metrics.isOptOut()) {

                Graph graphPercent = metrics.createGraph("Percentage of every MyPet type");
                Graph graphCount = metrics.createGraph("Counted MyPets per type");
                Graph graphTotalCount = metrics.createGraph("Total MyPets");

                for (final MyPetType petType : MyPetType.values()) {
                    Plotter plotter = new Metrics.Plotter(petType.getTypeName()) {
                        final MyPetType type = petType;

                        @Override
                        public int getValue() {
                            return MyPetList.countMyPets(type);
                        }
                    };
                    graphPercent.addPlotter(plotter);
                    graphCount.addPlotter(plotter);
                }

                Plotter plotter = new Metrics.Plotter("Total MyPets") {
                    @Override
                    public int getValue() {
                        return MyPetList.countMyPets();
                    }
                };
                graphTotalCount.addPlotter(plotter);
                plotter = new Metrics.Plotter("Active MyPets") {
                    @Override
                    public int getValue() {
                        return MyPetList.countActiveMyPets();
                    }
                };
                graphTotalCount.addPlotter(plotter);

                metricsActive = metrics.start();
            }
            DebugLogger.info("Metrics " + (metricsActive ? "" : "not ") + "activated");
        } catch (IOException e) {
            MyPetLogger.write(e.getMessage());
        }

        MyPetLogger.write("version " + MyPetVersion.getVersion() + "-b" + MyPetVersion.getBuild() + ChatColor.GREEN + " ENABLED");

        for (Player player : getServer().getOnlinePlayers()) {
            MyPetPlayer.onlinePlayerUUIDList.add(player.getUniqueId());
            if (MyPetPlayer.isMyPetPlayer(player)) {
                MyPetPlayer myPetPlayer = MyPetPlayer.getOrCreateMyPetPlayer(player);
                WorldGroup joinGroup = WorldGroup.getGroupByWorld(player.getWorld().getName());
                if (joinGroup != null && !myPetPlayer.hasMyPet() && myPetPlayer.hasMyPetInWorldGroup(joinGroup.getName())) {
                    UUID groupMyPetUUID = myPetPlayer.getMyPetForWorldGroup(joinGroup.getName());
                    for (InactiveMyPet inactiveMyPet : myPetPlayer.getInactiveMyPets()) {
                        if (inactiveMyPet.getUUID().equals(groupMyPetUUID)) {
                            MyPetList.setMyPetActive(inactiveMyPet);
                            break;
                        }
                    }
                    if (!myPetPlayer.hasMyPet()) {
                        myPetPlayer.setMyPetForWorldGroup(joinGroup.getName(), null);
                    }
                }
                if (myPetPlayer.hasMyPet()) {
                    MyPet myPet = MyPetList.getMyPet(player);
                    if (myPet.getStatus() == PetState.Dead) {
                        player.sendMessage(Util.formatText(Locales.getString("Message.Spawn.Respawn.In", BukkitUtil.getPlayerLanguage(player)), myPet.getPetName(), myPet.getRespawnTime()));
                    } else if (myPet.wantToRespawn()) {
                        myPet.createPet();
                    } else {
                        myPet.setStatus(PetState.Despawned);
                    }
                }
            }
        }
        this.isReady = true;
        savePets(false);
        Timer.addTask(this);
        DebugLogger.info("----------- MyPet ready -----------");
    }

    public static void registerSkills() {
        Skills.registerSkill(Inventory.class);
        Skills.registerSkill(HPregeneration.class);
        Skills.registerSkill(Pickup.class);
        Skills.registerSkill(Behavior.class);
        Skills.registerSkill(Damage.class);
        Skills.registerSkill(Control.class);
        Skills.registerSkill(HP.class);
        Skills.registerSkill(Poison.class);
        Skills.registerSkill(Ride.class);
        Skills.registerSkill(Thorns.class);
        Skills.registerSkill(Fire.class);
        Skills.registerSkill(Beacon.class);
        Skills.registerSkill(Wither.class);
        Skills.registerSkill(Lightning.class);
        Skills.registerSkill(Slow.class);
        Skills.registerSkill(Knockback.class);
        Skills.registerSkill(Ranged.class);
        Skills.registerSkill(Sprint.class);
        Skills.registerSkill(Stomp.class);
    }

    public static void registerSkillsInfo() {
        SkillsInfo.registerSkill(InventoryInfo.class);
        SkillsInfo.registerSkill(HPregenerationInfo.class);
        SkillsInfo.registerSkill(PickupInfo.class);
        SkillsInfo.registerSkill(BehaviorInfo.class);
        SkillsInfo.registerSkill(DamageInfo.class);
        SkillsInfo.registerSkill(ControlInfo.class);
        SkillsInfo.registerSkill(HPInfo.class);
        SkillsInfo.registerSkill(PoisonInfo.class);
        SkillsInfo.registerSkill(RideInfo.class);
        SkillsInfo.registerSkill(ThornsInfo.class);
        SkillsInfo.registerSkill(FireInfo.class);
        SkillsInfo.registerSkill(BeaconInfo.class);
        SkillsInfo.registerSkill(WitherInfo.class);
        SkillsInfo.registerSkill(LightningInfo.class);
        SkillsInfo.registerSkill(SlowInfo.class);
        SkillsInfo.registerSkill(KnockbackInfo.class);
        SkillsInfo.registerSkill(RangedInfo.class);
        SkillsInfo.registerSkill(SprintInfo.class);
        SkillsInfo.registerSkill(StompInfo.class);
    }

    @SuppressWarnings("unchecked")
    public static boolean registerMyPetEntity(Class<? extends EntityMyPet> myPetEntityClass, String entityTypeName, int entityTypeId) {
        try {
            Field EntityTypes_d = EntityTypes.class.getDeclaredField("d");
            Field EntityTypes_f = EntityTypes.class.getDeclaredField("f");
            EntityTypes_d.setAccessible(true);
            EntityTypes_f.setAccessible(true);

            Map<Class, String> d = (Map) EntityTypes_d.get(EntityTypes_d);
            Map<Class, Integer> f = (Map) EntityTypes_f.get(EntityTypes_f);

            Iterator cIterator = d.keySet().iterator();
            while (cIterator.hasNext()) {
                Class clazz = (Class) cIterator.next();
                if (clazz.getCanonicalName().equals(myPetEntityClass.getCanonicalName())) {
                    cIterator.remove();
                }
            }

            Iterator eIterator = f.keySet().iterator();
            while (eIterator.hasNext()) {
                Class clazz = (Class) eIterator.next();
                if (clazz.getCanonicalName().equals(myPetEntityClass.getCanonicalName())) {
                    eIterator.remove();
                }
            }

            d.put(myPetEntityClass, entityTypeName);
            f.put(myPetEntityClass, entityTypeId);

            return true;
        } catch (Exception e) {
            DebugLogger.severe("error while registering " + myPetEntityClass.getCanonicalName());
            DebugLogger.severe(e.getMessage());
            return false;
        }
    }

    int loadPets(File f) {
        if (!f.exists()) {
            MyPetLogger.write(ChatColor.YELLOW + "0" + ChatColor.RESET + " pet(s) loaded");
            return 0;
        }
        int petCount = 0;

        ConfigurationNBT nbtConfiguration = new ConfigurationNBT(f);
        if (!nbtConfiguration.load()) {
            return 0;
        }
        TagList petList = nbtConfiguration.getNBTCompound().getAs("Pets", TagList.class);
        if (nbtConfiguration.getNBTCompound().getCompoundData().containsKey("CleanShutdown")) {
            DebugLogger.info("Clean shutdown: " + nbtConfiguration.getNBTCompound().getAs("CleanShutdown", TagByte.class).getBooleanData());
        }

        DebugLogger.info("Loading players -------------------------");
        if (nbtConfiguration.getNBTCompound().getCompoundData().containsKey("Players")) {
            DebugLogger.info(loadPlayers(nbtConfiguration) + " PetPlayer(s) loaded");
        }
        DebugLogger.info("-----------------------------------------");

        DebugLogger.info("loading Pets: -----------------------------");
        for (int i = 0; i < petList.getReadOnlyList().size(); i++) {
            TagCompound myPetNBT = petList.getTagAs(i, TagCompound.class);
            MyPetPlayer petPlayer;
            if (myPetNBT.containsKeyAs("Internal-Owner-UUID", TagString.class)) {
                UUID ownerUUID = UUID.fromString(myPetNBT.getAs("Internal-Owner-UUID", TagString.class).getStringData());
                petPlayer = MyPetPlayer.getMyPetPlayer(ownerUUID);
            } else {
                if (BukkitUtil.isInOnlineMode()) {
                    if (myPetNBT.getCompoundData().containsKey("Mojang-Owner-UUID")) {
                        UUID playerUUID = UUID.fromString(myPetNBT.getAs("Mojang-Owner-UUID", TagString.class).getStringData());
                        petPlayer = MyPetPlayer.getMyPetPlayer(MyPetPlayer.getInternalUUID(playerUUID));
                    } else {
                        String playerName = myPetNBT.getAs("Owner", TagString.class).getStringData();
                        Map<String, UUID> fetchedUUIDs = UUIDFetcher.call(playerName);
                        if (!fetchedUUIDs.containsKey(playerName)) {
                            MyPetLogger.write(ChatColor.RED + "Can't get UUID for \"" + playerName + "\"! Pet not loaded for this player!");
                            continue;
                        } else {
                            petPlayer = MyPetPlayer.getMyPetPlayer(MyPetPlayer.getInternalUUID(fetchedUUIDs.get(playerName)));
                        }
                    }
                } else {
                    petPlayer = MyPetPlayer.getMyPetPlayer(myPetNBT.getAs("Owner", TagString.class).getStringData());
                }
            }
            if (petPlayer == null) {
                MyPetLogger.write("Owner for a pet (" + myPetNBT.getAs("Name", TagString.class) + " not found, pet loading skipped.");
                continue;
            }
            InactiveMyPet inactiveMyPet = new InactiveMyPet(petPlayer);
            inactiveMyPet.load(myPetNBT);

            MyPetList.addInactiveMyPet(inactiveMyPet);

            DebugLogger.info("   " + inactiveMyPet.toString());

            petCount++;
        }
        MyPetLogger.write("" + ChatColor.YELLOW + petCount + ChatColor.RESET + " pet(s) loaded");
        return petCount;
    }

    public int savePets(boolean shutdown) {
        autoSaveTimer = Configuration.AUTOSAVE_TIME;
        int petCount = 0;
        ConfigurationNBT nbtConfiguration = new ConfigurationNBT(NBTPetFile);
        List<TagCompound> petList = new ArrayList<TagCompound>();

        for (MyPet myPet : MyPetList.getAllActiveMyPets()) {
            try {
                TagCompound petNBT = myPet.save();
                petList.add(petNBT);
                petCount++;
            } catch (Exception e) {
                DebugLogger.printThrowable(e);
            }
        }
        for (InactiveMyPet inactiveMyPet : MyPetList.getAllInactiveMyPets()) {
            try {
                TagCompound petNBT = inactiveMyPet.save();
                petList.add(petNBT);
                petCount++;
            } catch (Exception e) {
                DebugLogger.printThrowable(e);
            }
        }
        nbtConfiguration.getNBTCompound().getCompoundData().put("Version", new TagString(MyPetVersion.getVersion()));
        nbtConfiguration.getNBTCompound().getCompoundData().put("Build", new TagInt(Integer.parseInt(MyPetVersion.getBuild())));
        nbtConfiguration.getNBTCompound().getCompoundData().put("CleanShutdown", new TagByte(shutdown));
        nbtConfiguration.getNBTCompound().getCompoundData().put("OnlineMode", new TagByte(BukkitUtil.isInOnlineMode()));
        nbtConfiguration.getNBTCompound().getCompoundData().put("Pets", new TagList(petList));
        nbtConfiguration.getNBTCompound().getCompoundData().put("Players", savePlayers());
        nbtConfiguration.save();

        return petCount;
    }

    private TagList savePlayers() {
        List<TagCompound> playerList = new ArrayList<TagCompound>();
        for (MyPetPlayer myPetPlayer : MyPetPlayer.getMyPetPlayers()) {
            if (myPetPlayer.hasMyPet() || myPetPlayer.hasInactiveMyPets() || myPetPlayer.hasCustomData()) {
                try {
                    playerList.add(myPetPlayer.save());
                } catch (Exception e) {
                    DebugLogger.printThrowable(e);
                }
            }
        }
        return new TagList(playerList);
    }

    private int loadPlayers(ConfigurationNBT nbtConfiguration) {
        int playerCount = 0;
        TagList playerList = nbtConfiguration.getNBTCompound().getAs("Players", TagList.class);

        if (BukkitUtil.isInOnlineMode()) {
            List<String> unknownPlayers = new ArrayList<String>();
            for (int i = 0; i < playerList.getReadOnlyList().size(); i++) {
                TagCompound playerTag = playerList.getTagAs(i, TagCompound.class);
                if (playerTag.containsKeyAs("Name", TagString.class)) {
                    if (playerTag.containsKeyAs("UUID", TagCompound.class)) {
                        TagCompound uuidTag = playerTag.getAs("UUID", TagCompound.class);
                        if (!uuidTag.containsKeyAs("Mojang-UUID", TagString.class)) {
                            String playerName = playerTag.getAs("Name", TagString.class).getStringData();
                            unknownPlayers.add(playerName);
                        }
                    } else if (!playerTag.getCompoundData().containsKey("Mojang-UUID")) {
                        String playerName = playerTag.getAs("Name", TagString.class).getStringData();
                        unknownPlayers.add(playerName);
                    }
                }
            }
            UUIDFetcher.call(unknownPlayers);
        }

        for (int i = 0; i < playerList.getReadOnlyList().size(); i++) {
            TagCompound playerTag = playerList.getTagAs(i, TagCompound.class);
            MyPetPlayer.createMyPetPlayer(playerTag);
            playerCount++;
        }
        return playerCount;
    }

    private int loadGroups(File f) {
        ConfigurationYAML yamlConfiguration = new ConfigurationYAML(f);
        FileConfiguration config = yamlConfiguration.getConfig();

        if (config == null) {
            return 0;
        }

        WorldGroup.clearGroups();

        Set<String> nodes;
        try {
            nodes = config.getConfigurationSection("Groups").getKeys(false);
        } catch (NullPointerException e) {
            nodes = new HashSet<String>();
            MyPetLogger.write("No groups found. Everything will be in 'default' group.");
        }

        DebugLogger.info("--- Loading WorldGroups ---------------------------");
        if (nodes.size() == 0) {
            List<String> worldNames = new ArrayList<String>();
            WorldGroup defaultGroup = new WorldGroup("default");
            defaultGroup.registerGroup();
            for (org.bukkit.World world : this.getServer().getWorlds()) {
                MyPetLogger.write("added " + ChatColor.GOLD + world.getName() + ChatColor.RESET + " to 'default' group.");
                worldNames.add(world.getName());
                defaultGroup.addWorld(world.getName());
            }
            config.set("Groups.default", worldNames);
            yamlConfiguration.saveConfig();
        } else {
            for (String node : nodes) {
                List<String> worlds = config.getStringList("Groups." + node);
                if (worlds.size() > 0) {
                    WorldGroup newGroup = new WorldGroup(node);
                    for (String world : worlds) {
                        DebugLogger.info("   added '" + world + "' to '" + newGroup.getName() + "'");
                        newGroup.addWorld(world);
                    }
                    if (newGroup.getWorlds().size() > 0) {
                        DebugLogger.info(" registered '" + newGroup.getName() + "' group");
                        newGroup.registerGroup();
                    }
                }
            }

            WorldGroup defaultGroup = WorldGroup.getGroupByName("default");
            if (defaultGroup == null) {
                defaultGroup = new WorldGroup("default");
                defaultGroup.registerGroup();
                DebugLogger.info(" registered 'default' group");
            }

            boolean saveConfig = false;
            for (org.bukkit.World world : getServer().getWorlds()) {
                if (WorldGroup.getGroupByWorld(world.getName()) == null) {
                    MyPetLogger.write("added " + ChatColor.GOLD + world.getName() + ChatColor.RESET + " to 'default' group.");
                    defaultGroup.addWorld(world.getName());
                    saveConfig = true;
                }
            }
            if (saveConfig) {
                config.set("Groups.default", defaultGroup.getWorlds());
                yamlConfiguration.saveConfig();
            }
        }
        DebugLogger.info("-------------------------------------------------");
        return 0;
    }

    @Override
    public void schedule() {
        if (Configuration.AUTOSAVE_TIME > 0 && autoSaveTimer-- <= 0) {
            MyPetPlugin.getPlugin().savePets(false);
            autoSaveTimer = Configuration.AUTOSAVE_TIME;
        }
    }

    public Backup getBackupManager() {
        return backupManager;
    }

    public File getFile() {
        return super.getFile();
    }
}