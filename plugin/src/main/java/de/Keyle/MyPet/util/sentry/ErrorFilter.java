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

package de.Keyle.MyPet.util.sentry;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTransientConnectionException;
import java.util.ArrayList;
import java.util.List;

/**
 * Decides which caught errors are MyPet's to report.
 * <p>
 * Two questions are asked of every error before it goes to Sentry. {@link #isMyPetError} asks
 * whether MyPet's own code threw it: the first stack frame that belongs to neither the JVM, the
 * server nor a library has to be in MyPet's package. A plugin that merely has "MyPet" in its name,
 * or a hook in another plugin that calls into MyPet's API, does not qualify, however deep MyPet is
 * on the stack below it. {@link #isIgnored} asks whether the error is one nobody can fix from
 * MyPet's side: a class or method missing at link time, a file the host will not let us write, a
 * database the host moved, a jar swapped while the server runs.
 * <p>
 * This class depends on nothing but the JDK so that its decisions can be unit-tested. Errors from
 * drivers and the server are matched by class name rather than by type, since those classes are
 * not on the test classpath.
 */
final class ErrorFilter {

    static final String MYPET_PACKAGE = "de.Keyle.MyPet.";

    /**
     * Libraries shaded under MyPet's package (see the relocate() calls in build.gradle.kts). They
     * are libraries, not MyPet code. The Sentry SDK's package also holds MyPet's own reporter
     * classes; they never throw anything worth reporting, so the whole package counts as library.
     */
    private static final String[] RELOCATED_PACKAGES = {
            "de.Keyle.MyPet.util.metrics.", "de.Keyle.MyPet.util.hikari.", "de.Keyle.MyPet.util.nbt.",
            "de.Keyle.MyPet.util.bson.", "de.Keyle.MyPet.util.gson.", "de.Keyle.MyPet.util.mongodb.",
            "de.Keyle.MyPet.util.sentry."
    };

    /**
     * Frames that never own an error: the JVM, the server and its forks, and the libraries MyPet
     * ships or calls into. The first frame outside these prefixes is the code that threw.
     */
    private static final String[] INFRASTRUCTURE = {
            "java.", "javax.", "jdk.", "sun.", "com.sun.",
            "net.minecraft.", "org.bukkit.", "io.papermc.", "com.destroystokyo.", "org.spigotmc.",
            "co.aikar.", "com.mojang.", "io.netty.", "ca.spottedleaf.", "me.earthme.luminol.",
            "gg.pufferfish.", "org.purpurmc.", "org.galemc.", "org.leavesmc.",
            "com.zaxxer.hikari.", "org.sqlite.", "com.mysql.", "org.mariadb.", "com.google.",
            "net.kyori.", "org.apache.", "io.sentry.", "org.slf4j.", "org.yaml.", "org.bstats.",
            "kotlin.", "org.jetbrains.", "org.bson.", "com.mongodb.", "de.keyle.knbt.",
            "net.md_5.bungee."
    };

    /** Package roots shared by unrelated plugins, where two segments say nothing about the plugin. */
    private static final String[] SHARED_ROOTS = {"com.github", "io.github", "org.github", "dev.github"};

    /**
     * SQLite result codes that describe the host, not MyPet: a read-only or moved database file,
     * disk errors, a file that cannot be opened, or a lock another process holds.
     */
    private static final String[] SQLITE_HOST_CODES = {
            "SQLITE_READONLY", "SQLITE_IOERR", "SQLITE_CANTOPEN", "SQLITE_BUSY", "SQLITE_LOCKED",
            "SQLITE_FULL", "SQLITE_CORRUPT", "SQLITE_NOTADB", "SQLITE_PERM", "SQLITE_PROTOCOL"
    };

    private enum Owner {MYPET, OTHER, UNKNOWN}

    private ErrorFilter() {
    }

    /**
     * Whether MyPet's own code threw this error, or another plugin's API threw while MyPet was
     * calling it directly.
     * <p>
     * The root cause decides: the server wraps a listener's or a command's exception in one of
     * its own (EventException, CommandException) whose stack points back at whoever fired the
     * event, and that is MyPet whenever the event was MyPet's. Only when a cause has no usable
     * stack does the next exception outward get a say.
     *
     * @param t the error
     * @return true if the code that threw is MyPet's, or is a plugin whose nearest caller outside
     * itself is MyPet
     */
    static boolean isMyPetError(Throwable t) {
        List<Throwable> chain = causeChain(t);
        for (int i = chain.size() - 1; i >= 0; i--) {
            if (isListenerWrapper(chain.get(i))) {
                // Bukkit's EventException only shows who fired the event, which is MyPet for its
                // own events, so it never decides. Its cause has the listener's stack, and when
                // that cause has none, nobody can tell whose listener threw.
                continue;
            }
            Owner owner = ownerOf(chain.get(i).getStackTrace());
            if (owner != Owner.UNKNOWN) {
                return owner == Owner.MYPET;
            }
        }
        return false;
    }

