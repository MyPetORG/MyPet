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
import de.Keyle.MyPet.util.player.ContributorCheck;
import de.Keyle.MyPet.api.repository.*;
import de.Keyle.MyPet.api.skill.experience.ExperienceCalculatorManager;
import de.Keyle.MyPet.skill.skilltree.SkillTreeLoaderJSON;
import de.Keyle.MyPet.api.util.Scheduler;
import de.Keyle.MyPet.util.Timer;
import de.Keyle.MyPet.commands.help.HelpRegistry;
import de.Keyle.MyPet.api.util.hooks.HookHelper;
import de.Keyle.MyPet.api.util.hooks.PluginHookManager;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.util.logger.DebugLogHandler;
import de.Keyle.MyPet.api.util.service.Load;
import de.Keyle.MyPet.api.util.service.ServiceManager;
import de.Keyle.MyPet.services.EggIconService;
import de.Keyle.MyPet.util.*;
import de.Keyle.MyPet.commands.BuiltInCommands;
import de.Keyle.MyPet.entity.info.MyPetInfoImpl;
import de.Keyle.MyPet.entity.leashing.BuiltInLeashFlags;
import de.Keyle.MyPet.listeners.PetListeners;
import de.Keyle.MyPet.migration.MigrationService;
import de.Keyle.MyPet.repository.Converter;
import de.Keyle.MyPet.repository.OnlinePlayerPetLoader;
import de.Keyle.MyPet.repository.Repository;
import de.Keyle.MyPet.repository.RepositoryFactory;
import de.Keyle.MyPet.services.BuiltInServices;
import de.Keyle.MyPet.skill.experience.JavaScriptExperienceCalculator;
import de.Keyle.MyPet.skill.skills.BuiltInSkills;
import de.Keyle.MyPet.skill.upgrades.BuiltInUpgradeParsers;
import de.Keyle.MyPet.skill.skilltree.requirements.BuiltInRequirements;
import de.Keyle.MyPet.util.hooks.BuiltInHooks;
import de.Keyle.MyPet.util.sentry.SentryErrorReporter;
import de.Keyle.MyPet.util.shop.ShopConfigGenerator;
import de.Keyle.MyPet.util.shop.ShopManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Optional;
import java.util.UUID;


/**
 * Main plugin class for MyPet — the entry point Paper loads.
 *
 * <p>Responsibilities are deliberately limited to:</p>
 * <ul>
 *   <li>Holding plugin-instance state (cached service handles, ready/disabling flags)</li>
 *   <li>Driving the three lifecycle hooks: {@link #onLoad()}, {@link #onEnable()},
 *       {@link #onDisable()}</li>
 *   <li>Delegating registration of heavy lists (skills, hooks, listeners, commands, services,
 *       leash flags, requirements) to dedicated {@code BuiltIn*}/registrar classes</li>
 * </ul>
 *
 * <h3>Lifecycle invariants</h3>
 * <ul>
 *   <li>{@link #onLoad()} runs once, before any plugin is enabled. It populates every
 *       service-handle field except {@code repository}, {@code miniMessage}, and
 *       {@code helpRegistry}.</li>
 *   <li>{@link #onEnable()} runs once, after {@code onLoad()} has completed for every plugin.
 *       It may early-return via {@link #setEnabled(boolean) setEnabled(false)} if the
 *       repository fails to initialize or a database migration fails. {@link #isReady} is set
 *       to {@code true} only after every prerequisite phase has completed successfully.</li>
 *   <li>{@link #onDisable()} runs once at shutdown. It tolerates partial initialization:
 *       cleanup steps that depend on a successful enable are gated on {@link #isReady}; hook
 *       and service shutdown are guarded against null handles. Marker {@link #isDisabling}
 *       flips to {@code true} at the start of the method so listeners observing teardown can
 *       short-circuit.</li>
 * </ul>
 *
 * @see MyPetApi
 */
@SuppressWarnings("unused")
public final class MyPetPlugin extends JavaPlugin implements de.Keyle.MyPet.api.plugin.MyPetPlugin {

    public static MyPetPlugin getInstance() {
        return JavaPlugin.getPlugin(MyPetPlugin.class);
    }

    /**
     * Becomes {@code true} once {@link #onEnable()} has completed every prerequisite phase
     * (repository up, migrations passed, hooks enabled, services ready). Cleanup logic in
     * {@link #onDisable()} that depends on a successful enable is gated on this flag.
     */
    private boolean isReady = false;

    /**
     * Set to {@code true} at the start of {@link #onDisable()} so listeners observing
     * teardown (e.g. world-save handlers) can short-circuit and avoid mutating state that is
     * about to be torn down.
     */
    @Getter
    private boolean isDisabling = false;

    /**
     * Active persistence backend, populated in {@link #onEnable()} via
     * {@link RepositoryFactory#initWithFallback()}. Reading before that point yields
     * {@code null}; if both backends fail to initialize, the plugin disables itself before
     * any caller has a chance to observe the field.
     */
    @Getter
    private Repository repository;

