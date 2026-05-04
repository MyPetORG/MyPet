/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
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

package de.Keyle.MyPet.api.event;

import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a pet's persisted state is being written to the repository.
 *
 * <p>Fires from numerous code paths that flush state to disk:
 * <ul>
 *   <li>{@code MyPetManager} — periodic global save and the deactivation save
 *       on owner-quit.</li>
 *   <li>{@code CommandTrade} — pet ownership transfer.</li>
 *   <li>{@code CommandOptionClone} — admin pet clone.</li>
 *   <li>{@code CommandOptionCreate} — admin pet creation.</li>
 *   <li>{@code EntityListener}, {@code CreakingHeartListener} — leash creation
 *       paths (alongside the corresponding {@link PetCreateEvent}).</li>
 * </ul>
 *
 * <p><b>Not cancellable:</b> the save will proceed regardless of listeners.
 * The pet exposed via {@link #getMyPet()} is in the state about to be (or
 * already being) serialized — listeners may inspect it for backup integrations
 * or last-write metadata.
 *
 * <p><b>Pet state:</b> may be live ({@link MyPet}) or persisted
 * ({@code PersistedMyPet}); the manager-side periodic save flushes both
 * categories and dispatches one event per pet.
 *
 * <p><b>Frequency note:</b> on a busy server the manager-side flush can fire
 * this event hundreds of times in a tight loop. Listeners that do non-trivial
 * work should defer it (e.g., enqueue UUID for a delayed batch) rather than
 * blocking the save loop.
 */
public class PetSaveEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final StoredMyPet myPet;

    public PetSaveEvent(StoredMyPet mypet) {
        this.myPet = mypet;
    }

    @SuppressWarnings("unused")
    public static HandlerList getHandlerList() {
        return handlers;
    }

    public StoredMyPet getMyPet() {
        return myPet;
    }

    public MyPetPlayer getOwner() {
        return myPet.getOwner();
    }

    public Player getPlayer() {
        return myPet.getOwner().getPlayer();
    }

    public HandlerList getHandlers() {
        return handlers;
    }
}