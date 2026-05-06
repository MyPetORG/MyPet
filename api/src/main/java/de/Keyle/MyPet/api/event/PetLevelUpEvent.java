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
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

/**
 * Fired after an experience change pushes a pet's level up. The new level is
 * available via {@link #getLevel()} (inherited) and the previous level via
 * {@link #fromLevel()}.
 *
 * <p>Fires from {@code PetExperience#updateExp} immediately after the level
 * is recomputed — only on the up-transition. If the change crosses multiple
 * levels at once, this event fires once with the final level and the
 * pre-change level as {@code fromLevel}.
 *
 * <p><b>Not cancellable:</b> the exp / level change has already been applied.
 * Cancel {@link PetExpEvent} earlier in the flow to suppress the entire
 * cascade.
 *
 * <p><b>Quiet flag:</b> inherited from {@link PetLevelEvent} — propagated
 * from the originating {@link PetExpEvent} via the experience updater.
 */
public class PetLevelUpEvent extends PetLevelEvent {
    private static final HandlerList handlers = new HandlerList();
    private final int fromLevel;

    public PetLevelUpEvent(Pet pet, int level, int fromLevel, boolean beQuiet) {
        super(pet, level, beQuiet);
        this.fromLevel = fromLevel;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public int fromLevel() {
        return fromLevel;
    }

    @Override
    public @NonNull HandlerList getHandlers() {
        return handlers;
    }
}