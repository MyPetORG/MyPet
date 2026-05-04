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
 * Notification that a pet's level value should be re-read by listeners. The
 * "level changed" semantic is split across this base and its subclasses, with
 * non-obvious firing sites:
 *
 * <ul>
 *   <li><b>Base event ({@code PetLevelEvent}):</b> fires from {@code MyPet}
 *       on every {@code setSkilltree(...)} call, with the pet's
 *       <i>current</i> (unchanged) level. The dispatch is a side-effect of
 *       skilltree assignment — primarily a "the level-related context just
 *       changed, please re-render" hint.</li>
 *   <li><b>Up- / down-transitions:</b> dispatched as the typed subclasses
 *       {@link PetLevelUpEvent} / {@link PetLevelDownEvent} from
 *       {@code MyPetExperience#updateExp}. These carry the previous level via
 *       {@code fromLevel()}.</li>
 * </ul>
 *
 * <p>Listeners attached to the base type receive the skilltree-change
 * notification but <i>not</i> the up/down events (subclass dispatch goes
 * only to listeners registered for the subclass). To observe real level
 * transitions, listen for {@link PetLevelUpEvent} and
 * {@link PetLevelDownEvent} directly.
 *
 * <p><b>Not cancellable:</b> the underlying state change has already been
 * applied when this event fires.
 *
 * <p><b>Quiet flag:</b> {@link #isQuiet()} mirrors the originating call's
 * quiet preference — for the skilltree-change firing the constructor passes
 * {@code beQuiet=true}.
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

    @SuppressWarnings("unused")
    public static HandlerList getHandlerList() {
        return handlers;
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

    public HandlerList getHandlers() {
        return handlers;
    }
}