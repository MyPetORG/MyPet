/*
 * This file is part of mypet-v1_8_R3
 *
 * Copyright (C) 2011-2016 Keyle
 * mypet-v1_8_R3 is licensed under the GNU Lesser General Public License.
 *
 * mypet-v1_8_R3 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mypet-v1_8_R3 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.util.hooks;

import de.Keyle.MyPet.api.Configuration;

public class Bungee {

    private static BungeeMode mode = BungeeMode.None;

    public enum BungeeMode {
        Offline, Online, None
    }

    public static boolean isEnabled() {
        return mode != BungeeMode.None;
    }

    public static boolean isOnlineModeEnabled() {
        return mode == BungeeMode.Online;
    }

    public static void reset() {
        try {
            Class spigotConfigClass = Class.forName("org.spigotmc.SpigotConfig");

            if (spigotConfigClass.getField("bungee").getBoolean(null)) {
                if (Configuration.Hooks.BUNGEE_MODE.equalsIgnoreCase("online")) {
                    mode = BungeeMode.Online;
                } else {
                    mode = BungeeMode.Offline;
                }
            }
        } catch (NoClassDefFoundError | ClassNotFoundException | NoSuchFieldException | IllegalAccessException ignored) {
            mode = BungeeMode.None;
        }
    }
}
