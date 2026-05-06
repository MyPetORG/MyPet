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

package de.Keyle.MyPet.entity.leashing;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.leashing.LeashFlag;
import de.Keyle.MyPet.api.entity.leashing.LeashFlagManager;

import java.util.List;
import java.util.function.Supplier;

/**
 * Registers MyPet's bundled leash-flag implementations with the active
 * {@link LeashFlagManager}.
 *
 * <p>A leash flag is a precondition (e.g. "pet must be a baby", "pet has low HP", "world
 * is allow-listed") that gates whether a player can leash a particular wild mob into a pet.
 * Each flag is instantiated lazily via the {@link Supplier} table so the same registrar can
 * be re-invoked across reloads without leaking shared state across runs.</p>
 *
 * <p>Invoked once during plugin enable, after the leash-flag manager service has been
 * activated.</p>
 */
public final class BuiltInLeashFlags {

    private static final List<Supplier<LeashFlag>> FLAGS = List.of(
            AdultFlag::new,
            AngryFlag::new,
            BabyFlag::new,
            BelowHpFlag::new,
            CanBreedFlag::new,
            ChanceFlag::new,
            ImpossibleFlag::new,
            LowHpFlag::new,
            ScreamingFlag::new,
            SizeFlag::new,
            TamedFlag::new,
            UserCreatedFlag::new,
            WildFlag::new,
            WorldFlag::new,
            PermissionFlag::new,
            HeartLinkedFlag::new
    );

    private BuiltInLeashFlags() {
    }

    /**
     * Constructs a fresh instance of each built-in leash flag and registers it with the
     * active {@link LeashFlagManager}.
     */
    public static void register() {
        LeashFlagManager manager = MyPetApi.getLeashFlagManager();
        for (Supplier<LeashFlag> flag : FLAGS) {
            manager.registerLeashFlag(flag.get());
        }
    }
}
