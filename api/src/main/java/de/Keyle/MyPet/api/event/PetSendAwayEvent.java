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
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

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
 * <p><b>Pet state:</b> live pet, owner online (the command requires it).
 * After successful dispatch, the entity is removed from the world but
 * {@link #getPet()} continues to refer to a live {@link MyPet} until its
 * owner logs out (the periodic save flushes it back to persisted form).
 *
 * <p><b>Pet exposure:</b> {@link #getPet()} returns {@link StoredMyPet} but
 * is always concretely a live {@link MyPet} — the constructor takes
 * {@code MyPet}. The widened return type is historical.
 */
public class PetSendAwayEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    @Getter
    private final StoredMyPet pet;
    boolean isCancelled = false;

    public PetSendAwayEvent(MyPet pet) {
        this.pet = pet;
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

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}