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

package de.Keyle.MyPet.dialog;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.dialog.ConfirmPromptSpec;
import de.Keyle.MyPet.api.gui.MenuId;
import de.Keyle.MyPet.api.gui.MenuIds;
import de.Keyle.MyPet.gui.context.PetReleaseConfirmContext;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

/** Opens the {@code PET_RELEASE_CONFIRM} chest menu as a fallback confirm dialog. */
public final class ConfirmFallbackMenu {

    private ConfirmFallbackMenu() {}

    public static void open(Player viewer, ConfirmPromptSpec spec, Runnable onYes, Runnable onNo) {
        MiniMessage mm = MiniMessage.miniMessage();
        String title = mm.serialize(spec.title());
        String message = mm.serialize(spec.message());
        PetReleaseConfirmContext ctx = new PetReleaseConfirmContext(viewer, title, message, onYes, onNo);
        @SuppressWarnings("unchecked")
        MenuId<PetReleaseConfirmContext> id = (MenuId<PetReleaseConfirmContext>) MenuIds.PET_RELEASE_CONFIRM;
        MyPetApi.getGuiService().openMenu(viewer, id, ctx);
    }
}
