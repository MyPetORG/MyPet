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
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import de.Keyle.MyPet.api.skill.skilltree.SkilltreeIcon;
import de.Keyle.MyPet.gui.MenuInstanceImpl;
import de.Keyle.MyPet.gui.context.ChooseSkilltreeContext;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;

import java.util.List;

public final class ChooseSkilltreeMenuHandler implements MenuHandler<ChooseSkilltreeContext> {

    @SuppressWarnings("unchecked")
    @Override public MenuId<ChooseSkilltreeContext> id() {
        return (MenuId<ChooseSkilltreeContext>) MenuIds.CHOOSE_SKILLTREE;
    }

    @Override public void onOpen(MenuInstance instance, ChooseSkilltreeContext context) {
        instance.refreshSection("skilltrees");
    }

    @Override
    public void onClick(MenuInstance instance, String sectionId, ClickPayload payload) {
        if (!"skilltrees".equals(sectionId)) return;
        ChooseSkilltreeContext ctx = (ChooseSkilltreeContext) ((MenuInstanceImpl) instance).context();
        if (payload.itemIndex() < 0 || payload.itemIndex() >= ctx.available().size()) return;
        ctx.onChoose().accept(ctx.available().get(payload.itemIndex()));
        instance.close();
    }

    @Override
    public List<?> templateItems(ChooseSkilltreeContext context, String sectionId) {
        return "skilltrees".equals(sectionId) ? context.available() : List.of();
    }

    @Override
    public TagResolver placeholders(ChooseSkilltreeContext context, String sectionId, int itemIndex) {
        if (!"skilltrees".equals(sectionId) || itemIndex < 0 || itemIndex >= context.available().size()) {
            return TagResolver.empty();
        }
        Skilltree tree = context.available().get(itemIndex);
        List<String> descLines = tree.getDescription();
        String desc = descLines == null || descLines.isEmpty() ? "" : String.join("<newline>", descLines);
        return TagResolver.builder()
            .resolver(Placeholder.unparsed("skilltree_name", tree.getName()))
            .resolver(Placeholder.parsed("skilltree_description", desc))
            .build();
    }

    @Override
    public ItemAppearance customizeTemplateItem(ChooseSkilltreeContext context, String sectionId,
                                                int itemIndex, ItemAppearance template) {
        if (!"skilltrees".equals(sectionId)) return template;
        if (itemIndex < 0 || itemIndex >= context.available().size()) return template;
        SkilltreeIcon icon = context.available().get(itemIndex).getIcon();
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
