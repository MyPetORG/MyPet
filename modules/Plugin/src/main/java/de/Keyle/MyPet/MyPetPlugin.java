/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2020 Keyle
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

import de.Keyle.MyPet.api.*;
import de.Keyle.MyPet.api.entity.EntityRegistry;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetInfo;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.entity.leashing.LeashFlagManager;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.repository.*;
import de.Keyle.MyPet.api.skill.SkillManager;
import de.Keyle.MyPet.api.skill.experience.ExperienceCache;
import de.Keyle.MyPet.api.skill.experience.ExperienceCalculatorManager;
import de.Keyle.MyPet.api.skill.skilltree.SkillTreeLoaderJSON;
import de.Keyle.MyPet.api.skill.skilltree.SkilltreeManager;
import de.Keyle.MyPet.api.util.*;
import de.Keyle.MyPet.api.util.hooks.HookHelper;
import de.Keyle.MyPet.api.util.hooks.PluginHook;
import de.Keyle.MyPet.api.util.hooks.PluginHookManager;
import de.Keyle.MyPet.api.util.inventory.material.ItemDatabase;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.api.util.logger.MyPetLogger;
import de.Keyle.MyPet.api.util.service.Load;
import de.Keyle.MyPet.api.util.service.ServiceManager;
import de.Keyle.MyPet.commands.*;
import de.Keyle.MyPet.entity.leashing.*;
import de.Keyle.MyPet.listeners.*;
import de.Keyle.MyPet.repository.Converter;
import de.Keyle.MyPet.repository.types.MongoDbRepository;
import de.Keyle.MyPet.repository.types.MySqlRepository;
import de.Keyle.MyPet.repository.types.SqLiteRepository;
import de.Keyle.MyPet.services.RepositoryMyPetConverterService;
import de.Keyle.MyPet.skill.experience.JavaScriptExperienceCalculator;
import de.Keyle.MyPet.skill.skills.*;
import de.Keyle.MyPet.skill.skilltree.requirements.NoSkilltreeRequirement;
import de.Keyle.MyPet.skill.skilltree.requirements.PermissionRequirement;
import de.Keyle.MyPet.skill.skilltree.requirements.PetLevelRequirement;
import de.Keyle.MyPet.skill.skilltree.requirements.SkilltreeRequirement;
import de.Keyle.MyPet.util.ConfigurationLoader;
import de.Keyle.MyPet.util.Updater;
import de.Keyle.MyPet.util.hooks.*;
import de.Keyle.MyPet.util.player.MyPetPlayerImpl;
import de.Keyle.MyPet.util.sentry.SentryErrorReporter;
import de.Keyle.MyPet.util.shop.ShopManager;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@SuppressWarnings("unused")
public class MyPetPlugin extends JavaPlugin implements de.Keyle.MyPet.api.plugin.MyPetPlugin {

    private boolean isReady = false;
    private Repository repo;
    private MyPetInfo petInfo;
    private PlatformHelper platformHelper;
    private EntityRegistry entityRegistry;
    private CompatUtil compatUtil;
    private CompatManager compatManager;
    private PlayerManager playerManager;
    private MyPetManager myPetManager;
    private HookHelper hookHelper;
    private PluginHookManager pluginHookManager;
    private ServiceManager serviceManager;
    private SentryErrorReporter errorReporter = null;

    public void onDisable() {
        if (isReady) {
            for (MyPet myPet : myPetManager.getAllActiveMyPets()) {
                if (myPet.getStatus() == MyPet.PetState.Here) {
                    myPet.removePet(true);
                }
            }
            repo.disable();
            entityRegistry.unregisterEntityTypes();
        }
        Timer.reset();
        Bukkit.getServer().getScheduler().cancelTasks(this);

        if (getLogger() instanceof MyPetLogger) {
            ((MyPetLogger) getLogger()).disableDebugLogger();
        }

        if (pluginHookManager != null) {
            pluginHookManager.disableHooks();
        }
        if (serviceManager != null) {
            serviceManager.disableServices();
        }
        if (errorReporter != null) {
            errorReporter.onDisable();
        }
    }

