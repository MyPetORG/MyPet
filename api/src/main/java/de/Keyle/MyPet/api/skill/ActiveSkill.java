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

package de.Keyle.MyPet.api.skill;

/**
 * Marker interface for skills that can be manually activated by the player (e.g., via
 * a command or hotkey). A skill implementing {@code ActiveSkill} exposes an
 * {@link #activate()} method that triggers the skill's one-shot effect, as opposed to
 * passive skills that fire automatically on combat events.
 *
 * <p>Examples of active skills include Beacon (applies a potion effect area) and
 * Lightning (strikes a target with lightning). The plugin's command layer calls
 * {@link #activate()} when the owning player requests the skill.
 *
 * @see OnHitSkill
 * @see OnDamageByEntitySkill
 */
public interface ActiveSkill {

    /**
     * Attempts to activate this skill's effect.
     *
     * @return {@code true} if the skill fired successfully; {@code false} if it
     *         could not activate (e.g., on cooldown, missing targets, or insufficient level)
     */
    boolean activate();
}