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
import io.sentry.SentryClient;
import io.sentry.SentryClientFactory;
import io.sentry.context.Context;
import io.sentry.event.BreadcrumbBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.bukkit.Bukkit;

import java.util.HashSet;
import java.util.Set;

public class ErrorReporter {

    protected SentryClient sentry;
    protected Context context;
    protected Appender loggerAppender = null;

    public ErrorReporter() {
        sentry = SentryClientFactory.sentryClient("https://14aec086f95d4fbe8a378638c80b68fa@sentry.io/1368849");
        context = sentry.getContext();

        context.addTag("premium", "" + MyPetVersion.isPremium());
        context.addTag("plugin_version", "" + MyPetVersion.getVersion());
        context.addTag("plugin_build", "" + MyPetVersion.getBuild());
        context.addTag("server_version", Bukkit.getServer().getVersion());
        sentry.setServerName(Bukkit.getServerId());
        sentry.setRelease(MyPetVersion.getVersion());
    }

    public void onEnable() {
        loggerAppender = new MyPetExceptionAppender();
        loggerAppender.start();
        Logger logger = (Logger) LogManager.getRootLogger();
        logger.addAppender(loggerAppender);
    }

    public void onDisable() {
        if (loggerAppender != null) {
            Logger logger = (Logger) LogManager.getRootLogger();
            loggerAppender.stop();
            logger.removeAppender(loggerAppender);
        }
    }

    public void sendError(Throwable t, String... context) {
        for (String c : context) {
            this.context.recordBreadcrumb(
                    new BreadcrumbBuilder().setMessage(c).build()
            );
        }

        sentry.sendException(t);
        this.context.clearBreadcrumbs();
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
