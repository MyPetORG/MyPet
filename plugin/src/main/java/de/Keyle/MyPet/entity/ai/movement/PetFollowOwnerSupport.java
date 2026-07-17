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

import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.ai.navigation.AbstractNavigation;
import de.Keyle.MyPet.entity.ai.PetGoalWorlds;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

/**
 * Cross-cutting helpers for "snap to owner" follow goals.
 *
 * <p>Both {@link PetFollowOwnerGoal} and {@link PetCubeMobFollowOwnerGoal} need the same
 * cross-region (Folia), cross-world, and distance-triggered teleport behavior. The logic
 * is not specific to either follow strategy — it's "any goal that snaps to owner needs
 * this" — so it lives here as static methods rather than being duplicated.
 */
public final class PetFollowOwnerSupport {

    private PetFollowOwnerSupport() {
    }

    /**
     * Mutable carrier for the {@code waitForGround} flag tracked across ticks.
     *
     * <p>{@code waitForGround} prevents teleporting to an airborne owner — when the owner
     * is flying or gliding mid-jump, we wait for them to land before snapping to them.
     * Held in a small object so {@link #teleportIfTooFar} can mutate it without callers
     * having to pass it by reference.
     */
    public static final class TeleportState {
        public boolean waitForGround;
    }

    /**
     * Snap-teleports the pet to the owner if the owner has moved into a different Folia
     * region. Returns {@code true} if the caller should bail out of its tick (either a
     * snap was issued, or the pet can't move / has a target so we abort safely without
     * touching cross-region state).
     *
     * @param nav optional navigation handle to stop before teleporting; pass {@code null}
     *            for movement strategies (e.g. slime hops) that don't use pathfinding. When
     *            {@code null}, the caller is responsible for stopping any ongoing movement
     *            itself before this method is called — {@code nav.stop()} is load-bearing
     *            state cleanup in the pathfinding-based caller.
     */
    public static boolean snapAcrossRegionsIfNeeded(Pet pet, Mob mob, Player owner,
                                                    @Nullable AbstractNavigation nav) {
        if (Bukkit.isOwnedByCurrentRegion(owner)) {
            return false;
        }
        if (!pet.canMove() || pet.hasTarget()) {
            return true;
        }
        Location ownerLocation = owner.getLocation();
        if (nav != null) {
            nav.stop();
        }
        mob.setVelocity(new Vector(0, 0, 0));
        // Reset fall distance BEFORE teleportAsync — once the async teleport begins,
        // the entity is in a transitional state on Folia and may no longer be owned
        // by the current region, so any further state mutation would fail the thread
        // check.
        mob.setFallDistance(0);
        mob.teleportAsync(new Location(owner.getWorld(),
                ownerLocation.getX(), ownerLocation.getY(), ownerLocation.getZ()));
        return true;
    }

    /** Returns {@code true} when the owner is in a different world than the pet. */
    public static boolean isCrossWorld(Mob mob, Player owner) {
        return PetGoalWorlds.isCrossWorld(mob, owner);
    }

    /**
     * Distance-triggered teleport. Returns {@code true} if a teleport was issued (caller
     * should bail out of its tick to avoid double-acting on a now-teleported pet).
     *
     * <p>Mutates {@code state.waitForGround}: sets it to true while the owner is flying
     * or gliding (so we don't snap to an airborne owner), clears it once the owner is on
     * the ground again. Subsequent ticks proceed to the distance check normally.
     *
     * <p><b>Precondition:</b> the caller must have already gated on
     * {@link #isCrossWorld(Mob, Player)} returning {@code false}. This helper does not
     * recheck cross-world state, and calling it with an owner in a different world would
     * mutate {@code state.waitForGround} based on the owner's airborne state in that other
     * world — which is rarely what the caller wants.
     *
     * @param flyingPet       whether this pet flies — flying pets ignore the
     *                        owner-airborne deferral and the fall-distance check.
     * @param controlIsMoving whether {@link PetControlGoal} currently has a {@code moveTo}
     *                        target — if so, control wins and we don't teleport.
     * @param nav             optional navigation handle to stop before teleporting; pass
     *                        {@code null} for non-pathfinding strategies.
     */
    public static boolean teleportIfTooFar(Pet pet, Mob mob, Player owner,
                                           double distanceSqr, double teleportDistanceSqr,
                                           boolean flyingPet, boolean controlIsMoving,
                                           @Nullable AbstractNavigation nav,
                                           TeleportState state) {
        if (!pet.canMove()) {
            return false;
        }
        if ((owner.isFlying() || owner.isGliding()) && !flyingPet) {
            state.waitForGround = true;
            return false;
        }
        if (state.waitForGround) {
            if (owner.isOnGround()) {
                state.waitForGround = false;
            }
            return false;
        }
        if (!flyingPet && owner.getFallDistance() > 4) {
            return false;
        }
        if (distanceSqr < teleportDistanceSqr) {
            return false;
        }
        if (controlIsMoving || pet.hasTarget()) {
            return false;
        }
        Location ownerLocation = owner.getLocation();
        // The canSpawn passability check was previously applied here, but it
        // reads blocks at the owner's location — which on Folia may be in a
        // different region from the pet. Skip it: the owner is already
        // standing at ownerLocation, so the space is passable by definition.
        if (nav != null) {
            nav.stop();
        }
        mob.setVelocity(new Vector(0, 0, 0));
        // Reset fall distance BEFORE teleportAsync — see snapAcrossRegionsIfNeeded for rationale.
        mob.setFallDistance(0);
        mob.teleportAsync(new Location(owner.getWorld(),
                ownerLocation.getX(), ownerLocation.getY(), ownerLocation.getZ()));
        return true;
    }
}
