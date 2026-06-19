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

package de.Keyle.MyPet.util.translation;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.Translator;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.text.MessageFormat;
import java.util.Locale;

/**
 * Version-neutral wrapper over Adventure's translation source. Adventure 5 replaced
 * {@code TranslationRegistry} with {@code TranslationStore}; this resolves whichever class is
 * present at runtime via method handles so one MyPet jar runs on both Adventure 4 and 5.
 */
final class VanillaTranslationStore {

    private final Translator translator;
    // (String key, Locale, MessageFormat) -> void, bound to the underlying registry/store.
    private final MethodHandle register;

    private VanillaTranslationStore(Translator translator, MethodHandle register) {
        this.translator = translator;
        this.register = register;
    }

    /** Tries the Adventure 5 TranslationStore first, then the Adventure 4 TranslationRegistry. */
    static VanillaTranslationStore create(Key key) {
        try {
            return createAdventure5(key);
        } catch (ClassNotFoundException notAdventure5) {
            try {
                return createAdventure4(key);
            } catch (Throwable t) {
                throw new IllegalStateException("No Adventure TranslationStore or TranslationRegistry available", t);
            }
        } catch (Throwable t) {
            throw new IllegalStateException("Failed to initialise Adventure 5 TranslationStore", t);
        }
    }

    private static VanillaTranslationStore createAdventure5(Key key) throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        Class<?> storeClass = Class.forName("net.kyori.adventure.translation.TranslationStore");
        Class<?> stringBased = Class.forName("net.kyori.adventure.translation.TranslationStore$StringBased");
        MethodHandle factory = lookup.findStatic(storeClass, "messageFormat",
                MethodType.methodType(stringBased, Key.class));
        Object store = factory.invoke(key);
        // TranslationStore#register(String, Locale, T) — T erases to Object on the interface.
        MethodHandle register = lookup.findVirtual(storeClass, "register",
                        MethodType.methodType(void.class, String.class, Locale.class, Object.class))
                .bindTo(store)
                .asType(MethodType.methodType(void.class, String.class, Locale.class, MessageFormat.class));
        return new VanillaTranslationStore((Translator) store, register);
    }

    private static VanillaTranslationStore createAdventure4(Key key) throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        Class<?> registryClass = Class.forName("net.kyori.adventure.translation.TranslationRegistry");
        MethodHandle factory = lookup.findStatic(registryClass, "create",
                MethodType.methodType(registryClass, Key.class));
        Object registry = factory.invoke(key);
        // TranslationRegistry#register(String, Locale, MessageFormat) — concrete MessageFormat param.
        MethodHandle register = lookup.findVirtual(registryClass, "register",
                        MethodType.methodType(void.class, String.class, Locale.class, MessageFormat.class))
                .bindTo(registry);
        return new VanillaTranslationStore((Translator) registry, register);
    }

    void register(String key, Locale locale, MessageFormat format) {
        try {
            register.invokeExact(key, locale, format);
        } catch (IllegalArgumentException e) {
            // Duplicate key/locale — let the caller skip it, same as the legacy direct call.
            throw e;
        } catch (Throwable t) {
            throw new IllegalStateException("Adventure translation register failed for key " + key, t);
        }
    }

    Translator asTranslator() {
        return translator;
    }
}
