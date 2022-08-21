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

package de.Keyle.MyPet.compat.v1_19_R1_2.util.iconmenu;

import de.Keyle.MyPet.api.gui.IconMenu;
import de.Keyle.MyPet.api.gui.IconMenuItem;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.api.util.ReflectionUtil;
import de.Keyle.MyPet.compat.v1_19_R1_2.util.inventory.CustomInventory;
import de.Keyle.MyPet.compat.v1_19_R1_2.util.inventory.ItemStackNBTConverter;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagList;
import de.keyle.knbt.TagShort;
import de.keyle.knbt.TagString;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_19_R1.util.CraftChatMessage;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Compat("v1_19_R1_2")
public class IconMenuInventory implements de.Keyle.MyPet.api.gui.IconMenuInventory {

    private static Method applyToItemMethod = null;

    static {
        try {
            Class<?> craftMetaItemClass = Class.forName("org.bukkit.craftbukkit.v1_19_R1.inventory.CraftMetaItem");
            applyToItemMethod = ReflectionUtil.getMethod(craftMetaItemClass, "applyToItem", CompoundTag.class);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    CustomInventory minecraftInventory;
    int size = 0;

    public CustomInventory getMinecraftInventory() {
        return minecraftInventory;
    }

    @Override
    public void open(IconMenu menu, HumanEntity player) {
        size = menu.getSize();
        minecraftInventory = new CustomInventory(size, menu.getTitle());

        for (int slot = 0; slot < size; slot++) {
            IconMenuItem menuItem = menu.getOption(slot);
            if (menuItem != null) {
                ItemStack item = createItemStack(menuItem);
                minecraftInventory.setItem(slot, item);
            }
        }
        minecraftInventory.open((Player) player);
    }

    @Override
    public void update(IconMenu menu) {
        if (minecraftInventory != null) {
            for (int slot = 0; slot < size; slot++) {
                IconMenuItem menuItem = menu.getOption(slot);
                if (menuItem != null) {
                    ItemStack item = createItemStack(menuItem);
                    minecraftInventory.setItem(slot, item);
                } else {
                    minecraftInventory.setItem(slot, ItemStack.EMPTY);
                }
            }
        }
    }

    @Override
    public void close() {
        List<HumanEntity> viewers = new ArrayList<>(getViewers());
        for (HumanEntity viewer : viewers) {
            viewer.closeInventory();
        }
        minecraftInventory = null;
    }

    @Override
    public boolean isMenuInventory(Inventory inv) {
        return minecraftInventory != null && minecraftInventory.getBukkitInventory().equals(inv);
    }

    @Override
    public List<HumanEntity> getViewers() {
        if (minecraftInventory == null) {
            return Collections.emptyList();
        }
        return minecraftInventory.getBukkitInventory().getViewers();
    }

    @Override
    public int getSize() {
        return size;
    }

    protected ItemStack createItemStack(IconMenuItem icon) { //TODO Check if this works properly
        ItemStack is = CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(icon.getMaterial(), icon.getAmount()));
        if (is == null) {
            is = CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(Material.STONE));
        }
        if (is.getTag() == null) {
            is.setTag(new CompoundTag());
        }

        if (icon.getBukkitMeta() != null) {
            try {
                applyToItemMethod.invoke(icon.getBukkitMeta(), is.getTag());
            } catch (InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        //add enchantment glowing
        if (icon.isGlowing()) {
            TagCompound enchTag = new TagCompound();
            enchTag.put("id", new TagString("minecraft:feather_falling"));
            enchTag.put("lvl", new TagShort(1));
            TagList enchList = new TagList();
            enchList.addTag(enchTag);

            is.getTag().put("Enchantments", ItemStackNBTConverter.compoundToVanillaCompound(enchList));
        } else {
            is.getTag().remove("Enchantments");
        }

        // hide item attributes like attack damage
        is.getTag().putInt("HideFlags", 63);

        // Prepare display tag
        CompoundTag display;
        if (is.getTag().contains("display")) {
            display = is.getTag().getCompound("display");
        } else {
            display = new CompoundTag();
            is.getTag().put("display", display);
        }

        // set Title
        if (!icon.getTitle().equals("")) {
            display.putString("Name", "{\"text\":\"" + icon.getTitle() + "\"}");
        }

        if (icon.getLore().size() > 0) {
            // set Lore
        	ListTag loreTag = new ListTag();
            display.put("Lore", loreTag);
            for (String loreLine : icon.getLore()) {
            	Component cm = CraftChatMessage.fromStringOrNull(loreLine);
                loreTag.add(StringTag.valueOf(Component.Serializer.toJson(cm)));
            }
        }

        if (icon.hasMeta()) {
            TagCompound tag = new TagCompound();
            icon.getMeta().applyTo(tag);
            CompoundTag vanillaTag = (CompoundTag) ItemStackNBTConverter.compoundToVanillaCompound(tag);
            for (String key : vanillaTag.getAllKeys()) {
                is.getTag().put(key, vanillaTag.get(key));
            }
        }

        if (icon.getTags() != null) {
        	CompoundTag vanillaTag = (CompoundTag) ItemStackNBTConverter.compoundToVanillaCompound(icon.getTags());
            for (String key : vanillaTag.getAllKeys()) {
                is.getTag().put(key, vanillaTag.get(key));
            }
        }

        return is;
    }
}