    /** Pet-type metadata provider. Populated in {@link #onLoad()}. */
    @Getter
    private MyPetInfo myPetInfo;

    /** Online-player registry. Populated in {@link #onLoad()}. */
    @Getter
    private PlayerManager playerManager;

    /** Active and stored pet registry. Populated in {@link #onLoad()}. */
    @Getter
    private MyPetManager myPetManager;

    /** Helper API for third-party plugin hooks. Populated in {@link #onLoad()}. */
    @Getter
    private HookHelper hookHelper;

    /**
     * Hook lifecycle manager. Hooks are registered into it during {@link #onLoad()}; they
     * remain dormant until {@link PluginHookManager#enableHooks()} is invoked from
     * {@link #onEnable()}, after the repository is up.
     */
    @Getter
    private PluginHookManager pluginHookManager;

    /**
     * Central service registry. Populated in {@link #onLoad()}; services activate in phases
     * via {@link ServiceManager#activate(Load.State)} as {@link #onEnable()} progresses.
     */
    @Getter
    private ServiceManager serviceManager;

    /** Shared MiniMessage instance for Adventure text deserialization. Populated in {@link #onEnable()}. */
    @Getter
    private MiniMessage miniMessage;

    /** Help-entry registry populated by command handlers. Populated in {@link #onEnable()}. */
    @Getter
    private HelpRegistry helpRegistry;

    /**
     * Sentry error reporter. Constructed unconditionally in {@link #onLoad()}; only enables
     * its uplink when {@code MyPet.Log.Report-Errors} is true in config. Always non-null
     * after {@link #onLoad()} completes.
     */
    @Getter
    private SentryErrorReporter errorReporter = null;

    /**
     * Tears down the plugin. Tolerates partial initialization: pet despawn, repository
     * shutdown, and {@link Timer#reset()} run only when {@link #isReady} is {@code true};
     * hook and service shutdown are guarded against null handles, since {@link #onLoad()}
     * may have failed before they were constructed.
     *
     * <p>Folia scheduler tasks are always canceled — both the global region scheduler and
     * the async scheduler — so any in-flight async work tied to this plugin is severed
     * before the JAR is unloaded.</p>
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
        Bukkit.getServer().getGlobalRegionScheduler().cancelTasks(this);
        Bukkit.getServer().getAsyncScheduler().cancelTasks(this);

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
     * Foundational initialization phase, run by Bukkit before any plugin is enabled.
     *
     * <p>Order of operations:</p>
     * <ol>
     *   <li>Publish this instance via {@link MyPetApi#setPlugin(de.Keyle.MyPet.api.plugin.MyPetPlugin)}
     *       so static accessors resolve correctly for the rest of {@code onLoad}</li>
     *   <li>Construct {@link SentryErrorReporter}, optionally enabling its uplink based on
     *       {@code MyPet.Log.Report-Errors} in config</li>
     *   <li>Load configuration</li>
     *   <li>Construct {@link ServiceManager} and {@link PluginHookManager}</li>
     *   <li>Populate every cached service-handle field except {@code repository},
     *       {@code miniMessage}, and {@code helpRegistry} (those wait for {@link #onEnable()})</li>
     *   <li>Register all built-in services and the {@code OnLoad}-state slice activates</li>
     *   <li>Register all third-party plugin hooks (they remain dormant until enable)</li>
     * </ol>
     *
     * <p>This phase does not interact with the world or other plugins and so cannot fail
     * fatally — there is no early-return path.</p>
     */
    public void onLoad() {
        MyPetApi.setPlugin(this);
        getDataFolder().mkdirs();

        VersionUtil.reset();

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

        ConfigurationLoader.upgradeConfig();
        ConfigurationLoader.setDefault();
        ConfigurationLoader.loadConfiguration();

        serviceManager = new ServiceManager();
        pluginHookManager = new PluginHookManager();

        myPetInfo = new MyPetInfoImpl();
        myPetManager = new de.Keyle.MyPet.repository.MyPetManager();
        playerManager = new de.Keyle.MyPet.repository.PlayerManager();
        hookHelper = new de.Keyle.MyPet.util.HookHelper();

        BuiltInServices.register(serviceManager);
        serviceManager.registerService(EggIconService.class);
        serviceManager.activate(Load.State.OnLoad);

        BuiltInHooks.register(pluginHookManager);
    }

