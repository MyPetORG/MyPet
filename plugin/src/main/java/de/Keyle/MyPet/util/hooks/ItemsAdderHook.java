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

package de.Keyle.MyPet.util.hooks;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.util.hooks.types.PetModelHook;
import de.Keyle.MyPet.api.util.hooks.types.PetModelSourceHook;
import de.Keyle.MyPet.api.util.service.Load;
import de.Keyle.MyPet.api.util.service.RequiresPlugin;
import de.Keyle.MyPet.api.util.service.ServiceName;
import dev.lone.itemsadder.api.CustomEntity;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

import java.util.HashSet;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;

@ServiceName("ItemsAdder")
@RequiresPlugin("ItemsAdder")
@Load(Load.State.Hooks)
public class ItemsAdderHook implements PetModelHook, PetModelSourceHook {

    @Override
    public boolean onEnable() {
        try {
            Class.forName("dev.lone.itemsadder.api.CustomEntity");
        } catch (ClassNotFoundException e) {
            return false;
        }
        return true;
    }

    @Override
    public void attach(Pet pet, String modelId) {
        renderOn(pet.getBukkitEntity(), modelId);
    }

    @Override
    public void renderOn(Mob mob, String modelId) {
        if (mob == null) {
            return;
        }
        if (!CustomEntity.isInRegistry(modelId)) {
            MyPetApi.getLogger().warning("ItemsAdder: unknown or not-yet-loaded model id '" + modelId + "'");
            return;
        }
        // Args: (namespacedId, livingEntity, frustumCulling, noHitbox, canBaseEntityBeDestroyed, hideBaseEntity)
        // frustumCulling=false, noHitbox=false, canBaseEntityBeDestroyed=false, hideBaseEntity=true
        CustomEntity.convert(modelId, (LivingEntity) mob, false, false, false, true);
    }

    @Override
    public void detach(Pet pet) {
        Mob mob = pet.getBukkitEntity();
        if (mob == null) {
            return;
        }
        CustomEntity ce = CustomEntity.byAlreadySpawned(mob);
        if (ce != null) {
            ce.destroy();
        }
    }

    @Override
    public void playAnimation(Pet pet, String animation, boolean loop) {
        // ItemsAdder plays per the animation's authored loop mode; MyPet's `loop` intent is
        // advisory here (best-effort provider) — author discrete events as non-looping.
        Mob mob = pet.getBukkitEntity();
        if (mob == null) {
            return;
        }
        CustomEntity ce = CustomEntity.byAlreadySpawned(mob);
        if (ce != null) {
            ce.playAnimation(animation);
        }
    }

    @Override
    public void stopAnimation(Pet pet, String animation) {
        Mob mob = pet.getBukkitEntity();
        if (mob == null) {
            return;
        }
        CustomEntity ce = CustomEntity.byAlreadySpawned(mob);
        if (ce == null) {
            return;
        }
        // Reflective: ItemsAdder's stop-animation API name varies by version; if absent, its
        // playAnimation replaces the current one, so a no-op here is acceptable.
        try {
            ce.getClass().getMethod("stopAnimation", String.class).invoke(ce, animation);
        } catch (Throwable ignored) {
        }
    }

    @Override
    public Set<String> currentModels(Mob mob) {
        if (mob == null) {
            return Set.of();
        }
        CustomEntity ce = CustomEntity.byAlreadySpawned(mob);
        return ce == null ? Set.of() : Set.of(ce.getNamespacedID());
    }

    @Override
    public OptionalDouble animationLength(Pet pet, String animation) {
        // ItemsAdder exposes no animation-length API; callers fall back to a fixed delay.
        return OptionalDouble.empty();
    }

    @Override
    public Set<String> availableModels() {
        try { return new HashSet<>(CustomEntity.getNamespacedIdsInRegistry()); }
        catch (Throwable t) { return Set.of(); }
    }

    @Override
    public Set<String> availableSources() {
        try { return new HashSet<>(CustomEntity.getNamespacedIdsInRegistry()); }
        catch (Throwable t) { return Set.of(); }
    }

    @Override
    public Optional<String> sourceIdOf(Entity entity) {
        CustomEntity ce = CustomEntity.byAlreadySpawned(entity);
        return ce == null ? Optional.empty() : Optional.of(ce.getNamespacedID());
    }

    @Override
    public Optional<Mob> spawnSource(String typeId, Location location) {
        // Renderers draw their model onto a host mob (rendered path); they never spawn a
        // standalone "source" creature. Release of a rendered pet uses releaseAsModeledWild.
        // This hook stays a PetModelSourceHook only for sourceIdOf() detection.
        return Optional.empty();
    }
}
