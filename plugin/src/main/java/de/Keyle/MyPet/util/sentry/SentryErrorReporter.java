/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
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

package de.Keyle.MyPet.util.sentry;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.MyPetVersion;
import de.Keyle.MyPet.api.util.ErrorReporter;
import de.Keyle.MyPet.api.util.hooks.PluginHookName;
import io.sentry.Breadcrumb;
import io.sentry.Hint;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import io.sentry.SentryOptions;
import io.sentry.protocol.User;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class SentryErrorReporter implements ErrorReporter {

    /**
     * Marks an event as captured by this reporter. The Sentry SDK keeps one global hub per JVM, so
     * anything else on the server that calls into {@code Sentry} (Pufferfish's log appender, another
     * plugin that bundles the SDK) lands in the same hub and would otherwise be sent to MyPet's
     * project. {@link #onEnable} drops every event that does not carry this mark.
     */
    private static final String HINT_MYPET_CAPTURE = "mypet:capture";

    @Getter @Setter private static UUID serverUUID = UUID.randomUUID();

    protected Appender loggerAppender = null;
    // sendError runs on whichever thread logged or caught the error, so the flags it reads are volatile.
    protected volatile boolean enabled = false;
    protected volatile boolean hooksLoaded = false;

    /**
     * The options this reporter installed into the global hub. If another SDK user re-initialises
     * Sentry afterwards, the hub's options are no longer these, and MyPet must stop capturing
     * rather than file its errors against someone else's DSN.
     */
    private volatile SentryOptions ownOptions = null;
    private final AtomicBoolean hubTakeoverLogged = new AtomicBoolean(false);

    public void onEnable() {
        // Don't report errors from self-compiled builds. Only CI builds set a non-"local"
        // Project-Type in the manifest (release via -PbuildType=release, snapshot via -PbuildType=dev),
        // so a plain `./gradlew build` stays "local" and never initializes Sentry.
        if (MyPetVersion.isLocalBuild()) {
            return;
        }

        // The DSN is injected at build time from a CI-only secret (see build.gradle.kts) rather than
        // hardcoded, so forks that build via our release/snapshot workflows - but don't have our
        // private SENTRY_DSN secret - get an empty DSN here and never report errors to our project.
        String dsn = MyPetVersion.getSentryDsn();
        if (dsn == null || dsn.isEmpty()) {
            return;
        }

        // A release jar repackaged with extra classes still carries the DSN. Its errors are the
        // fork's to look at, not ours: stay silent instead of reporting them upstream.
        if (!BuildIntegrity.isUnmodified(MyPetApi.getPlugin().getFile())) {
            MyPetApi.getLogger().info("This MyPet jar was modified after it was built, error reporting stays off.");
            return;
        }

        // Initialize Sentry with modern 8.0.0+ API
        Sentry.init(options -> {
            ownOptions = options;
            options.setDsn(dsn);
            options.setSendDefaultPii(true);
            options.setTracesSampleRate(1.0);
            options.setDebug(false);

            // Set release and environment
            options.setRelease(MyPetVersion.getFormattedVersion());
            options.setEnvironment(MyPetVersion.isDevBuild() ? "development" : "production");

            // Set server name (appears as server_name in Sentry)
            options.setServerName(Bukkit.getServer().getVersion());

            // No addIgnoredExceptionForType here: the SDK matches those by exact class and only on
            // the outermost exception, so a NoClassDefFoundError walked past an entry for
            // LinkageError and an OutOfMemoryError inside a CommandException was sent. ErrorFilter
            // checks the whole type family across the cause chain in beforeSend instead.

            // Filter noisy breadcrumbs
            options.setBeforeBreadcrumb((breadcrumb, hint) -> {
                String category = breadcrumb.getCategory();
                if (category == null) {
                    return breadcrumb;
                }

                // Filter out noisy scheduler/tick-related logs
                if (category.contains("Scheduler") ||
                        category.contains("ChunkIO") ||
                        category.contains("tick")) {
                    return null;
                }

                return breadcrumb;
            });

            // MyPet starts no transactions, so any that reach this hub came from another SDK
            // user on the same JVM.
            options.setBeforeSendTransaction((transaction, hint) -> null);

            // beforeSend hook - filter and enrich events before sending
            options.setBeforeSend((event, hint) -> {
                // Only what this reporter captured; see HINT_MYPET_CAPTURE.
                if (hint.get(HINT_MYPET_CAPTURE) == null) {
                    return null;
                }

                Throwable throwable = event.getThrowable();
                if (throwable == null) {
                    return null;
                }

                // MyPet's own code has to be what threw, not a plugin that called into MyPet
                if (!ErrorFilter.isMyPetError(throwable)) {
                    return null;
                }

                // Errors MyPet cannot act on: link failures, host file systems, hot-swapped jars
                if (ErrorFilter.isIgnored(throwable)) {
                    return null;
                }

                // Lazy-load plugin hooks on first error
                if (!hooksLoaded) {
                    addPluginHooks();
                }

                // Add repository type tag (searchable)
                String repoType = Configuration.Repository.REPOSITORY_TYPE;
                if (repoType != null && !repoType.isEmpty()) {
                    event.setTag("repository_type", repoType);
                }

                // Add runtime context (viewable on issue page)
                Map<String, Object> runtimeContext = new HashMap<>();
                try {
                    if (MyPetApi.getMyPetManager() != null) {
                        runtimeContext.put("active_pets", MyPetApi.getMyPetManager().countActiveMyPets());
                    }
                } catch (Exception ignored) {
                    // Manager may not be initialized yet
                }
                if (!runtimeContext.isEmpty()) {
                    event.getContexts().put("runtime", runtimeContext);
                }

                return event;
            });
        });

        // Set user and tags
        Sentry.configureScope(scope -> {
            // User (server UUID)
            User user = new User();
            user.setId(serverUUID.toString());
            scope.setUser(user);

            // Plugin build tag
            String build = MyPetVersion.getBuild();
            if (build != null && !build.isEmpty()) {
                scope.setTag("plugin_build", build);
            }

            // Minecraft version tag
            String mcVersion = MyPetVersion.getMinecraftVersion();
            if (mcVersion != null && !mcVersion.isEmpty() && !mcVersion.equals("0.0.0")) {
                scope.setTag("minecraft_version", mcVersion);
            }

            // Server type tag (Paper, Spigot, etc.)
            scope.setTag("server_type", detectServerType());

            // Java version tag
            scope.setTag("java_version", System.getProperty("java.version"));
        });

        // Add plugins list to context
        addPlugins();

        // Attach Log4j2 appender for automatic error capture
        loggerAppender = new MyPetExceptionAppender();
        loggerAppender.start();
        Logger logger = (Logger) LogManager.getRootLogger();
        logger.addAppender(loggerAppender);

        // Start session for release health tracking
        Sentry.startSession();

        enabled = true;
    }

    /**
     * Detects the server type (Paper, Spigot, CraftBukkit, etc.)
     */
    protected String detectServerType() {
        String version = Bukkit.getServer().getVersion().toLowerCase();
        String name = Bukkit.getServer().getName();

        // Check for forks first (most specific)
        if (version.contains("purpur")) {
            return "Purpur";
        } else if (version.contains("pufferfish")) {
            return "Pufferfish";
        } else if (version.contains("paper") || classExists("io.papermc.paper.configuration.Configuration")) {
            return "Paper";
        } else if (version.contains("spigot") || classExists("org.spigotmc.SpigotConfig")) {
            return "Spigot";
        } else if (name.contains("CraftBukkit")) {
            return "CraftBukkit";
        }

        return name;
    }

    /**
     * Checks if a class exists on the classpath.
     */
    private boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Whether the global hub still runs on the options this reporter installed. Another plugin or
     * the server itself may call {@code Sentry.init} later and replace them; from then on anything
     * MyPet captured would go to that DSN under that project's rules.
     */
    private boolean ownsGlobalHub() {
        SentryOptions options = ownOptions;
        if (options == null) {
            // Not initialised, or already shut down: nothing to own and nothing to say.
            return false;
        }
        if (Sentry.getCurrentScopes().getOptions() == options) {
            return true;
        }
        if (hubTakeoverLogged.compareAndSet(false, true)) {
            MyPetApi.getLogger().info("Another plugin re-initialised the Sentry SDK, MyPet error reporting is off for this session.");
        }
        return false;
    }

    protected void addPlugins() {
        String plugins = Arrays
                .stream(Bukkit.getPluginManager().getPlugins())
                .map(plugin -> plugin.getName() + " (" + plugin.getDescription().getVersion() + ")")
                .collect(Collectors.joining("\n"));

        Sentry.configureScope(scope -> scope.setExtra("plugins", plugins));
    }

    protected void addPluginHooks() {
        if (MyPetApi.getPluginHookManager() == null || MyPetApi.getPluginHookManager().getHooks() == null) {
            return;
        }
        String hooks = MyPetApi.getPluginHookManager().getHooks().stream()
                .map(hook -> {
                    PluginHookName hookNameAnnotation = hook.getClass().getAnnotation(PluginHookName.class);
                    String message = hook.getPluginName();
                    message += " (" + Bukkit.getPluginManager().getPlugin(hook.getPluginName()).getDescription().getVersion() + ")";
                    if (!hookNameAnnotation.classPath().equalsIgnoreCase("")) {
                        message += " (" + hookNameAnnotation.classPath() + ")";
                    }
                    message += hook.getActivationMessage();
                    return message;
                })
                .collect(Collectors.joining("\n"));

        Sentry.configureScope(scope -> scope.setExtra("hooks", hooks));

        hooksLoaded = true;
    }

    public void onDisable() {
        if (enabled && loggerAppender != null) {
            Logger logger = (Logger) LogManager.getRootLogger();
            loggerAppender.stop();
            logger.removeAppender(loggerAppender);
        }
        boolean wasEnabled = enabled;
        enabled = false;

        // Only tear down a hub that is still ours; closing someone else's would end their reporting.
        if (wasEnabled && ownsGlobalHub()) {
            // End session for release health tracking
            Sentry.endSession();

            // Close Sentry connection and flush pending events
            Sentry.close();
        }
        ownOptions = null;
    }

    public void sendError(Throwable t, String... breadcrumbs) {
        if (!enabled || !ownsGlobalHub()) {
            return;
        }

        // Add breadcrumbs to this event
        Sentry.configureScope(scope -> {
            for (String b : breadcrumbs) {
                Breadcrumb breadcrumb = new Breadcrumb();
                breadcrumb.setMessage(b);
                breadcrumb.setLevel(SentryLevel.INFO);
                scope.addBreadcrumb(breadcrumb);
            }
        });

        // Send exception to Sentry (beforeSend hook will filter if needed)
        Hint hint = new Hint();
        hint.set(HINT_MYPET_CAPTURE, Boolean.TRUE);
        Sentry.captureException(t, hint);

        // Clear breadcrumbs after sending
        Sentry.configureScope(scope -> scope.clearBreadcrumbs());
    }

    protected class MyPetExceptionAppender extends AbstractAppender {

        protected MyPetExceptionAppender() {
            super("MyPet-Exception-Appender", new MyPetFilter(), null);
        }

        @Override
        public void append(LogEvent logEvent) {
            if (logEvent.getThrown() != null) {
                sendError(logEvent.getThrown());
            }
        }
    }

    protected class MyPetFilter extends AbstractFilter {

        @Override
        public Result filter(LogEvent event) {
            // Quick pre-filter: only accept if MyPet's package is on the stack at all.
            // beforeSend then checks that MyPet is what threw, not just what was called.
            if (event.getThrown() != null && ErrorFilter.touchesMyPet(event.getThrown())) {
                return Result.ACCEPT;
            }
            return Result.DENY;
        }
    }
}
