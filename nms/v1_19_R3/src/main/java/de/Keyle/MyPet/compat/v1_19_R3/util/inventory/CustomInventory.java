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

package de.Keyle.MyPet.compat.v1_19_R3.util.inventory;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.util.Compat;
import de.keyle.knbt.TagByte;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagList;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

@Compat("v1_19_R3")
public class CustomInventory extends de.Keyle.MyPet.api.util.inventory.CustomInventory {

    @Override
    public TagCompound save(TagCompound compound) {
        List<TagCompound> itemList = new ArrayList<>();
        for (int i = 0; i < getSize(); i++) {
            ItemStack itemStack = getItem(i);
            if (itemStack != null && !itemStack.getType().isAir()) {
                TagCompound item = ItemStackNBTConverter.itemStackToCompound(itemStack);
                item.getCompoundData().put("Slot", new TagByte((byte) i));
                itemList.add(item);
            }
        }
        compound.getCompoundData().put("Items", new TagList(itemList));
        return compound;
    }

    @Override
    public void load(TagCompound nbtTagCompound) {
        TagList items = nbtTagCompound.getAs("Items", TagList.class);
        if (items == null) return;

        for (int i = 0; i < items.size(); i++) {
            TagCompound itemCompound = items.getTagAs(i, TagCompound.class);
            TagByte slotTag = itemCompound.getAs("Slot", TagByte.class);
            if (slotTag == null) {
                MyPetApi.getLogger().warning("Removed invalid item from pet inventory: missing Slot tag");
                continue;
            }

            int slot = slotTag.getByteData();

            // Skip items saved in incompatible BukkitItem format (from snapshot builds)
            if (itemCompound.containsKey("BukkitItem")) {
                MyPetApi.getLogger().warning("Removed item in slot " + slot + ": incompatible format");
                setItem(slot, null);
                continue;
            }

            try {
                ItemStack bukkitItem = ItemStackNBTConverter.compoundToBukkitItemStack(itemCompound);
                setItem(slot, bukkitItem);
            } catch (Exception e) {
                MyPetApi.getLogger().warning("Removed invalid item from slot " + slot + ": " + e.getMessage());
                setItem(slot, null);
            }
        }
    }
}