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

package de.Keyle.MyPet.api.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Enum representing internal Minecraft NMS versions.
 * <p>
 * Each constant maps one or more Minecraft versions to its NMS module name.
 * Used as a fallback when the server class package name does not contain the
 * NMS version (Paper 1.20.5+).
 */
public enum MinecraftVersion {
    v1_20_R4("1.20.5", "1.20.6"),
    v1_21_R1("1.21", "1.21.1"),
    v1_21_R2("1.21.2", "1.21.3"),
    v1_21_R3("1.21.4"),
    v1_21_R4("1.21.5"),
    v1_21_R5("1.21.6", "1.21.7", "1.21.8"),
    v1_21_R6("1.21.9", "1.21.10"),
    v1_21_R7("1.21.11"),
    ;

    private static final Map<String, MinecraftVersion> MC_VERSION_LOOKUP = new HashMap<>();

    static {
        for (MinecraftVersion version : values()) {
            for (String mcVersion : version.minecraftVersions) {
                MC_VERSION_LOOKUP.put(mcVersion, version);
            }
        }
    }

    private final String[] minecraftVersions;

    MinecraftVersion(String... minecraftVersions) {
        this.minecraftVersions = minecraftVersions;
    }

    /**
     * Returns the NMS module name for a given Minecraft version.
     *
     * @param minecraftVersion the Minecraft version (e.g. "1.21.11")
     * @return the NMS module name (e.g. "v1_21_R7"), or null if not found
     */
    public static String getNmsVersion(String minecraftVersion) {
        MinecraftVersion version = MC_VERSION_LOOKUP.get(minecraftVersion);
        return version != null ? version.name() : null;
    }
}
