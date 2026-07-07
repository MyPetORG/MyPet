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

package de.Keyle.MyPet.entity.model;

/**
 * The discrete model animations MyPet triggers on a pet's rendered model. Locomotion
 * (walk/run/idle/jump) is left to each renderer's own movement-driven auto-play and is
 * not represented here. Each constant's {@link #defaultName()} doubles as its
 * {@code Model.Animations.<name>} config key.
 */
public enum PetModelAnimation {
    SPAWN("spawn", false),
    DESPAWN("despawn", false),
    SIT("sit", false),
    SIT_LOOP("sit_loop", true),
    UNSIT("unsit", false),
    ATTACK("attack", false);

    private final String defaultName;
    private final boolean loops;

    PetModelAnimation(String defaultName, boolean loops) {
        this.defaultName = defaultName;
        this.loops = loops;
    }

    /** Canonical animation name, used when a pet type has no per-type override. */
    public String defaultName() {
        return defaultName;
    }

    /**
     * Whether this event is a continuous loop ({@code sit_loop}) rather than a one-shot.
     * MyPet forces the renderer's loop mode from this flag so a discrete event plays once and
     * auto-returns to idle regardless of how the model authored the animation's loop mode —
     * otherwise a model that authored e.g. {@code attack} as a loop would stick playing forever.
     */
    public boolean loops() {
        return loops;
    }
}
