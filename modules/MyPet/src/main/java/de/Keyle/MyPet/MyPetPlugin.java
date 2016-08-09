/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2016 Keyle
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

import com.google.common.base.Optional;
import de.Keyle.MyPet.api.*;
import de.Keyle.MyPet.api.entity.*;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.repository.*;
import de.Keyle.MyPet.api.skill.Skills;
import de.Keyle.MyPet.api.skill.SkillsInfo;
import de.Keyle.MyPet.api.skill.skills.*;
import de.Keyle.MyPet.api.skill.skilltree.SkillTreeMobType;
import de.Keyle.MyPet.api.skill.skilltreeloader.SkillTreeLoader;
import de.Keyle.MyPet.api.util.CompatUtil;
import de.Keyle.MyPet.api.util.Scheduler;
import de.Keyle.MyPet.api.util.Timer;
import de.Keyle.MyPet.api.util.configuration.ConfigurationYAML;
import de.Keyle.MyPet.api.util.hooks.HookHelper;
import de.Keyle.MyPet.api.util.hooks.PluginHookManager;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.commands.*;
import de.Keyle.MyPet.listeners.*;
import de.Keyle.MyPet.repository.Converter;
import de.Keyle.MyPet.repository.types.MongoDbRepository;
import de.Keyle.MyPet.repository.types.MySqlRepository;
import de.Keyle.MyPet.repository.types.NbtRepository;
import de.Keyle.MyPet.skill.skills.*;
import de.Keyle.MyPet.skill.skilltreeloader.SkillTreeLoaderJSON;
import de.Keyle.MyPet.skill.skilltreeloader.SkillTreeLoaderNBT;
import de.Keyle.MyPet.util.ConfigurationLoader;
import de.Keyle.MyPet.util.Metrics;
import de.Keyle.MyPet.util.UpdateCheck;
import de.Keyle.MyPet.util.hooks.*;
import de.Keyle.MyPet.util.logger.MyPetLogger;
import de.Keyle.MyPet.util.player.MyPetPlayerImpl;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

public class MyPetPlugin extends JavaPlugin implements de.Keyle.MyPet.api.plugin.MyPetPlugin {
    private boolean isReady = false;
    private Repository repo;
    private MyPetInfo petInfo;
    private PlatformHelper platformHelper;
    private EntityRegistry entityRegistry;
    private CompatUtil compatUtil;
    private PlayerManager playerManager;
    private MyPetManager myPetManager;
    private HookHelper hookHelper;
    private PluginHookManager pluginHookManager;

    public void onDisable() {
        if (isReady) {
            for (MyPet myPet : myPetManager.getAllActiveMyPets()) {
                myPet.removePet();
            }
            repo.disable();
            entityRegistry.unregisterEntityTypes();
        }
        Timer.reset();
        Bukkit.getServer().getScheduler().cancelTasks(this);

        if (getLogger() instanceof MyPetLogger) {
            ((MyPetLogger) getLogger()).disableDebugLogger();
        }
    }

    public void onLoad() {
        MyPetApi.setPlugin(this);
        replaceLogger();
        getDataFolder().mkdirs();

        // load version from manifest
        MyPetVersion.reset();

        compatUtil = new CompatUtil();

        petInfo = compatUtil.getComapatInstance(MyPetInfo.class, "entity", "MyPetInfo");
        platformHelper = compatUtil.getComapatInstance(PlatformHelper.class, "", "PlatformHelper");
        entityRegistry = compatUtil.getComapatInstance(EntityRegistry.class, "entity", "EntityRegistry");
        myPetManager = new de.Keyle.MyPet.repository.MyPetManager();
        playerManager = new de.Keyle.MyPet.repository.PlayerManager();
        hookHelper = new de.Keyle.MyPet.util.HookHelper();

        pluginHookManager = new PluginHookManager();

        ConfigurationLoader.setDefault();
        ConfigurationLoader.loadConfiguration();

        registerHooks();
    }

