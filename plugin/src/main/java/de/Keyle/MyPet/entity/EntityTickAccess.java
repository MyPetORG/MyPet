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

package de.Keyle.MyPet.entity;

import de.Keyle.MyPet.MyPetApi;
import org.bukkit.entity.Entity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Reflective access to NMS {@code Entity#tickCount} — the per-tick counter
 * vanilla uses to drive periodic logic (Warden darkness pulses every 120
 * ticks, Mob anger ticks every 20, etc.). Bukkit's
 * {@link Entity#getTicksLived()} sounds like the right surface but returns
 * Spigot's {@code totalEntityAge} field, which is only assigned on NBT load
 * and never incremented afterward — so it diverges from {@code tickCount}
 * the moment an entity is loaded from saved state and can't be used to
 * recreate vanilla's per-tick modulo decisions.
 *
 * <p>Same surgical-NMS-reflection pattern as
 * {@link de.Keyle.MyPet.entity.ai.BrainAccess} — a deliberate exception to
 * the codebase's "no NMS" stance for cases where Bukkit doesn't expose the
 * needed API.
 *
 * <p>Fail-soft: if reflection breaks (Mojang renames the field, class
 * loader oddity), a single warning is logged and subsequent calls return
 * {@code -1}. Callers should treat {@code -1} as "couldn't read" and
 * decide whether to fall back to a heuristic.
 */
public final class EntityTickAccess {

    private EntityTickAccess() {}

    private static volatile boolean initialized = false;
    private static volatile boolean available = false;
    private static volatile Method getHandleMethod;
    private static volatile Field tickCountField;

    private static synchronized void tryInit(Entity entity) {
        if (initialized) return;
        initialized = true;
        try {
            Class<?> craftEntity = Class.forName("org.bukkit.craftbukkit.entity.CraftEntity");
            Class<?> nmsEntity = Class.forName("net.minecraft.world.entity.Entity");
            getHandleMethod = craftEntity.getMethod("getHandle");
            tickCountField = nmsEntity.getField("tickCount");
            available = true;
        } catch (Throwable t) {
            MyPetApi.getLogger().warning(
                    "EntityTickAccess unavailable — features relying on vanilla tickCount (Warden darkness attribution) will fall back to heuristics. Cause: " + t);
        }
    }

    /**
     * Returns the entity's NMS {@code tickCount}, or {@code -1} if
     * reflection isn't available.
     */
    public static int getTickCount(Entity entity) {
        if (!initialized) tryInit(entity);
        if (!available) return -1;
        try {
            Object handle = getHandleMethod.invoke(entity);
            return tickCountField.getInt(handle);
        } catch (Throwable t) {
            return -1;
        }
    }
}
