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
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a pet with the Pickup skill is about to grab an {@link Item}
 * entity off the ground. Listeners receive the live world {@code Item} entity
 * before it is despawned and inserted into the pet's backpack.
 *
 * <p>Fires from {@code PickupImpl} during the proximity scan that runs every
 * Pickup tick, once per item entity that passes the skill's filter — distance,
 * cooldown, item-type allow/blocklist.
 *
 * <p><b>Cancellable:</b> cancellation leaves the {@link Item} on the ground
 * and aborts the pickup. The pet won't re-attempt the same item until the
 * Pickup tick comes back around.
 *
 * <p><b>Pet state:</b> live pet, owner online
 *
 * <p><b>Related events:</b> {@link PetInventoryActionEvent} fires immediately
 * after this on the {@link PetInventoryActionEvent.Action#PICKUP} branch,
 * with the same can-cancel semantics. Listen here for per-item filtering and
 * to that event for per-action veto control.
 */
@Getter
public class PetPickupItemEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    protected final MyPet pet;
    private final Item item;
    @Setter
    protected boolean isCancelled = false;

    public PetPickupItemEvent(MyPet pet, Item item) {
        this.pet = pet;
        this.item = item;
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