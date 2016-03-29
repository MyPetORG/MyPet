/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2016 Keyle
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

package de.Keyle.MyPet.compat.v1_8_R1.util;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.compat.v1_8_R1.util.inventory.ItemStackComparator;
import net.minecraft.server.v1_8_R1.Item;
import net.minecraft.server.v1_8_R1.MojangsonParser;
import net.minecraft.server.v1_8_R1.NBTBase;
import net.minecraft.server.v1_8_R1.NBTTagCompound;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_8_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

public class ConfigItem extends de.Keyle.MyPet.api.util.ConfigItem {

    public ConfigItem(ItemStack item, DurabilityMode durabilityMode) {
        super(item, durabilityMode);
    }

    public ConfigItem(String data) {
        super(data);
    }

    @Override
    public boolean compare(ItemStack compareItem) {
        boolean result = super.compare(compareItem);
        if (result && item.hasItemMeta()) {
            if (!ItemStackComparator.compareTagData(item, compareItem)) {
                return false;
            }
        }
        return result;
    }

    public boolean compare(Object o) {
        net.minecraft.server.v1_8_R1.ItemStack compareItem = (net.minecraft.server.v1_8_R1.ItemStack) o;
        if (item == null || item.getTypeId() == 0) {
            if (compareItem == null || Item.getId(compareItem.getItem()) == 0) {
                return true;
            } else {
                return false;
            }
        }
        if (compareItem == null) {
            return false;
        }
        if (item.getTypeId() != Item.getId(compareItem.getItem())) {
            return false;
        }
        switch (durabilityMode) {
            case Bigger:
                if (compareItem.getData() <= item.getDurability()) {
                    return false;
                }
                break;
            case Smaller:
                if (compareItem.getData() >= item.getDurability()) {
                    return false;
                }
                break;
            case Equal:
                if (compareItem.getData() != item.getDurability()) {
                    return false;
                }
                break;
        }
        if (item.hasItemMeta()) {
            return CraftItemStack.asNMSCopy(item).getTag().equals(compareItem.getTag());
        }
        return true;
    }

    public void load(String data) {
        NBTBase nbtBase = null;
        if (data.contains("{")) {
            String tagString = data.substring(data.indexOf("{"));
            data = data.substring(0, data.indexOf("{"));
            try {
                nbtBase = MojangsonParser.parse(tagString);
            } catch (Exception e) {
                MyPetApi.getLogger().warning(ChatColor.RED + "Error" + ChatColor.RESET + " in config: " + ChatColor.YELLOW + e.getLocalizedMessage() + ChatColor.RESET + " caused by:");
                MyPetApi.getLogger().warning(data + tagString);
            }
        }

        String[] splitData = data.split("\\s+");

        int itemId = 1;
        int itemDamage = 0;

        if (splitData.length == 0) {
            return;
        }
        if (splitData.length >= 1) {
            if (Util.isInt(splitData[0])) {
                itemId = Integer.parseInt(splitData[0]);
            }
        }
        if (itemId != 0) {
            if (splitData.length >= 2) {
                if (splitData[1].startsWith("<")) {
                    this.durabilityMode = DurabilityMode.Smaller;
                    splitData[1] = splitData[1].substring(1);
                } else if (splitData[1].startsWith(">")) {
                    this.durabilityMode = DurabilityMode.Bigger;
                    splitData[1] = splitData[1].substring(1);
                } else {
                    this.durabilityMode = DurabilityMode.Equal;
                }
                if (Util.isInt(splitData[1])) {
                    itemDamage = Integer.parseInt(splitData[1]);
                }
            }

            net.minecraft.server.v1_8_R1.ItemStack is = new net.minecraft.server.v1_8_R1.ItemStack(Item.getById(itemId), 1, itemDamage);
            if (nbtBase != null) {
                is.setTag((NBTTagCompound) nbtBase);
            }

            item = CraftItemStack.asBukkitCopy(is);
        }
    }
}