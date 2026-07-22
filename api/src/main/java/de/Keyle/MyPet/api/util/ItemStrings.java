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

package de.Keyle.MyPet.api.util;

import de.Keyle.MyPet.MyPetApi;
import org.bukkit.inventory.ItemStack;

import java.util.Base64;

/**
 * Round-trips a full {@link ItemStack} (including all meta) to and from a paste-safe
 * one-line string, for storing items in skilltree JSON.
 *
 * <p>{@link #serialize} emits a {@code base64:} token (lossless, versioned via Paper's
 * {@code serializeAsBytes}). {@link #deserialize} accepts that token, or a plain
 * data-component / material string (e.g. {@code minecraft:diamond}) parsed by
 * {@link ConfigItem}, so simple hand-authored rows work too.
 */
public final class ItemStrings {

    private static final String BASE64_PREFIX = "base64:";

    private ItemStrings() {
    }

    /** Serialize a full item (incl. meta) to a token {@link #deserialize} round-trips. */
    public static String serialize(ItemStack item) {
        return BASE64_PREFIX + Base64.getEncoder().encodeToString(item.serializeAsBytes());
    }

    /**
     * Parse a drop-pool item string. Returns {@code null} for blank/unparseable input.
     * A {@code base64:} token is decoded losslessly; anything else is delegated to
     * {@link ConfigItem} (data-component or material string).
     */
    public static ItemStack deserialize(String data) {
        if (data == null || data.isBlank()) {
            return null;
        }
        String trimmed = data.trim();
        if (trimmed.startsWith(BASE64_PREFIX)) {
            try {
                byte[] bytes = Base64.getDecoder().decode(trimmed.substring(BASE64_PREFIX.length()));
                return ItemStack.deserializeBytes(bytes);
            } catch (Throwable e) {
                MyPetApi.getLogger().warning("Invalid base64 item in skilltree drop: " + e.getMessage());
                return null;
            }
        }
        ConfigItem configItem = ConfigItem.createConfigItem(trimmed);
        return configItem == null ? null : configItem.getItem();
    }
}
