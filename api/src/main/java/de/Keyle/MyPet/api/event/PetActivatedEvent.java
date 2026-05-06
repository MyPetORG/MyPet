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
import de.Keyle.MyPet.api.entity.PersistedMyPet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

/**
 * Fired after a pet has finished transitioning from at-rest ({@link PersistedMyPet})
 * to live ({@link MyPet}) — i.e., once the runtime entity exists, all skills are
 * registered and rebuilt from persisted state, and the pet is in the active-pets map.
 *
 * <p>Fires from {@code PetManager.activateMyPet(StoredMyPet)} and from the clone
 * path that follows pet-type transformations.
 *
 * <p><b>Not cancellable:</b> activation has already completed before this event
 * is dispatched. Use {@link PetCallEvent} earlier in the call flow if you need
 * to veto a pet coming live.
 *
 * <p><b>Pet state:</b> {@link #getPet()} returns a fully initialized live pet.
 * The pet entity may not yet be spawned in the world — activation is logical, not
 * spatial; the world entity is created when the owner uses {@code /mypet call}.
 *
 * <p><b>Related events:</b> {@link PetLoadEvent} fires earlier in the same flow
 * (before any wiring happens, while the pet is still a {@code StoredMyPet}).
 * {@link PetCallEvent} can fire afterward when the owner spawns the pet entity.
 */
@Getter
public class PetActivatedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final MyPet pet;

    public PetActivatedEvent(MyPet mypet) {
        this.pet = mypet;
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
}