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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Voxel.Shop replaces the {@code %%__..__%%} placeholders in the bundled {@code voxel.properties},
 * byte-for-byte, in jars downloaded through its resource system.
 */
public class VoxelInfo {

    private static String polymart;
    private static String user;
    private static String resource;
    private static String nonce;
    private static String timestamp;
    private static String agent;
    private static String verifyToken;
    private static String injectVersion;
    private static boolean loaded = false;

    private VoxelInfo() {
    }

    /** True iff this jar was downloaded through Voxel.Shop — only Voxel replaces the POLYMART marker. */
    public static boolean isInjected() {
        load();
        return "1".equals(polymart);
    }

    public static String user() {
        load();
        return user;
    }

    public static String resource() {
        load();
        return resource;
    }

    public static String nonce() {
        load();
        return nonce;
    }

    public static String timestamp() {
        load();
        return timestamp;
    }

    public static String agent() {
        load();
        return agent;
    }

    public static String verifyToken() {
        load();
        return verifyToken;
    }

    public static String injectVersion() {
        load();
        return injectVersion;
    }

    private static void load() {
        if (loaded) {
            return;
        }
        Properties props = new Properties();
        try (InputStream in = VoxelInfo.class.getResourceAsStream("/voxel.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException ignored) {
            // Treated as not injected.
        }
        polymart = props.getProperty("polymart", "");
        user = props.getProperty("user", "");
        resource = props.getProperty("resource", "");
        nonce = props.getProperty("nonce", "");
        timestamp = props.getProperty("timestamp", "");
        agent = props.getProperty("agent", "");
        verifyToken = props.getProperty("verifyToken", "");
        injectVersion = props.getProperty("injectVersion", "");
        loaded = true;
    }
}
