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

package de.Keyle.MyPet.entity.ai.target;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import org.bukkit.entity.Mob;
import de.Keyle.MyPet.api.entity.ai.target.TargetPriority;
import de.Keyle.MyPet.api.skill.skills.Behavior;
import de.Keyle.MyPet.api.skill.skills.Behavior.BehaviorMode;
import de.Keyle.MyPet.entity.ai.PetGoalKey;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

/**
 * Paper {@link Goal} that acquires a hostile target for a pet whose
 * {@link Behavior} skill is set to {@link BehaviorMode#Aggressive}.
 *
 * <p>Scans entities near the owner (not near the pet), which produces the
 * intuitive "guard my owner" behaviour that players expect from aggressive
 * pets. Each candidate must pass every one of the following filters:
 * <ul>
 *   <li>It is a {@link LivingEntity} other than the pet itself.</li>
 *   <li>It is not an {@link ArmorStand} and not dead.</li>
 *   <li>The pet-to-candidate distance is at most ~9.5 blocks (91 m²).</li>
 *   <li>Ownership filtering rejects the owner's own player, other pets
 *       owned by allies (via {@code canHurt(..., true)}), and tamed
 *       mobs belonging to the owner or allies.</li>
 *   <li>A final unconditional
 *       {@code canHurt(owner, living)} check enforces region-plugin
 *       protection (WorldGuard, GriefPrevention, …) and catches untamed
 *       {@link Tameable} entities that slipped past the ownership chain.</li>
 * </ul>
 *
 * <p>Once selected, the target is installed via
 * {@link Pet#setTarget(LivingEntity, TargetPriority)} at
 * {@link TargetPriority#Aggressive}; the attack goals then pick the target
 * up on their own. The goal stops when the target dies, teleports worlds,
 * or the pet drifts more than ~20 blocks from it / more than ~24.5 blocks
 * from the owner.
 *
 * <p>Declares {@link GoalType#TARGET}, making it mutually exclusive with
 * the other target-acquisition goals in this package.
 */
public class PetAggressiveTargetGoal implements Goal<Mob> {

    private final Pet pet;
    private final Mob mob;
    private final double range;
    private LivingEntity target;

    /**
     * @param petEntity the pet that will acquire targets
     * @param range     radius (in blocks) of the "near owner" search box
     */
    public PetAggressiveTargetGoal(Pet pet, Mob mob, float range) {
        this.pet = pet;
        this.mob = mob;
        this.range = range;
    }

    @Override
    public boolean shouldActivate() {
        Behavior behaviorSkill = pet.getSkills().get(Behavior.class);
        if (!behaviorSkill.isActive() || behaviorSkill.getBehavior() != BehaviorMode.Aggressive) {
            return false;
        }
        if (pet.getDamage() <= 0 && pet.getRangedDamage() <= 0) {
            return false;
        }
        if (!pet.canMove()) {
            return false;
        }
        if (pet.hasTarget()) {
            return false;
        }

        Player owner = pet.getOwner().getPlayer();
        if (owner == null) {
            return false;
        }
        Location petLoc = mob.getLocation();
        // Skip the scan if the pet isn't currently owned by this region thread. Folia can run
        // goal activation checks during cross-region transitions or via `inactiveTick` on
        // regions that don't own the entity; getNearbyEntities would then read world data we
        // don't own.
        if (!Bukkit.isOwnedByCurrentRegion(mob)) {
            return false;
        }
        // Search around the pet. Searching around the owner would touch the owner's region from
        // the pet's thread, which Folia rejects. The existing distance-to-pet filter below
        // already narrowed results to a small radius of the pet, so this is behaviourally equivalent.

        for (Entity entity : mob.getWorld().getNearbyEntities(petLoc, range, range, range)) {
            if (!(entity instanceof LivingEntity living) || entity.equals(mob)) {
                continue;
            }
            if (entity instanceof ArmorStand || living.isDead()) {
                continue;
            }
            if (petLoc.distanceSquared(living.getLocation()) > 91) {
                continue;
            }
            if (entity instanceof Player targetPlayer) {
                if (pet.getOwner().equals(targetPlayer)) {
                    continue;
                }
                if (!MyPetApi.getHookHelper().canHurt(owner, targetPlayer, true)) {
                    continue;
                }
            } else if (PetEntityMarker.isMarked(entity)) {
                Pet otherPet = MyPetApi.getPetManager().getPetFromEntity(entity);
                if (otherPet != null && otherPet.getOwner() != null
                        && !MyPetApi.getHookHelper().canHurt(owner, otherPet.getOwner().getPlayer(), true)) {
                    continue;
                }
            } else if (entity instanceof Tameable tameable && tameable.isTamed() && tameable.getOwner() != null) {
                Player tameableOwner = (Player) tameable.getOwner();
                if (pet.getOwner().equals(tameableOwner)) {
                    continue;
                }
                if (!MyPetApi.getHookHelper().canHurt(owner, tameableOwner, true)) {
                    continue;
                }
            }
            // Final unconditional canHurt check — matches PetControlTargetGoal.
            // The if/else chain above handled *ownership* filtering (player vs
            // pet vs tamed); this check enforces region-plugin protection
            // (WorldGuard, GriefPrevention, etc.) on the entity itself,
            // including untamed Tameables (wild wolves, wild cats) that would
            // otherwise slip past the chain.
            if (!MyPetApi.getHookHelper().canHurt(owner, living)) {
                continue;
            }
            this.target = living;
            return true;
        }
        return false;
    }

    @Override
    public boolean shouldStayActive() {
        if (!Bukkit.isOwnedByCurrentRegion(mob)) {
            return false;
        }
        if (!pet.canMove()) {
            return false;
        }
        if (!pet.hasTarget()) {
            return false;
        }
        LivingEntity currentTarget = pet.getPetTarget();
        if (currentTarget == null || currentTarget.isDead()) {
            return false;
        }
        Behavior behaviorSkill = pet.getSkills().get(Behavior.class);
        if (behaviorSkill.getBehavior() != BehaviorMode.Aggressive) {
            return false;
        }
        if (pet.getDamage() <= 0 && pet.getRangedDamage() <= 0) {
            return false;
        }
        if (!currentTarget.getWorld().equals(mob.getWorld())) {
            return false;
        }
        if (mob.getLocation().distanceSquared(currentTarget.getLocation()) > 400) {
            return false;
        }
        Player owner = pet.getOwner().getPlayer();
        return owner != null && mob.getLocation().distanceSquared(owner.getLocation()) <= 600;
    }

    @Override
    public void start() {
        pet.setTarget(this.target, TargetPriority.Aggressive);
    }

    @Override
    public void stop() {
        pet.forgetTarget();
        target = null;
    }

    @Override
    public @NotNull GoalKey<Mob> getKey() {
        return PetGoalKey.AGGRESSIVE_TARGET;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.TARGET);
    }
}
