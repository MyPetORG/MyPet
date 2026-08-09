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
 * no concatenation — or BBB's byte-level replacement won't find it. The MyPet Hub can also
 * re-stamp jars with evidence via {@code builtbybit.properties} as a fallback.
 */
public class BuiltByBitInfo {

    private static final String MEMBER_ID = "%%__USER__%%";
    private static final String NONCE = "%%__NONCE__%%";
    private static final String TIMESTAMP = "%%__TIMESTAMP__%%";
    private static final String RESOURCE_ID = "%%__RESOURCE__%%";

    private static String hubMember;
    private static String hubNonce;
    private static String hubTimestamp;
    private static String sharedToken;
    private static boolean loaded = false;

    private BuiltByBitInfo() {
    }

    public static String memberId() {
        return classInjected() ? MEMBER_ID : hubValueOr(hubMemberValue(), MEMBER_ID);
    }

    public static String nonce() {
        return classInjected() ? NONCE : hubValueOr(hubNonceValue(), NONCE);
    }

    public static String timestamp() {
        return classInjected() ? TIMESTAMP : hubValueOr(hubTimestampValue(), TIMESTAMP);
    }

    /**
     * True iff this jar carries BuiltByBit evidence — injected by BuiltByBit's downloader or
     * re-stamped by the MyPet Hub. Voxel.Shop replaces the same class markers, so Voxel-injected
     * jars are explicitly excluded.
     */
    public static boolean isInjected() {
        return !VoxelInfo.isInjected() && (classInjected() || hubStamped());
    }

    private static boolean classInjected() {
        return !MEMBER_ID.startsWith("%%__") && !NONCE.startsWith("%%__")
                && !TIMESTAMP.startsWith("%%__") && !RESOURCE_ID.startsWith("%%__");
    }

    private static boolean hubStamped() {
        return isFilled(hubMemberValue()) && isFilled(hubNonceValue())
                && isFilled(hubTimestampValue());
    }

    private static String hubValueOr(String value, String fallback) {
        return isFilled(value) ? value : fallback;
    }

    private static boolean isFilled(String value) {
        return value != null && !value.isEmpty() && !value.startsWith("%%__");
    }

    private static String hubMemberValue() {
        load();
        return hubMember;
    }

    private static String hubNonceValue() {
        load();
        return hubNonce;
    }

    private static String hubTimestampValue() {
        load();
        return hubTimestamp;
    }

    /**
     * The BuiltByBit Shared API token bundled at CI build time via {@code builtbybit.properties},
     * or empty if none was injected (local builds, or CI runs missing the secret).
     */
    public static Optional<String> sharedToken() {
        load();
        return sharedToken.isEmpty() ? Optional.empty() : Optional.of(sharedToken);
    }

    private static synchronized void load() {
        if (loaded) {
            return;
        }
        Properties props = new Properties();
        try (InputStream in = BuiltByBitInfo.class.getResourceAsStream("/builtbybit.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException ignored) {
            // Treated as not stamped / no token bundled.
        }
        sharedToken = props.getProperty("sharedToken", "");
        hubMember = props.getProperty("hubMember", "");
        hubNonce = props.getProperty("hubNonce", "");
        hubTimestamp = props.getProperty("hubTimestamp", "");
        loaded = true;
    }
}
