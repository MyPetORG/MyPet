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

package de.Keyle.MyPet.compat.v1_16_R3.util;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.api.util.inventory.material.MaterialHolder;
import de.Keyle.MyPet.compat.v1_16_R3.util.inventory.ItemStackComparator;
import net.minecraft.server.v1_16_R3.*;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

@Compat("v1_16_R3")
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

	@Override
	public boolean compare(Object o) {
		net.minecraft.server.v1_16_R3.ItemStack compareItem = (net.minecraft.server.v1_16_R3.ItemStack) o;
		return this.compare(CraftItemStack.asCraftMirror(compareItem));
	}

	@Override
	public void load(MaterialHolder material, String data) {
		MinecraftKey key = new MinecraftKey(material.getId());
		Item item = IRegistry.ITEM.get(key);
		//TODO AIR now?
		if (item == null) {
			Block block = IRegistry.BLOCK.get(key);
			item = block.getItem();
		}
		if (item == null) {
			return;
		}

		net.minecraft.server.v1_16_R3.ItemStack is = new net.minecraft.server.v1_16_R3.ItemStack(item, 1);

		if (data != null) {
			NBTTagCompound tag = null;
			String nbtString = data.trim();
			if (nbtString.startsWith("{") && nbtString.endsWith("}")) {
				try {
					tag = MojangsonParser.parse(nbtString);
				} catch (Exception e) {
					MyPetApi.getLogger().warning("Error" + ChatColor.RESET + " in config: " + ChatColor.UNDERLINE + e.getLocalizedMessage() + ChatColor.RESET + " caused by:");
					MyPetApi.getLogger().warning(item.getName() + " " + nbtString);
				}
				if (tag != null) {
					is.setTag(tag);
				}
			}
		}

		this.item = CraftItemStack.asCraftMirror(is);
	}
}