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

package de.Keyle.MyPet.api.util;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.util.inventory.material.ItemDatabase;
import de.Keyle.MyPet.api.util.inventory.material.MaterialHolder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public abstract class ConfigItem {

    protected ItemStack item = null;
    protected DurabilityMode durabilityMode = DurabilityMode.NotUsed;

    public enum DurabilityMode {
        Smaller, Bigger, NotUsed, Equal
    }

    public ConfigItem(ItemStack item, DurabilityMode durabilityMode) {
        this.item = item;
        this.durabilityMode = durabilityMode;
    }

    public ConfigItem(String data) {
        String[] splitData = data.split("\\s+", 2);

        if (splitData.length == 0) {
            return;
        }
        if (Util.isInt(splitData[0])) {
            MyPetApi.getLogger().warning("Number IDs are not supported anymore! You need to use 1.13 item IDs from now on. Please check your configs.");
            return;
        }

        ItemDatabase itemDatabase = MyPetApi.getServiceManager().getService(ItemDatabase.class).get();
        MaterialHolder material = itemDatabase.getByID(splitData[0]);
        if (material == null) {
            MyPetApi.getLogger().warning(splitData[0] + " is not a valid 1.13 item ID! Please check your configs.");
            return;
        }

        load(material, splitData.length == 2 ? splitData[1] : null);
    }

    public static ConfigItem createConfigItem(String data) {
        return MyPetApi.getCompatUtil().getComapatInstance(ConfigItem.class, "util", "ConfigItem", data);
    }

    public static ConfigItem createConfigItem(ItemStack item, DurabilityMode durabilityMode) {
        return MyPetApi.getCompatUtil().getComapatInstance(ConfigItem.class, "util", "ConfigItem", item, durabilityMode);
    }

    public boolean compare(ItemStack compareItem) {
        if (item == null || item.getType() == Material.AIR) {
            return compareItem == null || compareItem.getType() == Material.AIR;
        }
        if (compareItem == null) {
            return false;
        }
        if (item.getType() != compareItem.getType()) {
            return false;
        }
        switch (durabilityMode) {
            case Bigger:
                if (compareItem.getDurability() <= item.getDurability()) {
                    return false;
                }
                break;
            case Smaller:
                if (compareItem.getDurability() >= item.getDurability()) {
                    return false;
                }
                break;
            case Equal:
                if (compareItem.getDurability() != item.getDurability()) {
                    return false;
                }
                break;
        }
        return true;
    }

    public ItemStack getItem() {
        return item;
    }

    public DurabilityMode getDurabilityMode() {
        return durabilityMode;
    }

    public String toString() {
        return "ConfigItem{mode: " + durabilityMode.name() + ", item: " + item + "}";
    }

    public abstract boolean compare(Object compareItem);

    public abstract void load(MaterialHolder material, String data);
}