    /**
     * Whether MyPet's package appears anywhere on the stack of this error or its causes. This is
     * the cheap pre-check for log capture; {@link #isMyPetError} makes the real decision.
     *
     * @param t the error
     * @return true if any frame in the chain is in MyPet's package
     */
    static boolean touchesMyPet(Throwable t) {
        for (Throwable current : causeChain(t)) {
            for (StackTraceElement frame : current.getStackTrace()) {
                if (frame.getClassName().startsWith(MYPET_PACKAGE)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Whether this error (or any error in its cause chain) is one MyPet cannot act on.
     *
     * @param t the error
     * @return true if the error should not be reported
     */
    static boolean isIgnored(Throwable t) {
        for (Throwable current : causeChain(t)) {
            if (isIgnoredType(current) || isHostProblem(current) || isReloadArtifact(current)
                    || isKnownNoise(current)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isListenerWrapper(Throwable t) {
        // Matched by simple name like the driver classes below, so the tests need no Bukkit.
        return "EventException".equals(t.getClass().getSimpleName());
    }

    /** The error and its causes, outermost first, stopping at the first repeat so a cycle ends. */
    private static List<Throwable> causeChain(Throwable t) {
        List<Throwable> chain = new ArrayList<>();
        for (Throwable current = t; current != null; current = current.getCause()) {
            for (Throwable seen : chain) {
                if (seen == current) {
                    return chain;
                }
            }
            chain.add(current);
        }
        return chain;
    }

    /**
     * Walks the frames from the top. The first frame that is not JVM, server or library code is
     * the owner. MyPet as owner settles it. Another plugin as owner is MyPet's problem only when
     * MyPet called that plugin itself: the frames right below the owner's own are MyPet's, with no
     * server frames in between. A plugin that the server called, which then reached into MyPet
     * (an event listener, a hook's scheduled task), is not. With no owner frame at all, nothing
     * can be said.
     */
    private static Owner ownerOf(StackTraceElement[] frames) {
        int i = 0;
        while (i < frames.length && isInfrastructure(frames[i].getClassName())) {
            i++;
        }
        if (i == frames.length) {
            return Owner.UNKNOWN;
        }
        String owner = frames[i].getClassName();
        if (isMyPetClass(owner)) {
            return Owner.MYPET;
        }
        String ownerPlugin = pluginKey(owner);
        while (i < frames.length && ownerPlugin.equals(pluginKey(frames[i].getClassName()))) {
            i++;
        }
        return i < frames.length && isMyPetClass(frames[i].getClassName()) ? Owner.MYPET : Owner.OTHER;
    }

    private static boolean isMyPetClass(String className) {
        return className.startsWith(MYPET_PACKAGE) && !isRelocated(className);
    }

    private static boolean isRelocated(String className) {
        for (String prefix : RELOCATED_PACKAGES) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The leading package segments that identify the plugin a class belongs to: two as a rule,
     * three under roots such as {@code com.github} that many unrelated plugins share.
     */
    private static String pluginKey(String className) {
        String key = leadingSegments(className, 2);
        for (String root : SHARED_ROOTS) {
            if (key.equals(root)) {
                return leadingSegments(className, 3);
            }
        }
        return key;
    }

    private static String leadingSegments(String className, int count) {
        int end = -1;
        for (int n = 0; n < count; n++) {
            end = className.indexOf('.', end + 1);
            if (end < 0) {
                return className;
            }
        }
        return className.substring(0, end);
    }

    private static boolean isInfrastructure(String className) {
        if (isRelocated(className)) {
            return true;
        }
        for (String prefix : INFRASTRUCTURE) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whole families of errors that are never a MyPet bug. Checked with instanceof: Sentry's own
     * ignored-type list matches the exact class only, so NoClassDefFoundError slipped past an entry
     * for LinkageError and NoSuchFileException past one for IOException. A StackOverflowError is
     * the one JVM error that usually is a MyPet bug (runaway recursion), so it stays reportable.
     */
    private static boolean isIgnoredType(Throwable t) {
        if (t instanceof LinkageError || t instanceof IOException) {
            return true;
        }
        if (t instanceof VirtualMachineError && !(t instanceof StackOverflowError)) {
            return true;
        }
        String name = t.getClass().getSimpleName();
        return "InvalidConfigurationException".equals(name)    // a server admin's broken YAML
                || "AuthenticationException".equals(name);
    }

    /** Database and file-system failures that only the server's host can fix. */
    private static boolean isHostProblem(Throwable t) {
        String name = t.getClass().getSimpleName();
        String message = t.getMessage() != null ? t.getMessage() : "";
        if ("CommunicationsException".equals(name) || "CJCommunicationsException".equals(name)) {
            return true;
        }
        if (t instanceof SQLNonTransientConnectionException || t instanceof SQLTransientConnectionException
                || t instanceof SQLRecoverableException) {
            return true;
        }
        if (t instanceof SQLException && (message.contains("Access denied") || message.contains("is not allowed to connect"))) {
            return true;
        }
        if ("SQLiteException".equals(name)) {
            for (String code : SQLITE_HOST_CODES) {
                if (message.contains(code)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Errors from a jar that was replaced, or a plugin that was unloaded, while the server kept
     * running. Code from the old jar keeps executing until nothing references it any more, and
     * whatever it throws in that time is not a bug in the new jar.
     */
    private static boolean isReloadArtifact(Throwable t) {
        String name = t.getClass().getSimpleName();
        if ("IllegalPluginAccessException".equals(name)) {
            return true;
        }
        if (t instanceof IllegalStateException && t.getMessage() != null) {
            String message = t.getMessage();
            return message.equals("zip file closed")
                    || message.startsWith("Cannot get plugin for class");
        }
        return false;
    }

    /** WorldEdit/WorldGuard adapter failures on server builds those plugins do not support. */
    private static boolean isKnownNoise(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        for (StackTraceElement frame : t.getStackTrace()) {
            if (frame.getClassName().endsWith("BukkitAdapter") && "adapt".equals(frame.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}
