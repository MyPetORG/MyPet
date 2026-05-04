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
import org.bukkit.event.HandlerList;

/**
 * Fired after an experience change pushes a pet's level down — the symmetric
 * counterpart of {@link PetLevelUpEvent}. New level via {@link #getLevel()}
 * (inherited); previous level via {@link #fromLevel()}.
 *
 * <p>Fires from {@code MyPetExperience#updateExp} on the down-transition
 * (e.g., an addon-side {@code removeExp} call, or skilltree-induced exp loss).
 *
 * <p><b>Not cancellable:</b> see {@link PetLevelUpEvent} — cancel the
 * upstream {@link PetExpEvent} to suppress the entire cascade.
 */
public class PetLevelDownEvent extends PetLevelEvent {
    private static final HandlerList handlers = new HandlerList();
    private final int fromLevel;

    public PetLevelDownEvent(MyPet myPet, int level, int fromLevel, boolean beQuiet) {
        super(myPet, level, beQuiet);
        this.fromLevel = fromLevel;
    }

    @SuppressWarnings("unused")
    public static HandlerList getHandlerList() {
        return handlers;
    }

    public int fromLevel() {
        return fromLevel;
    }

    public HandlerList getHandlers() {
        return handlers;
    }
}