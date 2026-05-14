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

package de.Keyle.MyPet.api.entity.leashing;

import de.Keyle.MyPet.api.util.AnnotationLookup;
import de.Keyle.MyPet.api.util.service.Load;
import de.Keyle.MyPet.api.util.service.ServiceContainer;
import de.Keyle.MyPet.api.util.service.ServiceName;

import java.util.HashMap;
import java.util.Map;

/**
 * Central registry for {@link LeashFlag} implementations. Each flag is
 * stored under the lowercase form of its {@link LeashFlagName} annotation
 * value.
 * <p>
 * Flags are registered at plugin startup (built-in flags) and when
 * third-party hook plugins enable (integration flags like WorldGuard,
 * MythicMobs). The leash-attempt logic in the plugin module queries this
 * manager to resolve flag names from the skilltree config into live
 * {@link LeashFlag} instances.
 */
@ServiceName("LeashFlagManager")
@Load(Load.State.OnLoad)
public class LeashFlagManager implements ServiceContainer {

    private final Map<String, LeashFlag> leashFlags = new HashMap<>();

    public void onDisable() {
        leashFlags.clear();
    }

    /**
     * Registers a flag instance. The name is derived from the
     * {@link LeashFlagName} annotation on the flag's class hierarchy.
     *
     * @param leashFlag the flag to register
     */
    public void registerLeashFlag(LeashFlag leashFlag) {
        String flagName = getLeashFlagName(leashFlag.getClass());
        if (flagName == null) {
            throw new IllegalArgumentException(
                    leashFlag.getClass().getName() + " is not annotated with @LeashFlagName");
        }
        leashFlags.put(flagName.toLowerCase(), leashFlag);
    }

    /**
     * Looks up a registered flag by its config name (case-insensitive).
     *
     * @return the flag instance, or {@code null} if no flag with that
     * name is registered
     */
    public LeashFlag getLeashFlag(String flagName) {
        return leashFlags.get(flagName.toLowerCase());
    }

    /**
     * Walks the class hierarchy (superclasses then interfaces) to find
     * the {@link LeashFlagName} annotation. Returns {@code null} if no
     * annotation is found before reaching {@code Object}.
     */
    public String getLeashFlagName(Class<?> clazz) {
        return AnnotationLookup.findName(clazz, LeashFlagName.class, LeashFlag.class, LeashFlagName::value);
    }

    /** Removes a flag by its config name (case-sensitive). */
    public void removeFlag(String flagName) {
        leashFlags.remove(flagName);
    }

    /** Removes a flag by deriving its name from the class annotation. */
    public void removeFlag(LeashFlag flag) {
        String flagName = getLeashFlagName(flag.getClass());
        removeFlag(flagName);
    }
}
