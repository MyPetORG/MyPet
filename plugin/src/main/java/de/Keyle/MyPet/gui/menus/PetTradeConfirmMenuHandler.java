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
import de.Keyle.MyPet.api.gui.ClickPayload;
import de.Keyle.MyPet.api.gui.MenuHandler;
import de.Keyle.MyPet.api.gui.MenuId;
import de.Keyle.MyPet.api.gui.MenuIds;
import de.Keyle.MyPet.api.gui.MenuInstance;
import de.Keyle.MyPet.gui.MenuInstanceImpl;
import de.Keyle.MyPet.gui.context.PetTradeConfirmContext;
import de.Keyle.MyPet.util.shop.PetTradeService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

/** Yes/No confirmation for a pending trade. Yes initiates the offer; No returns to the picker. */
public final class PetTradeConfirmMenuHandler implements MenuHandler<PetTradeConfirmContext> {

    @SuppressWarnings("unchecked")
    @Override public MenuId<PetTradeConfirmContext> id() {
        return (MenuId<PetTradeConfirmContext>) MenuIds.PET_TRADE_CONFIRM;
    }

    @Override public TagResolver titlePlaceholders(PetTradeConfirmContext context) {
        return Placeholder.component("trade_confirm_title", Component.translatable(
            "Gui.PetTradeConfirm.Title",
            Util.SANITIZED_MINIMESSAGE.deserialize(context.pet().getPetName()),
            Component.text(context.target().getName())));
    }

    @Override public void onOpen(MenuInstance instance, PetTradeConfirmContext context) {}

    @Override
    public void onClick(MenuInstance instance, String sectionId, ClickPayload payload) {
        PetTradeConfirmContext ctx = (PetTradeConfirmContext) ((MenuInstanceImpl) instance).context();
        if ("yes".equals(sectionId)) {
            PetTradeService.offerTrade(ctx.viewer(), ctx.target(), ctx.pet());
            instance.close();
        } else if ("no".equals(sectionId)) {
            instance.popBack();
        }
    }

    @Override
    public TagResolver placeholders(PetTradeConfirmContext context, String sectionId, int itemIndex) {
        return TagResolver.builder()
            .resolver(Placeholder.component("pet_name", Util.SANITIZED_MINIMESSAGE.deserialize(context.pet().getPetName())))
            .resolver(Placeholder.unparsed("target_name", context.target().getName()))
            .resolver(Placeholder.component("pet_type",
                Component.translatable("entity.minecraft." + context.pet().getPetType().getTypeID())))
            .build();
    }
}
