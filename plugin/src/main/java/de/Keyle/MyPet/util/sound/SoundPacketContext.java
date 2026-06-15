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

package de.Keyle.MyPet.util.sound;

import de.Keyle.MyPet.api.entity.Pet;
import net.kyori.adventure.key.Key;
import org.bukkit.entity.Player;

/**
 * Library-agnostic snapshot of a single outbound sound packet that has
 * been attributed to a specific {@link Pet} by the adapter (either via
 * entity-id lookup or by spatial proximity for positional sounds).
 * Consumed by {@link SoundPacketInterceptor}.
 */
public record SoundPacketContext(
        Player recipient,
        Pet pet,
        Key soundKey,
        float currentVolume,
        float currentPitch
) {}
