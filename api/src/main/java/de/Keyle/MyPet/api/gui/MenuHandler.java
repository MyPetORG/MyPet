/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2026 Keyle
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

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Per-menu Java logic. One implementation per {@link MenuId}, registered with
 * {@link GuiService#registerMenu}.
 */
public interface MenuHandler<C> {

    MenuId<C> id();

    void onOpen(MenuInstance instance, C context);

    void onClick(MenuInstance instance, String sectionId, ClickPayload payload);

    default void onClose(MenuInstance instance, CloseReason reason) {}

    /** Items for a paginated-list section. Called every render. */
    default List<?> templateItems(C context, String sectionId) { return List.of(); }

    /** Initial contents for a storage section. Called on open. */
    default ItemStack[] storageContents(C context, String sectionId) { return new ItemStack[0]; }

    /** Persist storage contents when the menu closes. */
    default void persistStorage(C context, String sectionId, ItemStack[] contents) {}

    /**
     * Placeholders for one rendered item. For paginated-list templates,
     * {@code itemIndex} is the page-relative index; for other sections it is -1.
     */
    default TagResolver placeholders(C context, String sectionId, int itemIndex) {
        return TagResolver.empty();
    }

    /**
     * Placeholders resolved into the JSON {@code title} when the menu is built.
     * Supply the dynamic part as an uncolored component (e.g.
     * {@code Placeholder.component("pet_title", Component.translatable("X.Title",
     * Component.text(petName)))}); the JSON wraps it with color. Lets dynamic
     * titles keep their styling in JSON rather than in code.
     */
    default TagResolver titlePlaceholders(C context) {
        return TagResolver.empty();
    }

    /**
     * Customize a paginated-list template's item for one specific entry (e.g. swap
     * the material / glow per-item based on the underlying data). Default returns
     * the template unchanged. Only called for {@code paginated-list} sections; for
     * other section types, the template is used as-is.
     */
    default ItemAppearance customizeTemplateItem(C context, String sectionId, int itemIndex, ItemAppearance template) {
        return template;
    }

    /**
     * Customize a {@code slot} section's item at render time (e.g. swap material per
     * pet type, recolor potion bottle). Default returns the appearance unchanged.
     * Called every render after state resolution for slot sections only.
     */
    default ItemAppearance customizeSlotItem(C context, String sectionId, ItemAppearance appearance) {
        return appearance;
    }

    /**
     * Per-render visibility check for a section. Returning false skips both
     * rendering and click routing for that slot. The slot's underlying area is
     * left to whatever lower-priority section painted there (border, fill, or
     * empty). Default true.
     */
    default boolean isSlotVisible(C context, String sectionId) { return true; }

    /**
     * Current 0-indexed fill position for a value-bar section (0..width-1).
     * Default 0 (renders as all-low). Clamped to range at render time.
     */
    default int valueBarPosition(C context, String sectionId) { return 0; }

    /**
     * Reshape the loaded {@link MenuDefinition} based on the open-time context.
     * Use when row count or section regions depend on per-pet state (e.g. the
     * backpack's upgrade-driven row count). Default returns {@code base} unchanged.
     */
    default MenuDefinition transformDefinition(C context, MenuDefinition base) {
        return base;
    }
}
