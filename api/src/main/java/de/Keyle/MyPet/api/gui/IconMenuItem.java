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

import lombok.Getter;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Immutable-style model for a single slot in an {@link IconMenu}. Holds
 * material, title, lore, glow state, and optional NBT tags — everything
 * needed by {@link IconMenuInventory} to render an {@code ItemStack}.
 * <p>
 * Setters return {@code this} for fluent chaining. A dirty flag
 * ({@code hasChanged}) tracks whether the item needs re-rendering.
 * <p>
 * Title and lore support both legacy {@code String} and Adventure
 * {@link Component} forms. When both are set, the Component variant
 * takes precedence during rendering. Setting one clears the other.
 */
public class IconMenuItem {

    @Getter
    protected Material material = Material.NAME_TAG;
    @Getter
    protected int data = 0;
    @Getter
    protected int amount = 1;
    @Getter
    protected String title = "";
    protected final List<String> lore = new ArrayList<>();
    @Getter
    protected Component componentTitle = null;
    protected final List<Component> componentLore = new ArrayList<>();
    @Getter
    protected boolean glowing = false;
    @Getter
    protected ItemMeta bukkitMeta;
    protected CompoundBinaryTag tag;

    protected boolean hasChanged = true;

    /** Creates an {@code IconMenuItem} from an existing Bukkit item. */
    public static IconMenuItem fromItemStack(ItemStack itemStack) {
        IconMenuItem icon = new IconMenuItem();
        icon.setMaterial(itemStack.getType());
        icon.setAmount(itemStack.getAmount());
        if (itemStack.hasItemMeta()) {
            icon.setMeta(itemStack.getItemMeta(), true, true);
        }
        return icon;
    }

    /**
     * Applies fields from an existing {@link ItemMeta} onto this item.
     *
     * @param meta     the source meta
     * @param useTitle if {@code true}, copies the display name
     * @param useLore  if {@code true}, copies the lore lines
     */
    public IconMenuItem setMeta(ItemMeta meta, boolean useTitle, boolean useLore) {
        if (useTitle && meta.hasDisplayName()) {
            this.componentTitle = meta.displayName();
            this.title = "";
            hasChanged = true;
        }
        if (useLore && meta.hasLore()) {
            List<Component> metaLore = meta.lore();
            this.componentLore.clear();
            if (metaLore != null) this.componentLore.addAll(metaLore);
            this.lore.clear();
            hasChanged = true;
        }
        if (this.bukkitMeta != meta) {
            this.bukkitMeta = meta;
            hasChanged = true;
        }

        return this;
    }

    /** Appends a legacy lore line (supports embedded {@code \n}). */
    public IconMenuItem addLoreLine(String line) {
        if (line != null) {
            if (!this.componentLore.isEmpty()) this.componentLore.clear();
            if (line.contains("\n")) {
                Collections.addAll(this.lore, line.split("\n"));
            } else {
                this.lore.add(line);
            }
            hasChanged = true;
        }
        return this;
    }

    /** Inserts a legacy lore line at the given index. */
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

    /** Appends all lines from the given list to the legacy lore. */
    public IconMenuItem addLore(List<String> lore) {
        if (lore != null && !lore.isEmpty()) {
            this.lore.addAll(lore);
            hasChanged = true;
        }
        return this;
    }

    /** Sets the display material for this menu slot. */
    public IconMenuItem setMaterial(Material material) {
        if (material != null && this.material != material) {
            this.material = material;
            hasChanged = true;
        }
        return this;
    }

    /** Sets the legacy data/damage value (unused on modern servers). */
    public IconMenuItem setData(int data) {
        if (this.data != data) {
            this.data = data;
            hasChanged = true;
        }
        return this;
    }

    /** Sets the stack size displayed in the slot (min 1). */
    public IconMenuItem setAmount(int amount) {
        amount = Math.max(1, amount);
        if (this.amount != amount) {
            this.amount = amount;
            hasChanged = true;
        }
        return this;
    }

    /** Sets the display name as a legacy string. Clears any Component title. */
    public IconMenuItem setTitle(String title) {
        if (title != null && !this.title.equals(title)) {
            this.title = title;
            this.componentTitle = null;
            hasChanged = true;
        }
        return this;
    }

    /** Sets the display name as an Adventure Component. Clears any legacy title. */
    public IconMenuItem setTitle(Component title) {
        if (title != null) {
            this.componentTitle = title;
            this.title = "";
            hasChanged = true;
        }
        return this;
    }

    /** Returns an unmodifiable view of the legacy lore lines. */
    public List<String> getLore() {
        return Collections.unmodifiableList(lore);
    }

    /** Returns an unmodifiable view of the Component lore lines. */
    public List<Component> getComponentLore() {
        return Collections.unmodifiableList(componentLore);
    }

    /** Appends a Component lore line. Clears any legacy lore. */
    public IconMenuItem addLoreLine(Component line) {
        if (line != null) {
            if (!this.lore.isEmpty()) this.lore.clear();
            this.componentLore.add(line);
            hasChanged = true;
        }
        return this;
    }

    /** Replaces all legacy lore with the given lines. */
    public IconMenuItem setLore(String... lore) {
        if (lore != null) {
            this.lore.clear();
            Collections.addAll(this.lore, lore);
            hasChanged = true;
        }
        return this;
    }

    /** Enables or disables the enchantment glint effect. */
    public IconMenuItem setGlowing(boolean glowing) {
        if (this.glowing != glowing) {
            this.glowing = glowing;
            hasChanged = true;
        }
        return this;
    }

    /** Adds a custom NBT tag entry to this item's tag compound. */
    public IconMenuItem addTag(String key, BinaryTag tag) {
        if (this.tag == null) {
            this.tag = CompoundBinaryTag.builder().put(key, tag).build();
        } else {
            this.tag = this.tag.put(key, tag);
        }
        return this;
    }

    /** Returns the custom NBT tags, or {@code null} if none set. */
    public CompoundBinaryTag getTags() {
        return tag;
    }

    /** Returns a deep copy of this item with independent lore lists. */
    public IconMenuItem copy() {
        IconMenuItem newItem = new IconMenuItem();
        newItem.material = this.material;
        newItem.data = this.data;
        newItem.amount = this.amount;
        newItem.title = this.title;
        newItem.lore.addAll(this.lore);
        newItem.componentTitle = this.componentTitle;
        newItem.componentLore.addAll(this.componentLore);
        newItem.glowing = this.glowing;
        if (this.bukkitMeta != null) {
            newItem.bukkitMeta = this.bukkitMeta.clone();
        }
        // CompoundBinaryTag is immutable, so we can share the reference safely
        newItem.tag = this.tag;
        newItem.hasChanged = true;

        return newItem;
    }
}