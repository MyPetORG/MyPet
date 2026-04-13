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
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetInfo;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.entity.leashing.LeashFlagManager;
import de.Keyle.MyPet.api.player.ContributorCheck;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.repository.*;
import de.Keyle.MyPet.api.skill.SkillManager;
import de.Keyle.MyPet.api.skill.skilltree.Skill;
import de.Keyle.MyPet.api.skill.experience.ExperienceCache;
import de.Keyle.MyPet.api.skill.experience.ExperienceCalculatorManager;
import de.Keyle.MyPet.api.skill.skilltree.SkillTreeLoaderJSON;
import de.Keyle.MyPet.api.skill.skilltree.SkilltreeManager;
import de.Keyle.MyPet.api.util.*;
import de.Keyle.MyPet.api.commands.HelpRegistry;
import de.Keyle.MyPet.api.util.hooks.HookHelper;
import de.Keyle.MyPet.api.util.hooks.PluginHook;
import de.Keyle.MyPet.api.util.hooks.PluginHookManager;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.api.util.logger.DebugLogHandler;
import de.Keyle.MyPet.api.util.service.Load;
import de.Keyle.MyPet.api.util.service.ServiceManager;
import de.Keyle.MyPet.api.util.service.types.EggIconService;
import de.Keyle.MyPet.commands.*;
import de.Keyle.MyPet.entity.ai.attack.PetProjectileHitListener;
import de.Keyle.MyPet.entity.ai.target.PetDamageTracker;
import de.Keyle.MyPet.entity.info.MyPetInfoImpl;
import de.Keyle.MyPet.entity.leashing.*;
import de.Keyle.MyPet.entity.ride.RideSkillFlightController;
import de.Keyle.MyPet.entity.visual.PetPotionParticleController;
import de.Keyle.MyPet.entity.visual.PetSitParticleController;
import de.Keyle.MyPet.listeners.*;
import de.Keyle.MyPet.repository.Converter;
import de.Keyle.MyPet.repository.types.MongoDbRepository;
import de.Keyle.MyPet.repository.types.MySqlRepository;
import de.Keyle.MyPet.repository.types.SqLiteRepository;
import de.Keyle.MyPet.services.DefaultCreakingService;
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
import de.Keyle.MyPet.util.shop.ShopConfigGenerator;
import de.Keyle.MyPet.util.shop.ShopManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.time.Year;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


/**
 * Main Bukkit plugin class for MyPet.
 *
 * <p>This is the entry point that Bukkit/Paper loads. It orchestrates the full plugin lifecycle
 * through {@link #onLoad()}, {@link #onEnable()}, and {@link #onDisable()}, coordinating
 * version-specific NMS compatibility, repository initialization, service activation, and
 * third-party plugin hook registration.</p>
 *
 * <h3>Lifecycle overview</h3>
 * <ol>
 *   <li>{@code onLoad} — configuration, version detection, NMS compat manager init, service
 *       registration, hook registration, {@link Load.State#OnLoad} services activated</li>
 *   <li>{@code onEnable} — entity registration, event listeners, commands, skilltrees,
 *       repository init, metrics, {@link Load.State#OnEnable}/{@link Load.State#AfterHooks}/
 *       {@link Load.State#OnReady} services activated, online player pet loading</li>
 *   <li>{@code onDisable} — active pets removed, repository closed, tasks cancelled,
 *       hooks and services disabled, error reporter shut down</li>
 * </ol>
 *
 * @see de.Keyle.MyPet.api.plugin.MyPetPlugin
 * @see MyPetApi
 */
@SuppressWarnings("unused")
public final class MyPetPlugin extends JavaPlugin implements de.Keyle.MyPet.api.plugin.MyPetPlugin {

    /** Whether the plugin has completed full initialization and is ready to serve. */
    private boolean isReady = false;

    /** Set to {@code true} at the start of {@link #onDisable()} to signal shutdown. */
    @Getter
    private boolean isDisabling = false;

    // Note: The following fields are initialized during onLoad()/onEnable() and are
    // guaranteed non-null once the plugin is fully active. They lack initializers because
    // they depend on runtime version detection, configuration, and NMS reflection.

