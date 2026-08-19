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

package de.Keyle.MyPet.commands;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.StoredPet;
import de.Keyle.MyPet.api.gui.MenuId;
import de.Keyle.MyPet.api.gui.MenuIds;
import de.Keyle.MyPet.gui.context.PetSelectionContext;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Picks which of a player's active Pets a GUI command should act on.
 *
 * <p>With one Pet out — the shipped active-Pet cap — the action runs directly, so every
 * caller behaves exactly as it did before multi-Pet support. The selection menu is
 * unreachable until the cap moves.
 */
public final class ActivePetChooser {

    private ActivePetChooser() {
    }

    /**
     * Runs {@code action} against one of the viewer's active Pets, asking which when
     * several are out.
     *
     * @param ifNone run when the viewer has no active Pet at all
     */
    @SuppressWarnings("unchecked")
    public static void withActivePet(Player viewer, Consumer<Pet> action, Runnable ifNone) {
        List<Pet> activePets = MyPetApi.getPetManager().getPets(viewer);

        if (activePets.isEmpty()) {
            ifNone.run();
            return;
        }
        if (activePets.size() == 1) {
            action.accept(activePets.get(0));
            return;
        }

        // Pet extends StoredPet, so the selection menu /petswitch already uses renders
        // active Pets unchanged. The cast back is safe: this is the list we supplied.
        MyPetApi.getGuiService().openMenu(
                viewer,
                (MenuId<PetSelectionContext>) (MenuId<?>) MenuIds.PET_SELECTION,
                new PetSelectionContext(
                        viewer,
                        () -> CompletableFuture.completedFuture(new ArrayList<StoredPet>(activePets)),
                        chosen -> action.accept((Pet) chosen))
        );
    }
}
