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
import java.util.Optional;
import java.util.Properties;

/**
 * BuiltByBit (BBB) replaces the {@code %%__..__%%} placeholder constants below with real
 * values, byte-for-byte, in jars downloaded through its resource system. Each constant must
 * stay a single {@code static final String} initialized directly with the literal marker —
 * no concatenation — or BBB's byte-level replacement won't find it.
 */
public class BuiltByBitInfo {

    private static final String MEMBER_ID = "%%__USER__%%";
    private static final String NONCE = "%%__NONCE__%%";
    private static final String TIMESTAMP = "%%__TIMESTAMP__%%";
    private static final String RESOURCE_ID = "%%__RESOURCE__%%";

    private static String sharedToken;
    private static boolean sharedTokenLoaded = false;

    private BuiltByBitInfo() {
    }

    public static String memberId() {
        return MEMBER_ID;
    }

    public static String nonce() {
        return NONCE;
    }

    public static String timestamp() {
        return TIMESTAMP;
    }

    public static String resourceId() {
        return RESOURCE_ID;
    }

    /**
     * True iff this jar was downloaded through BuiltByBit and all placeholders were replaced.
     */
    public static boolean isInjected() {
        return !MEMBER_ID.startsWith("%%__") && !NONCE.startsWith("%%__")
                && !TIMESTAMP.startsWith("%%__") && !RESOURCE_ID.startsWith("%%__");
    }

    /**
     * The BuiltByBit Shared API token bundled at CI build time via {@code builtbybit.properties},
     * or empty if none was injected (local builds, or CI runs missing the secret).
     */
    public static Optional<String> sharedToken() {
        if (!sharedTokenLoaded) {
            sharedToken = loadSharedToken();
            sharedTokenLoaded = true;
        }
        return sharedToken.isEmpty() ? Optional.empty() : Optional.of(sharedToken);
    }

    private static String loadSharedToken() {
        Properties props = new Properties();
        try (InputStream in = BuiltByBitInfo.class.getResourceAsStream("/builtbybit.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException ignored) {
            // Treated as no token bundled.
        }
        return props.getProperty("sharedToken", "");
    }
}
