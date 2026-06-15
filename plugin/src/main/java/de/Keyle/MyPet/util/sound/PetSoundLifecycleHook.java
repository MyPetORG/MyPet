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
import de.Keyle.MyPet.api.lifecycle.PetLifecycleHook;
import org.bukkit.entity.Mob;

/**
 * Owns the global {@link PetLifecycleHook} that keeps the
 * {@link PetSoundRegistry} in sync with every pet spawn / despawn,
 * regardless of pet type.
 *
 * <p>Static initialization happens lazily — the class is touched once
 * from {@link PetSoundService#onEnable()} to force the {@code GLOBAL_HOOK}
 * registration. The Bukkit-pet lifecycle then drives all subsequent
 * registry mutations.
 */
public final class PetSoundLifecycleHook {

    public static final PetLifecycleHook GLOBAL_HOOK = PetLifecycleHook.global(
            PetSoundLifecycleHook::onSpawn,
            PetSoundLifecycleHook::onDespawn
    );

    private PetSoundLifecycleHook() {}

    private static void onSpawn(Pet pet) {
        Mob mob = pet.getBukkitEntity();
        if (mob == null) return;
        PetSoundRegistry.add(mob.getEntityId(), pet);
    }

    private static void onDespawn(Pet pet) {
        Mob mob = pet.getBukkitEntity();
        if (mob == null) return;
        PetSoundRegistry.remove(mob.getEntityId());
    }
}
