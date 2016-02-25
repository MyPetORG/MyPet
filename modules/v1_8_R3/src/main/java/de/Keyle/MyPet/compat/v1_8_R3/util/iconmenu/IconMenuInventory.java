/*
 * This file is part of mypet-v1_8_R3
 *
 * Copyright (C) 2011-2016 Keyle
 * mypet-v1_8_R3 is licensed under the GNU Lesser General Public License.
 *
 * mypet-v1_8_R3 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mypet-v1_8_R3 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.compat.v1_8_R3.util.iconmenu;

import de.Keyle.MyPet.api.util.inventory.IconMenu;
import de.Keyle.MyPet.api.util.inventory.IconMenuItem;
import net.minecraft.server.v1_8_R3.ItemStack;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.NBTTagList;
import net.minecraft.server.v1_8_R3.NBTTagString;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class IconMenuInventory implements de.Keyle.MyPet.api.util.inventory.IconMenuInventory {
    private static Method applyToItemMethhod = null;

    static {
        try {
            Class craftMetaItemClass = Class.forName("org.bukkit.craftbukkit.v1_8_R3.inventory.CraftMetaItem");
            applyToItemMethhod = craftMetaItemClass.getDeclaredMethod("applyToItem", NBTTagCompound.class);
            applyToItemMethhod.setAccessible(true);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    Inventory bukkitInventory = null;
    int size = 0;

    @Override
    public void open(IconMenu menu, HumanEntity player) {
        if (bukkitInventory == null) {
            size = menu.getSize();
            bukkitInventory = Bukkit.getServer().createInventory(null, size, menu.getTitle());

            for (int slot = 0; slot < size; slot++) {
                IconMenuItem menuItem = menu.getOption(slot);
                org.bukkit.inventory.ItemStack item = createNmsItemStack(menuItem);
                bukkitInventory.setItem(slot, item);
            }
        }
    }

    @Override
    public void update(IconMenu menu) {
        if (bukkitInventory != null) {
            for (int slot = 0; slot < size; slot++) {
                IconMenuItem menuItem = menu.getOption(slot);
                org.bukkit.inventory.ItemStack item = createNmsItemStack(menuItem);
                bukkitInventory.setItem(slot, item);
            }
        }
    }

    @Override
    public void close() {
        if (bukkitInventory != null) {
            for (HumanEntity viewer : bukkitInventory.getViewers()) {
                viewer.closeInventory();
            }
            bukkitInventory = null;
        }
    }


    protected org.bukkit.inventory.ItemStack createNmsItemStack(IconMenuItem icon) {
        ItemStack is = CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(icon.getMaterial(), icon.getAmount(), (short) icon.getData()));

        NBTTagList emptyList = new NBTTagList();
        if (is.getTag() == null) {
            is.setTag(new NBTTagCompound());
        }

        if (icon.getMeta() != null) {
            try {
                applyToItemMethhod.invoke(icon.getMeta(), is.getTag());
            } catch (InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        // remove item attributes like attack damage
        is.getTag().set("AttributeModifiers", emptyList);

        //add enchantment glowing
        if (icon.isGlowing()) {
            is.getTag().set("ench", emptyList);
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
        if (!icon.getTitle().equals("")) {
            display.setString("Name", icon.getTitle());
        }

        if (icon.getLore().size() > 0) {
            // set Lore
            NBTTagList loreTag = new NBTTagList();
            display.set("Lore", loreTag);
            for (String loreLine : icon.getLore()) {
                loreTag.add(new NBTTagString(loreLine));
            }
        }

        return CraftItemStack.asCraftMirror(is);
    }

    @Override
    public boolean isMenuInventory(Inventory inv) {
        return bukkitInventory != null && bukkitInventory.equals(inv);
    }

    @Override
    public List<HumanEntity> getViewers() {
        return bukkitInventory != null ? bukkitInventory.getViewers() : new ArrayList<HumanEntity>();
    }

    @Override
    public int getSize() {
        return size;
    }
}