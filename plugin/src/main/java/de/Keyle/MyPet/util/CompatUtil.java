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

package de.Keyle.MyPet.util;

import org.bukkit.Bukkit;

/**
 * Version detection and comparison helpers.
 * <p>
 * {@link #minecraftVersionEqualsOrAbove(String)} is the feature-gate entry
 * point for Minecraft-version checks (e.g. Creaking added in 1.21.4).
 * {@link #versionCompare(String, String)} is a generic numeric-version
 * comparator also used by the self-updater for MyPet plugin versions.
 */
public final class CompatUtil {

    private CompatUtil() {
    }

    /**
     * Checks whether the running server's Minecraft version is numerically
     * greater than or equal to the given version. Intended for feature
     * gating — e.g. {@code minecraftVersionEqualsOrAbove("1.21.4")} returns
     * {@code true} on any server running 1.21.4 or later.
     *
     * @param version the Minecraft version to compare against, as a
     *                dotted-numeric string such as {@code "1.21"} or
     *                {@code "1.21.4"}
     * @return {@code true} if the server's Minecraft version is at or
     *         above {@code version}
     * @throws IllegalArgumentException if {@code version} is not a valid
     *         dotted-numeric version
     */
    public static boolean minecraftVersionEqualsOrAbove(String version) {
        return versionCompare(Bukkit.getMinecraftVersion(), version) >= 0;
    }

    /**
     * Compares two dotted-numeric version strings numerically.
     * <p>
     * Delegates to {@link Runtime.Version}, whose parser accepts any
     * dot-separated sequence of non-negative integers and does
     * component-wise numeric comparison with implicit zero-padding for
     * missing trailing components. This matches the Minecraft and MyPet
     * plugin version formats exactly.
     *
     * @param str1 a string of ordinal numbers separated by decimal points.
     * @param str2 a string of ordinal numbers separated by decimal points.
     * @return a negative integer, zero, or a positive integer as {@code str1}
     *         is numerically less than, equal to, or greater than {@code str2}.
     * @throws IllegalArgumentException if either string is not a valid
     *         dotted-numeric version
     */
    public static int versionCompare(String str1, String str2) {
        return Runtime.Version.parse(str1).compareTo(Runtime.Version.parse(str2));
    }
}
