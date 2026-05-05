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
import lombok.Getter;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

/**
 * Fired when an owner right-clicks a pet with a food {@link ItemStack}.
 * Subclass of {@link PetInteractEvent} — extends the right-click hook with
 * food-specific fields.
 *
 * <p>Fires from {@code MyPet#mobInteract} on player-pet right-click and from
 * the self-feed branch when the pet auto-eats a held item.
 *
 * <p><b>Cancellable</b> (inherited): cancellation suppresses the feed action
 * entirely — no saturation gain, no item consumption, no heal.
 *
 * <p><b>Mutable fields:</b>
 * <ul>
 *   <li>{@link #setSaturation(double)} adjusts the saturation gained from this
 *       feed; default is the food's vanilla saturation.</li>
 *   <li>{@link #setResult(Result)} chooses the visual / audio reaction:
 *       {@link Result#HEAL} (heart particles + heal animation),
 *       {@link Result#EAT} (eating particles + munch sound),
 *       {@link Result#SELF_FEED} (no player interaction; pet ate from its own
 *       backpack or AI behavior).</li>
 * </ul>
 */
@Getter
public class PetFeedEvent extends PetInteractEvent {
    private static final HandlerList handlers = new HandlerList();

    private double saturation;
    private Result result;
    public PetFeedEvent(MyPet pet, ItemStack item, double saturation, Result result) {
        super(pet, item);
        this.saturation = saturation;
        this.result = result;
    }

    public void setSaturation(double saturation) {
        this.saturation = saturation;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public enum Result {
        HEAL, EAT, SELF_FEED
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}