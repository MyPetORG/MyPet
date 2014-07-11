/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2014 Keyle
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

package de.Keyle.MyPet.skill.skills.implementation.inventory;

import org.bukkit.craftbukkit.v1_7_R4.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;

public class ItemStackComparator {
    public static boolean compareItem(ItemStack i1, ItemStack i2) {
        return compareItemType(i1, i2) && compareEnchantments(i1, i2) && compareMetaData(i1, i2);
    }

    public static boolean compareItemType(ItemStack i1, ItemStack i2) {
        if (i1.getTypeId() != i2.getTypeId()) {
            //MyPetLogger.write("TypID: " + i1.getTypeId() + "<->" + i2.getTypeId());
            return false;
        }
        if (i1.getData().getData() != i2.getData().getData()) {
            //MyPetLogger.write("Data: " + i1.getData().getData() + "<->" + i2.getData().getData());
            return false;
        }
        return true;
    }

    public static boolean compareEnchantments(ItemStack i1, ItemStack i2) {
        if (i1.getEnchantments().size() == i2.getEnchantments().size()) {
            Map<Enchantment, Integer> e1 = i1.getEnchantments();
            Map<Enchantment, Integer> e2 = i2.getEnchantments();
            Enchantment[] e1l = new Enchantment[e1.size()];

            int i = 0;
            for (Enchantment enchantment : e1.keySet()) {
                e1l[i++] = enchantment;
            }
            i = 0;
            for (Enchantment enchantment : e2.keySet()) {
                if (e1l[i].getId() != enchantment.getId()) {
                    //MyPetLogger.write("enchantment: " + e1l[i].getId() + "<->" + enchantment.getId());
                    return false;
                } else if (!e1.get(e1l[i]).equals(e2.get(enchantment))) {
                    //MyPetLogger.write("level: " + e1.get(e1l[i]) + "<->" + e2.get(enchantment));
                    return false;
                }
                i++;
            }
            return true;
        }
        //MyPetLogger.write("size: " + i1.getEnchantments().size() + "<->" + i2.getEnchantments().size());
        return false;
    }

    public static boolean compareMetaData(ItemStack i1, ItemStack i2) {
        if (i1.hasItemMeta() == i2.hasItemMeta()) {
            ItemMeta m1 = i1.getItemMeta();
            ItemMeta m2 = i2.getItemMeta();

            if (m1.hasDisplayName() != m2.hasDisplayName()) {
                //MyPetLogger.write("has displayname: " + m1.hasDisplayName() + "<->" + m2.hasDisplayName());
                return false;
            }
            if (m1.hasDisplayName() && !m1.getDisplayName().equals(m2.getDisplayName())) {
                //MyPetLogger.write("displayname: " + m1.getDisplayName() + "<->" + m2.getDisplayName());
                return false;
            }

            if (m1.hasLore() != m2.hasLore()) {
                //MyPetLogger.write("has lore: " + m1.hasLore() + "<->" + m2.hasLore());
                return false;
            }
            if (m1.hasLore()) {
                List<String> l1 = m1.getLore();
                List<String> l2 = m2.getLore();
                if (l1.size() != l2.size()) {
                    //MyPetLogger.write("lore size: " + m1.getLore().size() + "<->" + m2.getLore().size());
                    return false;
                }
                for (int i = 0; i < l1.size(); i++) {
                    if (!l1.get(i).equals(l2.get(i))) {
                        //MyPetLogger.write("lore: " + l1.get(i) + "<->" + l2.get(i));
                        return false;
                    }
                }
            }
            return true;
        }
        //MyPetLogger.write("has: " + i1.hasItemMeta() + "<->" + i2.hasItemMeta());
        return false;
    }

    public static boolean compareTagData(ItemStack i1, ItemStack i2) {
        if (i1.hasItemMeta() == i2.hasItemMeta()) {
            return CraftItemStack.asNMSCopy(i1).tag.equals(CraftItemStack.asNMSCopy(i2).tag);
        }
        return false;
    }
}