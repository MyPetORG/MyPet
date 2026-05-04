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
import de.Keyle.MyPet.api.player.MyPetPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a pet's saturation drops to zero — the pet is hungry and would
 * otherwise begin losing health. Lets addons award food, suppress the
 * starvation hit, or surface custom messaging.
 *
 * <p>Fires from {@code MyPet#updateSaturation} once per saturation-zero
 * transition; after firing, if not cancelled, the pet's health begins ticking
 * down from hunger.
 *
 * <p><b>Cancellable:</b> cancelling skips the starvation health-loss for this
 * tick — but the saturation remains at 0, so the event will fire again next
 * saturation tick unless something refills it (a food item, an addon, or
 * {@code /petadmin feed}).
 *
 * <p><b>Pet state:</b> live pet; owner online
 */
public class PetExhaustionEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    protected final MyPet pet;
    protected boolean cancelled;

    public PetExhaustionEvent(MyPet pet) {
        this.pet = pet;
    }

    public MyPet getPet() {
        return pet;
    }

    public MyPetPlayer getOwner() {
        return pet.getOwner();
    }

    public Player getPlayer() {
        return pet.getOwner().getPlayer();
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean b) {
        cancelled = b;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}