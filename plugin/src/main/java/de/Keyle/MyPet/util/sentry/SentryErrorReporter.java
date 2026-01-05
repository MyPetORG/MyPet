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
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.util.ErrorReporter;
import de.Keyle.MyPet.api.util.hooks.PluginHookName;
import io.sentry.Breadcrumb;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
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
import org.bukkit.configuration.InvalidConfigurationException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class SentryErrorReporter implements ErrorReporter {

    @Getter @Setter private static UUID serverUUID = UUID.randomUUID();

    protected Appender loggerAppender = null;
    protected boolean enabled = false;
    protected boolean hooksLoaded = false;

    public void onEnable() {
        // Initialize Sentry with modern 8.0.0+ API
        Sentry.init(options -> {
            options.setDsn("https://14aec086f95d4fbe8a378638c80b68fa@o221805.ingest.us.sentry.io/1368849");
            options.setSendDefaultPii(true);
            options.setTracesSampleRate(1.0);
            options.setDebug(false);

            // Set release and environment
            options.setRelease(MyPetVersion.getVersion());
            options.setEnvironment(MyPetVersion.isDevBuild() ? "development" : "production");

            // Set server name (appears as server_name in Sentry)
            options.setServerName(Bukkit.getServer().getVersion());

            // Ignored exception types (filtered before beforeSend)
            options.addIgnoredExceptionForType(IOException.class);
            options.addIgnoredExceptionForType(VirtualMachineError.class);
            options.addIgnoredExceptionForType(LinkageError.class);
            options.addIgnoredExceptionForType(FileNotFoundException.class);
            options.addIgnoredExceptionForType(InvalidConfigurationException.class);

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

            // beforeSend hook - filter and enrich events before sending
            options.setBeforeSend((event, hint) -> {
                Throwable throwable = event.getThrowable();
                if (throwable == null) {
                    return event;
                }

                // Check if MyPet is involved in the error
                if (!isMyPetRelated(throwable)) {
                    return null; // drop event
                }

                // Filter out noisy/irrelevant exception types
                if (shouldFilterException(throwable)) {
                    return null; // drop event
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
     * Checks if the throwable is related to MyPet.
     */
    protected boolean isMyPetRelated(Throwable t) {
        // Check the main exception and all causes
        Throwable current = t;
        while (current != null) {
            if (Util.findStringInThrowable(current, "MyPet")) {
                return true;
            }
            // For NPEs, require MyPet in top 3 stack frames
            if (current instanceof NullPointerException) {
                long myPetInTop3 = Arrays.stream(current.getStackTrace())
                        .limit(3)
                        .filter(frame -> frame.getClassName().contains("MyPet"))
                        .count();
                if (myPetInTop3 > 0) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * Determines if an exception should be filtered out (not sent to Sentry).
     * Note: Common exception types are handled by ignoredExceptions in init().
     * This method handles more complex filtering logic.
     */
    protected boolean shouldFilterException(Throwable t) {
        Throwable current = t;
        while (current != null) {
            // Filter by class name for exceptions we can't import
            String className = current.getClass().getSimpleName();
            if ("AuthenticationException".equals(className)) {
                return true;
            }

            // Filter out MySQL/database connection errors (server config issues, not plugin bugs)
            if ("CommunicationsException".equals(className) ||
                    "CJCommunicationsException".equals(className)) {
                return true;
            }
            if (current instanceof java.net.ConnectException) {
                // Only filter ConnectException if it's database-related
                boolean isDatabaseRelated = Arrays.stream(current.getStackTrace())
                        .anyMatch(frame -> {
                            String frameName = frame.getClassName();
                            return frameName.contains("mysql") ||
                                    frameName.contains("hikari") ||
                                    frameName.contains("mariadb") ||
                                    frameName.contains("jdbc");
                        });
                if (isDatabaseRelated) {
                    return true;
                }
            }

            // Filter out WorldEdit/WorldGuard adapter issues
            if (current instanceof NullPointerException) {
                boolean hasBukkitAdapter = Arrays.stream(current.getStackTrace())
                        .anyMatch(frame -> frame.toString().contains("BukkitAdapter.adapt"));
                if (hasBukkitAdapter) {
                    return true;
                }
            }

            // Filter out NPC-related errors from Citizens etc.
            boolean hasNpcTrace = Arrays.stream(current.getStackTrace())
                    .anyMatch(frame -> {
                        String name = frame.getClassName().toLowerCase();
                        return name.contains("mypet") && name.contains("npc");
                    });
            if (hasNpcTrace) {
                return true;
            }

            current = current.getCause();
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
        enabled = false;

        // End session for release health tracking
        Sentry.endSession();

        // Close Sentry connection and flush pending events
        Sentry.close();
    }

    public void sendError(Throwable t, String... breadcrumbs) {
        if (!enabled) {
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
        Sentry.captureException(t);

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
            // Quick pre-filter: only accept if MyPet is in the stack trace
            // The beforeSend hook will do the detailed filtering
            if (event.getThrown() != null) {
                Throwable thrown = event.getThrown();
                if (Util.findStringInThrowable(thrown, "MyPet")) {
                    return Result.ACCEPT;
                }
            }
            return Result.DENY;
        }
    }
}
