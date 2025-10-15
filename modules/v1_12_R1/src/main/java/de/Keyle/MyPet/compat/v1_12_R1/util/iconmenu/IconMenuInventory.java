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

package de.Keyle.MyPet.compat.v1_12_R1.util.iconmenu;

import de.Keyle.MyPet.api.gui.IconMenu;
import de.Keyle.MyPet.api.gui.IconMenuItem;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.api.util.ReflectionUtil;
import de.Keyle.MyPet.compat.v1_12_R1.util.inventory.CustomInventory;
import de.Keyle.MyPet.compat.v1_12_R1.util.inventory.ItemStackNBTConverter;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagList;
import de.keyle.knbt.TagShort;
import net.minecraft.server.v1_12_R1.ItemStack;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import net.minecraft.server.v1_12_R1.NBTTagList;
import net.minecraft.server.v1_12_R1.NBTTagString;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Compat("v1_12_R1")
public class IconMenuInventory implements de.Keyle.MyPet.api.gui.IconMenuInventory {

    private static Method applyToItemMethhod = null;

    static {
        try {
            Class craftMetaItemClass = Class.forName("org.bukkit.craftbukkit.v1_12_R1.inventory.CraftMetaItem");
            applyToItemMethhod = ReflectionUtil.getMethod(craftMetaItemClass, "applyToItem", NBTTagCompound.class);
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
        player.openInventory(minecraftInventory.getBukkitInventory());
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
                    minecraftInventory.setItem(slot, ItemStack.a);
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

    protected ItemStack createItemStack(IconMenuItem icon) {
        ItemStack is = CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(icon.getMaterial(), icon.getAmount(), (short) icon.getData()));
        if (is == null) {
            is = CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(Material.SAPLING));
        }

        NBTTagList emptyList = new NBTTagList();
        if (is.getTag() == null) {
            is.setTag(new NBTTagCompound());
        }

        if (icon.getBukkitMeta() != null) {
            try {
                applyToItemMethhod.invoke(icon.getBukkitMeta(), is.getTag());
            } catch (InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        // remove item attributes like attack damage
        is.getTag().set("AttributeModifiers", emptyList);

        //add enchantment glowing
        if (icon.isGlowing()) {
            TagCompound enchTag = new TagCompound();
            enchTag.put("id", new TagShort(2));
            enchTag.put("lvl", new TagShort(1));
            TagList enchList = new TagList();
            enchList.addTag(enchTag);

            is.getTag().set("ench", ItemStackNBTConverter.compoundToVanillaCompound(enchList));
            is.getTag().setInt("HideFlags", 1);
        } else {
            is.getTag().remove("ench");
        }

        // Prepare display tag
        NBTTagCompound display;
        if (is.getTag().hasKey("display")) {
            display = is.getTag().getCompound("display");
        } else {
            display = new NBTTagCompound();
            is.getTag().set("display", display);
        }

        // set Title
        if (!icon.getTitle().isEmpty()) {
            display.setString("Name", icon.getTitle());
        }

        if (!icon.getLore().isEmpty()) {
            // set Lore
            NBTTagList loreTag = new NBTTagList();
            display.set("Lore", loreTag);
            for (String loreLine : icon.getLore()) {
                loreTag.add(new NBTTagString(loreLine));
            }
        }

        if (icon.hasMeta()) {
            TagCompound tag = new TagCompound();
            icon.getMeta().applyTo(tag);
            NBTTagCompound vanillaTag = (NBTTagCompound) ItemStackNBTConverter.compoundToVanillaCompound(tag);
            for (String key : vanillaTag.c()) {
                is.getTag().set(key, vanillaTag.get(key));
            }
        }

        if (icon.getTags() != null) {
            NBTTagCompound vanillaTag = (NBTTagCompound) ItemStackNBTConverter.compoundToVanillaCompound(icon.getTags());
            for (String key : vanillaTag.c()) {
                is.getTag().set(key, vanillaTag.get(key));
            }
        }

        return is;
    }
}