    public void onLoad() {
        MyPetApi.setPlugin(this);
        replaceLogger();
        getDataFolder().mkdirs();

        // load version from manifest
        MyPetVersion.reset();

        if (getConfig().contains("MyPet.Log.Unique-ID")) {
            try {
                UUID serverUUID = UUID.fromString(getConfig().getString("MyPet.Log.Report-Errors"));
                SentryErrorReporter.setServerUUID(serverUUID);
            } catch (Throwable ignored) {
            }
        }
        this.errorReporter = new SentryErrorReporter();
        if (getConfig().getBoolean("MyPet.Log.Report-Errors", true)) {
            this.errorReporter.onEnable();
            MyPetApi.getLogger().info("Error-Reporter ENABLED");
        }

        compatUtil = new CompatUtil();

        ConfigurationLoader.upgradeConfig();
        ConfigurationLoader.setDefault();
        ConfigurationLoader.loadConfiguration();

        serviceManager = new ServiceManager();
        pluginHookManager = new PluginHookManager();

        if (compatUtil.getInternalVersion() == null || !MyPetVersion.isValidBukkitPacket(compatUtil.getInternalVersion())) {
            return;
        }

        petInfo = compatUtil.getComapatInstance(MyPetInfo.class, "entity", "MyPetInfo");
        platformHelper = compatUtil.getComapatInstance(PlatformHelper.class, "", "PlatformHelper");
        entityRegistry = compatUtil.getComapatInstance(EntityRegistry.class, "entity", "EntityRegistry");
        myPetManager = new de.Keyle.MyPet.repository.MyPetManager();
        playerManager = new de.Keyle.MyPet.repository.PlayerManager();
        hookHelper = new de.Keyle.MyPet.util.HookHelper();

        registerServices();

        compatManager = compatUtil.getComapatInstance(CompatManager.class, "", "CompatManager");
        compatManager.init();

        serviceManager.activate(Load.State.OnLoad);

        registerHooks();
    }

