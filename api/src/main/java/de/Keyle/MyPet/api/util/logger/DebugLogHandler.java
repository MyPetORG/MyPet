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

package de.Keyle.MyPet.api.util.logger;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.util.LogFormat;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages debug file logging for MyPet.
 * Adds a FileHandler to the plugin's logger for writing to plugins/MyPet/logs/MyPet.log
 */
public class DebugLogHandler {

    private static final String HANDLER_NAME = "MyPet-Debug-Logger-FileHandler";
    private static FileHandler debugLogFileHandler = null;

    /**
     * Sets up the debug file handler on the given logger.
     * Uses the log level from Configuration.Log.LEVEL.
     *
     * @param logger the logger to add the handler to
     * @return true if handler was set up successfully
     */
    public static boolean setup(Logger logger) {
        if (logger == null) {
            return false;
        }

        // Check if handler already exists
        for (Handler h : logger.getHandlers()) {
            if (HANDLER_NAME.equals(h.toString())) {
                if (Configuration.Log.LEVEL.equalsIgnoreCase("OFF")) {
                    logger.removeHandler(h);
                    h.close();
                    debugLogFileHandler = null;
                    return false;
                }
                debugLogFileHandler = (FileHandler) h;
                return true;
            }
        }

        if (Configuration.Log.LEVEL.equalsIgnoreCase("OFF")) {
            return false;
        }

        // Reuse existing handler if available
        if (debugLogFileHandler != null) {
            logger.addHandler(debugLogFileHandler);
            return true;
        }

        // Create new handler
        try {
            File logsFolder = new File(MyPetApi.getPlugin().getDataFolder(), "logs");
            logsFolder.mkdirs();
            File logFile = new File(logsFolder, "MyPet.log");

            FileHandler fileHandler = new FileHandler(logFile.getAbsolutePath(), true) {
                @Override
                public String toString() {
                    return HANDLER_NAME;
                }
            };

            Level level = parseLevel(Configuration.Log.LEVEL);
            fileHandler.setLevel(level);
            fileHandler.setFormatter(new LogFormat());
            logger.addHandler(fileHandler);
            debugLogFileHandler = fileHandler;
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Updates the log level of the debug handler.
     */
    public static void updateLogLevel() {
        if (debugLogFileHandler != null) {
            Level level = parseLevel(Configuration.Log.LEVEL);
            debugLogFileHandler.setLevel(level);
        }
    }

    /**
     * Disables and removes the debug handler from the given logger.
     *
     * @param logger the logger to remove the handler from
     */
    public static void disable(Logger logger) {
        if (logger == null) {
            return;
        }
        for (Handler h : logger.getHandlers()) {
            if (HANDLER_NAME.equals(h.toString())) {
                logger.removeHandler(h);
                h.close();
            }
        }
        debugLogFileHandler = null;
    }

    private static Level parseLevel(String levelName) {
        try {
            return Level.parse(levelName);
        } catch (IllegalArgumentException e) {
            return Level.OFF;
        }
    }
}