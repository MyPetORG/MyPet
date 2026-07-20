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

import de.Keyle.MyPet.api.gui.*;
import de.Keyle.MyPet.api.skill.skilltree.SkilltreeIcon;
import de.Keyle.MyPet.gui.context.PetShopSelectionContext;
import de.Keyle.MyPet.util.shop.PetShop;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;

import java.util.List;

/**
 * Lists the pet shops the viewer can open. Click on a shop opens
 * {@link MenuIds#PET_SHOP} with that shop's pets.
 */
public final class PetShopSelectionMenuHandler implements MenuHandler<PetShopSelectionContext> {

    @SuppressWarnings("unchecked")
    @Override public MenuId<PetShopSelectionContext> id() {
        return (MenuId<PetShopSelectionContext>) MenuIds.PET_SHOP_SELECTION;
    }

    @Override public void onOpen(MenuInstance instance, PetShopSelectionContext context) {
        instance.refreshSection("shops");
    }

    @Override
    public void onClick(MenuInstance instance, String sectionId, ClickPayload payload) {
        if (!"shops".equals(sectionId)) return;
        PetShopSelectionContext ctx = (PetShopSelectionContext) instance.context();
        int idx = payload.itemIndex();
        if (idx < 0 || idx >= ctx.shops().size()) return;
        // Replaces this menu with the selected shop — open() goes through
        // GuiService and pushes us onto the back stack automatically.
        ctx.shops().get(idx).open(instance.viewer());
    }

    @Override
    public List<?> templateItems(PetShopSelectionContext context, String sectionId) {
        return "shops".equals(sectionId) ? context.shops() : List.of();
    }

    @Override
    public TagResolver placeholders(PetShopSelectionContext context, String sectionId, int itemIndex) {
        if (!"shops".equals(sectionId) || itemIndex < 0 || itemIndex >= context.shops().size()) {
            return TagResolver.empty();
        }
        PetShop shop = context.shops().get(itemIndex);
        return TagResolver.builder()
            .resolver(Placeholder.unparsed("shop_name", shop.getDisplayName()))
            .build();
    }

    @Override
    public ItemAppearance customizeTemplateItem(PetShopSelectionContext context, String sectionId,
                                                int itemIndex, ItemAppearance template) {
        if (!"shops".equals(sectionId)) return template;
        if (itemIndex < 0 || itemIndex >= context.shops().size()) return template;
        SkilltreeIcon icon = context.shops().get(itemIndex).getIcon();
        Material mat = Material.matchMaterial(icon.getMaterial());
        if (mat == null) return template;
        return new ItemAppearance(
            mat,
            template.title(),
            template.lore(),
            icon.isGlowing() || template.glow(),
            template.amount(),
            template.customModelData(),
            template.headSkin(),
            template.potionColor()
        );
    }
}
