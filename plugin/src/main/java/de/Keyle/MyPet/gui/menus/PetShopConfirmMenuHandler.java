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
import de.Keyle.MyPet.gui.MenuInstanceImpl;
import de.Keyle.MyPet.gui.context.PetShopConfirmContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

public final class PetShopConfirmMenuHandler implements MenuHandler<PetShopConfirmContext> {

    @SuppressWarnings("unchecked")
    @Override public MenuId<PetShopConfirmContext> id() {
        return (MenuId<PetShopConfirmContext>) MenuIds.PET_SHOP_CONFIRM;
    }

    @Override public void onOpen(MenuInstance instance, PetShopConfirmContext context) {}

    @Override
    public void onClick(MenuInstance instance, String sectionId, ClickPayload payload) {
        PetShopConfirmContext ctx = (PetShopConfirmContext) ((MenuInstanceImpl) instance).context();
        if ("yes".equals(sectionId)) {
            ctx.onConfirm().run();
            instance.close();
        } else if ("no".equals(sectionId)) {
            instance.popBack();
        }
    }

    @Override
    public TagResolver placeholders(PetShopConfirmContext context, String sectionId, int itemIndex) {
        if (!"display".equals(sectionId)) return TagResolver.empty();
        return TagResolver.builder()
            .resolver(Placeholder.component("price_label",
                Component.translatable("Gui.PetShop.Template.Lore.Price")))
            .resolver(Placeholder.component("entry_name", context.entry().getDisplayName()))
            .resolver(Placeholder.unparsed("entry_price", String.valueOf(context.entry().getPrice())))
            .build();
    }
}
