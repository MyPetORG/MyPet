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
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.StoredPet;
import de.Keyle.MyPet.api.gui.*;
import de.Keyle.MyPet.gui.context.PetAdminSelectionContext;
import de.Keyle.MyPet.services.EggIconService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.List;

public final class PetAdminSelectionMenuHandler implements MenuHandler<PetAdminSelectionContext> {

    @SuppressWarnings("unchecked")
    @Override public MenuId<PetAdminSelectionContext> id() {
        return (MenuId<PetAdminSelectionContext>) MenuIds.PET_ADMIN_SELECTION;
    }

    @Override
    public void onOpen(MenuInstance instance, PetAdminSelectionContext context) {
        instance.refreshSection("pets");
    }

    @Override
    public void onClick(MenuInstance instance, String sectionId, ClickPayload payload) {
        if (!"pets".equals(sectionId)) return;
        PetAdminSelectionContext ctx = (PetAdminSelectionContext) instance.context();
        if (payload.itemIndex() < 0 || payload.itemIndex() >= ctx.pets().size()) return;
        ctx.onSelect().accept(ctx.pets().get(payload.itemIndex()));
        instance.close();
    }

    @Override
    public List<?> templateItems(PetAdminSelectionContext context, String sectionId) {
        return "pets".equals(sectionId) ? context.pets() : List.of();
    }

    @Override
    public TagResolver placeholders(PetAdminSelectionContext context, String sectionId, int itemIndex) {
        if (!"pets".equals(sectionId) || itemIndex < 0 || itemIndex >= context.pets().size()) {
            return TagResolver.empty();
        }
        StoredPet pet = context.pets().get(itemIndex);
        return TagResolver.builder()
            .resolver(Placeholder.component("pet_name", Util.SANITIZED_MINIMESSAGE.deserialize(pet.getPetName())))
            .resolver(Placeholder.component("pet_type",
                Component.translatable("entity.minecraft." + pet.getPetType().getTypeID())))
            .resolver(Placeholder.component("owner_label",
                Component.translatable("Gui.PetAdminSelection.Template.Lore.Owner")))
            .resolver(Placeholder.component("type_label",
                Component.translatable("Gui.PetAdminSelection.Template.Lore.Type")))
            .resolver(Placeholder.unparsed("pet_owner", pet.getOwner().getName()))
            .build();
    }

    @Override
    public ItemAppearance customizeTemplateItem(PetAdminSelectionContext context, String sectionId,
                                                int itemIndex, ItemAppearance template) {
        if (!"pets".equals(sectionId)) return template;
        if (itemIndex < 0 || itemIndex >= context.pets().size()) return template;
        EggIconService.Resolved icon = MyPetApi.getServiceManager()
            .getService(EggIconService.class)
            .map(svc -> svc.resolve(context.pets().get(itemIndex).getPetType()))
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