    /** The active persistence backend (SQLite, MySQL, or MongoDB). Initialized in {@link #onEnable()}. */
    @Getter
    private Repository repository;

    /** Version-specific pet metadata provider, loaded via NMS reflection in {@link #onLoad()}. */
    @Getter
    private MyPetInfo myPetInfo;

    /** Version-specific platform utilities, loaded via NMS reflection in {@link #onLoad()}. */
    @Getter
    private PlatformHelper platformHelper;

    /** Minecraft version detection and NMS class loading utility. Initialized in {@link #onLoad()}. */
    @Getter
    private CompatUtil compatUtil;

    /** Version-specific compatibility manager that registers NMS services and listeners. Initialized in {@link #onLoad()}. */

    /** Manages online {@link MyPetPlayer} instances. Initialized in {@link #onLoad()}. */
    @Getter
    private PlayerManager playerManager;

    /** Manages active and stored pet instances. Initialized in {@link #onLoad()}. */
    @Getter
    private MyPetManager myPetManager;

    /** Provides helper methods for third-party plugin integrations. Initialized in {@link #onLoad()}. */
    @Getter
    private HookHelper hookHelper;

    /** Registry and lifecycle manager for third-party plugin hooks. Initialized in {@link #onLoad()}. */
    @Getter
    private PluginHookManager pluginHookManager;

    /** Central registry for plugin services, activated at different lifecycle states. Initialized in {@link #onLoad()}. */
    @Getter
    private ServiceManager serviceManager;

    /** Shared MiniMessage instance for Adventure text deserialization. Initialized in {@link #onEnable()}. */
    @Getter
    private MiniMessage miniMessage;

    /** Registry of help entries for the /mypet help command. Initialized in {@link #onEnable()}. */
    @Getter
    private HelpRegistry helpRegistry;

    /** Sentry error reporter for remote error tracking in non-local builds. */
    @Getter
    private SentryErrorReporter errorReporter = null;

