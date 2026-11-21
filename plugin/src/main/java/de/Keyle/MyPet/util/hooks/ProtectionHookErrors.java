/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2020 Keyle
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

package de.Keyle.MyPet.util.hooks;

import de.Keyle.MyPet.MyPetApi;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Damage checks in protection hooks fail closed: when the other plugin throws, the pet is not
 * allowed to hurt the target. The error is logged once per hook as a warning, so server owners can see why
 * Pets stop doing damage without a broken hook spamming the log.
 */
final class ProtectionHookErrors {

    private static final Set<String> REPORTED = ConcurrentHashMap.newKeySet();

    private ProtectionHookErrors() {
    }

    static boolean failClosed(Class<?> hook, Throwable t) {
        if (REPORTED.add(hook.getName())) {
            MyPetApi.getLogger().log(Level.WARNING, hook.getSimpleName() + " damage check failed, so Pets are not allowed to hurt targets it protects until it works again. Is the plugin it hooks into up to date?", t);
        }
        return false;
    }
}
