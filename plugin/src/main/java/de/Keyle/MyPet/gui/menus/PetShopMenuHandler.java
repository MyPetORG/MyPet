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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.gui.*;
import de.Keyle.MyPet.gui.MenuInstanceImpl;
import de.Keyle.MyPet.gui.context.PetShopConfirmContext;
import de.Keyle.MyPet.gui.context.PetShopContext;
import de.Keyle.MyPet.services.EggIconService;
import de.Keyle.MyPet.util.shop.ShopPet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.List;

public final class PetShopMenuHandler implements MenuHandler<PetShopContext> {

    @SuppressWarnings("unchecked")
    @Override public MenuId<PetShopContext> id() {
        return (MenuId<PetShopContext>) MenuIds.PET_SHOP;
    }

    @Override public void onOpen(MenuInstance instance, PetShopContext context) {
        instance.refreshSection("entries");
    }

    @Override
    public void onClick(MenuInstance instance, String sectionId, ClickPayload payload) {
        if (!"entries".equals(sectionId)) return;
        PetShopContext ctx = (PetShopContext) ((MenuInstanceImpl) instance).context();
        if (payload.itemIndex() < 0 || payload.itemIndex() >= ctx.entries().size()) return;
        ShopPet picked = ctx.entries().get(payload.itemIndex());
        MyPetApi.getGuiService().openMenu(
            instance.viewer(),
            (MenuId<PetShopConfirmContext>) (MenuId<?>) MenuIds.PET_SHOP_CONFIRM,
            new PetShopConfirmContext(instance.viewer(), picked, () -> ctx.onSelect().accept(instance.viewer(), picked))
        );
    }

    @Override
    public List<?> templateItems(PetShopContext context, String sectionId) {
        return "entries".equals(sectionId) ? context.entries() : List.of();
    }

    @Override
    public TagResolver placeholders(PetShopContext context, String sectionId, int itemIndex) {
        if (!"entries".equals(sectionId) || itemIndex < 0 || itemIndex >= context.entries().size()) {
            return TagResolver.empty();
        }
        ShopPet sp = context.entries().get(itemIndex);
        return TagResolver.builder()
            .resolver(Placeholder.component("price_label",
                Component.translatable("Gui.PetShop.Template.Lore.Price")))
            .resolver(Placeholder.component("entry_name", sp.getDisplayName()))
            .resolver(Placeholder.unparsed("entry_price", String.valueOf(sp.getPrice())))
            .build();
    }

    @Override
    public ItemAppearance customizeTemplateItem(PetShopContext context, String sectionId,
                                                int itemIndex, ItemAppearance template) {
        if (!"entries".equals(sectionId)) return template;
        if (itemIndex < 0 || itemIndex >= context.entries().size()) return template;
        ShopPet sp = context.entries().get(itemIndex);
        EggIconService.Resolved icon = MyPetApi.getServiceManager()
            .getService(EggIconService.class)
            .map(svc -> svc.resolve(sp.getPetType()))
            .orElse(null);
        if (icon == null) return template;
        return new ItemAppearance(
            icon.material(),
            template.title(),
            template.lore(),
            icon.glowing() || template.glow(),
            template.amount(),
            template.customModelData(),
            template.headSkin(),
            template.potionColor()
        );
    }
}
