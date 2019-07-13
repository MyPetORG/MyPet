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
import de.Keyle.MyPet.api.util.LogFormat;
import de.Keyle.MyPet.api.util.ReflectionUtil;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginLogger;
import org.fusesource.jansi.Ansi;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;

public class MyPetLogger extends PluginLogger {

    protected boolean debugSetup = false;
    private final Map<ChatColor, String> replacements = new HashMap<>();
    private static FileHandler debugLogFileHandler = null;
    private static boolean AnsiSupported = true;

    public MyPetLogger(Plugin context) {
        super(context);

        try {
            if (Ansi.isEnabled()) {
                registerStyles();
            }
        } catch (NoClassDefFoundError e) {
            AnsiSupported = false;
        }

        String prefix = context.getDescription().getPrefix();
        String pluginName = prefix != null ? "[" + ChatColor.DARK_GREEN + prefix + ChatColor.RESET + "] " : "[" + ChatColor.DARK_GREEN + context.getDescription().getName() + ChatColor.RESET + "] ";
        if (AnsiSupported) {
            pluginName = applyStyles(pluginName);
        }

        try {
            Field logger = ReflectionUtil.getField(PluginLogger.class, "pluginName");
            logger.set(this, pluginName);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public void log(LogRecord logRecord) {
        if (!debugSetup) {
            setupDebugLogger();
            debugSetup = true;
        }

        String message = logRecord.getMessage();
        if (message != null) {
            if (logRecord.getLevel() == Level.WARNING || logRecord.getLevel() == Level.SEVERE) {
                message = ChatColor.RED + message;
            }
            if (AnsiSupported) {
                message = applyStyles(message);
            }
            logRecord.setMessage(message);
        }

        super.log(logRecord);
    }

    public String applyStyles(String message) {
        for (ChatColor color : replacements.keySet()) {
            if (this.replacements.containsKey(color)) {
                message = message.replaceAll("(?i)" + color.toString(), this.replacements.get(color));
            } else {
                message = message.replaceAll("(?i)" + color.toString(), "");
            }
        }
        return message + Ansi.ansi().reset().toString();
    }

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

    protected boolean setupDebugLogger() {
        if (getHandlers().length > 0) {
            for (Handler h : getHandlers()) {
                if (h.toString().equals("MyPet-Debug-Logger-FileHandler")) {
                    if (Configuration.Log.LEVEL.equalsIgnoreCase("OFF")) {
                        removeHandler(h);
                        h.close();
                        return false;
                    }
                    debugLogFileHandler = (FileHandler) h;
                    return true;
                }
            }
        }
        if (Configuration.Log.LEVEL.equalsIgnoreCase("OFF")) {
            return false;
        }
        if (debugLogFileHandler != null) {
            addHandler(debugLogFileHandler);
            return true;
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
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void registerStyles() {
        this.replacements.put(ChatColor.BLACK, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.BLACK).boldOff().toString());
        this.replacements.put(ChatColor.DARK_BLUE, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.BLUE).boldOff().toString());
        this.replacements.put(ChatColor.DARK_GREEN, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.GREEN).boldOff().toString());
        this.replacements.put(ChatColor.DARK_AQUA, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.CYAN).boldOff().toString());
        this.replacements.put(ChatColor.DARK_RED, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.RED).boldOff().toString());
        this.replacements.put(ChatColor.DARK_PURPLE, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.MAGENTA).boldOff().toString());
        this.replacements.put(ChatColor.GOLD, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.YELLOW).boldOff().toString());
        this.replacements.put(ChatColor.GRAY, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.WHITE).boldOff().toString());
        this.replacements.put(ChatColor.DARK_GRAY, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.BLACK).bold().toString());
        this.replacements.put(ChatColor.BLUE, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.BLUE).bold().toString());
        this.replacements.put(ChatColor.GREEN, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.GREEN).bold().toString());
        this.replacements.put(ChatColor.AQUA, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.CYAN).bold().toString());
        this.replacements.put(ChatColor.RED, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.RED).bold().toString());
        this.replacements.put(ChatColor.LIGHT_PURPLE, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.MAGENTA).bold().toString());
        this.replacements.put(ChatColor.YELLOW, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.YELLOW).bold().toString());
        this.replacements.put(ChatColor.WHITE, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.WHITE).bold().toString());
        this.replacements.put(ChatColor.MAGIC, Ansi.ansi().a(Ansi.Attribute.BLINK_SLOW).toString());
        this.replacements.put(ChatColor.BOLD, Ansi.ansi().a(Ansi.Attribute.UNDERLINE_DOUBLE).toString());
        this.replacements.put(ChatColor.STRIKETHROUGH, Ansi.ansi().a(Ansi.Attribute.STRIKETHROUGH_ON).toString());
        this.replacements.put(ChatColor.UNDERLINE, Ansi.ansi().a(Ansi.Attribute.UNDERLINE).toString());
        this.replacements.put(ChatColor.ITALIC, Ansi.ansi().a(Ansi.Attribute.ITALIC).toString());
        this.replacements.put(ChatColor.RESET, Ansi.ansi().a(Ansi.Attribute.RESET).toString());
    }

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
}