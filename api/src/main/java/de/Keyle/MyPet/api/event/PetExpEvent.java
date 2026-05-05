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
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

/**
 * Fired before a pet's experience changes — gain or loss. Dispatched once per
 * change from the central {@code MyPetExperience#updateExp} flow that backs
 * {@code addExp}, {@code removeExp}, and the percent-add helpers.
 *
 * <p>Fires from {@code MyPetExperience} immediately before the exp delta is
 * applied and the level is recomputed.
 *
 * <p><b>Cancellable:</b> cancellation skips the exp change entirely —
 * {@code updateExp} returns {@code 0} and the level recomputation does not
 * happen, so no follow-up {@link PetLevelUpEvent} or {@link PetLevelDownEvent}
 * is dispatched.
 *
 * <p><b>Mutable delta:</b> {@link #setExp(double)} adjusts the value to be
 * applied — useful for multipliers (XP-boost potions) or caps. The signed
 * value reflects the direction (positive for gain, negative for loss); the
 * post-clamp result is also bounded by {@code 0..maxExp} on the pet side.
 *
 * <p><b>Quiet flag:</b> {@link #isQuiet()} mirrors the caller's quiet
 * preference and is propagated forward to {@link PetLevelUpEvent} /
 * {@link PetLevelDownEvent} if the level changes.
 *
 * <p><b>Pet state:</b> live pet (experience changes only happen on the
 * runtime).
 */
@Getter
public class PetExpEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final MyPet pet;
    private final boolean quiet;
    @Setter
    private boolean isCancelled = false;
    @Setter
    private double exp;

    public PetExpEvent(MyPet pet, double exp, boolean quiet) {
        this.pet = pet;
        this.exp = exp;
        this.quiet = quiet;
    }

    public PetExpEvent(MyPet pet, double exp) {
        this(pet, exp, false);
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
}