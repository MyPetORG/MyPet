/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2013 Keyle
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

import net.minecraft.server.v1_6_R3.*;
import org.apache.commons.lang.Validate;
import org.bukkit.Material;

import java.util.*;

public class IconMenuItem {
    protected Material material = Material.NAME_TAG;
    protected int data = 0;
    protected int amount = 0;
    protected String title = "";
    protected List<String> lore = new ArrayList<String>();
    protected boolean glowing = false;
    protected Map<String, NBTBase> displayTags = new HashMap<String, NBTBase>();

    protected ItemStack oldItemStack;
    protected boolean hasChanged = true;

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


        ItemStack is = new ItemStack(material.getId(), amount, data);

        NBTTagList emptyList = new NBTTagList();
        if (is.tag == null) {
            is.tag = new NBTTagCompound("tag");
        }

        // remove item attributes like attack damage
        is.tag.set("AttributeModifiers", emptyList);

        //add enchantment glowing
        if (glowing) {
            is.tag.set("ench", emptyList);
        } else {
            is.tag.remove("ench");
        }

        // Prepare display tag
        NBTTagCompound display;
        if (is.tag.hasKey("display")) {
            display = is.tag.getCompound("display");
        } else {
            display = new NBTTagCompound("display");
            is.tag.set("display", display);
        }

        // set Title
        if (!title.equals("")) {
            display.setString("Name", title);
        }

        // set Lore
        NBTTagList loreTag = new NBTTagList("Lore");
        display.set("Lore", loreTag);
        for (String loreLine : lore) {
            loreTag.add(new NBTTagString(null, loreLine));
        }

        // add other display properties
        for (String tagName : displayTags.keySet()) {
            display.set(tagName, displayTags.get(tagName));
        }

        oldItemStack = is;
        hasChanged = false;

        return is;
    }

    public static IconMenuItem fromNmsItemStack(ItemStack is) {
        IconMenuItem icon = new IconMenuItem();

        icon.setMaterial(Material.getMaterial(is.id));
        icon.setData(is.getData());
        icon.setAmount(is.count);

        if (is.tag != null) {

            if (is.tag.hasKey("ench")) {
                icon.setGlowing(true);
            }

            if (is.tag.hasKey("display")) {
                NBTTagCompound display = is.tag.getCompound("display");

                if (display.hasKey("Name")) {
                    icon.setTitle(display.getString("Name"));
                }

                if (display.hasKey("Lore")) {
                    NBTTagList lore = display.getList("Lore");

                    for (int i = 0; i < lore.size(); i++) {
                        NBTTagString loreLine = (NBTTagString) lore.get(i);
                        icon.addLoreLine(loreLine.data);
                    }
                }

                for (Object o : display.c()) {
                    NBTBase nbtTag = (NBTBase) o;

                    if (nbtTag.getName().equals("Name") || nbtTag.getName().equals("Lore")) {
                        continue;
                    }

                    icon.setDisplayTag(nbtTag.getName(), nbtTag);
                }
            }
        }

        return icon;
    }
}