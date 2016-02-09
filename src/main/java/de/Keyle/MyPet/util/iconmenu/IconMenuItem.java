/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2016 Keyle
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

package de.Keyle.MyPet.util.iconmenu;

import de.Keyle.MyPet.util.logger.DebugLogger;
import net.minecraft.server.v1_8_R3.*;
import org.apache.commons.lang.Validate;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.util.CraftMagicNumbers;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class IconMenuItem {
    protected Material material = Material.NAME_TAG;
    protected int data = 0;
    protected int amount = 1;
    protected String title = "";
    protected List<String> lore = new ArrayList<>();
    protected boolean glowing = false;
    protected Map<String, NBTBase> displayTags = new HashMap<>();

    protected ItemStack oldItemStack;
    protected boolean hasChanged = true;
    private Method applyToItemMethhod = null;

    public IconMenuItem setMaterial(Material material) {
        Validate.notNull(material, "Material cannot be null");
        if (this.material != material) {
            this.material = material;
            hasChanged = true;
        }
        return this;
    }

    public IconMenuItem setData(int data) {
        if (this.data != data) {
            this.data = data;
            hasChanged = true;
        }
        return this;
    }

    public IconMenuItem setAmount(int amount) {
        Validate.isTrue(amount >= 0, "Amount must be greater than 0");
        if (this.amount != amount) {
            this.amount = amount;
            hasChanged = true;
        }
        return this;
    }

    public IconMenuItem setDisplayTag(String name, NBTBase tag) {
        Validate.notNull(name, "Name cannot be null");
        Validate.notNull(tag, "Tag cannot be null");
        Validate.isTrue(!name.equals(""), "Name can not be empty");
        this.displayTags.put(name, tag);
        hasChanged = true;
        return this;
    }

    @SuppressWarnings("unchecked")
    public IconMenuItem setMeta(ItemMeta meta, boolean useTitle, boolean useLore) {
        Validate.notNull(meta, "Name cannot be null");

        if (useTitle && meta.hasDisplayName()) {
            this.title = meta.getDisplayName();
            hasChanged = true;
        }
        if (useLore && meta.hasLore()) {
            this.lore.clear();
            this.lore.addAll(meta.getLore());
            hasChanged = true;
        }

        if (applyToItemMethhod == null) {
            try {
                Class craftMetaItemClass = Class.forName("org.bukkit.craftbukkit.v1_8_R3.inventory.CraftMetaItem");
                applyToItemMethhod = craftMetaItemClass.getDeclaredMethod("applyToItem", NBTTagCompound.class);
                applyToItemMethhod.setAccessible(true);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                DebugLogger.printThrowable(e);
                return this;
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                DebugLogger.printThrowable(e);
                return this;
            }
        }
        try {
            NBTTagCompound compound = new NBTTagCompound();
            applyToItemMethhod.invoke(meta, compound);

            if (compound.hasKey("display")) {
                compound = compound.getCompound("display");

                if (compound.hasKey("Name")) {
                    compound.remove("Name");
                }
                if (compound.hasKey("Lore")) {
                    compound.remove("Lore");
                }

                for (String key : (Set<String>) compound.c()) {
                    this.displayTags.put(key, compound.get(key).clone());
                }

                hasChanged = true;
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            DebugLogger.printThrowable(e);
        }
        return this;
    }

    public IconMenuItem setTitle(String title) {
        Validate.notNull(title, "Title cannot be null");
        if (!this.title.equals(title)) {
            this.title = title;
            hasChanged = true;
        }
        return this;
    }

    public IconMenuItem setLore(String... lore) {
        Validate.notNull(lore, "Lore cannot be null");
        this.lore.clear();
        Collections.addAll(this.lore, lore);
        hasChanged = true;
        return this;
    }

    public IconMenuItem addLoreLine(String line) {
        Validate.notNull(line, "Lore line cannot be null");
        this.lore.add(line);
        hasChanged = true;
        return this;
    }

    public IconMenuItem setGlowing(boolean glowing) {
        if (this.glowing != glowing) {
            this.glowing = glowing;
            hasChanged = true;
        }
        return this;
    }

    public IconMenuItem addLore(List<String> lore) {
        Validate.notNull(lore, "Lore cannot be null");
        if (lore.size() > 0) {
            this.lore.addAll(lore);
            hasChanged = true;
        }
        return this;
    }

    public Material getMaterial() {
        return material;
    }

    public int getData() {
        return data;
    }

    public int getAmount() {
        return amount;
    }

    public String getTitle() {
        return title;
    }

    public List<String> getLore() {
        return Collections.unmodifiableList(lore);
    }

    public boolean isGlowing() {
        return glowing;
    }

    protected ItemStack createNmsItemStack() {
        if (!hasChanged) {
            return oldItemStack;
        }

        ItemStack is;
        try {
            if (material.isBlock()) {
                is = new ItemStack(CraftMagicNumbers.getBlock(material), amount, data);
            } else {
                is = new ItemStack(CraftMagicNumbers.getItem(material), amount, data);
            }
        } catch (NullPointerException e) {
            is = new ItemStack(Items.NAME_TAG);
        }

        NBTTagList emptyList = new NBTTagList();
        if (is.getTag() == null) {
            is.setTag(new NBTTagCompound());
        }

        // remove item attributes like attack damage
        is.getTag().set("AttributeModifiers", emptyList);

        //add enchantment glowing
        if (glowing) {
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
        if (!title.equals("")) {
            display.setString("Name", title);
        }

        // set Lore
        NBTTagList loreTag = new NBTTagList();
        display.set("Lore", loreTag);
        for (String loreLine : lore) {
            loreTag.add(new NBTTagString(loreLine));
        }

        // add other display properties
        for (String tagName : displayTags.keySet()) {
            display.set(tagName, displayTags.get(tagName));
        }

        oldItemStack = is;
        hasChanged = false;

        return is;
    }

    @SuppressWarnings("unchecked")
    public static IconMenuItem fromNmsItemStack(ItemStack is) {
        IconMenuItem icon = new IconMenuItem();

        icon.setMaterial(Material.getMaterial(Item.getId(is.getItem())));
        icon.setData(is.getData());
        icon.setAmount(is.count);

        if (is.getTag() != null) {

            if (is.getTag().hasKey("ench")) {
                icon.setGlowing(true);
            }

            if (is.getTag().hasKey("display")) {
                NBTTagCompound display = is.getTag().getCompound("display");

                if (display.hasKey("Name")) {
                    icon.setTitle(display.getString("Name"));
                }

                if (display.hasKey("Lore")) {
                    NBTTagList lore = display.getList("Lore", 0);

                    for (int i = 0; i < lore.size(); i++) {
                        icon.addLoreLine(lore.getString(i));
                    }
                }

                for (String key : (Set<String>) display.c()) {

                    display.remove("Name");
                    display.remove("Lore");

                    icon.setDisplayTag(key, display.get(key).clone());
                }
            }
        }
        return icon;
    }
}