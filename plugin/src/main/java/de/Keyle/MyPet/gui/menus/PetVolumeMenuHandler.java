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
import de.Keyle.MyPet.api.gui.ClickPayload;
import de.Keyle.MyPet.api.gui.MenuHandler;
import de.Keyle.MyPet.api.gui.MenuId;
import de.Keyle.MyPet.api.gui.MenuIds;
import de.Keyle.MyPet.api.gui.MenuInstance;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.gui.context.PetVolumeContext;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;

/**
 * Volume slider menu. The single {@code value-bar} section snaps the
 * player's {@code petVolume} to one of five discrete presets
 * (0%, 25%, 50%, 75%, 100%) corresponding to the five bar cells.
 */
public final class PetVolumeMenuHandler implements MenuHandler<PetVolumeContext> {

    private static final int BAR_WIDTH = 5;

    @SuppressWarnings("unchecked")
    @Override public MenuId<PetVolumeContext> id() {
        return (MenuId<PetVolumeContext>) MenuIds.PET_VOLUME;
    }

    @Override
    public void onOpen(MenuInstance instance, PetVolumeContext context) {}

    @Override
    public void onClick(MenuInstance instance, String sectionId, ClickPayload payload) {
        if (!"volume".equals(sectionId)) return;
        int cell = payload.itemIndex();
        if (cell < 0 || cell >= BAR_WIDTH) return;
        Player viewer = ((PetVolumeContext) ((de.Keyle.MyPet.gui.MenuInstanceImpl) instance).context()).viewer();
        if (!MyPetApi.getPlayerManager().isMyPetPlayer(viewer)) return;
        MyPetPlayer player = MyPetApi.getPlayerManager().getMyPetPlayer(viewer);
        player.setPetVolume(percentForCell(cell) / 100f);
        instance.refreshSection("volume");
        instance.refreshSection("info");
    }

    @Override
    public int valueBarPosition(PetVolumeContext context, String sectionId) {
        if (!"volume".equals(sectionId)) return 0;
        return cellForVolume(currentVolume(context));
    }

    @Override
    public TagResolver placeholders(PetVolumeContext context, String sectionId, int itemIndex) {
        if ("info".equals(sectionId)) {
            return Placeholder.unparsed("volume_percent",
                String.valueOf(Math.round(currentVolume(context) * 100f)));
        }
        if ("volume".equals(sectionId) && itemIndex >= 0 && itemIndex < BAR_WIDTH) {
            return Placeholder.unparsed("percent", String.valueOf(percentForCell(itemIndex)));
        }
        return TagResolver.empty();
    }

    private static int percentForCell(int cell) {
        return cell * (100 / (BAR_WIDTH - 1));
    }

    private static int cellForVolume(float volume) {
        int rounded = Math.round(volume * (BAR_WIDTH - 1));
        return Math.max(0, Math.min(BAR_WIDTH - 1, rounded));
    }

    private static float currentVolume(PetVolumeContext context) {
        Player viewer = context.viewer();
        if (!MyPetApi.getPlayerManager().isMyPetPlayer(viewer)) return 1f;
        return MyPetApi.getPlayerManager().getMyPetPlayer(viewer).getPetVolume();
    }
}
