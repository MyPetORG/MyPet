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
 * Fired when a brand-new pet is created and assigned to an owner — the moment
 * a pet first enters the repository. The {@code Source} discriminates the
 * creation pathway: {@link Source#Leash} (player leashes a wild mob),
 * {@link Source#AdminCommand} (an admin {@code /petadmin create}-style command),
 * {@link Source#PetShop} (PetShop integration purchase), {@link Source#Other}
 * (third-party plugins).
 *
 * <p>Fires from {@code EntityListener} (leash path), {@code CreakingHeartListener}
 * (Creaking-specific leash path), {@code CommandOptionCreate}, and the
 * {@code PetShop} integration.
 *
 * <p><b>Not cancellable:</b> the pet has already been added to the manager when
 * this event fires. Listeners can use it to award welcome bonuses, log creation,
 * or trigger UI flows — not to veto creation. Veto must happen earlier in the
 * Bukkit-event chain ({@code PlayerLeashEntityEvent} for the leash sources, the
 * command's permission check for admin / shop sources).
 *
 * <p><b>Pet state:</b> {@link #getMyPet()} is in the inactive-pets pool — the
 * pet is persisted but not yet active. Owner is set; {@code getPlayer()} may be
 * null only if the owner has logged off in the same tick.
 *
 * <p><b>Related events:</b> {@link PetLoadEvent} fires whenever a pet is
 * loaded from disk (creation OR subsequent activations). {@link PetActivatedEvent}
 * fires after activation completes. {@link PetRemoveEvent} is the symmetric
 * deletion event.
 */
public class PetCreateEvent extends Event {
    protected static final HandlerList handlers = new HandlerList();
    private final StoredMyPet myPet;
    private final Source source;
    public PetCreateEvent(StoredMyPet myPet, Source source) {
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
        Leash, AdminCommand, PetShop, Other
    }
}