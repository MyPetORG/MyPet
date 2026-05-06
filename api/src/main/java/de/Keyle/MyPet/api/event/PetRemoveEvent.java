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

import de.Keyle.MyPet.api.entity.StoredPet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

/**
 * Fired when a pet is being permanently removed from the repository — the
 * symmetric counterpart to {@link PetCreateEvent}. The {@link Source}
 * discriminates the removal pathway:
 *
 * <ul>
 *   <li>{@link Source#RELEASE} — owner ran {@code /mypet release}.</li>
 *   <li>{@link Source#DEATH} — pet died and was configured (or required by
 *       respawn-time = -1) to be deleted on death.</li>
 *   <li>{@link Source#ADMIN_COMMAND} — admin removal via
 *       {@code /petadmin remove}.</li>
 *   <li>{@link Source#OTHER} — third-party plugin path.</li>
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
@Getter
public class PetRemoveEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final StoredPet pet;
    private final Source source;

    public PetRemoveEvent(StoredPet pet, Source source) {
        this.pet = pet;
        this.source = source;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public MyPetPlayer getOwner() {
        return pet.getOwner();
    }

    public Player getPlayer() {
        return pet.getOwner().getPlayer();
    }

    @Override
    public @NonNull HandlerList getHandlers() {
        return handlers;
    }

    public enum Source {
        RELEASE, DEATH, ADMIN_COMMAND, OTHER
    }
}