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

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.PetFlyingEntity;
import de.Keyle.MyPet.api.entity.PetSwimmingEntity;
import de.Keyle.MyPet.entity.ai.attack.PetMeleeAttackGoal;
import de.Keyle.MyPet.entity.ai.attack.PetRangedAttackGoal;
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
        Bukkit.getMobGoals().removeAllGoals(mob);

        boolean flying = pet instanceof PetFlyingEntity flyer && flyer.canFly();
        boolean swimming = pet instanceof PetSwimmingEntity swimmer && swimmer.canSwim();

        var goals = Bukkit.getMobGoals();
        PetFlyingMovementGoal flyingMovementGoal = null;
        if (!flying) {
            goals.addGoal(mob, 0, new PetFloatGoal(pet, mob));
        } else {
            flyingMovementGoal = new PetFlyingMovementGoal(pet, mob, 90.0f);
            goals.addGoal(mob, 0, flyingMovementGoal);
        }
        PetSitGoal sitGoal = new PetSitGoal(pet, mob);
        goals.addGoal(mob, 1, sitGoal);
        goals.addGoal(mob, 3, new PetSprintGoal(pet, mob, 0.25F));
        goals.addGoal(mob, 4, new PetRangedAttackGoal(pet, mob, -0.1F, 12.0F));
        if (mob instanceof Slime) {
            goals.addGoal(mob, 6, new PetCubeMobFollowOwnerGoal(pet, mob,
                    Configuration.Entity.MYPET_FOLLOW_START_DISTANCE, 2.0F, 16F));
        } else {
            goals.addGoal(mob, 6, new PetFollowOwnerGoal(pet, mob,
                    Configuration.Entity.MYPET_FOLLOW_START_DISTANCE, 2.0F, 16F, flying, swimming));
        }

        if (!flying) {
            PetControlGoal controlGoal = new PetControlGoal(pet, mob, 0.1F);
            goals.addGoal(mob, 2, controlGoal);
            PetControlTargetGoal controlTargetGoal = new PetControlTargetGoal(pet, mob, (float) mob.getWidth() + 2.5F);
            controlTargetGoal.setControlGoal(controlGoal);
            goals.addGoal(mob, 12, controlTargetGoal);
        }
        if (!swimming && !flying) {
            goals.addGoal(mob, 5, new PetMeleeAttackGoal(pet, mob, 0.1F, mob.getWidth() + 1.3, 20));
            goals.addGoal(mob, 7, new PetRandomStrollGoal(pet, mob));
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
}
