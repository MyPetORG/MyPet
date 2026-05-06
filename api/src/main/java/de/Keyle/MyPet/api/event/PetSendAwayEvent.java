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

package de.Keyle.MyPet.api.event;

import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

/**
 * Fired when an owner runs {@code /mypet sendaway} — the symmetric counterpart
 * of {@link PetCallEvent}. Despawns the world entity but keeps the pet
 * active in the manager (i.e., the owner can call it back without re-loading
 * from disk).
 *
 * <p>Fires from {@code CommandSendAway}.
 *
 * <p><b>Cancellable:</b> cancelling the event leaves the pet entity in the
 * world. Common addon use-cases: region-locked pets that can't be dismissed
 * inside arenas, anti-grief gates against dismissal in PvP zones.
 *
 * <p><b>Pet state:</b> live pet, with the owner online (the command requires it).
 * After successful dispatch, the entity is removed from the world but
 * {@link #getPet()} continues to refer to a live {@link Pet} until its
 * owner logs out (the periodic save flushes it back to persisted form).
 *
 * <p><b>Pet exposure:</b> always a live {@link Pet} — the pet is active
 * and its world entity is about to be despawned.
 */
public class PetSendAwayEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    @Getter
    private final Pet pet;
    boolean isCancelled = false;

    public PetSendAwayEvent(Pet pet) {
        this.pet = pet;
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
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        isCancelled = cancelled;
    }

    @Override
    public @NonNull HandlerList getHandlers() {
        return handlers;
    }
}