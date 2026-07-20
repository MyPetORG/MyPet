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
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.gui.ClickPayload;
import de.Keyle.MyPet.api.gui.ItemAppearance;
import de.Keyle.MyPet.api.gui.MenuHandler;
import de.Keyle.MyPet.api.gui.MenuId;
import de.Keyle.MyPet.api.gui.MenuIds;
import de.Keyle.MyPet.api.gui.MenuInstance;
import de.Keyle.MyPet.gui.context.PetTradeConfirmContext;
import de.Keyle.MyPet.gui.context.PetTradeTargetContext;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Player picker for the trade flow. Click a target to push the confirm menu.
 * Targets are online players in the viewer's world-group with the trade-offer permission.
 */
public final class PetTradeTargetMenuHandler implements MenuHandler<PetTradeTargetContext> {

    @SuppressWarnings("unchecked")
    @Override public MenuId<PetTradeTargetContext> id() {
        return (MenuId<PetTradeTargetContext>) MenuIds.PET_TRADE_TARGET;
    }

    @Override public void onOpen(MenuInstance instance, PetTradeTargetContext context) {
        instance.refreshSection("targets");
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onClick(MenuInstance instance, String sectionId, ClickPayload payload) {
        if (!"targets".equals(sectionId)) return;
        PetTradeTargetContext ctx = (PetTradeTargetContext) instance.context();
        List<Player> targets = onlineTargets(ctx);
        if (payload.itemIndex() < 0 || payload.itemIndex() >= targets.size()) return;
        Player target = targets.get(payload.itemIndex());
        MyPetApi.getGuiService().openMenu(
            ctx.viewer(),
            (MenuId<PetTradeConfirmContext>) (MenuId<?>) MenuIds.PET_TRADE_CONFIRM,
            new PetTradeConfirmContext(ctx.viewer(), ctx.pet(), target)
        );
    }

    @Override
    public List<?> templateItems(PetTradeTargetContext context, String sectionId) {
        return "targets".equals(sectionId) ? onlineTargets(context) : List.of();
    }

    @Override
    public TagResolver placeholders(PetTradeTargetContext context, String sectionId, int itemIndex) {
        if (!"targets".equals(sectionId) || itemIndex < 0) return TagResolver.empty();
        List<Player> targets = onlineTargets(context);
        if (itemIndex >= targets.size()) return TagResolver.empty();
        Player target = targets.get(itemIndex);
        return TagResolver.builder()
            .resolver(Placeholder.unparsed("target_name", target.getName()))
            .build();
    }

    @Override
    public ItemAppearance customizeTemplateItem(PetTradeTargetContext context, String sectionId,
                                                int itemIndex, ItemAppearance template) {
        // Per-head skull-owner customization needs an ItemAppearance.skullOwner field
        // (or a render hook); for v1 we accept default Steve heads — the player name
        // in the title is sufficient to identify the target.
        return template;
    }

    /**
     * Online players the viewer can offer the pet to: not the viewer themselves,
     * same world-group, and holding the trade-offer permission.
     */
    private static List<Player> onlineTargets(PetTradeTargetContext ctx) {
        Player viewer = ctx.viewer();
        String viewerGroup = WorldGroup.getGroupByWorld(viewer.getWorld()).getName();
        return Bukkit.getOnlinePlayers().stream()
            .filter(p -> !p.getUniqueId().equals(viewer.getUniqueId()))
            .filter(p -> WorldGroup.getGroupByWorld(p.getWorld()).getName().equals(viewerGroup))
            .filter(p -> p.hasPermission("MyPet.command.trade"))
            .collect(Collectors.toList());
    }
}
