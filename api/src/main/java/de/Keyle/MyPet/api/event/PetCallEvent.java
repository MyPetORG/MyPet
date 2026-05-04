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
 * Fired when an owner attempts to spawn the world entity for an already-active
 * pet — i.e. {@code /mypet call} or any equivalent call to {@code MyPet#createEntity}.
 * Fires once per call attempt, before the vanilla {@code Mob} is spawned.
 *
 * <p>The pet exposed via {@link #getPet()} is already active (skills loaded,
 * NBT applied) — only the world entity is missing. Activation itself is signaled
 * separately by {@link PetActivatedEvent}, which fires earlier and only once
 * per session.
 *
 * <p><b>Cancellable:</b> cancelling the event blocks the spawn, and the call
 * attempt fails silently from the caller's perspective. Common addon use-cases:
 * region-based call restrictions (WorldGuard / Towny integrations) and per-pet
 * cooldown enforcement.
 *
 * <p><b>Pet state:</b> {@link #getPet()} returns a {@link StoredMyPet} but is
 * always concretely a live {@link MyPet} — the constructor takes {@code MyPet}.
 * The widened return type is historical.
 */
public class PetCallEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    @Getter
    private final StoredMyPet pet;
    boolean isCancelled = false;

    public PetCallEvent(MyPet pet) {
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