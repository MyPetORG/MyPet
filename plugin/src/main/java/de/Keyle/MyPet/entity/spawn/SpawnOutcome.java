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

package de.Keyle.MyPet.entity.spawn;

/**
 * Result of a spawn attempt. Distinguishes a region/plugin refusal from a
 * genuine failure so callers can pick an accurate player-facing message.
 */
public enum SpawnOutcome {
    /** The mob is in the world. */
    SUCCESS,
    /** No valid location was found near the requested point. */
    NO_SPACE,
    /** A CreatureSpawnEvent listener cancelled the spawn. */
    DENIED,
    /** Snapshot corruption, missing Bukkit entity class, or unloaded world. */
    FAILED,
    /**
     * A source-driven pet's provider (e.g. MythicMobs) could not produce the real
     * creature — not loaded, bad model id, or its own spawn was refused. The summon
     * is refused rather than silently substituting a plain vanilla mob.
     */
    SOURCE_UNAVAILABLE
}
