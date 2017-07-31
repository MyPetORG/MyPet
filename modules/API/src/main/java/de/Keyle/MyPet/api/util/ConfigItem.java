/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2017 Keyle
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
        load(data);
    }

    public static ConfigItem createConfigItem(String data) {
        return MyPetApi.getCompatUtil().getComapatInstance(ConfigItem.class, "util", "ConfigItem", data);
    }

    public static ConfigItem createConfigItem(ItemStack item, DurabilityMode durabilityMode) {
        return MyPetApi.getCompatUtil().getComapatInstance(ConfigItem.class, "util", "ConfigItem", item, durabilityMode);
    }

    public boolean compare(ItemStack compareItem) {
        if (item == null || item.getTypeId() == 0) {
            if (compareItem == null || compareItem.getTypeId() == 0) {
                return true;
            } else {
                return false;
            }
        }
        if (compareItem == null) {
            return false;
        }
        if (item.getTypeId() != compareItem.getTypeId()) {
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

    public abstract void load(String data);
}