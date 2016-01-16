/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2016 Keyle
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

import de.Keyle.MyPet.api.repository.Repository;
import de.Keyle.MyPet.api.util.IScheduler;
import de.Keyle.MyPet.commands.*;
import de.Keyle.MyPet.entity.types.InactiveMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.entity.types.bat.EntityMyBat;
import de.Keyle.MyPet.entity.types.blaze.EntityMyBlaze;
import de.Keyle.MyPet.entity.types.cavespider.EntityMyCaveSpider;
import de.Keyle.MyPet.entity.types.chicken.EntityMyChicken;
import de.Keyle.MyPet.entity.types.cow.EntityMyCow;
import de.Keyle.MyPet.entity.types.creeper.EntityMyCreeper;
import de.Keyle.MyPet.entity.types.enderdragon.EntityMyEnderDragon;
import de.Keyle.MyPet.entity.types.enderman.EntityMyEnderman;
import de.Keyle.MyPet.entity.types.endermite.EntityMyEndermite;
import de.Keyle.MyPet.entity.types.ghast.EntityMyGhast;
import de.Keyle.MyPet.entity.types.giant.EntityMyGiant;
import de.Keyle.MyPet.entity.types.guardian.EntityMyGuardian;
import de.Keyle.MyPet.entity.types.horse.EntityMyHorse;
import de.Keyle.MyPet.entity.types.irongolem.EntityMyIronGolem;
import de.Keyle.MyPet.entity.types.magmacube.EntityMyMagmaCube;
import de.Keyle.MyPet.entity.types.mooshroom.EntityMyMooshroom;
import de.Keyle.MyPet.entity.types.ocelot.EntityMyOcelot;
import de.Keyle.MyPet.entity.types.pig.EntityMyPig;
import de.Keyle.MyPet.entity.types.pigzombie.EntityMyPigZombie;
import de.Keyle.MyPet.entity.types.rabbit.EntityMyRabbit;
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
import de.Keyle.MyPet.repository.*;
import de.Keyle.MyPet.repository.types.MySqlRepository;
import de.Keyle.MyPet.repository.types.NbtRepository;
import de.Keyle.MyPet.skill.Experience;
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
import de.Keyle.MyPet.util.configuration.ConfigurationYAML;
import de.Keyle.MyPet.util.hooks.*;
import de.Keyle.MyPet.util.hooks.arenas.*;
import de.Keyle.MyPet.util.locale.Translation;
import de.Keyle.MyPet.util.logger.DebugLogger;
import de.Keyle.MyPet.util.logger.MyPetLogger;
import de.Keyle.MyPet.util.player.MyPetPlayer;
import de.Keyle.MyPet.util.player.OnlineMyPetPlayer;
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
import java.util.*;

public class MyPetPlugin extends JavaPlugin {
    private static MyPetPlugin plugin;
    private boolean isReady = false;
    private Repository repo;
    public static String REPOSITORY_TYPE = "NBT";

    public static MyPetPlugin getPlugin() {
        return plugin;
    }

    public void onDisable() {
        if (isReady) {
            for (MyPet myPet : MyPetList.getAllActiveMyPets()) {
                myPet.removePet(myPet.wantToRespawn());
            }
            MyPetList.clearList();
            BukkitUtil.unregisterMyPetEntities();
        }
        Timer.reset();
        PlayerList.onlinePlayerUUIDList.clear();
        MyPetLogger.setConsole(null);
        Bukkit.getServer().getScheduler().cancelTasks(getPlugin());
        DebugLogger.info("MyPet disabled!");
    }

