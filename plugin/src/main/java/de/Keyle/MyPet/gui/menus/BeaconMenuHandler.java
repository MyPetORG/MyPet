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
import de.Keyle.MyPet.api.skill.skills.Beacon.Buff;
import de.Keyle.MyPet.gui.MenuInstanceImpl;
import de.Keyle.MyPet.gui.context.BeaconContext;
import de.Keyle.MyPet.skill.skills.BeaconImpl;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.List;

/**
 * Beacon settings menu. The buff buttons are rendered as a single
 * {@code paginated-list} section ({@code "buffs"}) that the handler populates
 * dynamically from {@link BeaconImpl#getAvailableBuffs()}. The receiver and
 * toggle slots are stateful per pet; the confirm slot persists state and closes.
 */
public final class BeaconMenuHandler implements MenuHandler<BeaconContext> {

    @SuppressWarnings("unchecked")
    @Override public MenuId<BeaconContext> id() {
        return (MenuId<BeaconContext>) MenuIds.BEACON;
    }

    @Override
    public void onOpen(MenuInstance instance, BeaconContext context) {
        BeaconImpl beacon = beaconSkill(context);
        if (beacon == null) { instance.close(); return; }

        // Drop stale selections from a previous skilltree configuration so schedule()
        // doesn't wipe everything when size > current limit.
        beacon.pruneUnavailableBuffs();

        instance.setSlotState("receiver", beacon.getReceiverModeStateName());
        instance.setSlotState("toggle",   beacon.isEnabled() ? "on" : "off");
    }

    @Override
    public void onClick(MenuInstance instance, String sectionId, ClickPayload payload) {
        BeaconContext ctx = (BeaconContext) ((MenuInstanceImpl) instance).context();
        BeaconImpl beacon = beaconSkill(ctx);
        if (beacon == null) { instance.close(); return; }

        if ("buffs".equals(sectionId)) {
            List<Buff> available = beacon.getAvailableBuffs();
            int idx = payload.itemIndex();
            if (idx < 0 || idx >= available.size()) return;
            Buff clicked = available.get(idx);
            int limit = beacon.getBuffLimit();

            if (beacon.isBuffEnabled(clicked)) {
                beacon.setBuffEnabled(clicked, false);
            } else if (limit <= 1) {
                // Single-buff mode: clear all available buffs first.
                for (Buff other : available) {
                    if (beacon.isBuffEnabled(other)) beacon.setBuffEnabled(other, false);
                }
                beacon.setBuffEnabled(clicked, true);
            } else if (beacon.getSelectedBuffCount() < limit) {
                beacon.setBuffEnabled(clicked, true);
            }
            // else: at limit — silently ignored.
            instance.refreshSection("buffs");
        } else if ("receiver".equals(sectionId)) {
            String next = beacon.cycleReceiverMode();
            instance.setSlotState("receiver", next);
        } else if ("toggle".equals(sectionId)) {
            boolean now = !beacon.isEnabled();
            beacon.setActive(now);
            instance.setSlotState("toggle", now ? "on" : "off");
        } else if ("confirm".equals(sectionId)) {
            beacon.persist();
            instance.close();
        }
    }

    @Override
    public List<?> templateItems(BeaconContext context, String sectionId) {
        if (!"buffs".equals(sectionId)) return List.of();
        BeaconImpl beacon = beaconSkill(context);
        if (beacon == null) return List.of();
        return beacon.getAvailableBuffs();
    }

    @Override
    public TagResolver placeholders(BeaconContext context, String sectionId, int itemIndex) {
        // The receiver slot's title leads with a label, so supply it as a leaf
        // translatable; a leading <lang:Key> would swallow the trailing value.
        TagResolver receiverLabel = Placeholder.component("receiver_label",
            Component.translatable("Gui.Beacon.Receiver.Label"));
        if (!"buffs".equals(sectionId) || itemIndex < 0) return receiverLabel;
        BeaconImpl beacon = beaconSkill(context);
        if (beacon == null) return receiverLabel;
        List<Buff> available = beacon.getAvailableBuffs();
        if (itemIndex >= available.size()) return receiverLabel;
        Buff buff = available.get(itemIndex);
        // Vanilla effect translation key resolves client-side against the player's
        // selected language. PotionEffectType implements Translatable on Paper 1.20+.
        return TagResolver.builder()
            .resolver(receiverLabel)
            .resolver(Placeholder.component("buff_name",
                Component.translatable(buff.getPotionEffectType().translationKey())))
            .build();
    }

    @Override
    public ItemAppearance customizeTemplateItem(BeaconContext context, String sectionId,
                                                int itemIndex, ItemAppearance template) {
        if (!"buffs".equals(sectionId)) return template;
        BeaconImpl beacon = beaconSkill(context);
        if (beacon == null) return template;
        List<Buff> available = beacon.getAvailableBuffs();
        if (itemIndex < 0 || itemIndex >= available.size()) return template;
        Buff buff = available.get(itemIndex);
        boolean selected = beacon.isBuffEnabled(buff);
        return new ItemAppearance(
            template.material(),
            template.title(),
            template.lore(),
            selected,
            template.amount(),
            template.customModelData(),
            template.headSkin(),
            buff.getPotionEffectType().getColor()
        );
    }

    private static BeaconImpl beaconSkill(BeaconContext ctx) {
        return ctx.pet().getSkills().get(BeaconImpl.class);
    }
}
