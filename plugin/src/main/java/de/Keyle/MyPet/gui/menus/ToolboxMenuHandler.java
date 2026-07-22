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
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.gui.ClickPayload;
import de.Keyle.MyPet.api.gui.MenuHandler;
import de.Keyle.MyPet.api.gui.MenuId;
import de.Keyle.MyPet.api.gui.MenuIds;
import de.Keyle.MyPet.api.gui.MenuInstance;
import de.Keyle.MyPet.api.skill.skills.Toolbox.Station;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.gui.MenuInstanceImpl;
import de.Keyle.MyPet.gui.context.ToolboxContext;
import de.Keyle.MyPet.skill.skills.ToolboxImpl;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Station chooser for the Toolbox skill. Shows one button per unlocked
 * workstation; clicking it opens that station as a virtual view.
 */
public final class ToolboxMenuHandler implements MenuHandler<ToolboxContext> {

    @SuppressWarnings("unchecked")
    @Override public MenuId<ToolboxContext> id() {
        return (MenuId<ToolboxContext>) MenuIds.TOOLBOX;
    }

    @Override
    public void onOpen(MenuInstance instance, ToolboxContext context) {}

    @Override
    public void onClick(MenuInstance instance, String sectionId, ClickPayload payload) {
        Station station = stationForSection(sectionId);
        if (station == null) return;
        ToolboxContext ctx = (ToolboxContext) ((MenuInstanceImpl) instance).context();
        ToolboxImpl toolbox = ctx.pet().getSkills().get(ToolboxImpl.class);
        if (toolbox == null || !toolbox.getStation(station).getValue()) return;
        scheduleStationOpen(ctx.viewer(), ctx.pet(), station);
    }

    @Override
    public boolean isSlotVisible(ToolboxContext context, String sectionId) {
        Station station = stationForSection(sectionId);
        if (station == null) return true;
        ToolboxImpl toolbox = context.pet().getSkills().get(ToolboxImpl.class);
        return toolbox != null && toolbox.getStation(station).getValue();
    }

    @Override
    public TagResolver placeholders(ToolboxContext context, String sectionId, int itemIndex) {
        Station station = stationForSection(sectionId);
        if (station == null) return TagResolver.empty();
        return Placeholder.component("station_name", Component.translatable(station.getTranslationKey()));
    }

    /**
     * Entry point shared by the pet-menu hub and {@code /pettoolbox}: opens the single
     * unlocked station directly, or this chooser menu when several are unlocked.
     */
    @SuppressWarnings("unchecked")
    public static void open(Player viewer, Pet pet) {
        ToolboxImpl toolbox = pet.getSkills().get(ToolboxImpl.class);
        if (toolbox == null) return;
        List<Station> unlocked = toolbox.getUnlockedStations();
        if (unlocked.isEmpty()) {
            viewer.sendMessage(Locale.getFormattedComponent(
                "Message.Skill.Toolbox.NotAvailable", viewer, pet.getDisplayName()));
            return;
        }
        if (unlocked.size() == 1) {
            scheduleStationOpen(viewer, pet, unlocked.get(0));
            return;
        }
        MyPetApi.getGuiService().openMenu(
            viewer,
            (MenuId<ToolboxContext>) (MenuId<?>) MenuIds.TOOLBOX,
            new ToolboxContext(viewer, pet));
    }

    /**
     * Opens the station view on the viewer's next entity tick — never mid-click,
     * so the chest menu (if any) closes cleanly before the vanilla view appears.
     * The pet then holds the station's block until the view closes.
     */
    private static void scheduleStationOpen(Player viewer, Pet pet, Station station) {
        viewer.getScheduler().run(MyPetApi.getPlugin(), task -> {
            station.open(viewer);
            ToolboxStationHold.get().begin(viewer, pet, station);
        }, null);
    }

    private static Station stationForSection(String sectionId) {
        for (Station station : Station.values()) {
            if (station.getUpgradeKey().equals(sectionId)) {
                return station;
            }
        }
        return null;
    }
}
