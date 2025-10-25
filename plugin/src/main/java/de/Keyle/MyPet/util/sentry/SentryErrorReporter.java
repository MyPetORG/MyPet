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
import de.Keyle.MyPet.api.MyPetVersion;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.util.ErrorReporter;
import de.Keyle.MyPet.api.util.hooks.PluginHookName;
import de.Keyle.MyPet.util.sentry.marshaller.gson.GsonSentryClientFactory;
import io.sentry.SentryClient;
import io.sentry.SentryClientFactory;
import io.sentry.context.Context;
import io.sentry.event.BreadcrumbBuilder;
import io.sentry.event.UserBuilder;
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

    protected SentryClient sentry;
    protected Context context;
    protected Appender loggerAppender = null;
    protected boolean enabled = false;
    protected boolean hooksLoaded = false;

    public void onEnable() {
        SentryClientFactory factory = new GsonSentryClientFactory();
        sentry = SentryClientFactory.sentryClient("https://14aec086f95d4fbe8a378638c80b68fa@sentry.io/1368849?" +
                "stacktrace.app.packages=", factory);
        context = sentry.getContext();

        context.addTag("plugin_build", "" + MyPetVersion.getBuild());
        context.setUser(new UserBuilder().setId(serverUUID.toString()).build());
        sentry.setServerName(Bukkit.getServer().getVersion());
        sentry.setRelease(MyPetVersion.getVersion());
        sentry.setEnvironment(MyPetVersion.isDevBuild() ? "development" : "production");

        addPlugins();

        loggerAppender = new MyPetExceptionAppender();
        loggerAppender.start();
        Logger logger = (Logger) LogManager.getRootLogger();
        logger.addAppender(loggerAppender);

        enabled = true;
    }

    protected void addPlugins() {
        List<String> plugins = Arrays
                .stream(Bukkit.getPluginManager().getPlugins())
                .map(plugin -> plugin.getName() + " (" + plugin.getDescription().getVersion() + ")")
                .collect(Collectors.toList());
        int pluginCounter = 1;
        StringBuilder part = new StringBuilder();
        for (String plugin : plugins) {
            if (part.length() + plugin.length() + "\n".length() > 400) {
                context.addExtra("plugins_" + pluginCounter, part.toString());
                pluginCounter++;
                part = new StringBuilder();
            }
            part.append(plugin).append("\n");
        }
        if (part.length() > 0) {
            context.addExtra("plugins_" + pluginCounter, part.toString());
        }
    }

    protected void addPluginHooks() {
        if (MyPetApi.getPluginHookManager() == null || MyPetApi.getPluginHookManager().getHooks() == null) {
            return;
        }
        List<String> hooks = MyPetApi.getPluginHookManager().getHooks().stream()
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
                .collect(Collectors.toList());
        int hookCounter = 1;
        StringBuilder part = new StringBuilder();
        for (String hook : hooks) {
            if (part.length() + hook.length() + "\n".length() > 400) {
                context.addExtra("hooks_" + hookCounter, part.toString());
                hookCounter++;
                part = new StringBuilder();
            }
            part.append(hook).append("\n");
        }
        if (part.length() > 0) {
            context.addExtra("hooks_" + hookCounter, part.toString());
        }
        hooksLoaded = true;
    }

    public void onDisable() {
        if (enabled && loggerAppender != null) {
            Logger logger = (Logger) LogManager.getRootLogger();
            loggerAppender.stop();
            logger.removeAppender(loggerAppender);
        }
        enabled = false;
    }

    public void sendError(Throwable t, String... context) {
        if (!enabled) {
            return;
        }
        boolean myPetWasCause = filter(t);
        Throwable throwable = t;
        while (throwable.getCause() != null) {
            throwable = throwable.getCause();
            myPetWasCause = myPetWasCause || filter(throwable);
        }
        if (!myPetWasCause) {
            return;
        }

        for (String c : context) {
            this.context.recordBreadcrumb(
                    new BreadcrumbBuilder().setMessage(c).build()
            );
        }
        if (!hooksLoaded) {
            addPluginHooks();
        }

        sentry.sendException(t);
        this.context.clearBreadcrumbs();
    }

    protected boolean filter(Throwable t) {
        if (t instanceof ConcurrentModificationException ||
                t instanceof IOException ||
                t instanceof VirtualMachineError ||
                t instanceof LinkageError ||
                t instanceof FileNotFoundException ||
                t instanceof InvalidConfigurationException
        ) {
            return false;
        }
        switch (t.getClass().getSimpleName()) {
            case "AuthenticationException":
                return false;
        }
        Optional<StackTraceElement> element;
        if (t instanceof NullPointerException) {
            element = Arrays.stream(t.getStackTrace())
                    .limit(3)
                    .filter(stackTraceElement -> stackTraceElement.getClassName().contains("MyPet"))
                    .findFirst();
            if (!element.isPresent()) {
                return false;
            }
            element = Arrays.stream(t.getStackTrace())
                    .filter(trace -> trace.toString().contains("BukkitAdapter.adapt"))
                    .findFirst();
            if (element.isPresent()) {
                return false;
            }
        }

        element = Arrays.stream(t.getStackTrace())
                .filter(trace -> trace.getClassName().contains("mypet") && trace.getClassName().contains("npc"))
                .findFirst();
        if (element.isPresent()) {
            return false;
        }
        long myPetTraces = Arrays.stream(t.getStackTrace())
                .limit(5)
                .filter(stackTraceElement -> stackTraceElement.getClassName().contains("MyPet"))
                .count();
        return myPetTraces >= 1;
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

        protected Set<String> alreadySent = new HashSet<>();

        @Override
        public Result filter(LogEvent event) {
            if (event.getThrown() != null) {
                Throwable thrown = event.getThrown();
                if (Util.findStringInThrowable(thrown, "MyPet")) {
                    if (!alreadySent.contains(thrown.getMessage() + thrown.getStackTrace()[0])) {
                        alreadySent.add(thrown.getMessage() + thrown.getStackTrace()[0]);
                        return Result.ACCEPT;
                    }
                }
            }
            return Result.DENY;
        }
    }
}
