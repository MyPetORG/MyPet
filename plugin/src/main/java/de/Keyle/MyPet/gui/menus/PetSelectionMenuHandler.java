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
import de.Keyle.MyPet.api.MyPetGlobal;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.PetType;
import de.Keyle.MyPet.api.entity.StoredPet;
import de.Keyle.MyPet.api.gui.*;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.gui.context.PetSelectionContext;
import de.Keyle.MyPet.services.EggIconService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.List;

public final class PetSelectionMenuHandler implements MenuHandler<PetSelectionContext> {

    @SuppressWarnings("unchecked")
    @Override public MenuId<PetSelectionContext> id() {
        return (MenuId<PetSelectionContext>) MenuIds.PET_SELECTION;
    }

    @Override
    public void onOpen(MenuInstance instance, PetSelectionContext context) {
        context.pets().get().thenAccept(pets -> {
            org.bukkit.Bukkit.getScheduler().runTask(
                de.Keyle.MyPet.MyPetApi.getPlugin(),
                () -> instance.refreshSection("pets"));
        });
    }

    @Override
    public void onClick(MenuInstance instance, String sectionId, ClickPayload payload) {
        if (!"pets".equals(sectionId)) return;
        PetSelectionContext ctx = (PetSelectionContext) ((de.Keyle.MyPet.gui.MenuInstanceImpl) instance).context();
        List<StoredPet> pets = currentPetsSync(ctx);
        if (payload.itemIndex() < 0 || payload.itemIndex() >= pets.size()) return;
        ctx.onSelect().accept(pets.get(payload.itemIndex()));
        instance.close();
    }

    @Override
    public List<?> templateItems(PetSelectionContext context, String sectionId) {
        if (!"pets".equals(sectionId)) return List.of();
        return currentPetsSync(context);
    }

    @Override
    public TagResolver placeholders(PetSelectionContext context, String sectionId, int itemIndex) {
        if (!"pets".equals(sectionId) || itemIndex < 0) return TagResolver.empty();
        List<StoredPet> pets = currentPetsSync(context);
        if (itemIndex >= pets.size()) return TagResolver.empty();
        StoredPet pet = pets.get(itemIndex);
        String locale = Locale.getPlayerLanguage(context.viewer());

        // Pre-translated labels avoid a MiniMessage gotcha where text after `<lang:KEY>`
        // becomes children of the TranslatableComponent and is dropped when
        // GlobalTranslator.render replaces it with the translated content.

        String hungerLine = MyPetGlobal.HungerSystem.USE_HUNGER_SYSTEM.get()
            ? Locale.renderPlain("Name.Hunger", locale) + ": <gold>" + Math.round(pet.getSaturation())
            : "";

        String healthLine = pet.getRespawnTime() > 0
            ? Locale.renderPlain("Name.Respawntime", locale) + ": <gold>" + pet.getRespawnTime() + "sec"
            : Locale.renderPlain("Name.HP", locale) + ": <gold>" + String.format("%1.2f", pet.getHealth());

        int level = pet.getLevel();
        String progressionLine = level > 0
            ? Locale.renderPlain("Name.Level", locale) + ": <gold>" + level
            : Locale.renderPlain("Name.Exp", locale) + ": <gold>" + String.format("%1.2f", pet.getExp());

        String skilltreeName = pet.getSkilltree() != null ? pet.getSkilltree().getDisplayName() : "-";

        return TagResolver.builder()
            .resolver(Placeholder.component("pet_name", Util.SANITIZED_MINIMESSAGE.deserialize(pet.getPetName())))
            .resolver(Placeholder.component("pet_type", petTypeComponent(pet.getPetType())))
            .resolver(Placeholder.component("type_label",
                Component.translatable("Gui.PetSelection.Template.Lore.Type")))
            .resolver(Placeholder.component("skilltree_label",
                Component.translatable("Gui.PetSelection.Template.Lore.Skilltree")))
            .resolver(Placeholder.parsed("pet_hunger_line", hungerLine))
            .resolver(Placeholder.parsed("pet_health_line", healthLine))
            .resolver(Placeholder.parsed("pet_progression_line", progressionLine))
            .resolver(Placeholder.unparsed("pet_skilltree", skilltreeName))
            .build();
    }

    /**
     * Build the displayed pet-type name as a {@link Component} that the client
     * translates against its own locale. {@code entity.minecraft.<id>} is the
     * vanilla translation key every Minecraft client already knows, so this works
     * across all client languages without needing per-type entries in MyPet's locale.
     */
    private static Component petTypeComponent(PetType petType) {
        return Component.translatable("entity.minecraft." + petType.getTypeID());
    }

    @Override
    public ItemAppearance customizeTemplateItem(PetSelectionContext context, String sectionId,
                                                int itemIndex, ItemAppearance template) {
        if (!"pets".equals(sectionId)) return template;
        List<StoredPet> pets = currentPetsSync(context);
        if (itemIndex < 0 || itemIndex >= pets.size()) return template;
        EggIconService.Resolved icon = MyPetApi.getServiceManager()
            .getService(EggIconService.class)
            .map(svc -> svc.resolve(pets.get(itemIndex).getPetType()))
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

    /** Blocking-ish: the future is expected to complete fast in practice. */
    private static List<StoredPet> currentPetsSync(PetSelectionContext ctx) {
        return ctx.pets().get().getNow(List.of());
    }
}
