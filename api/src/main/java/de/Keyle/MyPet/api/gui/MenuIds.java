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

import de.Keyle.MyPet.api.skill.skills.Beacon;

/**
 * Catalog of built-in {@link MenuId}s. Most context type parameters reference plugin-side
 * record classes loaded reflectively via {@code Class.forName} so this api-side class
 * does not depend on plugin-side packages; {@link Beacon.MenuContext} is an api-side type
 * and is referenced directly.
 */
public final class MenuIds {

    public static final MenuId<?> PET_SELECTION       = MenuId.of("pet-selection",       contextType("PetSelectionContext"));
    public static final MenuId<?> PET_ADMIN_SELECTION = MenuId.of("pet-admin-selection", contextType("PetAdminSelectionContext"));
    public static final MenuId<?> PET_SHOP_SELECTION  = MenuId.of("pet-shop-selection",  contextType("PetShopSelectionContext"));
    public static final MenuId<?> PET_SHOP            = MenuId.of("pet-shop",            contextType("PetShopContext"));
    public static final MenuId<?> PET_SHOP_CONFIRM    = MenuId.of("pet-shop-confirm",    contextType("PetShopConfirmContext"));
    public static final MenuId<?> CHOOSE_SKILLTREE    = MenuId.of("choose-skilltree",    contextType("ChooseSkilltreeContext"));
    public static final MenuId<?> BEACON              = MenuId.of("beacon",              Beacon.MenuContext.class);
    public static final MenuId<?> BACKPACK            = MenuId.of("backpack",            contextType("BackpackContext"));
    public static final MenuId<?> NPC_STORAGE_CONFIRM = MenuId.of("npc-storage-confirm", contextType("NpcStorageConfirmContext"));
    public static final MenuId<?> PET_MENU             = MenuId.of("pet-menu",             contextType("PetMenuContext"));
    public static final MenuId<?> PET_RELEASE_CONFIRM  = MenuId.of("pet-release-confirm",  contextType("PetReleaseConfirmContext"));
    public static final MenuId<?> PET_TRADE_TARGET     = MenuId.of("pet-trade-target",     contextType("PetTradeTargetContext"));
    public static final MenuId<?> PET_TRADE_CONFIRM    = MenuId.of("pet-trade-confirm",    contextType("PetTradeConfirmContext"));
    public static final MenuId<?> PET_VOLUME           = MenuId.of("pet-volume",           contextType("PetVolumeContext"));
    public static final MenuId<?> TOOLBOX              = MenuId.of("toolbox",              contextType("ToolboxContext"));

    private MenuIds() {}

    private static Class<?> contextType(String simpleName) {
        try {
            return Class.forName("de.Keyle.MyPet.gui.context." + simpleName);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Missing context class: " + simpleName, e);
        }
    }
}
