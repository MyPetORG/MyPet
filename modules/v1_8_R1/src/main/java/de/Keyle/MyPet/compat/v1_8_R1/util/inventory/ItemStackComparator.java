/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
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

package de.Keyle.MyPet.compat.v1_8_R1.util.inventory;

import de.Keyle.MyPet.api.util.Compat;
import net.minecraft.server.v1_8_R1.NBTTagCompound;
import org.bukkit.craftbukkit.v1_8_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

@Compat("v1_8_R1")
public class ItemStackComparator {
    public static boolean compareItem(ItemStack i1, ItemStack i2) {
        if (i1 == null || i2 == null) {
            return false;
        }
        return compareItemType(i1, i2) && compareTagData(i1, i2);
    }

    public static boolean compareItemType(ItemStack i1, ItemStack i2) {
        if (i1 == null || i2 == null) {
            return false;
        }
        if (i1.getData().getItemType() != i2.getData().getItemType()) {
            return false;
        }
        if (i1.getData().getData() != i2.getData().getData()) {
            return false;
        }
        return true;
    }

    public static boolean compareTagData(ItemStack i1, ItemStack i2) {
        if (i1 == null || i2 == null) {
            return false;
        }
        if (i1.hasItemMeta() && i2.hasItemMeta()) {
            NBTTagCompound tag1 = CraftItemStack.asNMSCopy(i1).getTag();
            NBTTagCompound tag2 = CraftItemStack.asNMSCopy(i2).getTag();

            if (tag1 != null) {
                if (tag1.equals(tag2)) {
                    return true;
                } else {
                    i1 = CraftItemStack.asBukkitCopy(CraftItemStack.asNMSCopy(i1));
                    tag1 = CraftItemStack.asNMSCopy(i1).getTag();
                    return tag1.equals(tag2);
                }
            }
            return false;
        }
        return i1.hasItemMeta() == i2.hasItemMeta();
    }
}