/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2026 Keyle
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
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.PetInfo;
import de.Keyle.MyPet.util.player.ContributorCheck;
import de.Keyle.MyPet.api.repository.*;
import de.Keyle.MyPet.api.skill.experience.ExperienceCalculatorManager;
import de.Keyle.MyPet.skill.skilltree.SkillTreeLoaderJSON;
import de.Keyle.MyPet.api.util.Scheduler;
import de.Keyle.MyPet.util.Timer;
import de.Keyle.MyPet.commands.help.HelpRegistry;
import de.Keyle.MyPet.api.util.hooks.HookHelper;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.util.translation.VanillaTranslationLoader;
import de.Keyle.MyPet.api.gui.GuiService;
import de.Keyle.MyPet.api.gui.MenuId;
import de.Keyle.MyPet.api.gui.MenuIds;
import de.Keyle.MyPet.api.util.service.Load;
import de.Keyle.MyPet.api.util.service.ServiceManager;
import de.Keyle.MyPet.dialog.DialogServiceImpl;
import de.Keyle.MyPet.gui.GuiServiceImpl;
import de.Keyle.MyPet.services.EggIconService;
import de.Keyle.MyPet.util.*;
import de.Keyle.MyPet.commands.BuiltInCommands;
import de.Keyle.MyPet.entity.info.PetInfoImpl;
import de.Keyle.MyPet.entity.leashing.BuiltInLeashFlags;
import de.Keyle.MyPet.listeners.PetListeners;
import de.Keyle.MyPet.migration.MigrationService;
import de.Keyle.MyPet.repository.Converter;
import de.Keyle.MyPet.repository.OnlinePlayerPetLoader;
import de.Keyle.MyPet.repository.Repository;
import de.Keyle.MyPet.repository.RepositoryFactory;
import de.Keyle.MyPet.services.BuiltInServices;
import de.Keyle.MyPet.skill.experience.JavaScriptExperienceCalculator;
import de.Keyle.MyPet.entity.types.BuiltInPetTypes;
import de.Keyle.MyPet.skill.skills.BuiltInSkillStateCodecs;
import de.Keyle.MyPet.skill.skills.BuiltInSkills;
import de.Keyle.MyPet.skill.upgrades.BuiltInUpgradeParsers;
import de.Keyle.MyPet.skill.skilltree.requirements.BuiltInRequirements;
import de.Keyle.MyPet.util.sentry.SentryErrorReporter;
import de.Keyle.MyPet.util.shop.ShopConfigGenerator;
import de.Keyle.MyPet.util.shop.ShopManager;
import de.Keyle.MyPet.util.sound.PetSoundService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
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
    private PetInfo petInfo;

    /** Online-player registry. Populated in {@link #onLoad()}. */
    @Getter
    private PlayerManager playerManager;

    /** Active and stored pet registry. Populated in {@link #onLoad()}. */
    @Getter
    private PetManager petManager;

    /** Helper API for third-party plugin hooks. Populated in {@link #onLoad()}. */
    @Getter
    private HookHelper hookHelper;

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

        // Interrupt any in-flight vanilla translation download so it cannot mutate the
        // JVM-singleton GlobalTranslator after this plugin instance is gone.
        VanillaTranslationLoader.cancelLoad();

        if (isReady) {
            for (Pet pet : petManager.getAllActivePets()) {
                if (pet.getStatus() == Pet.PetState.Here) {
                    pet.removePet(true);
                }
            }
            repository.disable();
            Timer.reset();
        }
        Bukkit.getServer().getGlobalRegionScheduler().cancelTasks(this);
        Bukkit.getServer().getAsyncScheduler().cancelTasks(this);

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
     *   <li>Construct {@link ServiceManager}</li>
     *   <li>Populate every cached service-handle field except {@code repository},
     *       {@code miniMessage}, and {@code helpRegistry} (those wait for {@link #onEnable()})</li>
     *   <li>Register all built-in services and the {@code OnLoad}-state slice activates</li>
     * </ol>
     *
     * <p>This phase does not interact with the world or other plugins and so cannot fail
     * fatally — there is no early-return path.</p>
     */
    public void onLoad() {
        MyPetApi.setPlugin(this);
        BuiltInPetTypes.register();
        PetPermissions.registerAll();
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

        petInfo = new PetInfoImpl();
        petManager = new de.Keyle.MyPet.repository.PetManager();
        playerManager = new de.Keyle.MyPet.repository.PlayerManager();
        hookHelper = new de.Keyle.MyPet.util.HookHelper();

        BuiltInServices.register(serviceManager);
        serviceManager.registerService(EggIconService.class);
        serviceManager.registerService(GuiServiceImpl.class);
        serviceManager.registerService(DialogServiceImpl.class);
        serviceManager.registerService(PetSoundService.class);
        serviceManager.activate(Load.State.OnLoad);
    }

    /**
     * Main initialization phase, run by Bukkit once all plugins have completed
     * {@link #onLoad()}.
     *
     * <p>initialization proceeds in roughly this order, with each phase building on the
     * previous:</p>
     * <ol>
     *   <li>Splash screen, compat-config layer</li>
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

        // Register the 8 built-in GUI menus and load their bundled JSON
        GuiService gui = MyPetApi.getGuiService();
        gui.registerMenu(
            (MenuId<de.Keyle.MyPet.gui.context.PetSelectionContext>) (MenuId<?>) MenuIds.PET_SELECTION,
            new de.Keyle.MyPet.gui.menus.PetSelectionMenuHandler(),
            () -> getClass().getResourceAsStream("/gui/menus/pet-selection.json"));
        gui.registerMenu(
            (MenuId<de.Keyle.MyPet.gui.context.PetAdminSelectionContext>) (MenuId<?>) MenuIds.PET_ADMIN_SELECTION,
            new de.Keyle.MyPet.gui.menus.PetAdminSelectionMenuHandler(),
            () -> getClass().getResourceAsStream("/gui/menus/pet-admin-selection.json"));
        gui.registerMenu(
            (MenuId<de.Keyle.MyPet.gui.context.PetShopSelectionContext>) (MenuId<?>) MenuIds.PET_SHOP_SELECTION,
            new de.Keyle.MyPet.gui.menus.PetShopSelectionMenuHandler(),
            () -> getClass().getResourceAsStream("/gui/menus/pet-shop-selection.json"));
        gui.registerMenu(
            (MenuId<de.Keyle.MyPet.gui.context.PetShopContext>) (MenuId<?>) MenuIds.PET_SHOP,
            new de.Keyle.MyPet.gui.menus.PetShopMenuHandler(),
            () -> getClass().getResourceAsStream("/gui/menus/pet-shop.json"));
        gui.registerMenu(
            (MenuId<de.Keyle.MyPet.gui.context.PetShopConfirmContext>) (MenuId<?>) MenuIds.PET_SHOP_CONFIRM,
            new de.Keyle.MyPet.gui.menus.PetShopConfirmMenuHandler(),
            () -> getClass().getResourceAsStream("/gui/menus/pet-shop-confirm.json"));
        gui.registerMenu(
            (MenuId<de.Keyle.MyPet.gui.context.ChooseSkilltreeContext>) (MenuId<?>) MenuIds.CHOOSE_SKILLTREE,
            new de.Keyle.MyPet.gui.menus.ChooseSkilltreeMenuHandler(),
            () -> getClass().getResourceAsStream("/gui/menus/choose-skilltree.json"));
        gui.registerMenu(
            (MenuId<de.Keyle.MyPet.gui.context.BeaconContext>) (MenuId<?>) MenuIds.BEACON,
            new de.Keyle.MyPet.gui.menus.BeaconMenuHandler(),
            () -> getClass().getResourceAsStream("/gui/menus/beacon.json"));
        gui.registerMenu(
            (MenuId<de.Keyle.MyPet.gui.context.BackpackContext>) (MenuId<?>) MenuIds.BACKPACK,
            new de.Keyle.MyPet.gui.menus.BackpackMenuHandler(),
            () -> getClass().getResourceAsStream("/gui/menus/backpack.json"));
        gui.registerMenu(
            (MenuId<de.Keyle.MyPet.gui.context.NpcStorageConfirmContext>) (MenuId<?>) MenuIds.NPC_STORAGE_CONFIRM,
            new de.Keyle.MyPet.gui.menus.NpcStorageConfirmMenuHandler(),
            () -> getClass().getResourceAsStream("/gui/menus/npc-storage-confirm.json"));
        gui.registerMenu(
            (MenuId<de.Keyle.MyPet.gui.context.PetReleaseConfirmContext>) (MenuId<?>) MenuIds.PET_RELEASE_CONFIRM,
            new de.Keyle.MyPet.gui.menus.PetReleaseConfirmMenuHandler(),
            () -> getClass().getResourceAsStream("/gui/menus/pet-release-confirm.json"));
        gui.registerMenu(
            (MenuId<de.Keyle.MyPet.gui.context.PetMenuContext>) (MenuId<?>) MenuIds.PET_MENU,
            new de.Keyle.MyPet.gui.menus.PetMenuMenuHandler(),
            () -> getClass().getResourceAsStream("/gui/menus/pet-menu.json"));
        gui.registerMenu(
            (MenuId<de.Keyle.MyPet.gui.context.PetTradeTargetContext>) (MenuId<?>) MenuIds.PET_TRADE_TARGET,
            new de.Keyle.MyPet.gui.menus.PetTradeTargetMenuHandler(),
            () -> getClass().getResourceAsStream("/gui/menus/pet-trade-target.json"));
        gui.registerMenu(
            (MenuId<de.Keyle.MyPet.gui.context.PetTradeConfirmContext>) (MenuId<?>) MenuIds.PET_TRADE_CONFIRM,
            new de.Keyle.MyPet.gui.menus.PetTradeConfirmMenuHandler(),
            () -> getClass().getResourceAsStream("/gui/menus/pet-trade-confirm.json"));
        gui.registerMenu(
            (MenuId<de.Keyle.MyPet.gui.context.PetVolumeContext>) (MenuId<?>) MenuIds.PET_VOLUME,
            new de.Keyle.MyPet.gui.menus.PetVolumeMenuHandler(),
            () -> getClass().getResourceAsStream("/gui/menus/pet-volume.json"));

        // Initialize the DialogService once it's been OnEnable-activated.
        ((DialogServiceImpl) MyPetApi.getDialogService()).init(this);

        // Load the bundled+overlay JSON for all registered menus.
        ((GuiServiceImpl) gui).reload();

        VanillaTranslationLoader.loadAsync(this);

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
        BuiltInSkillStateCodecs.register(MyPetApi.getSkillManager());

        // create folders
        File skilltreeFolder = new File(getDataFolder().getPath(), "skilltrees");
        getDataFolder().mkdirs();
        boolean createdSkilltreeFolder = skilltreeFolder.mkdirs();
        boolean createdLocaleFolder = new File(getDataFolder(), "locale").mkdirs();

        DefaultSkilltreeProvisioner.copyDefaultsIfFolderCreated(skilltreeFolder, createdSkilltreeFolder, this);

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

        serviceManager.activate(Load.State.Hooks);

        MyPetMetrics.register(this, petManager, errorReporter);

        this.isReady = true;

        ContributorCheck.startRefreshTask();

        serviceManager.activate(Load.State.OnReady);

        registerDeferredStartup();
    }

    /**
     * Defers skilltree loading and online-player pet restoration until {@link ServerLoadEvent}
     * fires — which Paper guarantees is after every plugin's {@code onEnable} has completed.
     * Required so that third-party plugins using {@code softdepend: [MyPet]} can register
     * custom skills and {@code UpgradeParser}s in their own {@code onEnable}, before MyPet
     * resolves skill names while parsing {@code .st.json} skilltree files.
     *
     * <p>Pet restoration is bundled into the same hop because pets reference skilltrees;
     * deferring skilltrees without deferring restoration would leave restored pets with
     * unresolved skilltree references on {@code /reload}. Cold-start servers have no
     * online players, so restoration is a no-op in that path.</p>
     */
    private void registerDeferredStartup() {
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onServerLoad(ServerLoadEvent event) {
                MyPetApi.getSkilltreeManager().clearSkilltrees();
                SkillTreeLoaderJSON.loadSkilltrees(new File(getDataFolder(), "skilltrees"));
                OnlinePlayerPetLoader.restoreForOnlinePlayers(MyPetPlugin.this, repository, petManager, playerManager);
                HandlerList.unregisterAll(this);
            }
        }, this);
    }

    @Override
    @NotNull
    public File getFile() {
        return super.getFile();
    }
}
