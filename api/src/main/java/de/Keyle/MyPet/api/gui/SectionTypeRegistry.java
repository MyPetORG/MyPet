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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Process-wide registry of {@link SectionType}s by id. Section type-holder classes
 * self-register via {@link SectionType#register} in their static initializers.
 * {@link #ensureBuiltinsRegistered()} forces every built-in type-holder class to
 * load so the registry is fully populated before the loader runs.
 */
public final class SectionTypeRegistry {
    private static final Map<String, SectionType<?>> BY_ID = new LinkedHashMap<>();
    private static boolean builtinsLoaded = false;

    private SectionTypeRegistry() {}

    public static synchronized void register(SectionType<?> type) {
        SectionType<?> previous = BY_ID.put(type.id(), type);
        if (previous != null && previous != type) {
            throw new IllegalStateException("SectionType id '" + type.id()
                + "' already registered by a different SectionType instance");
        }
    }

    public static synchronized Optional<SectionType<?>> byId(String id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    public static synchronized Map<String, SectionType<?>> all() {
        return Map.copyOf(BY_ID);
    }

    /**
     * Force-load every built-in section type-holder class so its static initializer
     * registers the type. Idempotent. Called from MyPetPlugin.onLoad.
     */
    public static synchronized void ensureBuiltinsRegistered() {
        if (builtinsLoaded) return;
        String[] holders = {
            "de.Keyle.MyPet.gui.sections.SlotSectionType",
            "de.Keyle.MyPet.gui.sections.PaginatedListSectionType",
            "de.Keyle.MyPet.gui.sections.BorderSectionType",
            "de.Keyle.MyPet.gui.sections.FillSectionType",
            "de.Keyle.MyPet.gui.sections.StorageSectionType",
            "de.Keyle.MyPet.gui.sections.ValueBarSectionType"
        };
        try {
            for (String fqcn : holders) {
                Class.forName(fqcn, true, SectionTypeRegistry.class.getClassLoader());
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Built-in section type holder missing: " + e.getMessage(), e);
        }
        builtinsLoaded = true;
    }
}