    public void onEnable() {
        this.isReady = false;

        Updater updater = new Updater("MyPet");
        updater.update();

        if (compatUtil.getInternalVersion() == null || !MyPetVersion.isValidBukkitPacket(compatUtil.getInternalVersion())) {
            getLogger().warning("This version of MyPet is not compatible with \"" + compatUtil.getInternalVersion() + "\". Is MyPet up to date?");
            updater.waitForDownload();
            setEnabled(false);
            return;
        }

        serviceManager.activate(Load.State.OnEnable);

        entityRegistry.registerEntityTypes();

        if (getLogger() instanceof MyPetLogger) {
            ((MyPetLogger) getLogger()).updateDebugLoggerLogLevel();
        }

        compatManager.enable();
        getLogger().info("Compat mode for " + compatUtil.getInternalVersion() + " loaded.");

        ConfigurationLoader.loadCompatConfiguration();

        //register leash flags
        registerLeashFlags();

        //register skilltree requirementSettings
        registerSkilltreeRequirements();

        //register exp calculators
        if (!new File(getDataFolder(), "exp.js").exists()) {
            platformHelper.copyResource(this, "exp.js", new File(getDataFolder(), "exp.js"));
        }
        serviceManager.getService(ExperienceCalculatorManager.class).ifPresent(calculatorManager -> {
            calculatorManager.registerCalculator("JS", JavaScriptExperienceCalculator.class);
            calculatorManager.registerCalculator("JavaScript", JavaScriptExperienceCalculator.class);
            calculatorManager.switchCalculator(Configuration.LevelSystem.CALCULATION_MODE.toLowerCase());
        });

        // register event listener
        PlayerListener playerListener = new PlayerListener();
        getServer().getPluginManager().registerEvents(playerListener, this);
        VehicleListener vehicleListener = new VehicleListener();
        getServer().getPluginManager().registerEvents(vehicleListener, this);
        EntityListener entityListener = new EntityListener();
        getServer().getPluginManager().registerEvents(entityListener, this);
        MyPetEntityListener myPetEntityListener = new MyPetEntityListener();
        getServer().getPluginManager().registerEvents(myPetEntityListener, this);
        LevelListener levelupListener = new LevelListener();
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
        getCommand("petchooseskilltree").setExecutor(new CommandChooseSkilltree());
        getCommand("petbeacon").setExecutor(new CommandBeacon());
        getCommand("petrespawn").setExecutor(new CommandRespawn());
        getCommand("petsettings").setExecutor(new CommandSettings());
        getCommand("petswitch").setExecutor(new CommandSwitch());
        getCommand("petstore").setExecutor(new CommandStore());
        getCommand("petlist").setExecutor(new CommandList());
        getCommand("petcapturehelper").setExecutor(new CommandCaptureHelper());
        getCommand("pettrade").setExecutor(new CommandTrade());
        getCommand("petshop").setExecutor(new CommandShop());

        // load worldgroups
        WorldGroup.loadGroups(new File(getDataFolder().getPath(), "worldgroups.yml"));

        // register skills
        registerSkills();

        // create folders
        File skilltreeFolder = new File(getDataFolder().getPath(), "skilltrees");
        getDataFolder().mkdirs();
        boolean createDefaultSkilltree = skilltreeFolder.mkdirs();
        boolean createLocaleReadme = new File(getDataFolder(), "locale").mkdirs();
        new File(getDataFolder(), "logs").mkdirs();

        if (!createDefaultSkilltree) {
            File legacyDefaultSkilltree = new File(skilltreeFolder, "default.st");
            if (legacyDefaultSkilltree.exists()) {
                if (Util.getSha256FromFile(legacyDefaultSkilltree) == -4323392001800132707L) {
                    createDefaultSkilltree = true;
                    legacyDefaultSkilltree.delete();
                }
            }
        }

        if (createDefaultSkilltree) {
            File skilltreeFile = new File(skilltreeFolder, "Combat.st.json");
            if (!skilltreeFile.exists()) {
                platformHelper.copyResource(this, "skilltrees/Combat.st.json", new File(skilltreeFolder, "Combat.st.json"));
            }
            skilltreeFile = new File(skilltreeFolder, "Farm.st.json");
            if (!skilltreeFile.exists()) {
                platformHelper.copyResource(this, "skilltrees/Farm.st.json", new File(skilltreeFolder, "Farm.st.json"));
            }
            skilltreeFile = new File(skilltreeFolder, "PvP.st.json");
            if (!skilltreeFile.exists()) {
                platformHelper.copyResource(this, "skilltrees/PvP.st.json", new File(skilltreeFolder, "PvP.st.json"));
            }
            skilltreeFile = new File(skilltreeFolder, "Ride.st.json");
            if (!skilltreeFile.exists()) {
                platformHelper.copyResource(this, "skilltrees/Ride.st.json", new File(skilltreeFolder, "Ride.st.json"));
            }
            skilltreeFile = new File(skilltreeFolder, "Utility.st.json");
            if (!skilltreeFile.exists()) {
                platformHelper.copyResource(this, "skilltrees/Utility.st.json", new File(skilltreeFolder, "Utility.st.json"));
            }
            MyPetApi.getLogger().info("Default skilltree files created.");
        }

        // load skilltrees
        MyPetApi.getSkilltreeManager().clearSkilltrees();
        SkillTreeLoaderJSON.loadSkilltrees(new File(getDataFolder(), "skilltrees"));

        for (int i = 0; i <= Configuration.Misc.MAX_STORED_PET_COUNT; i++) {
            try {
                Bukkit.getPluginManager().addPermission(new Permission("MyPet.petstorage.limit." + i));
            } catch (Exception ignored) {
            }
        }

        if (createLocaleReadme) {
            platformHelper.copyResource(this, "locale-readme.txt", new File(getDataFolder(), "locale" + File.separator + "readme.txt"));
        }
        Translation.init();

        for (Team team : Bukkit.getScoreboardManager().getMainScoreboard().getTeams()) {
            if (team.getName().startsWith("MyPet-")) {
                team.unregister();
            }
        }

        // init repository
        if (Configuration.Repository.REPOSITORY_TYPE.equalsIgnoreCase("NBT")) {
            Configuration.Repository.REPOSITORY_TYPE = "SQLite";
            Configuration.Repository.CONVERT_FROM = "NBT";
            repo = new SqLiteRepository();
            try {
                repo.init();
            } catch (RepositoryInitException e) {
                e.printStackTrace();
                repo = null;
            }
        } else if (Configuration.Repository.REPOSITORY_TYPE.equalsIgnoreCase("MySQL")) {
            MyPetApi.getLogger().info("Connect to MySQL database...");
            repo = new MySqlRepository();
            try {
                repo.init();
                MyPetApi.getLogger().info("MySQL connection successful.");
            } catch (RepositoryInitException e) {
                MyPetApi.getLogger().warning("MySQL connection failed!");
                e.printStackTrace();
                repo = null;
            }
        } else if (Configuration.Repository.REPOSITORY_TYPE.equalsIgnoreCase("MongoDB")) {
            MyPetApi.getLogger().info("Connect to MongoDB database...");
            repo = new MongoDbRepository();
            try {
                repo.init();
                MyPetApi.getLogger().info("MongoDB connection successful.");
            } catch (RepositoryInitException e) {
                MyPetApi.getLogger().warning("MongoDB connection failed!");
                e.printStackTrace();
                repo = null;
            }
        }

        if (repo == null) {
            MyPetApi.getLogger().info("Connect to SQLite database...");
            repo = new SqLiteRepository();
            try {
                repo.init();
                MyPetApi.getLogger().info("SQLite connection successful.");
            } catch (RepositoryInitException ignored) {
                MyPetApi.getLogger().warning("SQLite connection failed!");
                setEnabled(false);
                return;
            }
        }

        Converter.convert();

        if (repo instanceof Scheduler) {
            Timer.addTask((Scheduler) repo);
        }

        File shopConfig = new File(getDataFolder(), "pet-shops.yml");
        if (!shopConfig.exists()) {
            platformHelper.copyResource(this, "pet-shops.yml", shopConfig);
        }
        new ShopManager();

        Timer.startTimer();

        updater.waitForDownload();

        pluginHookManager.enableHooks();
        serviceManager.activate(Load.State.AfterHooks);

        // init Metrics
        try {
            Metrics metrics = new Metrics(this, 778);
            if (metrics.isEnabled()) {
                metrics.addCustomChart(new Metrics.SingleLineChart("active_pets", () -> myPetManager.countActiveMyPets()));
                metrics.addCustomChart(new Metrics.SimplePie("build", MyPetVersion::getBuild));
                metrics.addCustomChart(new Metrics.SimplePie("update_mode", () -> {
                    String mode = "Disabled";
                    if (Configuration.Update.CHECK) {
                        mode = "Check";
                        if (Configuration.Update.DOWNLOAD) {
                            mode += " & Download";
                        }
                    }
                    return mode;
                }
                ));
                metrics.addCustomChart(new Metrics.AdvancedPie("hooks", () -> {
                    Map<String, Integer> activatedHooks = new HashMap<>();
                    for (PluginHook hook : MyPetApi.getPluginHookManager().getHooks()) {
                        activatedHooks.put(hook.getPluginName(), 1);
                    }
                    return activatedHooks;
                }
                ));
            }
        } catch (Throwable e) {
            errorReporter.sendError(e, "Init Metrics failed");
        }

        getLogger().info("Version " + MyPetVersion.getVersion() + "-b" + MyPetVersion.getBuild() + ChatColor.GREEN + " ENABLED");
        this.isReady = true;

        serviceManager.activate(Load.State.OnReady);

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
                                if (joinGroup.isDisabled()) {
                                    return;
                                }
                                if (onlinePlayer.hasMyPet()) {
                                    MyPet myPet = onlinePlayer.getMyPet();
                                    if (!myPet.getWorldGroup().equals(joinGroup.getName())) {
                                        myPetManager.deactivateMyPet(onlinePlayer, true);
                                    }
                                }

                                if (!onlinePlayer.hasMyPet() && onlinePlayer.hasMyPetInWorldGroup(joinGroup.getName())) {
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
                                                                if (Configuration.Respawn.DISABLE_AUTO_RESPAWN) {
                                                                    runMyPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Call.Dead", myPet.getOwner()), myPet.getPetName()));
                                                                } else {
                                                                    runMyPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Spawn.Respawn.In", myPet.getOwner()), myPet.getPetName(), myPet.getRespawnTime()));
                                                                }
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

    private void registerServices() {
        serviceManager.registerService(RepositoryMyPetConverterService.class);
        serviceManager.registerService(ItemDatabase.class);
        serviceManager.registerService(LeashFlagManager.class);
        serviceManager.registerService(ExperienceCache.class);
        serviceManager.registerService(ExperienceCalculatorManager.class);
        serviceManager.registerService(SkillManager.class);
        serviceManager.registerService(SkilltreeManager.class);
        serviceManager.registerService(ShopManager.class);
    }

    private void registerHooks() {
        pluginHookManager.registerHook(AncientHook.class);
        pluginHookManager.registerHook(BattleArenaHook.class);
        pluginHookManager.registerHook(BossShopProHook.class);
        pluginHookManager.registerHook(CitizensHook.class);
        pluginHookManager.registerHook(CombatLogXHook.class);
        pluginHookManager.registerHook(FabledSkyBlockHook.class);
        pluginHookManager.registerHook(FactionsHook.class);
        pluginHookManager.registerHook(GangsPlusHook.class);
        pluginHookManager.registerHook(GriefPreventionHook.class);
        pluginHookManager.registerHook(GriefPreventionPlusHook.class);
        pluginHookManager.registerHook(GuildsHook.class);
        pluginHookManager.registerHook(HeroesHook.class);
        pluginHookManager.registerHook(KingdomsHook.class);
        pluginHookManager.registerHook(LandsHook.class);
        pluginHookManager.registerHook(MagicSpellsHook.class);
        pluginHookManager.registerHook(McMMOHook.class);
        pluginHookManager.registerHook(MiniaturePetsHook.class);
        pluginHookManager.registerHook(MinigamesHook.class);
        pluginHookManager.registerHook(MobArenaHook.class);
        pluginHookManager.registerHook(MobStackerAHook.class);
        pluginHookManager.registerHook(MobStackerBHook.class);
        pluginHookManager.registerHook(MythicMobsHook.class);
        pluginHookManager.registerHook(NoCheatPlusHook.class);
        pluginHookManager.registerHook(PlaceholderApiHook.class);
        pluginHookManager.registerHook(PlotSquaredHook.class);
        pluginHookManager.registerHook(PreciousStonesHook.class);
        pluginHookManager.registerHook(PremiumVanishHook.class);
        pluginHookManager.registerHook(ProtocolLibHook.class);
        pluginHookManager.registerHook(PvPArenaHook.class);
        pluginHookManager.registerHook(PvPDiffTimerHook.class);
        pluginHookManager.registerHook(PvPManagerHook.class);
        pluginHookManager.registerHook(RedProtectHook.class);
        pluginHookManager.registerHook(ResidenceHook.class);
        pluginHookManager.registerHook(SimpleClansHook.class);
        pluginHookManager.registerHook(SkillApiHook.class);
        pluginHookManager.registerHook(StackMobHook.class);
        pluginHookManager.registerHook(SuperVanishHook.class);
        pluginHookManager.registerHook(SurvivalGamesHook.class);
        pluginHookManager.registerHook(TownyHook.class);
        pluginHookManager.registerHook(UltimateSurvivalGamesHook.class);
        pluginHookManager.registerHook(VaultHook.class);
        pluginHookManager.registerHook(WorldGuardHook.class);
        pluginHookManager.registerHook(WorldGuardCustomFlagsHook.class);
    }

    public static void registerSkills() {
        MyPetApi.getSkillManager().registerSkill(BackpackImpl.class);
        MyPetApi.getSkillManager().registerSkill(HealImpl.class);
        MyPetApi.getSkillManager().registerSkill(PickupImpl.class);
        MyPetApi.getSkillManager().registerSkill(BehaviorImpl.class);
        MyPetApi.getSkillManager().registerSkill(DamageImpl.class);
        MyPetApi.getSkillManager().registerSkill(ControlImpl.class);
        MyPetApi.getSkillManager().registerSkill(LifeImpl.class);
        MyPetApi.getSkillManager().registerSkill(PoisonImpl.class);
        MyPetApi.getSkillManager().registerSkill(RideImpl.class);
        MyPetApi.getSkillManager().registerSkill(ThornsImpl.class);
        MyPetApi.getSkillManager().registerSkill(FireImpl.class);
        MyPetApi.getSkillManager().registerSkill(BeaconImpl.class);
        MyPetApi.getSkillManager().registerSkill(WitherImpl.class);
        MyPetApi.getSkillManager().registerSkill(LightningImpl.class);
        MyPetApi.getSkillManager().registerSkill(SlowImpl.class);
        MyPetApi.getSkillManager().registerSkill(KnockbackImpl.class);
        MyPetApi.getSkillManager().registerSkill(RangedImpl.class);
        MyPetApi.getSkillManager().registerSkill(SprintImpl.class);
        MyPetApi.getSkillManager().registerSkill(StompImpl.class);
        MyPetApi.getSkillManager().registerSkill(ShieldImpl.class);
    }

    public static void registerLeashFlags() {
        MyPetApi.getLeashFlagManager().registerLeashFlag(new AdultFlag());
        MyPetApi.getLeashFlagManager().registerLeashFlag(new AngryFlag());
        MyPetApi.getLeashFlagManager().registerLeashFlag(new BabyFlag());
        MyPetApi.getLeashFlagManager().registerLeashFlag(new BelowHpFlag());
        MyPetApi.getLeashFlagManager().registerLeashFlag(new CanBreedFlag());
        MyPetApi.getLeashFlagManager().registerLeashFlag(new ChanceFlag());
        MyPetApi.getLeashFlagManager().registerLeashFlag(new ImpossibleFlag());
        MyPetApi.getLeashFlagManager().registerLeashFlag(new LowHpFlag());
        MyPetApi.getLeashFlagManager().registerLeashFlag(new SizeFlag());
        MyPetApi.getLeashFlagManager().registerLeashFlag(new TamedFlag());
        MyPetApi.getLeashFlagManager().registerLeashFlag(new UserCreatedFlag());
        MyPetApi.getLeashFlagManager().registerLeashFlag(new WildFlag());
        MyPetApi.getLeashFlagManager().registerLeashFlag(new WorldFlag());
    }

    public static void registerSkilltreeRequirements() {
        MyPetApi.getSkilltreeManager().registerRequirement(new NoSkilltreeRequirement());
        MyPetApi.getSkilltreeManager().registerRequirement(new PermissionRequirement());
        MyPetApi.getSkilltreeManager().registerRequirement(new PetLevelRequirement());
        MyPetApi.getSkilltreeManager().registerRequirement(new SkilltreeRequirement());
    }

    @Override
    public PluginHookManager getPluginHookManager() {
        return pluginHookManager;
    }

    @Override
    public ServiceManager getServiceManager() {
        return serviceManager;
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

    public SentryErrorReporter getErrorReporter() {
        return errorReporter;
    }

    private void replaceLogger() {
        try {
            Field logger = ReflectionUtil.getField(JavaPlugin.class, "logger");
            if (logger != null) {
                logger.set(this, new MyPetLogger(this));
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
