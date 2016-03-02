/*
 * This file is part of mypet-api
 *
 * Copyright (C) 2011-2016 Keyle
 * mypet-api is licensed under the GNU Lesser General Public License.
 *
 * mypet-api is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mypet-api is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.api.util.inventory;

import de.Keyle.MyPet.api.util.inventory.meta.IconMeta;
import de.keyle.knbt.*;
import org.apache.commons.lang.Validate;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IconMenuItem {
    protected Material material = Material.NAME_TAG;
    protected int data = 0;
    protected int amount = 1;
    protected String title = "";
    protected List<String> lore = new ArrayList<>();
    protected boolean glowing = false;
    protected ItemMeta bukkitMeta;
    protected IconMeta meta = null;
    protected TagCompound tag;

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

    @SuppressWarnings("unchecked")
    public IconMenuItem setMeta(ItemMeta meta, boolean useTitle, boolean useLore) {
        Validate.notNull(meta, "Name cannot be null");

        this.meta = null;

        if (useTitle && meta.hasDisplayName()) {
            this.title = meta.getDisplayName();
            hasChanged = true;
        }
        if (useLore && meta.hasLore()) {
            this.lore.clear();
            this.lore.addAll(meta.getLore());
            hasChanged = true;
        }
        if (this.bukkitMeta != meta) {
            this.bukkitMeta = meta;
            hasChanged = true;
        }

        return this;
    }

    public IconMenuItem setMeta(IconMeta meta) {
        this.meta = meta;
        this.bukkitMeta = null;
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

    public IconMeta getMeta() {
        return meta;
    }

    public boolean hasMeta() {
        return meta != null;
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

    public ItemMeta getBukkitMeta() {
        return bukkitMeta;
    }

    public IconMenuItem addTag(String key, TagBase tag) {
        if (this.tag == null) {
            this.tag = new TagCompound();
        }
        this.tag.put(key, tag);
        return this;
    }

    public TagCompound getTags() {
        return tag;
    }

    public static IconMenuItem fromItemStack(ItemStack itemStack) {
        IconMenuItem icon = new IconMenuItem();
        icon.setMaterial(itemStack.getType());
        icon.setAmount(itemStack.getAmount());
        if (itemStack.hasItemMeta()) {
            icon.setMeta(itemStack.getItemMeta(), true, true);
        }
        return icon;
    }

    public static IconMenuItem fromTagCompund(TagCompound tag) {
        IconMenuItem icon = new IconMenuItem();
        int id = tag.getAs("id", TagShort.class).getShortData();
        int count = tag.getAs("Count", TagByte.class).getByteData();
        short damage = tag.getAs("Damage", TagShort.class).getShortData();

        icon.setMaterial(Material.getMaterial(id));
        icon.setAmount(count);
        icon.setData(damage);

        if (tag.containsKeyAs("tag", TagCompound.class)) {
            TagCompound metaTag = tag.get("tag");
            if (metaTag.containsKey("ench")) {
                icon.setGlowing(true);
            }
            if (metaTag.containsKey("display")) {
                TagCompound displayTag = metaTag.get("display");
                if (displayTag.containsKey("Name")) {
                    icon.setTitle(displayTag.getAs("Name", TagString.class).getStringData());
                }
                if (displayTag.containsKey("Lame")) {
                    TagList loreList = displayTag.getAs("Lame", TagList.class);
                    List<TagString> lines = loreList.getListAs(TagString.class);
                    for (TagString line : lines) {
                        icon.addLoreLine(line.getStringData());
                    }
                }
            }
        }
        return icon;
    }
}