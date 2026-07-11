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

package de.Keyle.MyPet.entity.ai.movement;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import de.Keyle.MyPet.api.entity.Pet;
import org.bukkit.entity.Mob;
import de.Keyle.MyPet.entity.ai.PetGoalKey;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Paper {@link Goal} replacement for vanilla's
 * {@code RandomLookAroundGoal} — makes an idle pet glance around in a
 * random horizontal direction for natural head motion when no nearby
 * player has claimed its attention.
 *
 * <p>Activates on a ~2% per-tick roll when the pet has no combat target
 * and is carrying no passengers. Once triggered, a random horizontal unit
 * vector is picked and the pet's head holds that heading for 20–39 ticks.
 *
 * <p>This is the default fallback for the {@link GoalType#LOOK} bucket;
 * {@link PetLookAtPlayerGoal} preempts it whenever a real player is
 * available to look at.
 */
public class PetRandomLookaroundGoal implements Goal<Mob> {

    private final Pet pet;
    private final Mob mob;
    private double directionX;
    private double directionZ;
    private int ticksUntilStopLooking;

    /**
     * @param petEntity the pet whose head will glance around
     */
    public PetRandomLookaroundGoal(Pet pet, Mob mob) {
        this.pet = pet;
        this.mob = mob;
    }

    @Override
    public boolean shouldActivate() {
        // Roll the (near-always-false) chance first — cheapest gate, no entity access.
        if (ThreadLocalRandom.current().nextFloat() >= 0.02F) {
            return false;
        }
        if (!Bukkit.isOwnedByCurrentRegion(mob)) {
            return false;
        }
        if (pet.hasTarget() && !pet.getPetTarget().isDead()) {
            return false;
        }
        return mob.isEmpty();
    }

    @Override
    public boolean shouldStayActive() {
        if (!Bukkit.isOwnedByCurrentRegion(mob)) {
            return false;
        }
        return this.ticksUntilStopLooking > 0 && mob.isEmpty();
    }

    @Override
    public void start() {
        double angle = Math.PI * 2.0 * ThreadLocalRandom.current().nextDouble();
        this.directionX = Math.cos(angle);
        this.directionZ = Math.sin(angle);
        this.ticksUntilStopLooking = 20 + ThreadLocalRandom.current().nextInt(20);
    }

    @Override
    public void tick() {
        // Skip this tick if the mob isn't currently owned by this region thread. This happens
        // briefly on Folia during cross-region teleport transitions — the old region may still
        // run the goal for a tick or two while the entity has already moved to the new region.
        if (!Bukkit.isOwnedByCurrentRegion(mob)) {
            return;
        }
        Location loc = mob.getLocation();
        mob.lookAt(
                loc.getX() + this.directionX,
                loc.getY() + mob.getEyeHeight(),
                loc.getZ() + this.directionZ,
                mob.getHeadRotationSpeed(),
                mob.getMaxHeadPitch()
        );
        this.ticksUntilStopLooking--;
    }

    @Override
    public @NotNull GoalKey<Mob> getKey() {
        return PetGoalKey.RANDOM_LOOKAROUND;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.LOOK);
    }
}
