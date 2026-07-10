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
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ServiceName("BetterModel")
@RequiresPlugin("BetterModel")
@Load(Load.State.Hooks)
public class BetterModelHook implements PetModelHook, PetModelSourceHook {

    // Cached reflective handles (resolved in onEnable). null when BetterModel absent.
    private Class<?> betterModelClass;
    private Method adapt;            // BukkitAdapter.adapt(Entity) -> PlatformEntity
    private Method model;            // BetterModel.model(String) -> Optional<ModelRenderer>
    private Method registryOrNull;   // BetterModel.registryOrNull(UUID) -> EntityTrackerRegistry|null

    @Override
    public boolean onEnable() {
        try {
            betterModelClass = Class.forName("kr.toxicity.model.api.BetterModel");
            Class<?> adapter = Class.forName("kr.toxicity.model.api.bukkit.platform.BukkitAdapter");
            adapt = adapter.getMethod("adapt", Entity.class);
            model = betterModelClass.getMethod("model", String.class);
            registryOrNull = betterModelClass.getMethod("registryOrNull", UUID.class);
            return true;
        } catch (Throwable t) {
            // BetterModel is present but its reflective API shape does not match the
            // 3.2.0 shape this hook targets (e.g. a different BetterModel version, or
            // a renamed method). Log WHY rather than disabling silently — this is the
            // difference between "BetterModel absent" (this never runs) and "BetterModel
            // present but incompatible" (this fires with the exact missing class/method).
            MyPetApi.getLogger().warning("BetterModel hook could NOT enable — its API does not match what MyPet expects ("
                    + t.getClass().getSimpleName() + ": " + t.getMessage()
                    + "). MyPet targets the BetterModel 3.2.0 API shape; report your BetterModel version.");
            return false;
        }
    }

    @Override
    public void attach(Pet pet, String modelId) {
        Mob mob = pet.getBukkitEntity();
        if (mob == null) {
            return;
        }
        applyModel(mob, modelId);
    }

    @Override
    public void renderOn(Mob mob, String modelId) {
        if (mob != null) {
            applyModel(mob, modelId);
        }
    }

    /** Render {@code modelId} on a live Bukkit mob. Shared by attach (pets), spawnSource, and renderOn. */
    private void applyModel(Mob mob, String modelId) {
        if (adapt == null) {
            return;
        }
        try {
            Object platformEntity = adapt.invoke(null, mob);
            Object optionalRenderer = model.invoke(null, modelId);                  // Optional<ModelRenderer>
            Object renderer = optionalRenderer.getClass().getMethod("orElse", Object.class)
                    .invoke(optionalRenderer, (Object) null);
            if (renderer == null) {
                MyPetApi.getLogger().warning("BetterModel: unknown model id '" + modelId + "'");
                return; // unknown model id
            }
            // Unlike ModelEngine/ItemsAdder, no explicit host-hide is needed here: BetterModel hides
            // the base entity itself once a model tracker is bound (verified in-server — the render
            // shows no double-body). This mirrors why detach doesn't un-hide the host.
            unaryMethod(renderer.getClass(), "getOrCreate").invoke(renderer, platformEntity);
        } catch (Throwable t) {
            MyPetApi.getLogger().warning("BetterModel applyModel('" + modelId + "') failed: " + t.getMessage());
        }
    }

    @Override
    public void detach(Pet pet) {
        Mob mob = pet.getBukkitEntity();
        if (mob == null || registryOrNull == null) {
            return;
        }
        try {
            Object registry = registryOrNull.invoke(null, mob.getUniqueId());
            if (registry != null) {
                registry.getClass().getMethod("close").invoke(registry);
            }
        } catch (Throwable t) {
            MyPetApi.getLogger().warning("BetterModel detach failed: " + t.getMessage());
        }
    }

    @Override
    public void playAnimation(Pet pet, String animation, boolean loop) {
        // BetterModel plays per the model's authored loop mode; MyPet's `loop` intent is advisory
        // here (best-effort provider) — author discrete events (e.g. attack) as non-looping.
        Mob mob = pet.getBukkitEntity();
        if (mob == null || registryOrNull == null) {
            return;
        }
        try {
            Object registry = registryOrNull.invoke(null, mob.getUniqueId());
            if (registry == null) {
                return;
            }
            Object trackers = registry.getClass().getMethod("trackers").invoke(registry);
            for (Object tracker : (Collection<?>) trackers) {
                tracker.getClass().getMethod("animate", String.class).invoke(tracker, animation);
            }
        } catch (Throwable t) {
            MyPetApi.getLogger().warning("BetterModel animate('" + animation + "') failed: " + t.getMessage());
        }
    }

