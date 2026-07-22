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

package de.Keyle.MyPet.gui.menus;

import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.gui.*;
import de.Keyle.MyPet.gui.MenuInstanceImpl;
import de.Keyle.MyPet.gui.context.BackpackContext;
import de.Keyle.MyPet.skill.skills.BackpackImpl;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

public final class BackpackMenuHandler implements MenuHandler<BackpackContext> {

    @SuppressWarnings("unchecked")
    @Override public MenuId<BackpackContext> id() {
        return (MenuId<BackpackContext>) MenuIds.BACKPACK;
    }

    @Override public void onOpen(MenuInstance instance, BackpackContext context) {
        setMenuOpen(context, true); // pause gathering skills while items are being moved around
    }

    @Override
    public void onClose(MenuInstance instance, CloseReason reason) {
        // Contents are persisted before this fires, so the gathering skills resume on fresh state.
        if (((MenuInstanceImpl) instance).context() instanceof BackpackContext context) {
            setMenuOpen(context, false);
        }
    }

    private static void setMenuOpen(BackpackContext context, boolean open) {
        BackpackImpl bp = context.pet().getSkills().get(BackpackImpl.class);
        if (bp != null) bp.setMenuOpen(open);
    }

    @Override
    public void onClick(MenuInstance instance, String sectionId, ClickPayload payload) {
        // Storage clicks pass through vanilla; not routed here.
    }

    @Override
    public ItemStack[] storageContents(BackpackContext context, String sectionId) {
        if (!"storage".equals(sectionId)) return new ItemStack[0];
        BackpackImpl bp = context.pet().getSkills().get(BackpackImpl.class);
        return bp == null ? new ItemStack[0] : bp.readContents(context.rows() * 9);
    }

    @Override
    public void persistStorage(BackpackContext context, String sectionId, ItemStack[] contents) {
        if (!"storage".equals(sectionId)) return;
        BackpackImpl bp = context.pet().getSkills().get(BackpackImpl.class);
        if (bp != null) bp.writeContents(contents);
    }

    @Override
    public TagResolver titlePlaceholders(BackpackContext context) {
        return Placeholder.component("backpack_title", Component.translatable(
            "Gui.Backpack.Title",
            Util.SANITIZED_MINIMESSAGE.deserialize(context.pet().getPetName())
        ));
    }

    @Override
    public MenuDefinition transformDefinition(BackpackContext context, MenuDefinition base) {
        int rows = Math.max(1, Math.min(6, context.rows()));
        if (rows == base.rows()) return base;
        Map<String, Section> sections = new LinkedHashMap<>(base.sections());
        sections.computeIfPresent("storage", (id, s) -> {
            if (!(s instanceof StorageSection st)) return s;
            int newHeight = Math.max(1, Math.min(st.height(), rows - st.row()));
            return new StorageSection(st.id(), st.type(), st.col(), st.row(),
                st.width(), newHeight, st.storageId());
        });
        return new MenuDefinition(
            base.menuId(), base.titleMiniMessage(), rows, base.escSupportsBack(),
            base.soundOnOpen(), base.soundOnClose(), base.soundOnBack(), sections
        );
    }
}
