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

import de.Keyle.MyPet.api.util.service.ServiceContainer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.InputStream;
import java.util.OptionalInt;
import java.util.function.Supplier;

/**
 * Service facade. Obtained via {@code MyPetApi.getGuiService()}. Plugins register
 * their menus with {@link #registerMenu}; the registered handler receives every
 * lifecycle callback for that menu.
 */
public interface GuiService extends ServiceContainer {

    /**
     * Register a menu. {@code bundledJson} is a fresh {@link InputStream} over the JSON
     * file shipped inside the registering plugin's JAR. The supplier is invoked once at
     * registration and again on {@code /mypet reload}.
     */
    <C> void registerMenu(MenuId<C> id, MenuHandler<C> handler, Supplier<InputStream> bundledJson);

    /** Open or push a menu for the viewer. */
    <C> void openMenu(Player viewer, MenuId<C> id, C context);

    /** Re-render the menu the viewer is currently in. No-op if none. */
    void refreshMenu(Player viewer);

    /** Close the viewer's current menu with {@link CloseReason#PLUGIN_CLOSED}. */
    void closeMenu(Player viewer);

    /**
     * Inserts {@code item} into the live storage of the menu {@code viewer} currently
     * has open (e.g. an open Backpack), so the addition is visible immediately and is
     * persisted when the menu closes. Returns the amount that did not fit, or empty
     * when the viewer has no open menu with a storage section — callers should then
     * persist through their own backing store instead.
     */
    OptionalInt addToOpenStorage(Player viewer, ItemStack item);

    SectionTypeRegistry getSectionTypeRegistry();
}
