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

import de.Keyle.MyPet.api.player.MyPetPlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

/**
 * Fired after a {@link MyPetPlayer} has finished joining the server — i.e.,
 * after the vanilla {@code PlayerJoinEvent} fires AND after MyPet has loaded
 * the player's pet roster from the repository, applied any pet-related
 * tracking ({@code lastSeen} timestamp, default-pet activation, etc.), and
 * registered them in the active player map.
 *
 * <p>Fires from {@code PlayerListener} on the join handler.
 *
 * <p><b>Not cancellable:</b> the player has already finished joining when
 * this event fires; this is a notification hook.
 *
 * <p><b>Related events:</b> for pet-level activation triggered by the join,
 * listen to {@link PetActivatedEvent} — it fires earlier in the join flow
 * for each pet that activates.
 */
public class PetPlayerJoinEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final MyPetPlayer myPetPlayer;

    public PetPlayerJoinEvent(MyPetPlayer myPetPlayer) {
        this.myPetPlayer = myPetPlayer;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public MyPetPlayer getPlayer() {
        return myPetPlayer;
    }

    @Override
    public @NonNull HandlerList getHandlers() {
        return handlers;
    }
}