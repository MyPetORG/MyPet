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

package de.Keyle.MyPet.compat.v1_7_R4.util;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.api.util.inventory.material.MaterialHolder;
import de.Keyle.MyPet.compat.v1_7_R4.util.inventory.ItemStackComparator;
import net.minecraft.server.v1_7_R4.Item;
import net.minecraft.server.v1_7_R4.MojangsonParser;
import net.minecraft.server.v1_7_R4.NBTBase;
import net.minecraft.server.v1_7_R4.NBTTagCompound;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_7_R4.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

@Compat("v1_7_R4")
public class ConfigItem extends de.Keyle.MyPet.api.util.ConfigItem {

    public ConfigItem(ItemStack item, DurabilityMode durabilityMode) {
        super(item, durabilityMode);
    }

    public ConfigItem(String data) {
        super(data);
    }

    @Override
    public boolean compare(ItemStack compareItem) {
        return super.compare(compareItem) && ItemStackComparator.compareTagData(item, compareItem);
    }

    public boolean compare(Object o) {
        net.minecraft.server.v1_7_R4.ItemStack compareItem = (net.minecraft.server.v1_7_R4.ItemStack) o;
        return this.compare(CraftItemStack.asCraftMirror(compareItem));
    }

    public void load(MaterialHolder material, String data) {
        Item item = (Item) Item.REGISTRY.get(material.getLegacyName().getName());
        if (item == null) {
            return;
        }

        net.minecraft.server.v1_7_R4.ItemStack is = new net.minecraft.server.v1_7_R4.ItemStack(item, 1, material.getLegacyName().getData());

        if (data != null) {
            NBTBase tag = null;
            String nbtString = data.trim();
            if (nbtString.startsWith("{") && nbtString.endsWith("}")) {
                String tagString = data.substring(data.indexOf("{"), data.lastIndexOf("}"));
                data = data.substring(0, data.indexOf("{"));
                try {
                    tag = MojangsonParser.parse(tagString);
                } catch (Exception e) {
                    MyPetApi.getLogger().warning("Error" + ChatColor.RESET + " in config: " + ChatColor.YELLOW + e.getLocalizedMessage() + ChatColor.RESET + " caused by:");
                    MyPetApi.getLogger().warning(data + tagString);
                }
                if (tag != null) {
                    is.setTag((NBTTagCompound) tag);
                }
            }
        }

        this.item = CraftItemStack.asCraftMirror(is);
    }
}