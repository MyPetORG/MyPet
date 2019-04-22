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

package de.Keyle.MyPet.util;

import de.Keyle.MyPet.api.MyPetVersion;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.util.ErrorReporter;
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

import java.util.*;
import java.util.stream.Collectors;

public class SentryErrorReporter implements ErrorReporter {

    @Getter @Setter private static UUID serverUUID = UUID.randomUUID();

    protected SentryClient sentry;
    protected Context context;
    protected Appender loggerAppender = null;
    protected boolean enabled = false;

    public void onEnable() {
        sentry = SentryClientFactory.sentryClient("https://14aec086f95d4fbe8a378638c80b68fa@sentry.io/1368849?" +
                "stacktrace.app.packages=");
        context = sentry.getContext();

        context.addTag("plugin_version", "" + MyPetVersion.getVersion());
        context.addTag("plugin_build", "" + MyPetVersion.getBuild());
        context.setUser(new UserBuilder().setId(serverUUID.toString()).build());
        sentry.setServerName(Bukkit.getServer().getVersion());
        sentry.setRelease(MyPetVersion.getVersion());
        sentry.setEnvironment(MyPetVersion.isDevBuild() ? "development" : "production");

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

        loggerAppender = new MyPetExceptionAppender();
        loggerAppender.start();
        Logger logger = (Logger) LogManager.getRootLogger();
        logger.addAppender(loggerAppender);

        enabled = true;
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
        if (!filter(t)) {
            return;
        }
        for (String c : context) {
            this.context.recordBreadcrumb(
                    new BreadcrumbBuilder().setMessage(c).build()
            );
        }

        sentry.sendException(t);
        this.context.clearBreadcrumbs();
    }

    protected boolean filter(Throwable t) {
        if (t instanceof ConcurrentModificationException ||
                t instanceof VirtualMachineError
        ) {
            return false;
        }
        if (t instanceof NullPointerException) {
            long myPetTraces = Arrays.stream(t.getStackTrace())
                    .limit(3)
                    .filter(stackTraceElement -> stackTraceElement.getClassName().contains("MyPet"))
                    .count();
            return myPetTraces >= 1;
        }
        long myPetTraces = Arrays.stream(t.getStackTrace())
                .filter(trace -> trace.getClassName().contains("mypet") && trace.getClassName().contains("npc"))
                .count();
        return myPetTraces == 0;
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
