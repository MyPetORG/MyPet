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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The item round trip itself (encode with the server's codec, decode through the data
 * fixers) needs a running server's registries and is checked on a server. These tests
 * cover the part every module shares: what the stamp looks like on a saved compound,
 * how it is read back, and that removing it restores the compound exactly.
 */
class ItemDataVersionTest {

    private static final int SERVER = 4671;

    private static TagCompound diamondSword() {
        TagCompound name = new TagCompound();
        name.getCompoundData().put("text", new TagString("Excalibur"));
        TagCompound components = new TagCompound();
        components.getCompoundData().put("minecraft:custom_name", name);
        TagCompound item = new TagCompound();
        item.getCompoundData().put("id", new TagString("minecraft:diamond_sword"));
        item.getCompoundData().put("count", new TagInt(1));
        item.getCompoundData().put("components", components);
        return item;
    }

    @Test
    void savedItemCarriesATopLevelIntDataVersion() {
        TagCompound item = ItemDataVersion.stamp(diamondSword(), SERVER);

        TagBase stamp = item.getCompoundData().get("DataVersion");
        assertInstanceOf(TagInt.class, stamp, "MyPet 4 reads DataVersion as a number");
        assertEquals(SERVER, ((TagInt) stamp).getIntData());
        assertEquals(SERVER, ItemDataVersion.read(item));
    }

    @Test
    void removingTheStampGivesBackTheSavedItemUnchanged() {
        TagCompound original = diamondSword();
        TagCompound stamped = ItemDataVersion.stamp(diamondSword(), SERVER);

        TagCompound loaded = ItemDataVersion.withoutStamp(stamped);

        assertEquals(original.getCompoundData().keySet(), loaded.getCompoundData().keySet());
        assertEquals(original.toString(), loaded.toString());
        assertTrue(stamped.getCompoundData().containsKey("DataVersion"), "the saved compound itself keeps its stamp");
    }

    @Test
    void unstampedItemIsReportedAsUnstampedAndLeftAsItWas() {
        TagCompound item = diamondSword();

        assertEquals(ItemDataVersion.NONE, ItemDataVersion.read(item));
        assertEquals(item.toString(), ItemDataVersion.withoutStamp(item).toString());
    }

    @Test
    void airIsNeverStamped() {
        TagCompound air = new TagCompound();
        air.getCompoundData().put("id", new TagString("minecraft:air"));
        air.getCompoundData().put("count", new TagInt(1));

        ItemDataVersion.stamp(air, SERVER);

        assertFalse(air.getCompoundData().containsKey("DataVersion"));
    }

    @Test
    void preFlatteningNumericIdIsNeverStamped() {
        TagCompound legacy = new TagCompound();
        legacy.getCompoundData().put("id", new TagShort((short) 276));
        legacy.getCompoundData().put("Count", new TagByte((byte) 1));

        ItemDataVersion.stamp(legacy, SERVER);

        assertFalse(legacy.getCompoundData().containsKey("DataVersion"));
    }

    @Test
    void fixersStartAtTheStoredVersionButNeverPastTheServer() {
        assertEquals(2586, ItemDataVersion.startVersion(2586, SERVER));
        assertEquals(SERVER, ItemDataVersion.startVersion(SERVER, SERVER));
        assertEquals(SERVER, ItemDataVersion.startVersion(SERVER + 100, SERVER), "an item from a newer server is not fixed backwards");
    }

    @Test
    void otherNumericStampTypesAreReadAndNonsenseIsIgnored() {
        TagCompound item = diamondSword();
        item.getCompoundData().put("DataVersion", new TagShort((short) 3955));
        assertEquals(3955, ItemDataVersion.read(item));

        item.getCompoundData().put("DataVersion", new TagString("3955"));
        assertEquals(ItemDataVersion.NONE, ItemDataVersion.read(item));

        item.getCompoundData().put("DataVersion", new TagInt(-5));
        assertEquals(ItemDataVersion.NONE, ItemDataVersion.read(item));
    }
}
