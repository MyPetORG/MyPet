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

package de.Keyle.MyPet.api.util.logger;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.util.ErrorUtil;
import de.Keyle.MyPet.api.util.LogFormat;
import de.Keyle.MyPet.api.util.ReflectionUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginLogger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;

public class MyPetLogger extends PluginLogger {

    protected boolean debugSetup = false;
    private static FileHandler debugLogFileHandler = null;
    private static final ANSIComponentSerializer ANSI_SERIALIZER = ANSIComponentSerializer.ansi();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();
    private final String customName;

    public MyPetLogger(Plugin context) {
        super(context);

        // Get the plugin name
        String prefix = context.getDescription().getPrefix();
        this.customName = prefix != null ? prefix : context.getDescription().getName();

        try {
            // Set pluginName to empty string to prevent double [MyPet] prefix
            // Paper's formatter will add the [PluginName] prefix automatically
            Field pluginNameField = ReflectionUtil.getField(PluginLogger.class, "pluginName");
            pluginNameField.set(this, "");

        } catch (IllegalAccessException e) {
            ErrorUtil.report(e);
        }
    }

    @Override
    public String getName() {
        return customName != null ? customName : super.getName();
    }

    @Override
    public void log(@NotNull LogRecord logRecord) {
        if (!debugSetup) {
            setupDebugLogger();
            debugSetup = true;
        }

        // Set the logger name in the record itself
        if (customName != null) {
            logRecord.setLoggerName(customName);
        }

        String message = logRecord.getMessage();
        if (message != null) {
            // If this already looks like an ANSI-colored string (i.e. produced from a Component),
            // skip legacy processing entirely.
            boolean containsAnsi = message.indexOf('\u001b') >= 0;

            if (!containsAnsi) {
                // Add red color for warnings and errors (legacy path only)
                if (logRecord.getLevel() == Level.WARNING || logRecord.getLevel() == Level.SEVERE) {
                    message = ChatColor.RED + message;
                }

                // Convert legacy color codes to ANSI escape sequences
                message = applyStyles(message);
            }

            logRecord.setMessage(message);
        }

        super.log(logRecord);
    }

    /**
     * Converts a legacy-colored string (section codes) to an ANSI-colored string
     * by going through an Adventure Component.
     */
    public String applyStyles(String message) {
        // Convert legacy color codes (§) to Adventure Component
        Component component = LEGACY_SERIALIZER.deserialize(message);

        // Convert Adventure Component to ANSI-colored string
        return ANSI_SERIALIZER.serialize(component);
    }

    /**
     * Logs an Adventure Component at the given level.
     * The Component is serialized to an ANSI-colored String for the console.
     */
    private void logComponent(Level level, Component component) {
        if (component == null) {
            return;
        }

        // Serialize the component directly to ANSI so we don't need legacy codes here
        String ansi = ANSI_SERIALIZER.serialize(component);

        LogRecord record = new LogRecord(level, ansi);
        // Let our overridden log(LogRecord) handle debug setup and name,
        // but it will detect ANSI and skip legacy processing.
        this.log(record);
    }

    // ----- Component-based convenience methods -----

    public void info(Component component) {
        logComponent(Level.INFO, component);
    }

    public void warning(Component component) {
        logComponent(Level.WARNING, component);
    }

    public void severe(Component component) {
        logComponent(Level.SEVERE, component);
    }

    public void config(Component component) {
        logComponent(Level.CONFIG, component);
    }

    public void fine(Component component) {
        logComponent(Level.FINE, component);
    }

    public void finer(Component component) {
        logComponent(Level.FINER, component);
    }

    public void finest(Component component) {
        logComponent(Level.FINEST, component);
    }

    // ----- Existing Object... helpers (string-based) -----

    public void info(Object... params) {
        this.info(Arrays.stream(params).map(String::valueOf).collect(Collectors.joining(" ")));
    }

    public void warning(Object... params) {
        this.warning(Arrays.stream(params).map(String::valueOf).collect(Collectors.joining(" ")));
    }

    public void severe(Object... params) {
        this.severe(Arrays.stream(params).map(String::valueOf).collect(Collectors.joining(" ")));
    }

    public void config(Object... params) {
        this.config(Arrays.stream(params).map(String::valueOf).collect(Collectors.joining(" ")));
    }

    public void fine(Object... params) {
        this.fine(Arrays.stream(params).map(String::valueOf).collect(Collectors.joining(" ")));
    }

    public void finer(Object... params) {
        this.finer(Arrays.stream(params).map(String::valueOf).collect(Collectors.joining(" ")));
    }

    public void finest(Object... params) {
        this.finest(Arrays.stream(params).map(String::valueOf).collect(Collectors.joining(" ")));
    }

    // ----- Debug logger management -----

    public void updateDebugLoggerLogLevel() {
        if (debugLogFileHandler != null) {
            Level level;
            try {
                level = Level.parse(Configuration.Log.LEVEL);
            } catch (IllegalArgumentException e) {
                level = Level.OFF;
                this.warning(e.getMessage());
            }
            debugLogFileHandler.setLevel(level);
        }
    }

    public void disableDebugLogger() {
        for (Handler h : getHandlers()) {
            if (h.toString().equals("MyPet-Debug-Logger-FileHandler")) {
                removeHandler(h);
                h.close();
            }
        }
    }

    protected void setupDebugLogger() {
        getHandlers();
        for (Handler h : getHandlers()) {
            if (h.toString().equals("MyPet-Debug-Logger-FileHandler")) {
                if (Configuration.Log.LEVEL.equalsIgnoreCase("OFF")) {
                    removeHandler(h);
                    h.close();
                    return;
                }
                debugLogFileHandler = (FileHandler) h;
                return;
            }
        }
        if (Configuration.Log.LEVEL.equalsIgnoreCase("OFF")) {
            return;
        }
        if (debugLogFileHandler != null) {
            addHandler(debugLogFileHandler);
            return;
        }
        try {
            File logsFolder = new File(MyPetApi.getPlugin().getDataFolder(), "logs");
            logsFolder.mkdirs();
            File logFile = new File(logsFolder, File.separator + "MyPet.log");
            FileHandler fileHandler = new FileHandler(logFile.getAbsolutePath(), true) {
                @Override
                public String toString() {
                    return "MyPet-Debug-Logger-FileHandler";
                }
            };

            Level level;
            try {
                level = Level.parse(Configuration.Log.LEVEL);
            } catch (IllegalArgumentException e) {
                level = Level.OFF;
                this.warning(e.getMessage());
            }
            fileHandler.setLevel(level);
            fileHandler.setFormatter(new LogFormat());
            addHandler(fileHandler);
            debugLogFileHandler = fileHandler;
        } catch (IOException e) {
            ErrorUtil.report(e);
        }
    }
}