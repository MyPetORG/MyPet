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
import lombok.Getter;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

/**
 * Fired when a pet's sit / follow toggle is about to flip. Subclass of
 * {@link PetInteractEvent}; the {@link Action} discriminates the new state:
 *
 * <ul>
 *   <li>{@link Action#STAY} — pet is being told to sit / stay in place.</li>
 *   <li>{@link Action#FOLLOW} — pet is being told to stand up and resume
 *       following its owner.</li>
 * </ul>
 *
 * <p>Fires from {@code Pet} on the right-click sit-toggle interaction.
 *
 * <p><b>Cancellable</b> (inherited): cancellation suppresses the toggle —
 * the pet stays in its current sit / follow state.
 *
 * <p><b>Pet state:</b> live pet, with the owner online.
 *
 * <p><b>Item:</b> {@code getItem()} (inherited from {@link PetInteractEvent})
 * always returns {@code null} for this event — the sit-toggle item is checked
 * by the caller, not stored on the event.
 */
@Getter
public class PetSitEvent extends PetInteractEvent {
    private static final HandlerList handlers = new HandlerList();
    private final Action action;

    public PetSitEvent(Pet pet, Action action) {
        super(pet, null);
        this.action = action;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @NonNull HandlerList getHandlers() {
        return handlers;
    }

    public enum Action {
        STAY, FOLLOW
    }
}