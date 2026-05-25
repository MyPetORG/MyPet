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

package de.Keyle.MyPet.api.brain;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Per-pet-type declaration of vanilla NMS brain behaviors that
 * {@code PetGoalInstaller} should strip at spawn time, parallel to the
 * existing {@code Bukkit.getMobGoals().removeAllGoals(mob)} call that
 * strips legacy {@code Goal}-system AI.
 *
 * <p>Brain mobs (every mob added by Mojang since ~1.17 — Camel, Breeze,
 * CopperGolem, Villager, Allay, Goat, etc.) keep their full vanilla brain
 * after {@code removeAllGoals} because that sweep only touches the
 * {@code Goal} list. Brain behaviors continue to write to memories and
 * drive autonomous actions (item transport, idle wander, attack windups)
 * that conflict with MyPet's "owner-directed pet" semantics. Per-pet
 * declarations on each {@code PetXxx} class identify which behaviors are
 * actually problematic — this keeps all pet-type-specific knowledge on the
 * pet class itself rather than scattered through shared infrastructure.
 *
 * <p>Declared as a static field on the matching {@code PetXxx} class:
 *
 * <pre>{@code
 * public static final PetBrainBehaviorRemoval BRAIN_BEHAVIOR_REMOVAL =
 *         new PetBrainBehaviorRemoval(
 *                 "CopperGolem",
 *                 "TransportItemsBetweenContainers");
 * }</pre>
 *
 * <p>Construction self-registers with {@link PetBrainBehaviorRemovalRegistry}.
 * {@code PetGoalInstaller} consults the registry per spawned pet, gathers
 * the union of every declaration for the pet's type, and removes any
 * vanilla {@code BehaviorControl} whose runtime class's simple name matches
 * one of the listed strings.
 *
 * <p>Behaviors are identified by Mojang-mapped simple class name (e.g.,
 * {@code TransportItemsBetweenContainers}) rather than fully-qualified
 * name because Mojang occasionally moves classes between packages but
 * rarely renames them — and matching on simple name keeps the per-pet
 * declarations readable. Mojang renames trigger a startup warning from
 * the reflection helper, and the strip becomes a no-op for that name.
 *
 * <p>The set of names is stored in declaration order (a {@link LinkedHashSet})
 * to keep startup logging stable for operators tracing which behaviors
 * were stripped on a given pet.
 *
 * <p>Coexists with {@code PetLifecycleHook} and per-tick brain-memory
 * suppressors. Strip-at-spawn is the preferred shape for any behavior
 * that can be cleanly removed by class name; memory-level clears stay as
 * the fallback when the offending behavior is too tightly entangled with
 * vanilla state to remove safely.
 */
public final class PetBrainBehaviorRemoval {

    private final String petType;
    private final Set<String> behaviorClassNames;

    public PetBrainBehaviorRemoval(String petType, String... behaviorClassNames) {
        this.petType = petType;
        Set<String> names = new LinkedHashSet<>(behaviorClassNames.length);
        Collections.addAll(names, behaviorClassNames);
        this.behaviorClassNames = Set.copyOf(names);
        PetBrainBehaviorRemovalRegistry.register(this);
    }

    public String petType() {
        return petType;
    }

    public Set<String> behaviorClassNames() {
        return behaviorClassNames;
    }
}