    public void onEnable() {
        plugin = this;
        this.isReady = false;
        new File(getPlugin().getDataFolder().getAbsolutePath() + File.separator + "skilltrees" + File.separator).mkdirs();
        new File(getPlugin().getDataFolder().getAbsolutePath() + File.separator + "locale" + File.separator).mkdirs();
        new File(getPlugin().getDataFolder().getAbsolutePath() + File.separator + "logs" + File.separator).mkdirs();

        MyPetVersion.reset();
        MyPetLogger.setConsole(getServer().getConsoleSender());

        if (!Bukkit.getServer().getClass().getName().contains(MyPetVersion.getBukkitPacket())) {
            MyPetLogger.write(ChatColor.RED + "This version of MyPet is only compatible with Craftbukkit/Spigot " + MyPetVersion.getMinecraftVersion() + " (" + MyPetVersion.getBukkitPacket() + ") !!!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        PvPChecker.reset();
        PluginHookManager.reset();
        Economy.reset();
        Experience.resetMode();
        Configuration.setDefault();
        Configuration.loadConfiguration();
        DebugLogger.setup();

        DebugLogger.info("----------- loading MyPet ... -----------");
        DebugLogger.info("MyPet " + MyPetVersion.getVersion() + " build: " + MyPetVersion.getBuild() + (MyPetVersion.isPremium() ? "P" : ""));
        DebugLogger.info("Bukkit " + getServer().getVersion());
        DebugLogger.info("OnlineMode: " + getServer().getOnlineMode());
        DebugLogger.info("Java: " + System.getProperty("java.version") + " (VM: " + System.getProperty("java.vm.version") + ") by " + System.getProperty("java.vendor"));
        DebugLogger.info("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch"));
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

        PluginHookManager pluginSupportListener = new PluginHookManager();
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
        getCommand("petoptions").setExecutor(new CommandOptions());

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

        Set<String> skilltreeNames = new LinkedHashSet<>();
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

        BukkitUtil.registerMyPetEntity(EntityMyCreeper.class, "Creeper", 50);
        BukkitUtil.registerMyPetEntity(EntityMySkeleton.class, "Skeleton", 51);
        BukkitUtil.registerMyPetEntity(EntityMySpider.class, "Spider", 52);
        BukkitUtil.registerMyPetEntity(EntityMyGiant.class, "Giant", 53);
        BukkitUtil.registerMyPetEntity(EntityMyZombie.class, "Zombie", 54);
        BukkitUtil.registerMyPetEntity(EntityMySlime.class, "Slime", 55);
        BukkitUtil.registerMyPetEntity(EntityMyGhast.class, "Ghast", 56);
        BukkitUtil.registerMyPetEntity(EntityMyPigZombie.class, "PigZombie", 57);
        BukkitUtil.registerMyPetEntity(EntityMyEnderman.class, "Enderman", 58);
        BukkitUtil.registerMyPetEntity(EntityMyCaveSpider.class, "CaveSpider", 59);
        BukkitUtil.registerMyPetEntity(EntityMySilverfish.class, "Silverfish", 60);
        BukkitUtil.registerMyPetEntity(EntityMyBlaze.class, "Blaze", 61);
        BukkitUtil.registerMyPetEntity(EntityMyMagmaCube.class, "LavaSlime", 62);
        BukkitUtil.registerMyPetEntity(EntityMyEnderDragon.class, "EnderDragon", 63);
        BukkitUtil.registerMyPetEntity(EntityMyWither.class, "WitherBoss", 64);
        BukkitUtil.registerMyPetEntity(EntityMyBat.class, "Bat", 65);
        BukkitUtil.registerMyPetEntity(EntityMyWitch.class, "Witch", 66);
        BukkitUtil.registerMyPetEntity(EntityMyEndermite.class, "Endermite", 67);
        BukkitUtil.registerMyPetEntity(EntityMyGuardian.class, "Guardian", 68);
        BukkitUtil.registerMyPetEntity(EntityMyPig.class, "Pig", 90);
        BukkitUtil.registerMyPetEntity(EntityMySheep.class, "Sheep", 91);
        BukkitUtil.registerMyPetEntity(EntityMyCow.class, "Cow", 92);
        BukkitUtil.registerMyPetEntity(EntityMyChicken.class, "Chicken", 93);
        BukkitUtil.registerMyPetEntity(EntityMySquid.class, "Squid", 94);
        BukkitUtil.registerMyPetEntity(EntityMyWolf.class, "Wolf", 95);
        BukkitUtil.registerMyPetEntity(EntityMyMooshroom.class, "MushroomCow", 96);
        BukkitUtil.registerMyPetEntity(EntityMySnowman.class, "SnowMan", 97);
        BukkitUtil.registerMyPetEntity(EntityMyOcelot.class, "Ozelot", 98);
        BukkitUtil.registerMyPetEntity(EntityMyIronGolem.class, "VillagerGolem", 99);
        BukkitUtil.registerMyPetEntity(EntityMyHorse.class, "EntityHorse", 100);
        BukkitUtil.registerMyPetEntity(EntityMyRabbit.class, "Rabbit", 101);
        BukkitUtil.registerMyPetEntity(EntityMyVillager.class, "Villager", 120);

        Translation.init();
        Bungee.reset();

        File groupsFile = new File(getPlugin().getDataFolder().getPath() + File.separator + "worldgroups.yml");

        if (REPOSITORY_TYPE.equalsIgnoreCase("MySQL")) {
            repo = new MySqlRepository();
            try {
                repo.init();
            } catch (RepositoryInitException e) {
                e.printStackTrace();
                repo = null;
            }
        }

        if (repo == null) {
            repo = new NbtRepository();
            try {
                repo.init();
            } catch (RepositoryInitException ignored) {
            }
        }

        Converter.convert();

        if (repo instanceof IScheduler) {
            Timer.addTask((IScheduler) repo);
        }

        loadGroups(groupsFile);
        Timer.startTimer();

        ProtocolLib.findPlugin();
        MobArena.findPlugin();
        Minigames.findPlugin();
        PvPArena.findPlugin();
        BattleArena.findPlugin();
        SurvivalGames.findPlugin();
        UltimateSurvivalGames.findPlugin();
        MyHungerGames.findPlugin();

        try {
            Metrics metrics = new Metrics(this);
            boolean metricsActive = false;
            if (!metrics.isOptOut()) {

                Graph graphTotalCount = metrics.createGraph("MyPets");

                Plotter plotter = new Metrics.Plotter("Active MyPets") {
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

        if (MyPetVersion.isPremium()) {
            MyPetLogger.write("Thank you for buying MyPet-" + ChatColor.YELLOW + "Premium" + ChatColor.RESET + "!");
        }
        MyPetLogger.write("version " + MyPetVersion.getVersion() + "-b" + MyPetVersion.getBuild() + (MyPetVersion.isPremium() ? "P" : "") + ChatColor.GREEN + " ENABLED");
        this.isReady = true;

        for (final Player player : getServer().getOnlinePlayers()) {
            PlayerList.onlinePlayerUUIDList.add(player.getUniqueId());

            MyPetPlugin.getPlugin().getRepository().getMyPetPlayer(player, new RepositoryCallback<MyPetPlayer>() {
                @Override
                public void callback(final MyPetPlayer joinedPlayer) {
                    if (joinedPlayer != null) {
                        PlayerList.setOnline(joinedPlayer);

                        if (BukkitUtil.isInOnlineMode()) {
                            if (joinedPlayer instanceof OnlineMyPetPlayer) {
                                ((OnlineMyPetPlayer) joinedPlayer).setLastKnownName(player.getName());
                            }
                        }

                        final WorldGroup joinGroup = WorldGroup.getGroupByWorld(player.getWorld().getName());
                        if (joinedPlayer.hasMyPet()) {
                            MyPet myPet = joinedPlayer.getMyPet();
                            if (!myPet.getWorldGroup().equals(joinGroup.getName())) {
                                MyPetList.deactivateMyPet(joinedPlayer);
                            }
                        }

                        if (joinGroup != null && !joinedPlayer.hasMyPet() && joinedPlayer.hasMyPetInWorldGroup(joinGroup.getName())) {
                            final UUID groupMyPetUUID = joinedPlayer.getMyPetForWorldGroup(joinGroup.getName());

                            joinedPlayer.getInactiveMyPet(groupMyPetUUID, new RepositoryCallback<InactiveMyPet>() {
                                @Override
                                public void callback(InactiveMyPet inactiveMyPet) {
                                    MyPetList.activateMyPet(inactiveMyPet);

                                    if (joinedPlayer.hasMyPet()) {
                                        final MyPet myPet = joinedPlayer.getMyPet();
                                        final MyPetPlayer myPetPlayer = myPet.getOwner();
                                        if (myPet.wantToRespawn()) {
                                            if (myPetPlayer.hasMyPet()) {
                                                MyPet runMyPet = myPetPlayer.getMyPet();
                                                switch (runMyPet.createPet()) {
                                                    case Canceled:
                                                        runMyPet.sendMessageToOwner(Util.formatText(Translation.getString("Message.Spawn.Prevent", myPet.getOwner()), runMyPet.getPetName()));
                                                        break;
                                                    case NoSpace:
                                                        runMyPet.sendMessageToOwner(Util.formatText(Translation.getString("Message.Spawn.NoSpace", myPet.getOwner()), runMyPet.getPetName()));
                                                        break;
                                                    case NotAllowed:
                                                        runMyPet.sendMessageToOwner(Translation.getString("Message.No.AllowedHere", myPet.getOwner()).replace("%petname%", myPet.getPetName()));
                                                        break;
                                                    case Dead:
                                                        runMyPet.sendMessageToOwner(Translation.getString("Message.Spawn.Respawn.In", myPet.getOwner()).replace("%petname%", myPet.getPetName()).replace("%time%", "" + myPet.getRespawnTime()));
                                                        break;
                                                    case Flying:
                                                        runMyPet.sendMessageToOwner(Util.formatText(Translation.getString("Message.Spawn.Flying", myPet.getOwner()), myPet.getPetName()));
                                                        break;
                                                    case Success:
                                                        runMyPet.sendMessageToOwner(Util.formatText(Translation.getString("Message.Command.Call.Success", myPet.getOwner()), runMyPet.getPetName()));
                                                        break;
                                                }
                                            }
                                        } else {
                                            myPet.setStatus(MyPet.PetState.Despawned);
                                        }
                                    }
                                }
                            });
                        }
                        joinedPlayer.checkForDonation();
                    }
                }
            });
        }
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
            nodes = new HashSet<>();
            MyPetLogger.write("No groups found. Everything will be in 'default' group.");
        }

        DebugLogger.info("--- Loading WorldGroups ---------------------------");
        if (nodes.size() == 0) {
            List<String> worldNames = new ArrayList<>();
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

    public File getFile() {
        return super.getFile();
    }

    public Repository getRepository() {
        return repo;
    }
}