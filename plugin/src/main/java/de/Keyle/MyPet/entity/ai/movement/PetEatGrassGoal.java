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

import de.Keyle.MyPet.api.config.PetConfigKeys;
import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import de.Keyle.MyPet.api.entity.Pet;
import org.bukkit.*;
import org.bukkit.entity.Mob;
import de.Keyle.MyPet.entity.types.PetSheep;
import de.Keyle.MyPet.entity.ai.PetGoalKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Sheep;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Paper {@link Goal} that makes a sheared sheep pet stop, eat grass, and
 * regrow its wool — the Paper-goal equivalent of vanilla's
 * {@code EatBlockGoal}.
 *
 * <p>Activates only when <em>all</em> of the following are true:
 * <ul>
 *   <li>{@code PetConfigKeys.Sheep.CAN_REGROW_WOOL}
 *       is enabled in config.yml.</li>
 *   <li>The underlying {@link PetSheep} is currently sheared.</li>
 *   <li>A random 1-in-1000 roll succeeds (matches vanilla frequency).</li>
 *   <li>The pet has no active target.</li>
 *   <li>The block at the pet's feet is {@link Material#SHORT_GRASS} or the
 *       block below is {@link Material#GRASS_BLOCK}.</li>
 * </ul>
 *
 * <p>While active the goal halts navigation, broadcasts entity event byte
 * {@code 10} to play the sheep head-bob eat animation on clients, and after
 * a 30-tick wind-up consumes the grass (subject to the {@link GameRule#MOB_GRIEFING}
 * game rule) and resets the sheared flag so the pet re-wears its wool.
 *
 * <p>Declares {@link GoalType#MOVE} and {@link GoalType#LOOK} because during
 * the animation the pet must neither wander nor glance around.
 */
public class PetEatGrassGoal implements Goal<Mob> {

    private final Pet pet;
    private final Mob mob;
    private int eatTicks = 0;

    /**
     * @param petEntity the sheep pet that should eat grass when sheared
     */
    public PetEatGrassGoal(Pet pet, Mob mob) {
        this.pet = pet;
        this.mob = mob;
    }

    @Override
    public boolean shouldActivate() {
        if (!Bukkit.isOwnedByCurrentRegion(mob)) {
            return false;
        }
        if (!PetConfigKeys.Sheep.CAN_REGROW_WOOL.get()) {
            return false;
        }
        if (!(pet instanceof PetSheep) || !(mob instanceof Sheep sheep) || !sheep.isSheared()) {
            return false;
        }
        if (ThreadLocalRandom.current().nextInt(1000) != 0) {
            return false;
        }
        if (pet.hasTarget() && !pet.getPetTarget().isDead()) {
            return false;
        }
        Location loc = mob.getLocation();
        Block blockAt = loc.getBlock();
        Block blockBelow = blockAt.getRelative(BlockFace.DOWN);
        return blockAt.getType() == Material.SHORT_GRASS || blockBelow.getType() == Material.GRASS_BLOCK;
    }

    @Override
    public boolean shouldStayActive() {
        return this.eatTicks > 0;
    }

    @Override
    public void start() {
        if (!Bukkit.isOwnedByCurrentRegion(mob)) {
            return;
        }
        this.eatTicks = 30;
        pet.getPetNavigation().stop();
        Location loc = mob.getLocation();
        World world = loc.getWorld();
        if (world != null) {
            world.spawnParticle(
                    Particle.BLOCK,
                    loc.clone().add(0, 0.2, 0),
                    12,
                    0.3, 0.1, 0.3,
                    0.05,
                    Material.GRASS_BLOCK.createBlockData());
            world.playSound(loc, Sound.ENTITY_SHEEP_AMBIENT, 0.6f, 1.2f);
        }
    }

    @Override
    public void stop() {
        this.eatTicks = 0;
    }

    @Override
    public void tick() {
        if (!Bukkit.isOwnedByCurrentRegion(mob)) {
            return;
        }
        if (--this.eatTicks == 0) {
            Location loc = mob.getLocation();
            World world = loc.getWorld();
            Block blockAt = loc.getBlock();
            Block blockBelow = blockAt.getRelative(BlockFace.DOWN);

            Boolean mobGriefing = world.getGameRuleValue(GameRule.MOB_GRIEFING);
            if (mobGriefing != null && !mobGriefing) {
                return;
            }

            if (!(mob instanceof Sheep sheep)) return;
            if (blockAt.getType() == Material.SHORT_GRASS) {
                blockAt.setType(Material.AIR);
                sheep.setSheared(false);
            } else if (blockBelow.getType() == Material.GRASS_BLOCK) {
                blockBelow.setType(Material.DIRT);
                sheep.setSheared(false);
            }
        }
    }

    @Override
    public @NotNull GoalKey<Mob> getKey() {
        return PetGoalKey.EAT_GRASS;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE, GoalType.LOOK);
    }
}
