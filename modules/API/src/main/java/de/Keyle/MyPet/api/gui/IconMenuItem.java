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

package de.Keyle.MyPet.api.gui;

import de.Keyle.MyPet.api.util.inventory.meta.IconMeta;
import de.keyle.knbt.TagBase;
import de.keyle.knbt.TagCompound;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class IconMenuItem implements Cloneable {

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
        if (material != null && this.material != material) {
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
        amount = Math.max(1, amount);
        if (this.amount != amount) {
            this.amount = amount;
            hasChanged = true;
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    public IconMenuItem setMeta(ItemMeta meta, boolean useTitle, boolean useLore) {
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
        if (title != null && !this.title.equals(title)) {
            this.title = title;
            hasChanged = true;
        }
        return this;
    }

    public IconMenuItem setLore(String... lore) {
        if (lore != null) {
            this.lore.clear();
            Collections.addAll(this.lore, lore);
            hasChanged = true;
        }
        return this;
    }

    public IconMenuItem addLoreLine(String line) {
        if (line != null) {
            if (line.contains("\n")) {
                Collections.addAll(this.lore, line.split("\n"));
            } else {
                this.lore.add(line);
            }
            hasChanged = true;
        }
        return this;
    }

    public IconMenuItem addLoreLine(String line, int position) {
        if (line != null && position >= 0) {
            if (line.contains("\n")) {
                List<String> lore = new LinkedList<>();
                Collections.addAll(lore, line.split("\n"));
                Collections.reverse(lore);
                for (String l : lore) {
                    this.lore.add(position, l);
                }
            } else {
                this.lore.add(position, line);
            }
            hasChanged = true;
        }
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
        if (lore != null && lore.size() > 0) {
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

    public IconMenuItem clone() {
        IconMenuItem newItem = new IconMenuItem();
        newItem.material = this.material;
        newItem.data = this.data;
        newItem.amount = this.amount;
        newItem.title = this.title;
        newItem.lore.addAll(this.lore);
        newItem.glowing = this.glowing;
        if (this.bukkitMeta != null) {
            newItem.bukkitMeta = this.bukkitMeta.clone();
        }
        if (this.meta != null) {
            newItem.meta = this.meta.clone();
        }
        if (this.tag != null) {
            newItem.tag = this.tag.clone();
        }
        newItem.hasChanged = true;

        return newItem;
    }
}