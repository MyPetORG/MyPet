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

package de.Keyle.MyPet.api.util.inventory;

import de.keyle.knbt.*;

import java.util.Map;

/**
 * The {@code DataVersion} stamp on saved item compounds.
 * <p>
 * Vanilla item NBT carries no version of its own, so a reader cannot tell which Minecraft
 * version wrote it and has to guess from the compound's shape before running the data
 * fixers. MyPet therefore writes the server's data version next to the item, as a top-level
 * int {@code DataVersion} (the same key vanilla uses on chunks and entities), and uses it as
 * the start version for the data fixers when the item is loaded again. MyPet 4's MyPet 3
 * importer reads the same key.
 * <p>
 * Only items saved by Minecraft 1.13 or newer are stamped; older items are recognised by
 * their shape. Air is never stamped.
 */
public final class ItemDataVersion {

    public static final String KEY = "DataVersion";

    /** Returned by {@link #read} when an item carries no stamp. */
    public static final int NONE = -1;

    private ItemDataVersion() {
    }

    /**
     * Writes {@code dataVersion} onto a saved, non-empty item compound.
     *
     * @return the same compound
     */
    public static TagCompound stamp(TagCompound item, int dataVersion) {
        if (item == null || dataVersion <= 0 || isAir(item)) {
            return item;
        }
        item.getCompoundData().put(KEY, new TagInt(dataVersion));
        return item;
    }

    /**
     * @return the stored data version, or {@link #NONE} if the item has no usable stamp
     */
    public static int read(TagCompound item) {
        if (item == null) {
            return NONE;
        }
        TagBase tag = item.getCompoundData().get(KEY);
        int value;
        if (tag instanceof TagInt) {
            value = ((TagInt) tag).getIntData();
        } else if (tag instanceof TagShort) {
            value = ((TagShort) tag).getShortData();
        } else if (tag instanceof TagByte) {
            value = ((TagByte) tag).getByteData();
        } else if (tag instanceof TagLong) {
            long longValue = ((TagLong) tag).getLongData();
            value = longValue > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) longValue;
        } else {
            return NONE;
        }
        return value >= 0 ? value : NONE;
    }

    /**
     * The version to start the data fixers from: the stored one, but never past the running
     * server, whose fixers cannot run backwards.
     */
    public static int startVersion(int stored, int serverDataVersion) {
        return Math.min(stored, serverDataVersion);
    }

    /**
     * @return a shallow copy of the item without the stamp, ready for the vanilla parser
     */
    public static TagCompound withoutStamp(TagCompound item) {
        TagCompound copy = new TagCompound();
        for (Map.Entry<String, TagBase> entry : item.getCompoundData().entrySet()) {
            if (!KEY.equals(entry.getKey())) {
                copy.getCompoundData().put(entry.getKey(), entry.getValue());
            }
        }
        return copy;
    }

    private static boolean isAir(TagCompound item) {
        TagBase id = item.getCompoundData().get("id");
        if (!(id instanceof TagString)) {
            // Pre-1.13 numeric ids never reach here; a compound without a string id is not
            // an item the codec wrote, so leave it alone.
            return true;
        }
        String value = ((TagString) id).getStringData();
        return value.isEmpty() || "minecraft:air".equals(value) || "air".equals(value);
    }
}
