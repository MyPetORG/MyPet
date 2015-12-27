/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2016 Keyle
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

package de.Keyle.MyPet.util.logger;

import org.bukkit.command.ConsoleCommandSender;

import static org.bukkit.ChatColor.*;

public class MyPetLogger {
    private static ConsoleCommandSender consoleCommandSender = null;

    public static void setConsole(ConsoleCommandSender console) {
        consoleCommandSender = console;
    }

    public static void write(String msg) {
        if (consoleCommandSender != null) {
            consoleCommandSender.sendMessage("[" + GREEN + "M" + DARK_GREEN + "y" + GREEN + "P" + DARK_GREEN + "et" + RESET + "] " + msg);
            DebugLogger.info("(L) " + msg);
        }
    }

    public static void write(String msg, String source) {
        if (consoleCommandSender != null) {
            consoleCommandSender.sendMessage("[" + AQUA + source + RESET + "] " + msg);
            DebugLogger.info("(L) " + msg, source);
        }
    }
}