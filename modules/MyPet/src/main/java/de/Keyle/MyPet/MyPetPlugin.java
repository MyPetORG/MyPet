/*
 * This file is part of mypet
 *
 * Copyright (C) 2011-2016 Keyle
 * mypet is licensed under the GNU Lesser General Public License.
 *
 * mypet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mypet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet;

import de.Keyle.MyPet.api.*;
import de.Keyle.MyPet.api.entity.*;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.repository.*;
import de.Keyle.MyPet.api.skill.Skills;
import de.Keyle.MyPet.api.skill.SkillsInfo;
import de.Keyle.MyPet.api.skill.skills.*;
import de.Keyle.MyPet.api.skill.skilltree.SkillTreeMobType;
import de.Keyle.MyPet.api.skill.skilltreeloader.SkillTreeLoader;
import de.Keyle.MyPet.api.skill.skilltreeloader.SkillTreeLoaderJSON;
import de.Keyle.MyPet.api.skill.skilltreeloader.SkillTreeLoaderNBT;
import de.Keyle.MyPet.api.util.CompatUtil;
import de.Keyle.MyPet.api.util.Scheduler;
import de.Keyle.MyPet.api.util.Timer;
import de.Keyle.MyPet.api.util.configuration.ConfigurationYAML;
import de.Keyle.MyPet.api.util.hooks.HookManager;
import de.Keyle.MyPet.api.util.hooks.PluginHookManager;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.commands.*;
import de.Keyle.MyPet.listeners.*;
import de.Keyle.MyPet.repository.Converter;
import de.Keyle.MyPet.repository.types.MongoDbRepository;
import de.Keyle.MyPet.repository.types.MySqlRepository;
import de.Keyle.MyPet.repository.types.NbtRepository;
import de.Keyle.MyPet.skill.skills.*;
import de.Keyle.MyPet.util.ConfigurationLoader;
import de.Keyle.MyPet.util.Metrics;
import de.Keyle.MyPet.util.UpdateCheck;
import de.Keyle.MyPet.util.hooks.Bungee;
import de.Keyle.MyPet.util.hooks.Hooks;
import de.Keyle.MyPet.util.logger.MyPetLogger;
import de.Keyle.MyPet.util.player.OnlineMyPetPlayer;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

public class MyPetPlugin extends JavaPlugin implements de.Keyle.MyPet.api.plugin.MyPetPlugin {
    private boolean isReady = false;
    private Repository repo;
    private MyPetInfo petInfo;
    private BukkitHelper bukkitHelper;
    private EntityRegistry entityRegistry;
    private CompatUtil compatUtil;
    private PlayerList playerList;
    private MyPetList myPetList;
    private HookManager hookManager;

    public void onDisable() {
        if (isReady) {
            for (ActiveMyPet myPet : myPetList.getAllActiveMyPets()) {
                myPet.removePet();
            }
            repo.disable();
            entityRegistry.unregisterEntityTypes();
        }
        Timer.reset();

        PluginHookManager.reset();
        Hooks.disable();

        Bukkit.getServer().getScheduler().cancelTasks(this);
    }

    public void onEnable() {
        MyPetApi.setPlugin(this);
        replaceLogger();

        this.isReady = false;

        // load version from manifest
        MyPetVersion.reset();

        compatUtil = new CompatUtil();

        if (getConfig().getBoolean("MyPet.Update-Check", true)) {
            String message = UpdateCheck.checkForUpdate("MyPet");
            if (message != null) {
                message = "#  A new version is available: " + message + "  #";
                MyPetApi.getLogger().info(StringUtils.repeat("#", message.length()));
                MyPetApi.getLogger().info(message);
                MyPetApi.getLogger().info(StringUtils.repeat("#", message.length()));
            }
        }

        if (compatUtil.getInternalVersion() == null || !MyPetVersion.isValidBukkitPacket(compatUtil.getInternalVersion())) {
            getLogger().info(ChatColor.RED + "This version of MyPet is not compatible with \"" + compatUtil.getInternalVersion() + "\". Is MyPet up to date?");
            setEnabled(false);
            return;
        }
        getLogger().info("Compat mode for " + compatUtil.getInternalVersion() + " loaded.");

        petInfo = compatUtil.getComapatInstance(MyPetInfo.class, "entity", "MyPetInfo");
        bukkitHelper = compatUtil.getComapatInstance(BukkitHelper.class, "", "BukkitHelper");
        entityRegistry = compatUtil.getComapatInstance(EntityRegistry.class, "entity", "EntityRegistry");
        myPetList = new de.Keyle.MyPet.repository.MyPetList();
        playerList = new de.Keyle.MyPet.repository.PlayerList();
        hookManager = new Hooks();

        entityRegistry.registerEntityTypes();

        PluginHookManager.reset();

        ConfigurationLoader.setDefault();
        ConfigurationLoader.loadConfiguration();

        // register event listener
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

        // register commands
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
        getCommand("petswitch").setExecutor(new CommandSwitch());
        getCommand("pettrade").setExecutor(new CommandTrade());

        // register skills
        registerSkillsInfo();
        registerSkills();

        // create folders
        File skilltreeFolder = new File(getDataFolder().getPath(), "skilltrees");
        getDataFolder().mkdirs();
        boolean createDefaultSkilltree = skilltreeFolder.mkdirs();
        new File(getDataFolder(), "locale").mkdirs();
        new File(getDataFolder(), "logs").mkdirs();

        if (createDefaultSkilltree) {
            bukkitHelper.copyResource(this, "skilltrees/default.st", new File(skilltreeFolder, "default.st"));
            getLogger().info("Default skilltree file created (default.st).");
        }

        // load skilltrees
        String[] petTypes = new String[MyPetType.values().length + 1];
        petTypes[0] = "default";
        for (int i = 1; i <= MyPetType.values().length; i++) {
            petTypes[i] = MyPetType.values()[i - 1].name();
        }

        SkillTreeMobType.clearMobTypes();
        SkillTreeLoaderNBT.getSkilltreeLoader().loadSkillTrees(getDataFolder().getPath() + File.separator + "skilltrees", petTypes);
        SkillTreeLoaderJSON.getSkilltreeLoader().loadSkillTrees(getDataFolder().getPath() + File.separator + "skilltrees", petTypes);

        Set<String> skilltreeNames = new LinkedHashSet<>();
        for (MyPetType mobType : MyPetType.values()) {
            SkillTreeMobType skillTreeMobType = SkillTreeMobType.byPetType(mobType);
            SkillTreeLoader.addDefault(skillTreeMobType);
            SkillTreeLoader.manageInheritance(skillTreeMobType);
            skilltreeNames.addAll(skillTreeMobType.getSkillTreeNames());
        }
        // register skilltree permissions
        for (String skilltreeName : skilltreeNames) {
            try {
                Bukkit.getPluginManager().addPermission(new Permission("MyPet.custom.skilltree." + skilltreeName));
            } catch (Exception ignored) {
            }
        }

        File translationReadme = new File(getDataFolder(), "locale" + File.separator + "readme.txt");
        if (!translationReadme.exists()) {
            bukkitHelper.copyResource(this, "locale-readme.txt", translationReadme);
        }
        Translation.init();

        Bungee.reset();

        // init repository
        if (Configuration.Repository.REPOSITORY_TYPE.equalsIgnoreCase("MySQL")) {
            repo = new MySqlRepository();
            try {
                repo.init();
            } catch (RepositoryInitException e) {
                e.printStackTrace();
                repo = null;
            }
        } else if (Configuration.Repository.REPOSITORY_TYPE.equalsIgnoreCase("MongoDB")) {
            repo = new MongoDbRepository();
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

        if (repo instanceof Scheduler) {
            Timer.addTask((Scheduler) repo);
        }

        // load worldgroups
        loadGroups(new File(getDataFolder().getPath(), "worldgroups.yml"));
        Timer.startTimer();

        // init Metrics
        try {
            Metrics metrics = new Metrics(this);
            if (!metrics.isOptOut()) {

                Metrics.Graph graphTotalCount = metrics.createGraph("MyPets");

                Metrics.Plotter plotter = new Metrics.Plotter("Active MyPets") {
                    @Override
                    public int getValue() {
                        return myPetList.countActiveMyPets();
                    }
                };
                graphTotalCount.addPlotter(plotter);

                metrics.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (MyPetVersion.isPremium()) {
            getLogger().info("Thank you for buying MyPet-" + ChatColor.YELLOW + "Premium" + ChatColor.RESET + "!");
        }
        getLogger().info("version " + MyPetVersion.getVersion() + "-b" + MyPetVersion.getBuild() + (MyPetVersion.isPremium() ? "P" : "") + ChatColor.GREEN + " ENABLED");
        this.isReady = true;

        // load pets for online players
        for (final Player player : getServer().getOnlinePlayers()) {
            repo.getMyPetPlayer(player, new RepositoryCallback<MyPetPlayer>() {
                @Override
                public void callback(final MyPetPlayer joinedPlayer) {
                    if (joinedPlayer != null) {
                        playerList.setOnline(joinedPlayer);

                        if (isInOnlineMode()) {
                            if (joinedPlayer instanceof OnlineMyPetPlayer) {
                                ((OnlineMyPetPlayer) joinedPlayer).setLastKnownName(player.getName());
                            }
                        }

                        final WorldGroup joinGroup = WorldGroup.getGroupByWorld(player.getWorld().getName());
                        if (joinedPlayer.hasMyPet()) {
                            ActiveMyPet myPet = joinedPlayer.getMyPet();
                            if (!myPet.getWorldGroup().equals(joinGroup.getName())) {
                                myPetList.deactivateMyPet(joinedPlayer, true);
                            }
                        }

                        if (joinGroup != null && !joinedPlayer.hasMyPet() && joinedPlayer.hasMyPetInWorldGroup(joinGroup.getName())) {
                            final UUID petUUID = joinedPlayer.getMyPetForWorldGroup(joinGroup.getName());

                            MyPetApi.getRepository().getMyPet(petUUID, new RepositoryCallback<MyPet>() {
                                @Override
                                public void callback(MyPet inactiveMyPet) {
                                    myPetList.activateMyPet(inactiveMyPet);

                                    if (joinedPlayer.hasMyPet()) {
                                        final ActiveMyPet myPet = joinedPlayer.getMyPet();
                                        final MyPetPlayer myPetPlayer = myPet.getOwner();
                                        if (myPet.wantsToRespawn()) {
                                            if (myPetPlayer.hasMyPet()) {
                                                ActiveMyPet runMyPet = myPetPlayer.getMyPet();
                                                switch (runMyPet.createEntity()) {
                                                    case Canceled:
                                                        runMyPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Spawn.Prevent", myPet.getOwner()), runMyPet.getPetName()));
                                                        break;
                                                    case NoSpace:
                                                        runMyPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Spawn.NoSpace", myPet.getOwner()), runMyPet.getPetName()));
                                                        break;
                                                    case NotAllowed:
                                                        runMyPet.getOwner().sendMessage(Translation.getString("Message.No.AllowedHere", myPet.getOwner()).replace("%petname%", myPet.getPetName()));
                                                        break;
                                                    case Dead:
                                                        runMyPet.getOwner().sendMessage(Translation.getString("Message.Spawn.Respawn.In", myPet.getOwner()).replace("%petname%", myPet.getPetName()).replace("%time%", "" + myPet.getRespawnTime()));
                                                        break;
                                                    case Flying:
                                                        runMyPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Spawn.Flying", myPet.getOwner()), myPet.getPetName()));
                                                        break;
                                                    case Success:
                                                        runMyPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Command.Call.Success", myPet.getOwner()), runMyPet.getPetName()));
                                                        break;
                                                }
                                            }
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
        Skills.registerSkill(Shield.class);
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
        SkillsInfo.registerSkill(ShieldInfo.class);
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
            getLogger().info("No groups found. Everything will be in 'default' group.");
        }

        getLogger().info("--- Loading WorldGroups ---------------------------");
        if (nodes.size() == 0) {
            List<String> worldNames = new ArrayList<>();
            WorldGroup defaultGroup = new WorldGroup("default");
            defaultGroup.registerGroup();
            for (org.bukkit.World world : this.getServer().getWorlds()) {
                getLogger().info("added " + ChatColor.GOLD + world.getName() + ChatColor.RESET + " to 'default' group.");
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
                        getLogger().info("   added '" + world + "' to '" + newGroup.getName() + "'");
                        newGroup.addWorld(world);
                    }
                    if (newGroup.getWorlds().size() > 0) {
                        getLogger().info(" registered '" + newGroup.getName() + "' group");
                        newGroup.registerGroup();
                    }
                }
            }

            WorldGroup defaultGroup = WorldGroup.getGroupByName("default");
            if (defaultGroup == null) {
                defaultGroup = new WorldGroup("default");
                defaultGroup.registerGroup();
                getLogger().info(" registered 'default' group");
            }

            boolean saveConfig = false;
            for (org.bukkit.World world : getServer().getWorlds()) {
                if (WorldGroup.getGroupByWorld(world.getName()) == null) {
                    getLogger().info("added " + ChatColor.GOLD + world.getName() + ChatColor.RESET + " to 'default' group.");
                    defaultGroup.addWorld(world.getName());
                    saveConfig = true;
                }
            }
            if (saveConfig) {
                config.set("Groups.default", defaultGroup.getWorlds());
                yamlConfiguration.saveConfig();
            }
        }
        getLogger().info("-------------------------------------------------");
        return 0;
    }

    public boolean isInOnlineMode() {
        return (Bungee.isEnabled() && Bungee.isOnlineModeEnabled()) || Bukkit.getOnlineMode();
    }

    public File getFile() {
        return super.getFile();
    }

    @Override
    public MyPetInfo getMyPetInfo() {
        return petInfo;
    }

    @Override
    public EntityRegistry getEntityRegistry() {
        return entityRegistry;
    }

    @Override
    public CompatUtil getCompatUtil() {
        return compatUtil;
    }

    @Override
    public PlayerList getPlayerList() {
        return playerList;
    }

    @Override
    public MyPetList getMyPetList() {
        return myPetList;
    }

    @Override
    public HookManager getHookManager() {
        return hookManager;
    }

    public Repository getRepository() {
        return repo;
    }

    @Override
    public BukkitHelper getBukkitHelper() {
        return bukkitHelper;
    }

    private void replaceLogger() {
        try {
            Field logger = JavaPlugin.class.getDeclaredField("logger");
            logger.setAccessible(true);
            logger.set(this, new MyPetLogger(this));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}