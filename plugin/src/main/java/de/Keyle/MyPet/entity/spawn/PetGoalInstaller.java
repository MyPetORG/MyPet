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

package de.Keyle.MyPet.entity.spawn;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.MobGoals;
import de.Keyle.MyPet.api.MyPetGlobal;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.PetAmphibiousEntity;
import de.Keyle.MyPet.api.entity.PetAquaticEntity;
import de.Keyle.MyPet.api.entity.PetFlyingEntity;
import de.Keyle.MyPet.api.entity.PetSwimmingEntity;
import de.Keyle.MyPet.api.goal.PetGoalRetentionRegistry;
import de.Keyle.MyPet.entity.ai.BrainDisableSpec;
import de.Keyle.MyPet.entity.ai.attack.PetMeleeAttackGoal;
import de.Keyle.MyPet.entity.ai.attack.PetRangedAttackGoal;
import de.Keyle.MyPet.entity.ai.movement.PetAquaticMovementGoal;
import de.Keyle.MyPet.entity.ai.movement.PetClimbGoal;
import de.Keyle.MyPet.entity.ai.movement.PetFlyingMovementGoal;
import de.Keyle.MyPet.entity.ai.movement.PetControlGoal;
import de.Keyle.MyPet.entity.ai.movement.PetFloatGoal;
import de.Keyle.MyPet.entity.ai.movement.PetFollowOwnerGoal;
import de.Keyle.MyPet.entity.ai.movement.PetCubeMobFollowOwnerGoal;
import de.Keyle.MyPet.entity.ai.movement.PetLookAtPlayerGoal;
import de.Keyle.MyPet.entity.ai.movement.PetRandomFlyGoal;
import de.Keyle.MyPet.entity.ai.movement.PetRandomLookaroundGoal;
import de.Keyle.MyPet.entity.ai.movement.PetRandomStrollGoal;
import de.Keyle.MyPet.entity.ai.movement.PetSitGoal;
import de.Keyle.MyPet.entity.ai.movement.PetSprintGoal;
import de.Keyle.MyPet.entity.ai.target.PetAggressiveTargetGoal;
import de.Keyle.MyPet.entity.ai.target.PetControlTargetGoal;
import de.Keyle.MyPet.entity.ai.target.PetDuelTargetGoal;
import de.Keyle.MyPet.entity.ai.target.PetFarmTargetGoal;
import de.Keyle.MyPet.entity.ai.target.PetHurtByTargetGoal;
import de.Keyle.MyPet.entity.ai.target.PetOwnerHurtByTargetGoal;
import org.bukkit.Bukkit;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Slime;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Strips vanilla AI from a Bukkit {@link Mob} and installs MyPet's AI goals.
 * Called from {@link VanillaMobSpawner} inside the {@code world.spawn()} setup
 * consumer and from {@code VanillaMobSpawner#convertInPlace} for leash-based
 * tames.
 *
 * <p>All goal constructors take {@code (Pet pet, Mob mob)} directly,
 */
public final class PetGoalInstaller {

    private PetGoalInstaller() {
    }

    public static void install(Pet pet, Mob mob) {
        stripVanillaGoals(mob, PetGoalRetentionRegistry.goalNamesFor(pet));
        BrainDisableSpec.apply(pet, mob);

        boolean flying = pet instanceof PetFlyingEntity flyer && flyer.canFly();
        boolean swimming = pet instanceof PetSwimmingEntity swimmer && swimmer.canSwim();
        // Amphibians (Axolotl, Drowned, Frog, Turtle) swim, but they also walk on land
        // under their own vanilla pathfinding — so the ground goal set below applies to
        // them and only strict water-breathers are excluded from it. `swimming` still
        // decides the swim-mode *follow* path, which PetFollowOwnerGoal evaluates
        // dynamically against mob.isInWater(); amphibians therefore keep swim follow in
        // water and gain the ground goals on land.
        boolean waterBound = swimming && !(pet instanceof PetAmphibiousEntity);

        var goals = Bukkit.getMobGoals();
        PetFlyingMovementGoal flyingMovementGoal = null;
        if (!flying) {
            goals.addGoal(mob, 0, new PetFloatGoal(pet, mob));
        } else {
            flyingMovementGoal = new PetFlyingMovementGoal(pet, mob, 90.0f);
            goals.addGoal(mob, 0, flyingMovementGoal);
        }
        // Water-breathers only (PetAquaticEntity, not the amphibious siblings): their
        // vanilla navigation is water-bound, so out of water they have no movement input
        // at all and just flop. Amphibious pets (axolotl, frog, turtle, drowned) walk on
        // land under their own pathfinding and must not be pushed around by velocity.
        // Gated on `swimming` for the same reason Control/Melee below are: with CanSwim
        // off the pet is treated as a ground pet everywhere, and the velocity push would
        // fight the pathfinder-driven goals it gets instead.
        if (swimming && pet instanceof PetAquaticEntity) {
            goals.addGoal(mob, 0, new PetAquaticMovementGoal(pet, mob));
        }
        PetSitGoal sitGoal = new PetSitGoal(pet, mob);
        goals.addGoal(mob, 1, sitGoal);
        goals.addGoal(mob, 3, new PetSprintGoal(pet, mob, 0.25F));
        goals.addGoal(mob, 4, new PetRangedAttackGoal(pet, mob, -0.1F, 12.0F));
        if (mob instanceof Slime) {
            goals.addGoal(mob, 6, new PetCubeMobFollowOwnerGoal(pet, mob,
                    MyPetGlobal.Entity.MYPET_FOLLOW_START_DISTANCE.get(), 2.0F, 12F));
        } else {
            goals.addGoal(mob, 6, new PetFollowOwnerGoal(pet, mob,
                    MyPetGlobal.Entity.MYPET_FOLLOW_START_DISTANCE.get(), 2.0F, 12F, flying, swimming));
        }

        // Control + melee install for ground, flying and amphibian pets (strict
        // water-breathers excluded — off their water-bound navigation they have no
        // pathfinder to drive, and PetAquaticMovementGoal owns their land motion). The
        // higher walkSpeedModifier for flying pets (0.8F control, 0.7F melee
        // vs 0.1F ground) matches the pre-NMS EntityMyFlyingPet#setPathfinder
        // values: flying movement needs a steeper modifier to close on the
        // steered/melee target. Without these, on-hit skills (Fire, Poison,
        // Bleed, etc.) silently no-op on flying pets because melee never lands.
        if (!waterBound) {
            PetControlGoal controlGoal = new PetControlGoal(pet, mob, flying ? 0.8F : 0.1F);
            goals.addGoal(mob, 2, controlGoal);
            PetControlTargetGoal controlTargetGoal = new PetControlTargetGoal(pet, mob, (float) mob.getWidth() + 2.5F);
            controlTargetGoal.setControlGoal(controlGoal);
            goals.addGoal(mob, 12, controlTargetGoal);
            goals.addGoal(mob, 5, new PetMeleeAttackGoal(pet, mob, flying ? 0.7F : 0.1F, mob.getWidth() + 1.3, 20));
        }
        if (!waterBound && !flying) {
            goals.addGoal(mob, 7, new PetRandomStrollGoal(pet, mob));
            goals.addGoal(mob, 6, new PetClimbGoal(pet, mob));
        }
        if (flying) {
            goals.addGoal(mob, 7, new PetRandomFlyGoal(pet, mob, flyingMovementGoal));
        }
        goals.addGoal(mob, 8, new PetLookAtPlayerGoal(pet, mob, 8.0F));
        goals.addGoal(mob, 9, new PetRandomLookaroundGoal(pet, mob));
        goals.addGoal(mob, 10, new PetOwnerHurtByTargetGoal(pet, mob));
        goals.addGoal(mob, 11, new PetHurtByTargetGoal(pet, mob));
        goals.addGoal(mob, 13, new PetAggressiveTargetGoal(pet, mob, 16));
        goals.addGoal(mob, 14, new PetFarmTargetGoal(pet, mob, 16));
        goals.addGoal(mob, 15, new PetDuelTargetGoal(pet, mob, 5));
    }

    /**
     * Strips vanilla goals from the mob, keeping any whose key matches a name
     * in {@code retain}. Empty {@code retain} → fast-path
     * {@code removeAllGoals} (the default for pets that don't declare a
     * {@link de.Keyle.MyPet.api.goal.PetGoalRetention}). Retained goals
     * continue to tick alongside MyPet's installed goals; the per-pet
     * declaration owns the responsibility for picking goals that compose
     * cleanly with the MyPet movement / target stack.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void stripVanillaGoals(Mob mob, Set<String> retain) {
        MobGoals mobGoals = Bukkit.getMobGoals();
        if (retain.isEmpty()) {
            mobGoals.removeAllGoals(mob);
            return;
        }
        // Snapshot first — removeGoal during iteration of the live collection
        // would CME. Match by the path part of the GoalKey's NamespacedKey
        // (e.g., "phantom_circle_around_anchor") so declarations stay readable.
        List<Goal> current = new ArrayList<>(mobGoals.getAllGoals(mob));
        for (Goal goal : current) {
            String keyName = goal.getKey().getNamespacedKey().getKey();
            if (!retain.contains(keyName)) {
                mobGoals.removeGoal(mob, goal);
            }
        }
    }
}
