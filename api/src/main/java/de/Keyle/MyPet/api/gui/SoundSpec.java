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

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Polymorphic sound description: a fixed sound, a sound with pitch/volume ranges,
 * a uniform-random choice among other specs, or silence.
 * Resolves to an Adventure {@link Sound} at fire time (or {@code null} if silent).
 */
public sealed interface SoundSpec {

    /** Resolve to an Adventure Sound. Returns null for {@link Silent}. */
    @Nullable Sound resolve(RandomGenerator random);

    record Fixed(Key key, float volume, float pitch, Sound.Source source) implements SoundSpec {
        @Override public Sound resolve(RandomGenerator random) {
            return Sound.sound(key, source, volume, pitch);
        }
    }

    record Range(Key key, FloatRange volume, FloatRange pitch, Sound.Source source) implements SoundSpec {
        @Override public Sound resolve(RandomGenerator random) {
            return Sound.sound(key, source, volume.sample(random), pitch.sample(random));
        }
    }

    record Choice(List<SoundSpec> options) implements SoundSpec {
        public Choice {
            if (options.isEmpty()) {
                throw new IllegalArgumentException("SoundSpec.Choice requires at least one option");
            }
            options = List.copyOf(options);
        }
        @Override public @Nullable Sound resolve(RandomGenerator random) {
            return options.get(random.nextInt(options.size())).resolve(random);
        }
    }

    record Silent() implements SoundSpec {
        @Override public @Nullable Sound resolve(RandomGenerator random) { return null; }
        public static final Silent INSTANCE = new Silent();
    }
}