    public void onEnable() {
        this.isReady = false;

        if (getConfig().getBoolean("MyPet.Update-Check", true)) {
            Optional<String> message = UpdateCheck.checkForUpdate("MyPet");
            if (message.isPresent()) {
                String m = "#  A new version is available: " + message.get() + "  #";
                MyPetApi.getLogger().info(StringUtils.repeat("#", m.length()));
                MyPetApi.getLogger().info(m);
                MyPetApi.getLogger().info(StringUtils.repeat("#", m.length()));
            }
        }

        if (compatUtil.getInternalVersion() == null || !MyPetVersion.isValidBukkitPacket(compatUtil.getInternalVersion())) {
            getLogger().warning(ChatColor.RED + "This version of MyPet is not compatible with \"" + compatUtil.getInternalVersion() + "\". Is MyPet up to date?");
            setEnabled(false);
            return;
        }

        pluginHookManager.enableHooks();

        entityRegistry.registerEntityTypes();

        if (getLogger() instanceof MyPetLogger) {
            ((MyPetLogger) getLogger()).updateDebugLoggerLogLevel();
        }

        getLogger().info("Compat mode for " + compatUtil.getInternalVersion() + " loaded.");

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
        getCommand("petlist").setExecutor(new CommandList());

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
            platformHelper.copyResource(this, "skilltrees/default.st", new File(skilltreeFolder, "default.st"));
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
                Bukkit.getPluginManager().addPermission(new Permission("MyPet.skilltree." + skilltreeName));
            } catch (Exception ignored) {
            }
        }

        File translationReadme = new File(getDataFolder(), "locale" + File.separator + "readme.txt");
        if (!translationReadme.exists()) {
            platformHelper.copyResource(this, "locale-readme.txt", translationReadme);
        }
        Translation.init();

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
                        return myPetManager.countActiveMyPets();
                    }
                };
                graphTotalCount.addPlotter(plotter);

                metrics.start();
            }
            metrics = new Metrics(this, "MyPet-Premium");
            if (!metrics.isOptOut()) {

                Metrics.Graph graphTotalCount = metrics.createGraph("MyPets");

                Metrics.Plotter plotter = new Metrics.Plotter("MyPets") {
                    @Override
                    public int getValue() {
                        return myPetManager.countActiveMyPets();
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
        new BukkitRunnable() {
            @Override
            public void run() {
                for (final Player player : getServer().getOnlinePlayers()) {
                    repo.getMyPetPlayer(player, new RepositoryCallback<MyPetPlayer>() {
                        @Override
                        public void callback(final MyPetPlayer p) {
                            if (p != null) {
                                final MyPetPlayerImpl onlinePlayer = (MyPetPlayerImpl) p;

                                onlinePlayer.setLastKnownName(player.getName());
                                if (!player.getUniqueId().equals(onlinePlayer.getOfflineUUID())) {
                                    if (onlinePlayer.getMojangUUID() == null) {
                                        onlinePlayer.setMojangUUID(player.getUniqueId());
                                    }
                                    onlinePlayer.setOnlineMode(true);
                                }

                                playerManager.setOnline(onlinePlayer);

                                final WorldGroup joinGroup = WorldGroup.getGroupByWorld(player.getWorld().getName());
                                if (onlinePlayer.hasMyPet()) {
                                    MyPet myPet = onlinePlayer.getMyPet();
                                    if (!myPet.getWorldGroup().equals(joinGroup.getName())) {
                                        myPetManager.deactivateMyPet(onlinePlayer, true);
                                    }
                                }

                                if (joinGroup != null && !onlinePlayer.hasMyPet() && onlinePlayer.hasMyPetInWorldGroup(joinGroup.getName())) {
                                    final UUID petUUID = onlinePlayer.getMyPetForWorldGroup(joinGroup.getName());

                                    MyPetApi.getRepository().getMyPet(petUUID, new RepositoryCallback<StoredMyPet>() {
                                        @Override
                                        public void callback(StoredMyPet storedMyPet) {
                                            myPetManager.activateMyPet(storedMyPet);

                                            if (onlinePlayer.hasMyPet()) {
                                                final MyPet myPet = onlinePlayer.getMyPet();
                                                final MyPetPlayer myPetPlayer = myPet.getOwner();
                                                if (myPet.wantsToRespawn()) {
                                                    if (myPetPlayer.hasMyPet()) {
                                                        MyPet runMyPet = myPetPlayer.getMyPet();
                                                        switch (runMyPet.createEntity()) {
                                                            case Canceled:
                                                                runMyPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Spawn.Prevent", myPet.getOwner()), runMyPet.getPetName()));
                                                                break;
                                                            case NoSpace:
                                                                runMyPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Spawn.NoSpace", myPet.getOwner()), runMyPet.getPetName()));
                                                                break;
                                                            case NotAllowed:
                                                                runMyPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.No.AllowedHere", myPet.getOwner()), myPet.getPetName()));
                                                                break;
                                                            case Dead:
                                                                runMyPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Spawn.Respawn.In", myPet.getOwner()), myPet.getPetName(), myPet.getRespawnTime()));
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
                                onlinePlayer.checkForDonation();
                            }
                        }
                    });
                }
            }
        }.runTaskLater(this, 0);
    }

    private void registerHooks() {
        pluginHookManager.registerHook(AncientHook.class);
        pluginHookManager.registerHook(BattleArenaHook.class);
        pluginHookManager.registerHook(CitizensHook.class);
        pluginHookManager.registerHook(FactionsHook.class);
        pluginHookManager.registerHook(GriefPreventionHook.class);
        pluginHookManager.registerHook(HeroesHook.class);
        pluginHookManager.registerHook(KingdomsHook.class);
        pluginHookManager.registerHook(MagicSpellsHook.class);
        pluginHookManager.registerHook(McMMOHook.class);
        pluginHookManager.registerHook(MinigamesHook.class);
        pluginHookManager.registerHook(MobArenaHook.class);
        pluginHookManager.registerHook(NoCheatPlusHook.class);
        pluginHookManager.registerHook(PlotSquaredHook.class);
        pluginHookManager.registerHook(ProtocolLibHook.class);
        pluginHookManager.registerHook(PvPArenaHook.class);
        pluginHookManager.registerHook(PvPManagerHook.class);
        pluginHookManager.registerHook(ResidenceHook.class);
        pluginHookManager.registerHook(ResourcePackApiHook.class);
        pluginHookManager.registerHook(SkillApiHook.class);
        pluginHookManager.registerHook(SurvivalGamesHook.class);
        pluginHookManager.registerHook(TownyHook.class);
        pluginHookManager.registerHook(UltimateSurvivalGamesHook.class);
        pluginHookManager.registerHook(VaultHook.class);
        pluginHookManager.registerHook(WorldGuardHook.class);
        pluginHookManager.registerHook(WorldGuardCustomFlagsHook.class);
    }

    public static void registerSkills() {
        Skills.registerSkill(Inventory.class);
        Skills.registerSkill(Heal.class);
        Skills.registerSkill(Pickup.class);
        Skills.registerSkill(Behavior.class);
        Skills.registerSkill(Damage.class);
        Skills.registerSkill(Control.class);
        Skills.registerSkill(Life.class);
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
        SkillsInfo.registerSkill(HealInfo.class);
        SkillsInfo.registerSkill(PickupInfo.class);
        SkillsInfo.registerSkill(BehaviorInfo.class);
        SkillsInfo.registerSkill(DamageInfo.class);
        SkillsInfo.registerSkill(ControlInfo.class);
        SkillsInfo.registerSkill(LifeInfo.class);
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
                        newGroup.registerGroup();
                    }
                }
            }

            WorldGroup defaultGroup = WorldGroup.getGroupByName("default");
            if (defaultGroup == null) {
                defaultGroup = new WorldGroup("default");
                defaultGroup.registerGroup();
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

    @Deprecated
    public boolean isInOnlineMode() {
        return Bukkit.getOnlineMode();
    }

    @Override
    public PluginHookManager getPluginHookManager() {
        return pluginHookManager;
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
    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public MyPetManager getMyPetManager() {
        return myPetManager;
    }

    public HookHelper getHookHelper() {
        return hookHelper;
    }

    public Repository getRepository() {
        return repo;
    }

    public PlatformHelper getPlatformHelper() {
        return platformHelper;
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