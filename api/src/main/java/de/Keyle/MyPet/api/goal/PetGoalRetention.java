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

package de.Keyle.MyPet.api.goal;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Per-pet-type declaration of vanilla Bukkit goals that {@code PetGoalInstaller}
 * should keep on the live mob at spawn time, instead of stripping every goal
 * via {@code Bukkit.getMobGoals().removeAllGoals(mob)}.
 *
 * <p>The default behavior — when no {@code PetGoalRetention} is declared for a
 * pet type — is to strip every vanilla goal. This works for pets whose vanilla
 * AI MyPet replaces entirely with its own goals (Wolf, Pig, Camel, etc.). But
 * some pet types have vanilla goals that are uniquely valuable (e.g., Phantom's
 * {@code PhantomCircleAroundAnchorGoal} translates the per-tick
 * {@code setAnchorLocation} into orbit motion via {@code moveTargetPoint};
 * stripping it leaves the phantom flying toward {@code Vec3.ZERO}). Declaring a
 * retention list keeps those goals intact.
 *
 * <p>Declared as a static field on the matching {@code PetXxx} class:
 *
 * <pre>{@code
 * public static final PetGoalRetention GOAL_RETENTION = new PetGoalRetention(
 *         "Phantom",
 *         "phantom_circle_around_anchor");
 * }</pre>
 *
 * <p>Construction self-registers with {@link PetGoalRetentionRegistry}.
 * {@code PetGoalInstaller} consults the registry per spawned pet and removes
 * only the goals whose key isn't in the retention set.
 *
 * <p>Goal names are the path part of the vanilla {@code GoalKey}'s
 * {@code NamespacedKey} (i.e., the snake_case identifier without the
 * {@code minecraft:} namespace). Match by name avoids per-mob {@code Goal}
 * subclass typing concerns and is stable across Mojang's occasional
 * reorganization of vanilla goal classes.
 *
 * <p>The set of names is stored in declaration order (a {@link LinkedHashSet})
 * for stable log output if the installer ever logs retained goals.
 */
public final class PetGoalRetention {

    private final String petType;
    private final Set<String> goalKeyNames;

    public PetGoalRetention(String petType, String... goalKeyNames) {
        this.petType = petType;
        Set<String> names = new LinkedHashSet<>(goalKeyNames.length);
        Collections.addAll(names, goalKeyNames);
        this.goalKeyNames = Set.copyOf(names);
        PetGoalRetentionRegistry.register(this);
    }

    public String petType() {
        return petType;
    }

    public Set<String> goalKeyNames() {
        return goalKeyNames;
    }
}