    @Override
    public void stopAnimation(Pet pet, String animation) {
        Mob mob = pet.getBukkitEntity();
        if (mob == null || registryOrNull == null) {
            return;
        }
        try {
            Object registry = registryOrNull.invoke(null, mob.getUniqueId());
            if (registry == null) {
                return;
            }
            Object trackers = registry.getClass().getMethod("trackers").invoke(registry);
            for (Object tracker : (Collection<?>) trackers) {
                try {
                    tracker.getClass().getMethod("stopAnimation", String.class).invoke(tracker, animation);
                } catch (Throwable ignored) {
                    // BetterModel may auto-replace animations (no explicit stop) → nothing to do
                }
            }
        } catch (Throwable t) {
            MyPetApi.getLogger().warning("BetterModel stopAnimation('" + animation + "') failed: " + t.getMessage());
        }
    }

    @Override
    public OptionalDouble animationLength(Pet pet, String animation) {
        // BetterModel's 3.2.0 reflective surface exposes no stable animation-length getter;
        // callers fall back to a fixed delay.
        return OptionalDouble.empty();
    }

    @Override
    public Set<String> currentModels(Mob mob) {
        if (mob == null || registryOrNull == null) {
            return Set.of();
        }
        try {
            Object registry = registryOrNull.invoke(null, mob.getUniqueId());
            if (registry == null) {
                return Set.of();
            }
            Object trackers = registry.getClass().getMethod("trackers").invoke(registry);
            Set<String> names = new HashSet<>();
            for (Object tracker : (Collection<?>) trackers) {
                Object name = tracker.getClass().getMethod("name").invoke(tracker);
                if (name != null) {
                    names.add(name.toString());
                }
            }
            return names;
        } catch (Throwable t) {
            return Set.of();
        }
    }

    @Override
    public Set<String> availableModels() {
        if (betterModelClass == null) return Set.of();
        try {
            Object keys = betterModelClass.getMethod("modelKeys").invoke(null);
            return keys instanceof Collection<?> c
                    ? c.stream().map(Object::toString).collect(Collectors.toSet()) : Set.of();
        } catch (Throwable t) { return Set.of(); }
    }

    @Override
    public Set<String> availableSources() {
        // A BetterModel "source" creature is just any model id — the same set the renderer exposes.
        return availableModels();
    }

    @Override
    public Optional<String> sourceIdOf(Entity entity) {
        if (registryOrNull == null || PetEntityMarker.isMarked(entity)) {
            return Optional.empty(); // not modeled by us, or already a MyPet pet
        }
        try {
            Object registry = registryOrNull.invoke(null, entity.getUniqueId());
            if (registry == null) {
                return Optional.empty();
            }
            Object trackers = registry.getClass().getMethod("trackers").invoke(registry);
            for (Object tracker : (Collection<?>) trackers) {
                Object name = tracker.getClass().getMethod("name").invoke(tracker);
                if (name != null) {
                    return Optional.of(name.toString());
                }
            }
        } catch (Throwable ignored) {
            // API mismatch / no model on this entity → not a BetterModel source
        }
        return Optional.empty();
    }

    @Override
    public Optional<Mob> spawnSource(String typeId, Location location) {
        // Renderers draw their model onto a host mob (rendered path); they never spawn a
        // standalone "source" creature. Release of a rendered pet uses releaseAsModeledWild.
        // This hook stays a PetModelSourceHook only for sourceIdOf() detection.
        return Optional.empty();
    }

    /** Finds the single-argument overload of {@code name} (e.g. getOrCreate(PlatformEntity)). */
    private static Method unaryMethod(Class<?> type, String name) throws NoSuchMethodException {
        for (Method m : type.getMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == 1) {
                return m;
            }
        }
        throw new NoSuchMethodException(type.getName() + "#" + name + " (1-arg)");
    }
}
