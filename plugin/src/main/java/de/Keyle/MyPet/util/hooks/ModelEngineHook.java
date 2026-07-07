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

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.animation.BlueprintAnimation;
import com.ticxo.modelengine.api.animation.property.IAnimationProperty;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
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

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;

@ServiceName("ModelEngine")
@RequiresPlugin("ModelEngine")
@Load(Load.State.Hooks)
public class ModelEngineHook implements PetModelHook, PetModelSourceHook {

    @Override
    public boolean onEnable() {
        try {
            Class.forName("com.ticxo.modelengine.api.ModelEngineAPI");
        } catch (ClassNotFoundException e) {
            return false;
        }
        return true;
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
        // Idempotent: if this mob already renders the model (e.g. re-adopting a released modeled
        // mob), just re-assert the hidden base rather than building a second ModeledEntity/model.
        ModeledEntity existing = ModelEngineAPI.getModeledEntity(mob);
        if (existing != null && existing.getModels().containsKey(modelId)) {
            mob.setInvisible(true);
            existing.setBaseEntityVisible(false);
            return;
        }
        ActiveModel model = ModelEngineAPI.createActiveModel(modelId);
        if (model == null) {
            MyPetApi.getLogger().warning("ModelEngine: unknown model id '" + modelId + "'");
            return;
        }
        // Hide the vanilla base synchronously (Bukkit invisibility is reflected in the very first
        // spawn packet) so the host mob never flashes for the tick before ModelEngine's tracker
        // starts rendering the model. setBaseEntityVisible below is ModelEngine's own hide.
        mob.setInvisible(true);
        ModeledEntity modeled = ModelEngineAPI.createModeledEntity(mob);
        modeled.addModel(model, true);
        modeled.setBaseEntityVisible(false); // hide the vanilla host under the model
    }

    @Override
    public void detach(Pet pet) {
        Mob mob = pet.getBukkitEntity();
        if (mob == null) {
            return;
        }
        ModeledEntity modeled = ModelEngineAPI.getModeledEntity(mob);
        if (modeled != null) {
            modeled.destroy();
        }
        mob.setInvisible(false); // model removed → let the vanilla mob show again
    }

    @Override
    public void playAnimation(Pet pet, String animation, boolean loop) {
        Mob mob = pet.getBukkitEntity();
        if (mob == null) {
            return;
        }
        ModeledEntity modeled = ModelEngineAPI.getModeledEntity(mob);
        if (modeled == null) {
            return;
        }
        // Force MyPet's loop intent onto the played animation so a discrete one-shot (attack,
        // spawn, …) auto-returns to idle even if the blueprint authored it as LOOP/HOLD — without
        // this a looping-authored attack sticks playing forever after the hit.
        BlueprintAnimation.LoopMode mode = loop ? BlueprintAnimation.LoopMode.LOOP : BlueprintAnimation.LoopMode.ONCE;
        for (ActiveModel model : modeled.getModels().values()) {
            IAnimationProperty property = model.getAnimationHandler().playAnimation(animation, 0.2, 0.2, 1.0, false);
            if (property != null) {
                property.setForceLoopMode(mode);
            }
        }
    }

    @Override
    public void stopAnimation(Pet pet, String animation) {
        Mob mob = pet.getBukkitEntity();
        if (mob == null) {
            return;
        }
        ModeledEntity modeled = ModelEngineAPI.getModeledEntity(mob);
        if (modeled == null) {
            return;
        }
        for (ActiveModel model : modeled.getModels().values()) {
            try {
                model.getAnimationHandler().stopAnimation(animation);
            } catch (Throwable ignored) {
                // unknown / not-currently-playing animation → nothing to stop
            }
        }
    }

    @Override
    public OptionalDouble animationLength(Pet pet, String animation) {
        Mob mob = pet.getBukkitEntity();
        if (mob == null) {
            return OptionalDouble.empty();
        }
        // Reflective so a ModelEngine API-shape change degrades to the caller's fallback
        // delay rather than a compile/runtime break. ModelEngine's BlueprintAnimation#getLength()
        // is in SECONDS (BlockBench authors animation length in seconds); the PetModelHook contract
        // is TICKS, so convert (×20). Without this a 0.4s despawn reports as ~0.4 "ticks" and the
        // host is removed after 1 tick, cutting the animation off before it's visible.
        try {
            ModeledEntity modeled = ModelEngineAPI.getModeledEntity(mob);
            if (modeled == null) {
                return OptionalDouble.empty();
            }
            for (ActiveModel model : modeled.getModels().values()) {
                Object blueprint = model.getClass().getMethod("getBlueprint").invoke(model);
                Object animations = blueprint.getClass().getMethod("getAnimations").invoke(blueprint);
                if (animations instanceof Map<?, ?> map) {
                    Object anim = map.get(animation);
                    if (anim != null) {
                        Object length = anim.getClass().getMethod("getLength").invoke(anim);
                        if (length instanceof Number n) {
                            return OptionalDouble.of(n.doubleValue() * 20.0);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
            // unknown animation / API mismatch → let the caller use its fallback delay
        }
        return OptionalDouble.empty();
    }

    @Override
    public Set<String> currentModels(Pet pet) {
        Mob mob = pet.getBukkitEntity();
        if (mob == null) {
            return Set.of();
        }
        ModeledEntity modeled = ModelEngineAPI.getModeledEntity(mob);
        if (modeled == null) {
            return Set.of();
        }
        return new HashSet<>(modeled.getModels().keySet());
    }

    @Override
    public Set<String> availableModels() {
        try { return new HashSet<>(ModelEngineAPI.getAPI().getModelRegistry().getKeys()); }
        catch (Throwable t) { return Set.of(); }
    }

    @Override
    public Set<String> availableSources() {
        // A ModelEngine "source" creature is just any model id — the same set the renderer exposes.
        return availableModels();
    }

    @Override
    public Optional<String> sourceIdOf(Entity entity) {
        if (PetEntityMarker.isMarked(entity)) {
            return Optional.empty(); // already a MyPet pet, not a wild source
        }
        try {
            ModeledEntity modeled = ModelEngineAPI.getModeledEntity(entity);
            if (modeled == null) {
                return Optional.empty();
            }
            for (String id : modeled.getModels().keySet()) {
                return Optional.of(id);
            }
        } catch (Throwable ignored) {
            // no ME model on this entity / API mismatch → not an ME source
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
}