    /**
     * Main initialization phase, run by Bukkit once all plugins have completed
     * {@link #onLoad()}.
     *
     * <p>initialization proceeds in roughly this order, with each phase building on the
     * previous:</p>
     * <ol>
     *   <li>Splash screen, debug logger, compat-config layer</li>
     *   <li>Leash flags, skilltree requirements, JS experience calculator</li>
     *   <li>Bukkit listeners ({@link PetListeners}), Brigadier commands ({@link BuiltInCommands})</li>
     *   <li>World groups, built-in skills, default skilltree files, storage permissions</li>
     *   <li>Repository init via {@link RepositoryFactory#initWithFallback()} — early-exits via
     *       {@link #setEnabled(boolean) setEnabled(false)} if both backends fail</li>
     *   <li>Database migrations — early-exits if any migration reports failure</li>
     *   <li>Shop config, scheduler timer, updater wait, third-party hook activation</li>
     *   <li>{@link MyPetMetrics} bStats setup, {@link ContributorCheck}</li>
     *   <li>{@link Load.State#OnReady} service activation, then async restoration of pets for
     *       players already online (typical {@code /reload} case) via {@link OnlinePlayerPetLoader}</li>
     * </ol>
     *
     * <p>{@link #isReady} flips to {@code true} only after every prerequisite phase has
     * completed successfully and just before the {@code OnReady} state is activated.</p>
     */
    public void onEnable() {
        this.isReady = false;

        miniMessage = MiniMessage.miniMessage();

        Updater updater = new Updater("MyPet");
        String updateStatus = updater.update();
        SplashScreen.print(updateStatus, Configuration.Repository.REPOSITORY_TYPE);

        serviceManager.activate(Load.State.OnEnable);

        DebugLogHandler.setup(getLogger());

        ConfigurationLoader.loadCompatConfiguration();

        BuiltInLeashFlags.register();
        BuiltInRequirements.register();

        if (!new File(getDataFolder(), "exp.js").exists()) {
            ResourceUtil.copyResource(this, "exp.js", new File(getDataFolder(), "exp.js"));
        }
        serviceManager.getService(ExperienceCalculatorManager.class).ifPresent(calculatorManager -> {
            calculatorManager.registerCalculator("JS", JavaScriptExperienceCalculator.class);
            calculatorManager.registerCalculator("JavaScript", JavaScriptExperienceCalculator.class);
            calculatorManager.switchCalculator(Configuration.LevelSystem.CALCULATION_MODE.toLowerCase());
        });

        PetListeners.registerAll(this);

        this.helpRegistry = new HelpRegistry();
        this.getLifecycleManager().registerEventHandler(io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents.COMMANDS,
                event -> BuiltInCommands.register(event.registrar(), helpRegistry));

        WorldGroup.loadGroups(new File(getDataFolder().getPath(), "worldgroups.yml"));

        BuiltInSkills.register();
        BuiltInUpgradeParsers.register(MyPetApi.getSkillManager());

        // create folders
        File skilltreeFolder = new File(getDataFolder().getPath(), "skilltrees");
        getDataFolder().mkdirs();
        boolean createdSkilltreeFolder = skilltreeFolder.mkdirs();
        boolean createdLocaleFolder = new File(getDataFolder(), "locale").mkdirs();
        new File(getDataFolder(), "logs").mkdirs();

        DefaultSkilltreeProvisioner.copyDefaultsIfFolderCreated(skilltreeFolder, createdSkilltreeFolder, this);

        MyPetApi.getSkilltreeManager().clearSkilltrees();
        SkillTreeLoaderJSON.loadSkilltrees(new File(getDataFolder(), "skilltrees"));

        for (int i = 0; i <= Configuration.Misc.MAX_STORED_PET_COUNT; i++) {
            try {
                Bukkit.getPluginManager().addPermission(new Permission("MyPet.petstorage.limit." + i));
            } catch (Exception ignored) {
            }
        }

        if (createdLocaleFolder) {
            ResourceUtil.copyResource(this, "locale-readme.txt", new File(getDataFolder(), "locale" + File.separator + "readme.txt"));
        }
        Locale.init();

        Optional<Repository> repositoryOpt = RepositoryFactory.initWithFallback();
        if (repositoryOpt.isEmpty()) {
            setEnabled(false);
            return;
        }
        repository = repositoryOpt.get();

        Converter.convert();

        if (repository instanceof Scheduler) {
            Timer.addTask((Scheduler) repository);
        }

        // Run migrations synchronously — any failure disables the plugin before hooks/pets load.
        serviceManager.activate(Load.State.Migration);
        Optional<MigrationService> migrationServiceOpt = serviceManager.getService(MigrationService.class);
        if (migrationServiceOpt.isPresent() && !migrationServiceOpt.get().wasSuccessful()) {
            MyPetApi.getLogger().severe("Migration failed — disabling plugin.");
            setEnabled(false);
            return;
        }

        ShopConfigGenerator.generateIfMissing(new File(getDataFolder(), "pet-shops.yml"));
        new ShopManager();

        Timer.startTimer();

        updater.waitForDownload();

        pluginHookManager.enableHooks();
        serviceManager.activate(Load.State.AfterHooks);

        MyPetMetrics.register(this, myPetManager, errorReporter);

        this.isReady = true;

        ContributorCheck.startRefreshTask();

        serviceManager.activate(Load.State.OnReady);

        OnlinePlayerPetLoader.restoreForOnlinePlayers(this, repository, myPetManager, playerManager);
    }

    @Override
    @NotNull
    public File getFile() {
        return super.getFile();
    }
}
