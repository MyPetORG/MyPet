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
 * The MyPet Hub replaces the {@code %%__..__%%} placeholders in the bundled {@code hub.properties}
 * with the downloader's Discord snowflake and a secret nonce, byte-for-byte, in jars it serves.
 */
public class HubInfo {

    private static String discordId;
    private static String nonce;
    private static boolean loaded = false;

    private HubInfo() {
    }

    public static String discordId() {
        load();
        return discordId;
    }

    public static String nonce() {
        load();
        return nonce;
    }

    /**
     * True iff this jar was downloaded through the MyPet Hub and both placeholders were replaced.
     */
    public static boolean isInjected() {
        load();
        return !discordId.startsWith("%%__") && !discordId.isEmpty()
                && !nonce.startsWith("%%__") && !nonce.isEmpty();
    }

    private static void load() {
        if (loaded) {
            return;
        }
        Properties props = new Properties();
        try (InputStream in = HubInfo.class.getResourceAsStream("/hub.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException ignored) {
            // Treated as not injected.
        }
        discordId = props.getProperty("discordId", "");
        nonce = props.getProperty("nonce", "");
        loaded = true;
    }
}
