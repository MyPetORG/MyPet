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
 * Fired when the repository hands a stored pet to {@code MyPetManager} for
 * activation — i.e., just before the manager wires it up as a live
 * {@link MyPet}. The pet exposed via {@link #getPet()} is still a
 * {@link StoredMyPet} (concretely a {@code PersistedMyPet} record); skills
 * have not yet been instantiated.
 *
 * <p>Fires from {@code MyPetManager.activateMyPet} before any setup runs.
 *
 * <p><b>Not cancellable:</b> activation always proceeds. Listeners use this
 * for read-only auditing, attaching addon-side state to the about-to-be-live
 * pet, or pre-populating caches keyed on UUID.
 *
 * <p><b>Pet state:</b> persisted, never live. Owner is set; player may be
 * online or offline (the manager activates pets eagerly on owner-join, but
 * the resolved player may be null in race-condition windows).
 *
 * <p><b>Related events:</b> {@link PetActivatedEvent} fires after activation
 * completes. The pair {@code Load → Activated} brackets the entire activation
 * flow; nothing else dispatches between them on the main thread.
 */
public class PetLoadEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final StoredMyPet myPet;

    public PetLoadEvent(StoredMyPet mypet) {
        this.myPet = mypet;
    }

    @SuppressWarnings("unused")
    public static HandlerList getHandlerList() {
        return handlers;
    }

    public StoredMyPet getPet() {
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