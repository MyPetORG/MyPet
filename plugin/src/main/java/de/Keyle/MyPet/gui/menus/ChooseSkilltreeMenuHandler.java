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
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.gui.context.ChooseSkilltreeContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public final class ChooseSkilltreeMenuHandler implements MenuHandler<ChooseSkilltreeContext> {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

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
        ChooseSkilltreeContext ctx = (ChooseSkilltreeContext) instance.context();
        int index = payload.itemIndex();
        if (index < 0 || index >= ctx.available().size() + ctx.locked().size()) return;
        if (isLocked(ctx, index)) {
            ctx.viewer().sendMessage(Locale.getComponent("Message.Menu.ChooseSkilltree.Locked", ctx.viewer()));
            return;
        }
        ctx.onChoose().accept(ctx.available().get(index));
        instance.close();
    }

    @Override
    public List<?> templateItems(ChooseSkilltreeContext context, String sectionId) {
        if (!"skilltrees".equals(sectionId)) return List.of();
        List<Skilltree> combined = new ArrayList<>(context.available());
        combined.addAll(context.locked());
        return combined;
    }

    @Override
    public TagResolver placeholders(ChooseSkilltreeContext context, String sectionId, int itemIndex) {
        if (!"skilltrees".equals(sectionId) || itemIndex < 0
                || itemIndex >= context.available().size() + context.locked().size()) {
            return TagResolver.empty();
        }
        Skilltree tree = treeAt(context, itemIndex);
        List<String> descLines = tree.getDescription();
        String desc = descLines == null || descLines.isEmpty() ? "" : String.join("<newline>", descLines);

        if (!isLocked(context, itemIndex)) {
            return TagResolver.builder()
                .resolver(Placeholder.unparsed("skilltree_name", tree.getDisplayName()))
                .resolver(Placeholder.parsed("skilltree_description", desc))
                .build();
        }

        String hint = MINI.serialize(lockedHint(context, tree));
        desc = desc.isEmpty() ? "<gray>" + hint : desc + "<newline><gray>" + hint;
        return TagResolver.builder()
            // component (not parsed) so a tree name containing MiniMessage syntax renders literally, greyed.
            .resolver(Placeholder.component("skilltree_name", Component.text(tree.getDisplayName(), NamedTextColor.GRAY)))
            .resolver(Placeholder.parsed("skilltree_description", desc))
            .build();
    }

    /** The reason a locked entry is teased: a level gate or a pending ascension. */
    private Component lockedHint(ChooseSkilltreeContext context, Skilltree tree) {
        int level = context.pet().getExperience().getLevel();
        if (level < tree.getRequiredLevel()) {
            return Locale.getFormattedComponent("Message.Menu.ChooseSkilltree.LockedLevel",
                    context.viewer(), tree.getDisplayName(), tree.getRequiredLevel());
        }
        return Locale.getComponent("Message.Menu.ChooseSkilltree.LockedAscension", context.viewer());
    }

    @Override
    public ItemAppearance customizeTemplateItem(ChooseSkilltreeContext context, String sectionId,
                                                int itemIndex, ItemAppearance template) {
        if (!"skilltrees".equals(sectionId)) return template;
        if (itemIndex < 0 || itemIndex >= context.available().size() + context.locked().size()) return template;

        if (isLocked(context, itemIndex)) {
            return new ItemAppearance(
                Material.GRAY_DYE,
                template.title(),
                template.lore(),
                false,
                template.amount(),
                template.customModelData(),
                template.headSkin(),
                template.potionColor()
            );
        }

        SkilltreeIcon icon = treeAt(context, itemIndex).getIcon();
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

    private Skilltree treeAt(ChooseSkilltreeContext ctx, int index) {
        return index < ctx.available().size()
                ? ctx.available().get(index)
                : ctx.locked().get(index - ctx.available().size());
    }

    private boolean isLocked(ChooseSkilltreeContext ctx, int index) {
        return index >= ctx.available().size();
    }
}
