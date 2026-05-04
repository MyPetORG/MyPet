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

import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a pet is being permanently removed from the repository — the
 * symmetric counterpart to {@link PetCreateEvent}. The {@link Source}
 * discriminates the removal pathway:
 *
 * <ul>
 *   <li>{@link Source#Release} — owner ran {@code /mypet release}.</li>
 *   <li>{@link Source#Death} — pet died and was configured (or required by
 *       respawn-time = -1) to be deleted on death.</li>
 *   <li>{@link Source#AdminCommand} — admin removal via
 *       {@code /petadmin remove}.</li>
 *   <li>{@link Source#Other} — third-party plugin path.</li>
 * </ul>
 *
 * <p>Fires from {@code CommandRelease}, {@code CommandOptionRemove}, and
 * {@code PetDeathListener}.
 *
 * <p><b>Not cancellable:</b> the deletion has already been committed (or
 * scheduled) when this event fires. Veto must happen earlier — for releases,
 * intercept the command; for deaths, intercept {@code EntityDeathEvent}; for
 * admin removals, use permissions on the admin command.
 *
 * <p><b>Pet state:</b> may be live or persisted depending on source. For
 * {@code Death} the pet was live; for {@code Release} and {@code AdminCommand}
 * either is possible. The repository lookup is still valid at event time —
 * listeners can read the final pet state for logging / cleanup.
 */
public class PetRemoveEvent extends Event {
    protected static final HandlerList handlers = new HandlerList();
    private final StoredMyPet myPet;
    private final Source source;
    public PetRemoveEvent(StoredMyPet myPet, Source source) {
        this.myPet = myPet;
        this.source = source;
    }

    @SuppressWarnings("unused")
    public static HandlerList getHandlerList() {
        return handlers;
    }

    public Source getSource() {
        return source;
    }

    public MyPetPlayer getOwner() {
        return myPet.getOwner();
    }

    public Player getPlayer() {
        return myPet.getOwner().getPlayer();
    }

    public StoredMyPet getMyPet() {
        return myPet;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public enum Source {
        Release, Death, AdminCommand, Other
    }
}