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

package de.Keyle.MyPet.gui;

import de.Keyle.MyPet.api.gui.SoundSpec;
import net.kyori.adventure.sound.Sound;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

/** Resolves and plays {@link SoundSpec}s for one viewer. */
public final class SoundDispatch {

    private SoundDispatch() {}

    public static void play(Player viewer, SoundSpec spec) {
        if (spec == null || spec instanceof SoundSpec.Silent) return;
        Sound s = spec.resolve(ThreadLocalRandom.current());
        if (s == null) return;
        viewer.playSound(s);
    }
}
