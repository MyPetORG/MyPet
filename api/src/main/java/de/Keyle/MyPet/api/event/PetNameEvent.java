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
import lombok.Setter;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

/**
 * Fired when a pet's display name is about to change. Listeners can rewrite
 * the proposed name via {@link #setNewName(String)} — useful for filtering
 * profanity, enforcing length / character constraints, or applying server-wide
 * formatting rules (color codes, bracket prefixes, etc.).
 *
 * <p>Fires from {@code Pet#setPetName} for every name change source — owner
 * rename via {@code /mypet name}, admin rename via {@code /petadmin name}, and
 * any third-party setter call. The new name is applied with the value of
 * {@link #getNewName()} at the end of the event dispatch, so listeners that
 * mutate it later in the chain win over earlier ones.
 *
 * <p><b>Cancellable:</b> cancellation suppresses the rename entirely — the
 * pet keeps its current name.
 *
 * <p><b>Pet state:</b> live pet (name changes only happen on active pets).
 *
 * <p><b>Format note:</b> the name string is in MiniMessage format —
 * {@code Pet#getDisplayName} deserializes it into a {@code Component} via
 * the sanitized MiniMessage parser. Listeners modifying the name should
 * preserve MiniMessage syntax.
 */
public class PetNameEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    @Getter
    private final Pet pet;
    @Getter
    @Setter
    private String newName;
    @Getter
    @Setter
    private boolean cancelled;

    public PetNameEvent(Pet pet, String newName) {
        this.pet = pet;
        this.newName = newName;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @NonNull HandlerList getHandlers() {
        return handlers;
    }
}