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
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Common parent for level-transition events. Not directly fired — listeners
 * should subscribe to the concrete subclasses {@link PetLevelUpEvent} and
 * {@link PetLevelDownEvent}, which fire from {@code MyPetExperience#updateExp}
 * on real level transitions and carry the previous level via
 * {@code fromLevel()}.
 *
 * <p><b>Not cancellable:</b> the level change has already been applied when
 * the subclass events fire. Cancel the upstream {@link PetExpEvent} to
 * suppress the cascade.
 *
 * <p><b>Quiet flag:</b> {@link #isQuiet()} mirrors the originating
 * {@link PetExpEvent}'s quiet preference, propagated through the experience
 * updater.
 */
public class PetLevelEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    @Getter
    private final MyPet pet;
    @Getter
    private final int level;
    private final boolean beQuiet;

    public PetLevelEvent(MyPet pet, int Level) {
        this.pet = pet;
        this.level = Level;
        this.beQuiet = true;
    }

    public PetLevelEvent(MyPet pet, int level, boolean beQuiet) {
        this.pet = pet;
        this.level = level;
        this.beQuiet = beQuiet;
    }

    public MyPetPlayer getOwner() {
        return pet.getOwner();
    }

    public Player getPlayer() {
        return pet.getOwner().getPlayer();
    }

    public boolean isQuiet() {
        return beQuiet;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}