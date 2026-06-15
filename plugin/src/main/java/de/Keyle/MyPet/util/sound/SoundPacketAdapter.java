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

import org.bukkit.plugin.Plugin;

/**
 * Library-agnostic outbound ENTITY_SOUND packet hook. Each
 * implementation wraps one packet library (PacketEvents, ProtocolLib).
 * The semantic logic lives in {@link SoundPacketInterceptor} and is
 * shared across implementations.
 */
public interface SoundPacketAdapter {

    /** Whether the adapter's required library class is loadable in the current JVM. */
    boolean isAvailable();

    /** Idempotent: registers the packet listener if not already registered. */
    void register(Plugin plugin, SoundPacketInterceptor interceptor);

    /** Idempotent: removes the packet listener if registered. */
    void unregister();

    /** Display name (e.g. "PacketEvents") for log lines. */
    String name();

    /** Volume-attenuation mode read from this hook's hooks-config section. */
    PetSoundService.Mode getMode();
}
