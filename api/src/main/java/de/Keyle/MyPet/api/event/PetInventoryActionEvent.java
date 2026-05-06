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
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

/**
 * Fired when something interacts with a pet's Backpack inventory. The
 * {@link Action} discriminates the firing site:
 *
 * <ul>
 *   <li>{@link Action#OPEN} — fires from {@code BackpackImpl} when an owner
 *       opens the backpack UI ({@code /mypet inventory} or right-click with
 *       the open-backpack item).</li>
 *   <li>{@link Action#PICKUP} — fires from {@code PickupImpl} when a Pickup
 *       skill grab is about to deposit an item into the backpack.</li>
 *   <li>{@link Action#USE} — fires from {@code PickupImpl} when the pet would
 *       use a stored item (e.g., equip armor, eat food) instead of dropping
 *       it.</li>
 * </ul>
 *
 * <p><b>Cancellable:</b> cancellation suppresses the action specifically:
 * <ul>
 *   <li>{@code Open}: the inventory UI doesn't open.</li>
 *   <li>{@code Pickup}: the item is left on the ground.</li>
 *   <li>{@code Use}: the item stays in the backpack instead of being equipped
 *       or consumed.</li>
 * </ul>
 *
 * <p><b>Pet state:</b> live pet, with the owner online (Open) or near (Pickup / Use).
 *
 * <p><b>Related events:</b> {@link PetPickupItemEvent} fires earlier on the
 * Pickup path with the actual {@code Item} entity — listen to that for
 * per-item filtering, and to this event for per-action veto control.
 */
@Getter
public class PetInventoryActionEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final Pet pet;
    private final Action action;
    @Setter
    private boolean isCancelled = false;

    public PetInventoryActionEvent(Pet pet, Action action) {
        this.pet = pet;
        this.action = action;
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

    public enum Action {
        OPEN, PICKUP, USE
    }
}