    /**
     * Registers all 21 built-in pet skill implementations with the {@link SkillManager}.
     *
     * <p>Skills define pet abilities such as dealing damage, healing the owner, carrying items,
     * and more. Each skill is registered by its implementation class and later bound to pets
     * via skilltrees.</p>
     *
     * @see SkillManager#registerSkill(Class)
     */
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
        MyPetApi.getSkillManager().registerSkill(BleedImpl.class);
    }

    /**
     * Registers all built-in leash flag implementations with the {@link LeashFlagManager}.
     *
     * <p>Leash flags define conditions that must be met before a mob can be leashed as a pet
     * (e.g., the mob must be a baby, tamed, below a certain HP threshold, etc.).</p>
     *
     * @see LeashFlagManager#registerLeashFlag(de.Keyle.MyPet.api.entity.leashing.LeashFlag)
     */
    public static void registerLeashFlags() {
        MyPetApi.getLeashFlagManager().registerLeashFlag(new AdultFlag());
        MyPetApi.getLeashFlagManager().registerLeashFlag(new AngryFlag());
        MyPetApi.getLeashFlagManager().registerLeashFlag(new BabyFlag());
        MyPetApi.getLeashFlagManager().registerLeashFlag(new BelowHpFlag());
        MyPetApi.getLeashFlagManager().registerLeashFlag(new CanBreedFlag());
        MyPetApi.getLeashFlagManager().registerLeashFlag(new ChanceFlag());
        MyPetApi.getLeashFlagManager().registerLeashFlag(new ImpossibleFlag());
        MyPetApi.getLeashFlagManager().registerLeashFlag(new LowHpFlag());
        MyPetApi.getLeashFlagManager().registerLeashFlag(new ScreamingFlag());
        MyPetApi.getLeashFlagManager().registerLeashFlag(new SizeFlag());
        MyPetApi.getLeashFlagManager().registerLeashFlag(new TamedFlag());
        MyPetApi.getLeashFlagManager().registerLeashFlag(new UserCreatedFlag());
        MyPetApi.getLeashFlagManager().registerLeashFlag(new WildFlag());
        MyPetApi.getLeashFlagManager().registerLeashFlag(new WorldFlag());
        MyPetApi.getLeashFlagManager().registerLeashFlag(new PermissionFlag());
        MyPetApi.getLeashFlagManager().registerLeashFlag(new HeartLinkedFlag());
    }

    /**
     * Registers all built-in skilltree requirement types with the {@link SkilltreeManager}.
     *
     * <p>Requirements define conditions that control which skilltrees a pet can use
     * (e.g., having a specific permission, reaching a certain level, or already having
     * a particular skilltree).</p>
     */
    public static void registerSkilltreeRequirements() {
        MyPetApi.getSkilltreeManager().registerRequirement(new NoSkilltreeRequirement());
        MyPetApi.getSkilltreeManager().registerRequirement(new PermissionRequirement());
        MyPetApi.getSkilltreeManager().registerRequirement(new PetLevelRequirement());
        MyPetApi.getSkilltreeManager().registerRequirement(new SkilltreeRequirement());
    }

    /**
     * Handles plugin shutdown.
     *
     * <p>Performs the following cleanup in order:</p>
     * <ol>
     *   <li>Removes all active pet entities from the world</li>
     *   <li>Closes the persistence repository</li>
     *   <li>Unregisters custom entity types</li>
     *   <li>Cancels all scheduled Bukkit tasks</li>
     *   <li>Disables debug logging, plugin hooks, services, and the error reporter</li>
     * </ol>
     */
    public void onDisable() {
        isDisabling = true;

        if (isReady) {
            for (MyPet myPet : myPetManager.getAllActiveMyPets()) {
                if (myPet.getStatus() == MyPet.PetState.Here) {
                    myPet.removePet(true);
                }
            }
            repository.disable();
            Timer.reset();
        }
        Bukkit.getServer().getScheduler().cancelTasks(this);

        DebugLogHandler.disable(getLogger());

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

    /**
     * Handles the early plugin load phase (before any plugins are enabled).
     *
     * <p>Initializes foundational systems in order:</p>
     * <ol>
     *   <li>Sets this plugin as the active instance via {@link MyPetApi#setPlugin(de.Keyle.MyPet.api.plugin.MyPetPlugin)}</li>
     *   <li>Reads version info from the JAR manifest</li>
     *   <li>Initializes Sentry error reporting (if enabled in config)</li>
     *   <li>Detects Minecraft version via {@link CompatUtil}</li>
     *   <li>Loads, upgrades, and applies configuration</li>
     *   <li>Creates the {@link ServiceManager} and {@link PluginHookManager}</li>
     *   <li>Loads version-specific NMS instances (pet info, platform helper, entity registry)</li>
     *   <li>Registers {@code EggIconService} and activates {@link Load.State#OnLoad} services</li>
     *   <li>Registers third-party plugin hooks</li>
     * </ol>
     *
     * <p>If the detected Minecraft version is unsupported, initialization stops early and
     * the plugin will be disabled in {@link #onEnable()}.</p>
     */
    public void onLoad() {
        MyPetApi.setPlugin(this);
        getDataFolder().mkdirs();

        // load version from manifest
        MyPetVersion.reset();

        if (getConfig().contains("MyPet.Log.Unique-ID")) {
            try {
                UUID serverUUID = UUID.fromString(getConfig().getString("MyPet.Log.Unique-ID"));
                SentryErrorReporter.setServerUUID(serverUUID);
            } catch (Throwable ignored) {
            }
        }
        this.errorReporter = new SentryErrorReporter();
        if (getConfig().getBoolean("MyPet.Log.Report-Errors", true)) {
            this.errorReporter.onEnable();
        }

        compatUtil = new CompatUtil();

        ConfigurationLoader.upgradeConfig();
        ConfigurationLoader.setDefault();
        ConfigurationLoader.loadConfiguration();

        serviceManager = new ServiceManager();
        pluginHookManager = new PluginHookManager();

        myPetInfo = new MyPetInfoImpl();
        platformHelper = new PlatformHelper();
        myPetManager = new de.Keyle.MyPet.repository.MyPetManager();
        playerManager = new de.Keyle.MyPet.repository.PlayerManager();
        hookHelper = new de.Keyle.MyPet.util.HookHelper();

        registerServices();

        MyPetApi.getServiceManager().registerService(EggIconService.class);

        serviceManager.activate(Load.State.OnLoad);

        registerHooks();
    }

    /**
     * Handles the main plugin enable phase.
     *
     * <p>This is the heaviest lifecycle method, performing all remaining initialization:</p>
     * <ul>
     *   <li>Version compatibility check (disables plugin if incompatible)</li>
     *   <li>Entity type registration with the server</li>
     *   <li>Event listener registration (player, entity, vehicle, world, level, ride, creaking)</li>
     *   <li>Command registration</li>
     *   <li>Leash flag, skilltree requirement, and experience calculator registration</li>
     *   <li>World group loading and default skilltree extraction</li>
     *   <li>Repository initialization with fallback chain: MySQL/MongoDB → SQLite</li>
     *   <li>Pet shop configuration and bStats metrics setup</li>
     *   <li>Service activation through {@link Load.State#OnEnable}, {@link Load.State#AfterHooks},
     *       and {@link Load.State#OnReady}</li>
     *   <li>Loading pets for already-online players (handles server reloads)</li>
     * </ul>
     */
    public void onEnable() {
        this.isReady = false;

        miniMessage = MiniMessage.miniMessage();

        Updater updater = new Updater("MyPet");
        String updateStatus = updater.update();
        printSplashScreen(updateStatus);

        serviceManager.activate(Load.State.OnEnable);

        DebugLogHandler.setup(getLogger());

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
        LevelListener levelupListener = new LevelListener();
        getServer().getPluginManager().registerEvents(levelupListener, this);
        WorldListener worldListener = new WorldListener();
        getServer().getPluginManager().registerEvents(worldListener, this);
        RideInteractListener rideInteractListener = new RideInteractListener();
        getServer().getPluginManager().registerEvents(rideInteractListener, this);
        // Register CreakingHeartListener for 1.21.4+ (when Creaking Heart was added)
        if (MyPetApi.getCompatUtil().minecraftVersionEqualsOrAbove("1.21.4")) {
            CreakingHeartListener creakingHeartListener = new CreakingHeartListener();
            getServer().getPluginManager().registerEvents(creakingHeartListener, this);
        }

        // Paper Mob Goal API support listeners
        getServer().getPluginManager().registerEvents(new PetDamageTracker(), this);
        getServer().getPluginManager().registerEvents(new PetProjectileHitListener(), this);

        getServer().getPluginManager().registerEvents(new PetInteractionListener(), this);
        getServer().getPluginManager().registerEvents(new PetDamageListener(), this);
        getServer().getPluginManager().registerEvents(new PetEnvironmentListener(), this);
        getServer().getPluginManager().registerEvents(new PetInfoOnLeashListener(), this);
        getServer().getPluginManager().registerEvents(new PetSurvivalListener(), this);
        getServer().getPluginManager().registerEvents(new PetXpAttributionListener(), this);
        getServer().getPluginManager().registerEvents(new PetPvPListener(), this);
        getServer().getPluginManager().registerEvents(new PetSkillTriggerListener(), this);
        getServer().getPluginManager().registerEvents(new PetDeathListener(), this);
        getServer().getPluginManager().registerEvents(new PetDespawnListener(), this);

        RideSkillFlightController.start(this);

        PetPotionParticleController.start(this);

        PetSitParticleController.start(this);

        // Register commands via Paper's Brigadier Lifecycle API
        this.helpRegistry = new HelpRegistry();
        this.getLifecycleManager().registerEventHandler(io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents.COMMANDS, event -> {
            final io.papermc.paper.command.brigadier.Commands commands = event.registrar();
            registerBrigadierCommands(commands, helpRegistry);
        });

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
        if (Configuration.Repository.REPOSITORY_TYPE.equalsIgnoreCase("MySQL")) {
            repository = new MySqlRepository();
            try {
                repository.init();
                MyPetApi.getLogger().info("MySQL connection successful.");
            } catch (RepositoryInitException e) {
                ErrorUtil.reportSevere("MySQL database connection failed during initialization", e);
                repository = null;
            }
        } else if (Configuration.Repository.REPOSITORY_TYPE.equalsIgnoreCase("MongoDB")) {
            repository = new MongoDbRepository();
            try {
                repository.init();
                MyPetApi.getLogger().info("MongoDB connection successful.");
            } catch (RepositoryInitException e) {
                ErrorUtil.reportSevere("MongoDB database connection failed during initialization", e);
                repository = null;
            }
        }

        if (repository == null) {
            repository = new SqLiteRepository();
            try {
                repository.init();
                MyPetApi.getLogger().info("SQLite connection successful.");
            } catch (RepositoryInitException ignored) {
                MyPetApi.getLogger().warning("SQLite connection failed!");
                setEnabled(false);
                return;
            }
        }

        Converter.convert();

        if (repository instanceof Scheduler) {
            Timer.addTask((Scheduler) repository);
        }

        File shopConfig = new File(getDataFolder(), "pet-shops.yml");
        ShopConfigGenerator.generateIfMissing(shopConfig);
        new ShopManager();

        Timer.startTimer();

        updater.waitForDownload();

        pluginHookManager.enableHooks();
        serviceManager.activate(Load.State.AfterHooks);

        // init Metrics
        try {
            Metrics metrics = new Metrics(this, 778);
            if (metrics.isEnabled() && !MyPetVersion.isLocalBuild()) {
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
                metrics.addCustomChart(new Metrics.AdvancedPie("pet_types", () -> {
                    Map<String, Integer> petTypes = new HashMap<>();
                    for (MyPet pet : myPetManager.getAllActiveMyPets()) {
                        petTypes.merge(pet.getPetType().name(), 1, Integer::sum);
                    }
                    return petTypes;
                }
                ));
                metrics.addCustomChart(new Metrics.SimplePie("database_type", () -> {
                    String type = null;
                    if (Configuration.Repository.REPOSITORY_TYPE.equalsIgnoreCase("SQLite")) {
                        type = "SQLite";
                    } else if (Configuration.Repository.REPOSITORY_TYPE.equalsIgnoreCase("MySQL")) {
                        type = "MySQL";
                    } else if (Configuration.Repository.REPOSITORY_TYPE.equalsIgnoreCase("MongoDB")) {
                        type = "MongoDB";
                    }
                    return type;
                }));
                metrics.addCustomChart(new Metrics.AdvancedPie("active_skills", () -> {
                    Map<String, Integer> skillCounts = new HashMap<>();
                    for (MyPet pet : myPetManager.getAllActiveMyPets()) {
                        for (Skill skill : pet.getSkills().all()) {
                            if (skill.isActive()) {
                                skillCounts.merge(skill.getName(), 1, Integer::sum);
                            }
                        }
                    }
                    return skillCounts;
                }
                ));
            }
        } catch (Throwable e) {
            errorReporter.sendError(e, "Init Metrics failed");
        }

        this.isReady = true;

        ContributorCheck.startRefreshTask();

        serviceManager.activate(Load.State.OnReady);

        // load pets for online players
        new BukkitRunnable() {
            @Override
            public void run() {
                for (final Player player : getServer().getOnlinePlayers()) {
                    repository.getMyPetPlayer(player).thenAccept(p -> {
                        if (p == null) return;
                        Bukkit.getScheduler().runTask(MyPetApi.getPlugin(), () -> {
                            final MyPetPlayerImpl onlinePlayer = (MyPetPlayerImpl) p;

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

                                MyPetApi.getRepository().getMyPet(petUUID).thenAccept(storedMyPet -> {
                                    Bukkit.getScheduler().runTask(MyPetApi.getPlugin(), () -> {
                                        myPetManager.activateMyPet(storedMyPet);

                                            if (onlinePlayer.hasMyPet()) {
                                                final MyPet myPet = onlinePlayer.getMyPet();
                                                final MyPetPlayer myPetPlayer = myPet.getOwner();
                                                if (myPet.wantsToRespawn()) {
                                                    if (myPetPlayer.hasMyPet()) {
                                                        MyPet runMyPet = myPetPlayer.getMyPet();
                                                        switch (runMyPet.createEntity()) {
                                                            case Canceled:
                                                                runMyPet.getOwner().sendMessage(Translation.getFormattedComponent("Message.Spawn.Prevent", myPet.getOwner(), runMyPet.getDisplayName()));
                                                                break;
                                                            case NoSpace:
                                                                runMyPet.getOwner().sendMessage(Translation.getFormattedComponent("Message.Spawn.NoSpace", myPet.getOwner(), runMyPet.getDisplayName()));
                                                                break;
                                                            case NotAllowed:
                                                                runMyPet.getOwner().sendMessage(Translation.getFormattedComponent("Message.No.AllowedHere", myPet.getOwner(), myPet.getDisplayName()));
                                                                break;
                                                            case Dead:
                                                                if (Configuration.Respawn.DISABLE_AUTO_RESPAWN) {
                                                                    runMyPet.getOwner().sendMessage(Translation.getFormattedComponent("Message.Call.Dead", myPet.getOwner(), myPet.getDisplayName()));
                                                                } else {
                                                                    runMyPet.getOwner().sendMessage(Translation.getFormattedComponent("Message.Spawn.Respawn.In", myPet.getOwner(), myPet.getDisplayName(), myPet.getRespawnTime()));
                                                                }
                                                                break;
                                                            case Flying:
                                                                runMyPet.getOwner().sendMessage(Translation.getFormattedComponent("Message.Spawn.Flying", myPet.getOwner(), myPet.getDisplayName()));
                                                                break;
                                                            case Success:
                                                                runMyPet.getOwner().sendMessage(Translation.getFormattedComponent("Message.Command.Call.Success", myPet.getOwner(), runMyPet.getDisplayName()));
                                                                break;
                                                        }
                                                    }
                                                }
                                            }
                                        });
                                    });
                                }
                                onlinePlayer.checkForContribution();
                            });
                        });
                }
            }
        }.runTaskLater(this, 0);
    }

    /**
     * Registers core plugin services with the {@link ServiceManager}.
     *
     * <p>Services registered here are activated later during specific lifecycle states.
     * This includes leash flag management, experience caching and calculation, skill and
     * skilltree management, shop management, and version-specific services like the
     * Creaking entity service.</p>
     */
    private void registerServices() {
        serviceManager.registerService(LeashFlagManager.class);
        serviceManager.registerService(ExperienceCache.class);
        serviceManager.registerService(ExperienceCalculatorManager.class);
        serviceManager.registerService(SkillManager.class);
        serviceManager.registerService(SkilltreeManager.class);
        serviceManager.registerService(ShopManager.class);
        serviceManager.registerService(DefaultCreakingService.class);
    }

    /**
     * Registers all third-party plugin hook classes with the {@link PluginHookManager}.
     *
     * <p>Hooks are registered here during {@link #onLoad()} but not enabled until
     * {@link #onEnable()} calls {@link PluginHookManager#enableHooks()}. Each hook
     * checks at enable-time whether its target plugin is present and loaded.</p>
     */
    private void registerHooks() {
        pluginHookManager.registerHook(BattleArenaHook.class);
        pluginHookManager.registerHook(CitizensHook.class);
        pluginHookManager.registerHook(CombatLogXHook.class);
        pluginHookManager.registerHook(FabledSkyBlockHook.class);
        pluginHookManager.registerHook(FactionsUUIDHook.class);
        pluginHookManager.registerHook(GangsPlusHook.class);
        pluginHookManager.registerHook(GriefPreventionHook.class);
        pluginHookManager.registerHook(GuildsHook.class);
        pluginHookManager.registerHook(HeroesHook.class);
        pluginHookManager.registerHook(KingdomsHook.class);
        pluginHookManager.registerHook(LandsHook.class);
        pluginHookManager.registerHook(McMMOHook.class);
        pluginHookManager.registerHook(MiniaturePetsHook.class);
        pluginHookManager.registerHook(MobArenaHook.class);
        pluginHookManager.registerHook(MythicMobsHook.class);
        pluginHookManager.registerHook(NoCheatPlusHook.class);
        pluginHookManager.registerHook(PlaceholderApiHook.class);
        pluginHookManager.registerHook(PlotSquaredHook.class);
        pluginHookManager.registerHook(PremiumVanishHook.class);
        pluginHookManager.registerHook(ProtocolLibHook.class);
        pluginHookManager.registerHook(PvPArenaHook.class);
        pluginHookManager.registerHook(PvPManagerHook.class);
        pluginHookManager.registerHook(RedProtectHook.class);
        pluginHookManager.registerHook(ResidenceHook.class);
        pluginHookManager.registerHook(SimpleClansHook.class);
        pluginHookManager.registerHook(StackMobHook.class);
        pluginHookManager.registerHook(SuperVanishHook.class);
        pluginHookManager.registerHook(SurvivalGamesHook.class);
        pluginHookManager.registerHook(TownyHook.class);
        pluginHookManager.registerHook(UltimateSurvivalGamesHook.class);
        pluginHookManager.registerHook(VaultHook.class);
        pluginHookManager.registerHook(WorldGuardHook.class);
    }

    /**
     * Returns the plugin's JAR file.
     *
     * <p>Exposes the protected {@link JavaPlugin#getFile()} for use by the update system
     * and resource loading.</p>
     *
     * @return the plugin JAR {@link File}
     */
    @Override
    @NotNull
    public File getFile() {
        return super.getFile();
    }

    @SuppressWarnings("UnstableApiUsage")
    private void registerBrigadierCommands(io.papermc.paper.command.brigadier.Commands commands, HelpRegistry helpRegistry) {
        new CommandAdmin().register(commands, helpRegistry);
        new CommandCall().register(commands, helpRegistry);
        new CommandStop().register(commands, helpRegistry);
        new CommandSendAway().register(commands, helpRegistry);
        new CommandPickup().register(commands, helpRegistry);
        new CommandCaptureHelper().register(commands, helpRegistry);
        new CommandName().register(commands, helpRegistry);
        new CommandRelease().register(commands, helpRegistry);
        new CommandRespawn().register(commands, helpRegistry);
        new CommandBehavior().register(commands, helpRegistry);
        new CommandSettings().register(commands, helpRegistry);
        new CommandInfo().register(commands, helpRegistry);
        new CommandSkill().register(commands, helpRegistry);
        new CommandList().register(commands, helpRegistry);
        new CommandTrade().register(commands, helpRegistry);
        new CommandBeacon().register(commands, helpRegistry);
        new CommandChooseSkilltree().register(commands, helpRegistry);
        new CommandSwitch().register(commands, helpRegistry);
        new CommandStore().register(commands, helpRegistry);
        new CommandInventory().register(commands, helpRegistry);
        new CommandShop().register(commands, helpRegistry);
        new CommandMyPet().register(commands, helpRegistry);

        getLogger().info("Brigadier commands registered");
    }

    /**
     * Prints the MyPet ASCII art splash screen to the server console using MiniMessage formatting.
     *
     * <p>Displays the plugin version, year range, compatibility status, database type,
     * and an optional update status message.</p>
     *
     * @param updateStatus the update check result message, or {@code null} if no update info is available
     */
    private void printSplashScreen(@Nullable String updateStatus) {
        String version = MyPetVersion.getFormattedVersion();
        String dbType = Configuration.Repository.REPOSITORY_TYPE;

        String splash = String.join("\n",
                "",
                "<green>          ▄▄       </green>",
                "<green>    ▄██▄ ████      </green><green>  MyPet </green>" + version,
                "<green>    ████ ▀██▀      </green><green>  Created by Keyle | Maintained by UserDerezzed</green>",
                "<green>  ▄▄ ▀▀      ▄██▄  </green><green>  2011-" + Year.now() + "</green>",
                "<green> ████  ▄███▄ ▀██▀  </green>",
                "<green>  ▀▀ ▄███████▄     </green>" + (updateStatus != null ? "  " + updateStatus : ""),
                "<green>   ▄███████████▄   </green>",
                "<green>   ▀███▀▀▀▀▀███▀   </green>  Connecting to " + dbType + "...",
                "",
                "<green>Please consider supporting active development: https://ko-fi.com/userderezzed</green>"
        );

        Bukkit.getConsoleSender().sendMessage(MiniMessage.miniMessage().deserialize(splash));
    }
}