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
import lombok.Setter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a pet's display name is about to change. Listeners can rewrite
 * the proposed name via {@link #setNewName(String)} — useful for filtering
 * profanity, enforcing length / character constraints, or applying server-wide
 * formatting rules (color codes, bracket prefixes, etc.).
 *
 * <p>Fires from {@code MyPet#setPetName} for every name change source — owner
 * rename via {@code /mypet name}, admin rename via {@code /petadmin name}, and
 * any third-party setter call. The new name is applied with the value of
 * {@link #getNewName()} at the end of the event dispatch, so listeners that
 * mutate it later in the chain win over earlier ones.
 *
 * <p><b>Not cancellable:</b> there is no {@code Cancellable} on this event.
 * To suppress a rename, set {@code newName} back to the pet's current name
 * via {@code event.setNewName(event.getMyPet().getPetName())}.
 *
 * <p><b>Pet state:</b> live pet (name changes only happen on active pets).
 *
 * <p><b>Format note:</b> the name string is in MiniMessage format —
 * {@code MyPet#getDisplayName} deserializes it into a {@code Component} via
 * the sanitized MiniMessage parser. Listeners modifying the name should
 * preserve MiniMessage syntax.
 */
public class PetNameEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    @Getter
    private MyPet myPet;
    @Getter
    @Setter
    private String newName;

    public PetNameEvent(MyPet myPet, String newName) {
        this.myPet = myPet;
        this.newName = newName;
    }

    @SuppressWarnings("unused")
    public static HandlerList getHandlerList() {
        return handlers;
    }

    public HandlerList getHandlers() {
        return handlers;
    }
}