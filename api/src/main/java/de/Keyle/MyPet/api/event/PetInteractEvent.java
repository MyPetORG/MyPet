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
import lombok.Getter;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

/**
 * Base class for pet right-click interaction events. Used directly only as a
 * type for listeners that want to catch any interaction; the built-in firing
 * sites all dispatch concrete subclasses.
 *
 * <p>Listeners receive instances via subclasses:
 * {@link PetFeedEvent} (right-click with food) and {@link PetSitEvent}
 * (right-click with the sit-toggle item). Listening for {@code PetInteractEvent}
 * is equivalent to listening for both subclasses.
 *
 * <p><b>Cancellable:</b> cancellation suppresses the interaction; the concrete
 * subclass defines what that means in detail.
 *
 * <p><b>Item:</b> {@link #getItem()} is the {@link ItemStack} the player was
 * holding when the interaction fired. May be {@code null} for interactions
 * that aren't item-driven — {@link PetSitEvent} (sit-toggle is non-item) is
 * the built-in example.
 */
public class PetInteractEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    @Getter
    private final StoredMyPet pet;
    @Getter
    private final ItemStack item;
    boolean isCancelled = false;

    public PetInteractEvent(MyPet pet, ItemStack item) {
        this.pet = pet;
        this.item = item;
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