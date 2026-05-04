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
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a pet's lifecycle {@code PetState} transitions — e.g., from
 * {@code Here} (entity in the world) to {@code Despawned} (entity removed
 * but pet still active) or to {@code Dead} (post-death respawn timer).
 *
 * <p>Fires from {@code MyPet#updateStatus} once per state transition.
 *
 * <p><b>Not cancellable:</b> the status change is already applied when this
 * event fires.
 *
 * <p><b>API gap:</b> the new status is captured in the constructor but not
 * exposed via a getter — listeners cannot read which state was entered. The
 * field is package-private (protected) and unused by the api itself. Until
 * this is remedied (see audit), the only practical use of the event is "some
 * status changed; I'll re-read it from {@link MyPet#getStatus()}".
 *
 * <p><b>Pet state:</b> live pet (status transitions only happen on the live
 * runtime).
 *
 * <p><b>Related events:</b> {@link PetCallEvent} fires before a {@code Here}
 * transition; {@link PetSendAwayEvent} fires before a {@code Despawned}
 * transition. Death has no dedicated MyPet-side event — listen to
 * {@code EntityDeathEvent} on the marked pet entity, or to the
 * {@link PetRemoveEvent} {@link PetRemoveEvent.Source#Death} source if
 * the pet is configured to delete on death.
 */
public class PetStatusEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    @Getter
    protected final StoredMyPet pet;
    protected final MyPet.PetState state;

    public PetStatusEvent(MyPet pet, MyPet.PetState state) {
        this.pet = pet;
        this.state = state;
    }

    public MyPetPlayer getOwner() {
        return pet.getOwner();
    }

    public Player getPlayer() {
        return pet.getOwner().getPlayer();
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}