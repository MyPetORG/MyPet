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

package de.Keyle.MyPet.api.util.hooks.types;

import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.util.service.ServiceContainer;
import org.bukkit.entity.Mob;

import java.util.OptionalDouble;
import java.util.Set;

/**
 * A renderer provider (ModelEngine, BetterModel, ItemsAdder) that can draw a
 * BlockBench model on a pet's host Bukkit mob. One implementation per plugin,
 * {@code @RequiresPlugin}-gated.
 */
public interface PetModelHook extends ServiceContainer {

    /** Render {@code modelId} on the pet's live host mob and hide the host. No-op if the pet has no live entity. */
    void attach(Pet pet, String modelId);

    /**
     * Render {@code modelId} on an arbitrary (non-pet) mob and hide it — e.g. to leave a wild
     * modeled creature in the world when a rendered pet is released. Best-effort no-op if the
     * model id is unknown.
     */
    void renderOn(Mob mob, String modelId);

    /** Remove any model this hook placed on the pet's host. Safe to call when none is present. */
    void detach(Pet pet);

    /**
     * Best-effort: play a named model animation. {@code loop} carries MyPet's intent — {@code true}
     * for the one continuous event ({@code sit_loop}), {@code false} for one-shot events
     * (spawn/despawn/sit/unsit/attack) — so the renderer can force the animation's loop mode and a
     * discrete event plays once regardless of how the model authored it. No-op if unsupported or
     * the animation is unknown.
     */
    void playAnimation(Pet pet, String animation, boolean loop);

    /**
     * Best-effort: stop a named animation on the pet's model. Needed to leave a looping state
     * (e.g. {@code sit_loop}) — playing another animation only covers a loop, it doesn't remove it.
     * No-op if unsupported or the animation isn't playing.
     */
    void stopAnimation(Pet pet, String animation);

    /**
     * Best-effort length, in ticks, of a named animation on the pet's model; empty if the
     * provider can't report it (callers fall back to a fixed delay). Used to size the
     * despawn-removal delay and the sit→sit_loop hand-off.
     */
    OptionalDouble animationLength(Pet pet, String animation);

    /**
     * The model ids this provider currently renders on the given host mob (empty if none).
     * Takes the raw {@link Mob} rather than the {@link Pet} so it can also be queried against a
     * source creature that has not yet been adopted into a pet (e.g. while waiting for a
     * source plugin to finish applying its model). Used at spawn time to reconcile against
     * config: a model present here that isn't the configured one — e.g. one revived from the
     * persisted snapshot after a config change — is removed. Usually zero or one entry.
     */
    Set<String> currentModels(Mob mob);

    /** All model ids this provider has registered; empty if none are loaded or the provider is not ready. */
    Set<String> availableModels();
}
