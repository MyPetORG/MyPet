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

package de.Keyle.MyPet.api.entity.ai.target;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Determines which targeting goal wins when multiple goals attempt to
 * assign a combat target simultaneously. Higher numeric values take
 * precedence — a goal may only override the current target if its
 * priority is strictly greater than the one that set the existing target.
 * <p>
 * The scale is intentionally sparse so that third-party integrations
 * (via {@link #Bukkit}) can slot in without colliding. The sentinel
 * values {@link #None} ({@code Integer.MIN_VALUE}) and
 * {@link #Overwrite} ({@code Integer.MAX_VALUE}) anchor the extremes.
 */
@Getter
@RequiredArgsConstructor
public enum TargetPriority {

    /** No target set — always loses to any real priority. */
    None(0x80000000),

    /** External target set by another plugin via the Bukkit API. */
    Bukkit(0),

    /** Target acquired by the Farm (mob-harvesting) behavior. */
    Farm(3),

    /** Target forced by the player's Control skill (manual aim). */
    Control(4),

    /** Retaliatory target — the pet was directly damaged. */
    GetHurt(5),

    /** Target inherited from the owner's outgoing attack. */
    OwnerHurts(6),

    /** Target inherited from something that damaged the owner. */
    OwnerGetsHurt(7),

    /** Autonomous aggression toward configured mob types. */
    Aggressive(9),

    /** PvP duel target — highest normal-play priority. */
    Duel(10),

    /** Unconditional override — always wins. Used by admin commands. */
    Overwrite(0x7fffffff);

    /**
     * Numeric priority value. Higher wins when two goals compete.
     *
     * @return the priority ordinal for this level
     */
    private final int priority;
}