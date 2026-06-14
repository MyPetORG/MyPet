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

package de.Keyle.MyPet.api.gui;

import java.util.random.RandomGenerator;

/** Inclusive `[min, max]` float range; samples uniformly. Used for pitch/volume randomization. */
public record FloatRange(float min, float max) {
    public FloatRange {
        if (Float.isNaN(min) || Float.isNaN(max) || min > max) {
            throw new IllegalArgumentException("FloatRange invalid: min=" + min + " max=" + max);
        }
    }

    public static FloatRange of(float value) {
        return new FloatRange(value, value);
    }

    public float sample(RandomGenerator r) {
        return min == max ? min : min + r.nextFloat() * (max - min);
    }
}
