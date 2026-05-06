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

import de.Keyle.MyPet.api.entity.PersistedPet;
import de.Keyle.MyPet.api.entity.StoredPet;
import net.kyori.adventure.nbt.CompoundBinaryTag;

/**
 * Plugin-internal accessors for the raw NBT blobs carried by every
 * {@link StoredPet}: the vanilla entity snapshot ({@code info}) and the
 * aggregate per-skill compound ({@code skillInfo}). Both blobs were removed
 * from the public api in 4.0.0 ({@code StoredPet.getInfo} /
 * {@code StoredPet.getSkillInfo} / {@code MyPet.setInfo} /
 * {@code MyPet.setSkills}) so addons cannot manipulate raw vendor NBT —
 * repository, migration, and listener serialization paths route through
 * here instead.
 */
public final class PetInfoAccess {

    private PetInfoAccess() {}

    /** Read the entity-NBT blob from any stored form. Sealed switch is exhaustive. */
    public static CompoundBinaryTag read(StoredPet pet) {
        return switch (pet) {
            case PersistedPet p -> p.info();
            case de.Keyle.MyPet.api.entity.MyPet live -> ((MyPet) live).getInfo();
        };
    }

    /** Write the entity-NBT blob to a live pet. */
    public static void write(de.Keyle.MyPet.api.entity.MyPet live, CompoundBinaryTag info) {
        ((MyPet) live).setInfo(info);
    }

    /** Read the aggregate per-skill NBT compound from any stored form. */
    public static CompoundBinaryTag readSkillInfo(StoredPet pet) {
        return switch (pet) {
            case PersistedPet p -> p.skillInfo();
            case de.Keyle.MyPet.api.entity.MyPet live -> ((MyPet) live).getSkillInfo();
        };
    }
}
