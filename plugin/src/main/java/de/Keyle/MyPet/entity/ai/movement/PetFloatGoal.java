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
import de.Keyle.MyPet.api.entity.MyPet;
import org.bukkit.entity.Mob;
import de.Keyle.MyPet.entity.ai.PetGoalKey;
import org.bukkit.Bukkit;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

/**
 * Paper {@link Goal} that keeps a pet at the surface when it enters water
 * (or lava, for pets that can float on lava) and nudges it back toward its
 * owner when stuck in lava. This replaces vanilla's {@code FloatGoal} for
 * MyPet entities.
 *
 * <p>Activation:
 * <ul>
 *   <li>A non-lava-floating pet only activates in water.</li>
 *   <li>A lava-floating pet (see {@code EntityMyPet.floatsInLava()})
 *       activates in either water or lava.</li>
 * </ul>
 *
 * <p>While active, each tick applies a small upward velocity so the pet
 * rises to the surface. If the pet has been in lava for {@code ~0.5s} of
 * continuous ticks, the goal also issues a navigation request to the owner
 * so a pet that can't naturally escape the lava column gets pulled out.
 *
 * <p>The goal declares no {@link GoalType GoalTypes}, meaning Paper's
 * selector won't treat it as mutually exclusive with any other goal — it
 * always runs concurrently with movement and target goals, mirroring
 * vanilla's always-on floating behaviour.
 *
 * <p>This goal also sets {@code canFloat(true)} on the pathfinder at
 * construction time so A* path candidates over water are not rejected.
 */
public class PetFloatGoal implements Goal<Mob> {

    private final MyPet pet;
    private final Mob mob;
    private int lavaCounter = 10;
    private boolean inLava = false;

    /**
     * @param petEntity the pet that should float on water (and possibly lava)
     */
    public PetFloatGoal(MyPet pet, Mob mob) {
        this.pet = pet;
        this.mob = mob;
        mob.getPathfinder().setCanFloat(true);
    }

    @Override
    public boolean shouldActivate() {
        if (!Bukkit.isOwnedByCurrentRegion(mob)) {
            return false;
        }
        if (pet.getPetType().floatsInLava()) {
            return mob.isInWater() || mob.isInLava();
        }
        return mob.isInWater();
    }

    @Override
    public void stop() {
        inLava = false;
    }

    @Override
    public void tick() {
        if (!Bukkit.isOwnedByCurrentRegion(mob)) {
            return;
        }
        if (pet.getPetType().specialFloat()) {
            return;
        }
        // During pet removal (unlink / death / release) the owner reference
        // may be cleared before the entity is despawned, so both getOwner()
        // and its .getPlayer() result can be null. Bail out before touching
        // either.
        if (pet.getOwner() == null) {
            return;
        }

        Vector velocity = mob.getVelocity();
        mob.setVelocity(velocity.add(new Vector(0, 0.05D, 0)));

        if (inLava && lavaCounter-- <= 0) {
            Player owner = pet.getOwner().getPlayer();
            if (owner != null && pet.getPetNavigation().navigateTo(owner)) {
                lavaCounter = 10;
            }
        }
        if (!inLava && mob.isInLava()) {
            // Approximate isEyeInFluid(LAVA) — if in lava at all, treat as eye-level
            inLava = true;
        }
    }

    @Override
    public @NotNull GoalKey<Mob> getKey() {
        return PetGoalKey.FLOAT;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.noneOf(GoalType.class);
    }
}
